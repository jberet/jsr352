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
 
package org.mybatch.runtime;

import java.util.Date;
import java.util.Properties;
import javax.batch.runtime.JobExecution;

public final class JobExecutionImpl extends AbstractExecution implements JobExecution {
    private long id;

    private JobInstanceImpl jobInstance;

    private Properties jobParameters;

    protected long createTime;
    protected long lastUpdatedTime;


    public JobExecutionImpl(JobInstanceImpl in, Properties jobParameters) {
        this.jobInstance = in;
        this.jobParameters = jobParameters;
    }

    @Override
    public long getExecutionId() {
        return id;
    }

    @Override
    public long getInstanceId() {
        return jobInstance.getInstanceId();
    }

    @Override
    public Date getCreateTime() {
        return new Date(createTime);
    }

    @Override
    public Date getLastUpdatedTime() {
        return new Date(lastUpdatedTime);
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setLastUpdatedTime(long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public JobInstanceImpl getJobInstance() {
        return jobInstance;
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

}
