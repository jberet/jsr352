/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.testapps.common;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

import org.jberet.runtime.JobExecutionImpl;

abstract public class AbstractIT {
    protected void startJob(String jobXmlName) throws Exception {
        Properties props = new Properties();
        props.setProperty("job-param", "job-param");

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long jobExecutionId = jobOperator.start(jobXmlName, props);
        JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);

        long timeout = Long.getLong(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_KEY, JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT);
        jobExecution.awaitTerminatioin(timeout, TimeUnit.SECONDS);
    }
}
