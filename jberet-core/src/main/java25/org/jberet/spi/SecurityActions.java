package org.jberet.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.wildfly.security.manager.WildFlySecurityManager;

public class SecurityActions {
          static ClassLoader getContextClassLoader() {
          if (WildFlySecurityManager.isChecking()) {
              return AccessController.doPrivileged(
                      (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
          }
          return Thread.currentThread().getContextClassLoader();
      }

}
