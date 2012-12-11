/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 
package org.mybatch.state;

import java.sql.Timestamp;
import javax.batch.state.JobExecution;

public class JobExecutionImpl implements JobExecution {
    private long id;

    private JobInstanceImpl instance;

    public JobExecutionImpl(JobInstanceImpl in) {
        this.instance = in;
    }

    @Override
    public long getExecutionId() {
        return 0;
    }

    @Override
    public long getInstanceId() {
        return instance.getInstanceId();
    }

    @Override
    public Timestamp getCreateTime() {
        return null;
    }

    @Override
    public Timestamp getStartTime() {
        return null;
    }

    @Override
    public Timestamp getEndTime() {
        return null;
    }

    @Override
    public Timestamp getLastUpdateTime() {
        return null;
    }

    @Override
    public String getBatchStatus() {
        return null;
    }

    @Override
    public String getExitStatus() {
        return null;
    }
}
