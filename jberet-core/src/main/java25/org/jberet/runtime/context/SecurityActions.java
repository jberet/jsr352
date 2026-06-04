package org.jberet.runtime.context;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jberet.job.model.JobFactory;
import org.jberet.job.model.Step;

class SecurityActions {

    static Step cloneStep(final Step step) {
        return JobFactory.cloneStep(step);
    }
}
