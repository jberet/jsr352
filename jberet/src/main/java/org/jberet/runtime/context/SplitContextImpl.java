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

package org.jberet.runtime.context;

import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.Split;
import org.jberet.runtime.FlowExecutionImpl;
import org.jberet.runtime.SplitExecutionImpl;

public class SplitContextImpl extends AbstractContext {
    private Split split;

    private SplitExecutionImpl splitExecution;

    private List<FlowExecutionImpl> flowExecutions = new ArrayList<FlowExecutionImpl>();

    public SplitContextImpl(Split split, AbstractContext[] outerContexts) {
        super(split.getId(), outerContexts);
        this.split = split;
        this.classLoader = getJobContext().getClassLoader();
        this.splitExecution = new SplitExecutionImpl(split.getId());
        splitExecution.setBatchStatus(BatchStatus.STARTING);
    }

    public Split getSplit() {
        return split;
    }

    public List<FlowExecutionImpl> getFlowExecutions() {
        return this.flowExecutions;
    }

    @Override
    public BatchStatus getBatchStatus() {
        return splitExecution.getBatchStatus();
    }

    @Override
    public void setBatchStatus(BatchStatus status) {
        splitExecution.setBatchStatus(status);
    }

    @Override
    public String getExitStatus() {
        return splitExecution.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        splitExecution.setExitStatus(exitStatus);
    }

    @Override
    public org.jberet.job.Properties getProperties2() {
        return null;  //split has no <properties>
    }

}
