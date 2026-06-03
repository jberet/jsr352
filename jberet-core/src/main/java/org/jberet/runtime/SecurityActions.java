package org.jberet.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobFactory;
import org.jberet.spi.SerializableDataProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

class SecurityActions {

    static Job cloneJob(final Job job) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Job>) () -> JobFactory.cloneJob(job));
        } else {
            return JobFactory.cloneJob(job);
        }
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
        return WildFlySecurityManager.isChecking()
                ? AccessController.doPrivileged(action) : action.run();
    }
}
