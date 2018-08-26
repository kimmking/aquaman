package io.github.kimmking.cloud.gateway.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class GatewayApplication extends AbstractVerticle {
    public static void main(String[] args) {
//        Vertx.vertx().createHttpServer().requestHandler(req -> req.response().
//                end("Hello World!")).listen(8888);

        int port = 8000;

        System.out.println("Vert.X starting...");
        Vertx.vertx(new VertxOptions().setWorkerPoolSize(40)).createHttpServer().
                requestHandler(new ProxyHandler("http://localhost:8088/")).listen(port);
        System.out.println("Vert.X started at port:" + port);
    }
}