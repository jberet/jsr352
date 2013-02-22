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

package org.mybatch.testapps;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Decider1 implements Decider {
    @BatchProperty(name = "decision-prop")
    private String decisionProp;

    @Inject
    private JobContext jobContext;

    @Override
    public String decide(StepExecution stepExecution) throws Exception {
        System.out.printf("Running %s, decisionProp=%s, job batch/exit status: %s/%s, previous step batch/exit status: %s/%s%n",
                this, decisionProp, jobContext.getBatchStatus(), jobContext.getExitStatus(),
                stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        return "next";
    }

    @Override
    public String decide(StepExecution[] stepExecutions) throws Exception {
        throw new IllegalStateException("Should not reach here.");
    }
}
