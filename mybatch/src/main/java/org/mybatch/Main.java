/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.mybatch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.mybatch.runtime.JobExecutionImpl;
import org.mybatch.util.ConcurrencyService;

import static org.mybatch.util.BatchLogger.LOGGER;

public class Main {
    public static void main(String[] args) throws BatchRuntimeException {
        if (args.length == 0) {
            usage(args);
        }
        String jobXml = args[0];
        if (jobXml == null || jobXml.isEmpty()) {
            usage(args);
        }

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long jobExecutionId;
        long timeout = Long.getLong(JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_KEY, JobExecutionImpl.JOB_EXECUTION_TIMEOUT_SECONDS_DEFAULT);
        try {
            jobExecutionId = jobOperator.start(jobXml, null);
            JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
            jobExecution.awaitTerminatioin(timeout, TimeUnit.SECONDS);

            if (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
                throw new BatchRuntimeException(String.format("The job did not complete: %s%n", jobXml));
            }
        } catch (InterruptedException e) {
            //ignore
        } finally {
            try {
                ConcurrencyService.shutdown();
                ConcurrencyService.getExecutorService().awaitTermination(timeout, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    private static void usage(String[] args) {
        LOGGER.mainUsage(Arrays.asList(args));
    }
}
