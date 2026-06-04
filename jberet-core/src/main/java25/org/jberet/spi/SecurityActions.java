package org.jberet.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class SecurityActions {
    static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}


