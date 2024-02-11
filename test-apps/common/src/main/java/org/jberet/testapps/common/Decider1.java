/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Decider;
import jakarta.batch.runtime.StepExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Assertions;

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
        Assertions.assertEquals("decision-prop", decisionProp);
        Assertions.assertEquals("job-prop", referencingJobProp);
        Assertions.assertThat(referencingStepProp, not(equalTo("step-prop")));
        Assertions.assertEquals(System.getProperty("java.version"), referencingSystemProp);
        Assertions.assertEquals("job-param", referencingJobParam);

        System.out.printf("Running %s, decisionProp=%s, job batch/exit status: %s/%s, previous step batch/exit status: %s/%s%n",
                this, decisionProp, jobContext.getBatchStatus(), jobContext.getExitStatus(),
                stepExecution.getBatchStatus(), stepExecution.getExitStatus());
        return "next";
    }
}
