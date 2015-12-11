/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;

public class Main {
    public static void main(final String[] args) throws BatchRuntimeException {
        if (args.length == 0) {
            usage(args);
            return;
        }
        final String jobXml = args[0];
        if (jobXml == null || jobXml.isEmpty()) {
            usage(args);
            return;
        }

        final java.util.Properties jobParameters = new java.util.Properties();
        for (int i = 1; i < args.length; i++) {
            final int equalSignPos = args[i].indexOf('=');
            if (equalSignPos <= 0) {
                usage(args);
                return;
            }
            final String key = args[i].substring(0, equalSignPos).trim();
            final String val = args[i].substring(equalSignPos + 1).trim();
            jobParameters.setProperty(key, val);
        }

        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final long jobExecutionId;
        try {
            jobExecutionId = jobOperator.start(jobXml, jobParameters);
            final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
            jobExecution.awaitTermination(0, TimeUnit.SECONDS);  //no timeout

            if (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
                throw new BatchRuntimeException(String.format("The job did not complete: %s%n", jobXml));
            }
        } catch (InterruptedException e) {
            throw new BatchRuntimeException(e);
        }
    }

    private static void usage(final String[] args) {
        System.err.printf("Usage: java -classpath ... -Dkey1=val1 ... org.jberet.se.Main jobXML%nThe following application args are invalid:%n%s", Arrays.asList(args));
    }
}
