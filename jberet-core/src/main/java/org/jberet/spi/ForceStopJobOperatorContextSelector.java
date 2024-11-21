package org.jberet.spi;

import org.jberet.operations.ForceStopJobOperatorImpl;

public class ForceStopJobOperatorContextSelector implements JobOperatorContextSelector {
    private final JobOperatorContext jobOperatorContext;

    /**
     * Creates a new default context selector
     */
    public ForceStopJobOperatorContextSelector() {
        jobOperatorContext = JobOperatorContext.create(new ForceStopJobOperatorImpl());
    }

    @Override
    public JobOperatorContext getJobOperatorContext() {
        return jobOperatorContext;
    }
}
