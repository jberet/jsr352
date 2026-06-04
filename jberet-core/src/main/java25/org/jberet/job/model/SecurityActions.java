package org.jberet.job.model;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

class SecurityActions {
    public static List<JobElement> cloneJobElements(List<JobElement> jobElements) {
        return JobFactory.cloneJobElements(jobElements);
    }
}
