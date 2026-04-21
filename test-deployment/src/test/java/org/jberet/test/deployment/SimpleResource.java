/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jberet.test.deployment;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.Properties;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;

@Path("/simple")
public class SimpleResource {

    private static final int MAX_TRIES = 40;
    private static final int THREAD_SLEEP = 1000;

    @GET
    public String get() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("simple", new Properties());
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        try {
            keepTestAlive(jobExecution);
            if (jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
                return "OK";
            } else {
                return "BAD";
            }

        } catch (Exception e) {
            return "ERROR";
        }
    }

    private JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
        int maxTries = 0;
        while (!jobExecution.getBatchStatus().equals(COMPLETED)) {
            if (maxTries < MAX_TRIES) {
                maxTries++;
                Thread.sleep(THREAD_SLEEP);
                jobExecution = BatchRuntime.getJobOperator().getJobExecution(jobExecution.getExecutionId());
            } else {
                break;
            }
        }
        Thread.sleep(THREAD_SLEEP);
        return jobExecution;
    }
}