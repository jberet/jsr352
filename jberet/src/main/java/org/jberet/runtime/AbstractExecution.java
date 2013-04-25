/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.jberet.runtime;

import java.util.Date;
import javax.batch.runtime.BatchStatus;

public abstract class AbstractExecution implements Cloneable {
    protected long startTime;
    protected long endTime;

    protected String exitStatus;
    protected BatchStatus batchStatus;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Date getStartTime() {
        return new Date(startTime);
    }

    public Date getEndTime() {
        return new Date(endTime);
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

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

}
