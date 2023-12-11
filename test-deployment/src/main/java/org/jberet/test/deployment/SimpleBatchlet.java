package org.jberet.test.deployment;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;


@Named("simpleBatchlet")
public class SimpleBatchlet extends AbstractBatchlet {
    @Inject
    JobContext jobContext;

    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }
}