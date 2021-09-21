package com.bom.test;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class SimpleBatchlet extends AbstractBatchlet {
    @Override
    public String process() throws Exception {
        return BatchStatus.COMPLETED.toString();
    }
}
