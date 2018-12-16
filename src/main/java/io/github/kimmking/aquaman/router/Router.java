package io.github.kimmking.aquaman.router;

public interface Router<T,P> {

    P route(T t);

}
