package org.jberet.repository;
  
  import java.security.AccessController;
  import java.security.PrivilegedAction;

  import org.wildfly.security.manager.WildFlySecurityManager;

  class SecurityActions {

      static ClassLoader getContextClassLoader() {
          if (WildFlySecurityManager.isChecking()) {
              return AccessController.doPrivileged(
                      (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
          }
          return Thread.currentThread().getContextClassLoader();
      }

      static ClassLoader getClassLoader(final Class<?> clazz) {
          if (WildFlySecurityManager.isChecking()) {
              return AccessController.doPrivileged(
                      (PrivilegedAction<ClassLoader>) clazz::getClassLoader);
          }
          return clazz.getClassLoader();
      }
  }