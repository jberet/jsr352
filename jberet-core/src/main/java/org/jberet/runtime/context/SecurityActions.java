package org.jberet.runtime.context;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jberet.job.model.JobFactory;
import org.jberet.job.model.Step;
import org.wildfly.security.manager.WildFlySecurityManager;

class SecurityActions {

    static Step cloneStep(final Step step) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Step>) () -> JobFactory.cloneStep(step));
        } else {
            return JobFactory.cloneStep(step);
        }
    }
}
