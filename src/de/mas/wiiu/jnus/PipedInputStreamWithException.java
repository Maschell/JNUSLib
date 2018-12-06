/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
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
