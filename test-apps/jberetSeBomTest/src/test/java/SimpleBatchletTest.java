import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

import static jakarta.batch.runtime.BatchStatus.COMPLETED;

public class SimpleBatchletTest {
    private static final int MAX_TRIES = 40;
    private static final int THREAD_SLEEP = 1000;

    @Test
    public void givenBatchLetStarted_whenStopped_thenBatchStopped() throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start("simpleBatchlet", new Properties());
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        jobExecution = keepTestAlive(jobExecution);

        Assert.assertEquals(jobExecution.getBatchStatus(), BatchStatus.COMPLETED);
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