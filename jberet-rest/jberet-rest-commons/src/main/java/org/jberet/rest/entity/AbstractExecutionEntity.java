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
import java.util.Date;
import javax.batch.runtime.BatchStatus;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * The base class for {@link JobExecutionEntity} and {@link StepExecutionEntity},
 * and includes common fields such as start time, end time, batch status,
 * and exit status.
 *
 * @see JobExecutionEntity
 * @see StepExecutionEntity
 *
 * @since 1.3.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractExecutionEntity implements Serializable {
    private static final long serialVersionUID = -6861630889634554990L;

    @XmlElement
    Date startTime;

    @XmlElement
    Date endTime;

    @XmlElement
    BatchStatus batchStatus;

    @XmlElement
    String exitStatus;

    public AbstractExecutionEntity() {
    }

    public AbstractExecutionEntity(Date startTime, Date endTime, BatchStatus batchStatus, String exitStatus) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.batchStatus = batchStatus;
        this.exitStatus = exitStatus;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }
}
