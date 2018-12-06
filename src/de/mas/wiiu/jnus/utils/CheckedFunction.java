package de.mas.wiiu.jnus.utils;

import java.io.IOException;

@FunctionalInterface
public interface CheckedFunction<T> {
    void apply(T t) throws IOException;
}