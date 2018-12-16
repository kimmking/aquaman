package io.github.kimmking.aquaman.filter;

public interface Filter<R,T> {

    FilterType getFilterType();

    T filter(R r);

}
