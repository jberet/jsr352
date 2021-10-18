package com.bom.test;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Named;

@Named
public class SimpleBatchlet extends AbstractBatchlet {
    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }
}