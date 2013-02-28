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

package org.mybatch.testapps.common;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class Batchlet0 extends AbstractBatchlet {
    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @BatchProperty(name="batchlet-prop")
    protected String batchletProp;

    @BatchProperty(name="reference-job-prop")
    protected String referencingJobProp;

    @BatchProperty(name = "reference-system-prop")
    protected String referencingSystemProp;

    @BatchProperty(name = "reference-job-param")
    protected String referencingJobParam;

    @Override
    public String process() throws Exception {
        System.out.printf("%nIn %s, running step %s, job batch/exit status: %s/%s, step batch/exit status: %s/%s%n, job properties: %s, step properties: %s%n%n",
                this, stepContext.getId(),
                jobContext.getBatchStatus(), jobContext.getExitStatus(),
                stepContext.getBatchStatus(), stepContext.getExitStatus(),
                jobContext.getProperties(), stepContext.getProperties()
        );
        return "Processed";
    }

    public JobContext getJobContext() {
        return jobContext;
    }

    public StepContext getStepContext() {
        return stepContext;
    }
}
