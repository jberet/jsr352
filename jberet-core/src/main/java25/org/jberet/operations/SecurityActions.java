package org.jberet.operations;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.spi.BatchEnvironment;
import org.wildfly.security.manager.WildFlySecurityManager;

class SecurityActions {
    static BatchEnvironment loadBatchEnvironment() {
        final PrivilegedAction<BatchEnvironment> action = () -> {
            final ServiceLoader<BatchEnvironment> serviceLoader = ServiceLoader.load(BatchEnvironment.class);
            if (serviceLoader.iterator().hasNext()) {
                return serviceLoader.iterator().next();
            }
            return null;
        };
        return WildFlySecurityManager.isChecking()
                ? AccessController.doPrivileged(action) : action.run();
    }
}