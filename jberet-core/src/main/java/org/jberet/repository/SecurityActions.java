package org.jberet.repository;
  
import java.security.AccessController;
import java.security.PrivilegedAction;

class SecurityActions {

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        }
        return Thread.currentThread().getContextClassLoader();
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) clazz::getClassLoader);
        }
        return clazz.getClassLoader();
    }
}