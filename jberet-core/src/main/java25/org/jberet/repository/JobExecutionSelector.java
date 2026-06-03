/*
 * Copyright (c) 2015-2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.util.Collection;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;

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
