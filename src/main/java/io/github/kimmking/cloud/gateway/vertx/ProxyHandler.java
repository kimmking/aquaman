package io.github.kimmking.cloud.gateway.vertx;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;

import java.io.InputStream;
import java.util.concurrent.*;

public class ProxyHandler implements Handler<HttpServerRequest> {

    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private CloseableHttpAsyncClient httpclient;
    private ExecutorService proxyService;
    private String backendUrl;

    public ProxyHandler(String backendUrl){
        this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();//.DiscardPolicy();
        proxyService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"), handler);

        IOReactorConfig ioConfig = IOReactorConfig.custom()
                .setConnectTimeout(1000)
                .setSoTimeout(1000)
                .setIoThreadCount(cores)
                .setRcvBufSize(4096)
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
                .setKeepAliveStrategy((response,context) -> 60000)
                .build();
        httpclient.start();
    }


    @Override
    public void handle(final HttpServerRequest request) {
        final String url = this.backendUrl + request.path();
        proxyService.submit(()->fetchGet(request,url));
    }

    private void fetchGet(final HttpServerRequest inbound,final String url) {
        final HttpGet httpGet = new HttpGet(url);
        //httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);
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

        for (Header e : response.getAllHeaders()) {
            outbound.putHeader(e.getName(),e.getValue());
        }

//        byte[] body = EntityUtils.toByteArray(response.getEntity());
//        outbound.end(Buffer.buffer(body));

        HttpEntity entity = response.getEntity();

        Args.notNull(entity, "Entity");
        final InputStream instream = entity.getContent();
        if (instream == null) {
            outbound.end();
        }
        try {
            Args.check(entity.getContentLength() <= Integer.MAX_VALUE,
                    "HTTP entity too large to be buffered in memory");
            int i = (int)entity.getContentLength();
            if (i < 0) {
                i = 4096;
            }
            final Buffer buffer = Buffer.buffer(i);
            final byte[] tmp = new byte[4096];
            int l;
            while((l = instream.read(tmp)) != -1) {
                buffer.appendBytes(tmp, 0, l);
            }
            outbound.write(buffer);
        } finally {
            instream.close();
            outbound.end();
        }

    }
}
