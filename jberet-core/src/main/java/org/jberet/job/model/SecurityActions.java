package org.jberet.job.model;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.wildfly.security.manager.WildFlySecurityManager;

class SecurityActions {
    public static List<JobElement> cloneJobElements(List<JobElement> jobElements) {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(
                (PrivilegedAction<List<JobElement>>) () -> JobFactory.cloneJobElements(jobElements));
        }
        else {
            return JobFactory.cloneJobElements(jobElements);
        }
    }
}
