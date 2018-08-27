package io.github.kimmking.cloud.gateway.router;

public interface Router<T,P> {

    P route(T t);

}
