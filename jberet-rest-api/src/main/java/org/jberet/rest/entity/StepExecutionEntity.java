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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents a step execution, which includes fields such as step execution id,
 * step name, and those fields inherited from {@link AbstractExecutionEntity}
 * (start time, end time, batch status, and exit status).
 *
 * @see AbstractExecutionEntity
 *
 * @since 1.3.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(propOrder =
//        {"stepExecutionId", "stepName", "batchStatus", "exitStatus", "startTime", "endTime", "metrics"})
public class StepExecutionEntity extends AbstractExecutionEntity implements StepExecution, Serializable {
    private static final long serialVersionUID = -8528930845788535109L;

    @XmlElement
    private long stepExecutionId;

    @XmlElement
    private String stepName;

    //unused property, kept here to satisfy Jackson mapping.  Otherwise will get error:
    //UnrecognizedPropertyException: Unrecognized field "persistentUserData"
    //we don't want to annotate Jackson-specific annotations either (@JsonIgnoreProperties(ignoreUnknown = true)
    @XmlTransient
    private Serializable persistentUserData;

    @XmlElement
    private MetricEntity[] metrics;

    public StepExecutionEntity() {
    }

    public StepExecutionEntity(final StepExecution stepExe) {
        super(stepExe.getStartTime(), stepExe.getEndTime(), stepExe.getBatchStatus(), stepExe.getExitStatus());
        this.stepExecutionId = stepExe.getStepExecutionId();
        this.stepName = stepExe.getStepName();
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
        return null;
    }

    public MetricEntity[] getMetrics() {
        return metrics;
    }

    public void setMetrics(final MetricEntity[] metrics) {
        this.metrics = metrics;
    }
}
