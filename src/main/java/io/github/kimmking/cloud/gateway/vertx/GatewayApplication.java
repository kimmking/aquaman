package io.github.kimmking.cloud.gateway.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class GatewayApplication extends AbstractVerticle {
    public static void main(String[] args) {
//        Vertx.vertx().createHttpServer().requestHandler(req -> req.response().
//                end("Hello World!")).listen(8888);
        Vertx.vertx().createHttpServer().requestHandler(new ProxyHandler("http://localhost:8088/")).listen(8000);
    }
}