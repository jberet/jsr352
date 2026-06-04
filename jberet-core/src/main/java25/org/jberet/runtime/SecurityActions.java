package org.jberet.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobFactory;
import org.jberet.spi.SerializableDataProvider;

class SecurityActions {

    static Job cloneJob(final Job job) {
        return JobFactory.cloneJob(job);
    }

    static SerializableDataProvider loadSerializableDataProvider() {
        final PrivilegedAction<SerializableDataProvider> action = () -> {
            final ServiceLoader<SerializableDataProvider> serviceLoader =
                    ServiceLoader.load(SerializableDataProvider.class);
            if (serviceLoader.iterator().hasNext()) {
                return serviceLoader.iterator().next();
            }
            return new SerializableDataProvider.DefaultSerializableDataProvider();
        };
        return action.run();
    }
}
