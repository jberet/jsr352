package org.jberet.repository;
  
  import java.security.AccessController;
  import java.security.PrivilegedAction;

  import org.wildfly.security.manager.WildFlySecurityManager;

  class SecurityActions {

      static ClassLoader getContextClassLoader() {
          return Thread.currentThread().getContextClassLoader();
      }

      static ClassLoader getClassLoader(final Class<?> clazz) {
          return clazz.getClassLoader();
      }
  }