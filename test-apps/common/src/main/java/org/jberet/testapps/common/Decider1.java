/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.common;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

@Named
public class Decider1 implements Decider {
    @Inject @BatchProperty(name = "decision-prop")
    private String decisionProp;

    @Inject @BatchProperty(name="reference-job-prop")
    private String referencingJobProp;

    @Inject @BatchProperty(name="reference-step-prop")
    private String referencingStepProp;  //not injected

    @Inject @BatchProperty(name = "reference-system-prop")
    private String referencingSystemProp;

    @Inject @BatchProperty(name = "reference-job-param")
    private String referencingJobParam;

    @Inject
    private JobContext jobContext;

    @Override
    public String decide(final StepExecution[] stepExecutions) throws Exception {
        final StepExecution stepExecution = stepExecutions[0];
        Assert.assertEquals("decision-prop", decisionProp);
        Assert.assertEquals("job-prop", referencingJobProp);
        Assert.assertThat(referencingStepProp, not(equalTo("step-prop")));
        Assert.assertEquals(System.getProperty("java.version"), referencingSystemProp);
        Assert.assertEquals("job-param", referencingJobParam);

        System.out.printf("Running %s, decisionProp=%s, job batch/exit status: %s/%s, previous step batch/exit status: %s/%s%n",
                this, decisionProp, jobContext.getBatchStatus(), jobContext.getExitStatus(),
                stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        return "next";
    }
}
