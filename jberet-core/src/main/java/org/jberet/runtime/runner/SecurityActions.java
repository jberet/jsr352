package org.jberet.runtime.runner;

import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.spi.PartitionHandlerFactory;

class SecurityActions {

    static PartitionHandlerFactory loadPartitionHandlerFactory() {
        return LOADER_ACTION.run();
    }

    private static final PrivilegedAction<PartitionHandlerFactory> LOADER_ACTION = () -> {
        final ServiceLoader<PartitionHandlerFactory> serviceLoader = ServiceLoader.load(PartitionHandlerFactory.class);
        if (serviceLoader.iterator().hasNext()) {
            return serviceLoader.iterator().next();
        }
        return null;
    };
}
