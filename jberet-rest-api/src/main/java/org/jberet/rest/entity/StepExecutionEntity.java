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

package org.jberet.rest.entity;

import java.io.Serializable;
import javax.batch.runtime.StepExecution;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StepExecutionEntity extends AbstractExecutionEntity implements StepExecution, Serializable {
    private static final long serialVersionUID = -8528930845788535109L;

    private long stepExecutionId;

    private String stepName;

    private Serializable persistentUserData;

    private MetricEntity[] metrics;

    public StepExecutionEntity() {
    }

    public StepExecutionEntity(final StepExecution stepExe) {
        super(stepExe.getStartTime(), stepExe.getEndTime(), stepExe.getBatchStatus(), stepExe.getExitStatus());
        this.stepExecutionId = stepExe.getStepExecutionId();
        this.stepName = stepExe.getStepName();
        this.persistentUserData = stepExe.getPersistentUserData();
        this.metrics = MetricEntity.copyOf(stepExe.getMetrics());
    }

    public long getStepExecutionId() {
        return stepExecutionId;
    }

    public void setStepExecutionId(final long stepExecutionId) {
        this.stepExecutionId = stepExecutionId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }

    public Serializable getPersistentUserData() {
        return persistentUserData;
    }

    public void setPersistentUserData(final Serializable persistentUserData) {
        this.persistentUserData = persistentUserData;
    }

    public MetricEntity[] getMetrics() {
        return metrics;
    }

    public void setMetrics(final MetricEntity[] metrics) {
        this.metrics = metrics;
    }
}
