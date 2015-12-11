/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.runtime;

import java.io.Serializable;
import java.util.Date;
import javax.batch.runtime.BatchStatus;

public abstract class AbstractExecution implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    protected long version;
    protected long startTime;
    protected long endTime;

    protected String exitStatus;
    protected BatchStatus batchStatus;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Date getStartTime() {
        final long startTime = this.startTime;
        if (startTime > 0L) {
            return new Date(startTime);
        }
        return null;
    }

    public Date getEndTime() {
        if (endTime > 0L) {
            return new Date(endTime);
        }
        return null;
    }

    public String getExitStatus() {
        if (this.exitStatus != null) {
            return this.exitStatus;
        }
        if (this.batchStatus != null &&
                (this.batchStatus == BatchStatus.COMPLETED ||
                this.batchStatus == BatchStatus.FAILED ||
                this.batchStatus == BatchStatus.ABANDONED ||
                this.batchStatus == BatchStatus.STOPPED)) {
            return this.batchStatus.name();
        }
        return null;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setExitStatus(final String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public void setBatchStatus(final BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
        switch (batchStatus) {
            case STARTED:
                startTime = System.currentTimeMillis();
                break;
            case COMPLETED:
            case FAILED:
            case STOPPED:
                endTime = System.currentTimeMillis();
                break;
        }
    }

}
