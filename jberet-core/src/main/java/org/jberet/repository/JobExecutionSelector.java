/*
 * Copyright (c) 2015-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.repository;

import java.util.Collection;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

/**
 * A selector that determines if a job execution matches this selector's
 * requirement.
 *
 * @since 1.1.0
 */
public interface JobExecutionSelector {
    /**
     * Determines if a job execution matches this selector's requirement.
     *
     * @param jobExecution the job execution to check for match
     * @param allJobExecutionIds all job execution ids
     * @return true if {@code jobExecution} matches this selector's requirement; false otherwise
     */
    boolean select(JobExecution jobExecution, Collection<Long> allJobExecutionIds);

    /**
     * Gets the job context for this selector.
     *
     * @return the job context
     */
    JobContext getJobContext();

    /**
     * Sets the job context for this selector.
     *
     * @param jobContext the job context for this selector
     */
    void setJobContext(JobContext jobContext);

    /**
     * Gets the step context.

     * @return the step context
     */
    StepContext getStepContext();

    /**
     * Sets the step context for this selector.
     *
     * @param stepContext the step context
     */
    void setStepContext(StepContext stepContext);
}
