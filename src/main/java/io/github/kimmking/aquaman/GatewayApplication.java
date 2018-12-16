package io.github.kimmking.aquaman;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;


public class GatewayApplication extends AbstractVerticle {

    public final static String GATEWAY_NAME = "AquamanGateway";
    public final static String GATEWAY_VERSION = "1.0.0";

    public static void main(String[] args) {

        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory");


        String proxyServer = System.getProperty("proxyServer","http://localhost:8088");
        String proxyPort = System.getProperty("proxyPort","8000");

        int port = Integer.parseInt(proxyPort);

        System.out.println(GATEWAY_NAME + " " + GATEWAY_VERSION +" starting...");
        Vertx.vertx().createHttpServer().
                requestHandler(new ProxyHandler(proxyServer)).listen(port);
        System.out.println(GATEWAY_NAME + " " + GATEWAY_VERSION +" started at port:" + port + " for server:" + proxyServer);

    }
}