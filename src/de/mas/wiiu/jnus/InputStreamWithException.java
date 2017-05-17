package de.mas.wiiu.jnus;

import java.io.Closeable;

public interface InputStreamWithException extends Closeable {
    public void checkForException() throws Exception;
}
