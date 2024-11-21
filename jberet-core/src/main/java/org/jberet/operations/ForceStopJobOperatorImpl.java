package org.jberet.operations;

import jakarta.batch.runtime.BatchStatus;
import org.jberet.runtime.JobExecutionImpl;

public class ForceStopJobOperatorImpl extends DefaultJobOperatorImpl {
    public void forceStop(final long executionId) {
        final JobExecutionImpl jobExecution = getJobExecutionImpl(executionId);
        jobExecution.setBatchStatus(BatchStatus.STOPPED);
        getJobRepository().updateJobExecution(jobExecution, false, false);
    }
}
