package org.jberet.se.test;

import jakarta.batch.runtime.BatchStatus;
import org.jberet.operations.ForceStopJobOperatorImpl;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.spi.ForceStopJobOperatorContextSelector;
import org.jberet.spi.JobOperatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class ForceStopBatchletTest {
    private static final String jobName = "org.jberet.se.test.sleepBatchlet";

    @Test
    public void sleepForceStop() throws Exception {

        JobOperatorContext.setJobOperatorContextSelector(new ForceStopJobOperatorContextSelector());
        ForceStopJobOperatorImpl operator = (ForceStopJobOperatorImpl) JobOperatorContext.getJobOperatorContext().getJobOperator();

        final int sleepMinutes = 6;
        final Properties params = new Properties();
        params.setProperty("sleep.minutes", String.valueOf(sleepMinutes));
        final long jobExecutionId = operator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) operator.getJobExecution(jobExecutionId);

        operator.forceStop(jobExecutionId);
        // don't await.
//        jobExecution.awaitTermination(1, TimeUnit.MINUTES);

        System.out.printf("jobExecution id=%s, batchStatus=%s, exitStatus=%s, jobParameters=%s, restartPosition=%s, " +
                        "createTime=%s, startTime=%s, lastUpdateTime=%s, endTime=%s%n",
                jobExecutionId, jobExecution.getBatchStatus(), jobExecution.getExitStatus(), jobExecution.getJobParameters(),
                jobExecution.getRestartPosition(), jobExecution.getCreateTime(), jobExecution.getStartTime(),
                jobExecution.getLastUpdatedTime(), jobExecution.getLastUpdatedTime(), jobExecution.getEndTime());
        Assertions.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());
        Assertions.assertEquals(BatchStatus.STOPPED.name(), jobExecution.getExitStatus());
    }
}
