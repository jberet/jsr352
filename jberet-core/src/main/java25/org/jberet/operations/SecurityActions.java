package org.jberet.operations;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.spi.BatchEnvironment;

class SecurityActions {
    static BatchEnvironment loadBatchEnvironment() {
        final PrivilegedAction<BatchEnvironment> action = () -> {
            final ServiceLoader<BatchEnvironment> serviceLoader = ServiceLoader.load(BatchEnvironment.class);
            if (serviceLoader.iterator().hasNext()) {
                return serviceLoader.iterator().next();
            }
            return null;
        };
        return action.run();
    }
}