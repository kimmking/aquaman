package io.github.kimmking.cloud.gateway.vertx;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.*;

public class ProxyHandler implements Handler<HttpServerRequest> {

    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private static final int HTTP_OK = 200;

    public static final long MAX_CLIENT_LIVE_TIME_MS = 1000 * 60 * 5;
    private static final long MAX_LAST_FETCH_INTERVAL_MS = 1000 * 15;
    private static final long FETCH_INTERVAL_SECOND = 6;
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private CloseableHttpAsyncClient httpclient;
    private ExecutorService fetchService;
    private ExecutorService fetchWorker;
    private String backendUrl  = "";

    public ProxyHandler(String backendUrl){
        this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        long keepAliveTime = 0;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();
        fetchService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxy-fetchService"), handler);
        fetchWorker = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxy-fetchWorker"), handler);
        IOReactorConfig ioConfig = IOReactorConfig.custom()
                .setConnectTimeout(3000)
                .setSoTimeout(3000)
                .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2)
                .build();

        httpclient = HttpAsyncClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy() {
                    @Override
                    protected boolean isRedirectable(final String method) {
                        return false;
                    }
                }).setMaxConnTotal(4000)
                .setMaxConnPerRoute(1000)
                .setDefaultIOReactorConfig(ioConfig)
                .build();
        httpclient.start();
    }

    /**
     * @param event the event to handle
     */
    @Override
    public void handle(HttpServerRequest request) {
        String url = this.backendUrl + request.path();
        fetchGet(request,url);
    }

    private void fetchGet(final HttpServerRequest inbound,final String url) {
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                try {
                    handleResponse(inbound,response);
                } catch (Exception e) {
                    logger.error("fetch " + url +":", e);
                } finally {
                }
            }

            @Override
            public void failed(final Exception ex) {
                httpGet.abort();
                logger.error("failed:", ex);
            }

            @Override
            public void cancelled() {
                httpGet.abort();
            }
        });
    }

    private void handleResponse(final HttpServerRequest inbound,final HttpResponse response) throws Exception {
        HttpServerResponse outbound = inbound.response();
        outbound.setStatusCode(response.getStatusLine().getStatusCode());
        Arrays.asList(response.getAllHeaders()).forEach( e ->
                outbound.putHeader(e.getName(),e.getValue())
        );

        String body = EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
        if (StringUtils.isEmpty(body)) {
            outbound.end("NIL");
        }
        outbound.end(body);
    }
}
