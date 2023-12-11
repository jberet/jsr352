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