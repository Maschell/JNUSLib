package de.mas.wiiu.jnus;

import java.io.IOException;
import java.io.PipedInputStream;

import de.mas.wiiu.jnus.utils.Utils;

public class PipedInputStreamWithException extends PipedInputStream implements InputStreamWithException {
    private Exception e = null;
    private boolean exceptionSet = false;
    private boolean closed = false;
    private Object lock = new Object();

    @Override
    public void close() throws IOException {
        super.close();
        synchronized (lock) {
            closed = true;
        }
    }

    public void throwException(Exception e) {
        synchronized (lock) {
            exceptionSet = true;
            this.e = e;
        }
    }

    public boolean isClosed() {
        boolean isClosed = false;
        synchronized (lock) {
            isClosed = closed;
        }
        return isClosed;
    }

    @Override
    public void checkForException() throws Exception {
        if (isClosed()) {
            boolean waiting = true;
            int tries = 0;
            while (waiting) {
                synchronized (lock) {
                    waiting = !exceptionSet;
                }
                if (waiting) {
                    Utils.sleep(10);
                }
                if (tries > 100) {
                    // TODO: warning?
                    break;
                }
            }
        }
        synchronized (lock) {
            if (e != null) {
                Exception tmp = e;
                e = null;
                exceptionSet = true;
                throw tmp;
            }
        }
    }
}
