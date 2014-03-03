/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.repository;

import java.io.Serializable;

/**
 * Used as a key to uniquely identify a job with both the name of the job and the name of the application that
 * containing the job definition.
 */
public final class ApplicationAndJobName implements Serializable {

    private static final long serialVersionUID = -2560203183829105420L;
    public final String appName;
    public final String jobName;

    public ApplicationAndJobName(final String appName, final String jobName) {
        this.appName = appName;
        this.jobName = jobName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ApplicationAndJobName that = (ApplicationAndJobName) o;

        if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
        if (jobName != null ? !jobName.equals(that.jobName) : that.jobName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appName != null ? appName.hashCode() : 0;
        result = 31 * result + (jobName != null ? jobName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApplicationAndJobName{");
        sb.append("appName='").append(appName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
