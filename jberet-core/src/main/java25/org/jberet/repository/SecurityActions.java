package org.jberet.repository;
  
import java.security.AccessController;
import java.security.PrivilegedAction;

class SecurityActions {

    static ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        return clazz.getClassLoader();
    }
}