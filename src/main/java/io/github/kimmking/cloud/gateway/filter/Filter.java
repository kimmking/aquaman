package io.github.kimmking.cloud.gateway.filter;

public interface Filter<R,T> {

    FilterType getFilterType();

    T filter(R r);

}
