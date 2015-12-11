/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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

public interface JobExecutionSelector {
    boolean select(JobExecution jobExecution, Collection<Long> allJobExecutionIds);

    JobContext getJobContext();

    void setJobContext(JobContext jobContext);

    StepContext getStepContext();

    void setStepContext(StepContext stepContext);
}
