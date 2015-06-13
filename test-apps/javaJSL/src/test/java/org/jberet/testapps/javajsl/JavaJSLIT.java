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

package org.jberet.testapps.javajsl;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.job.model.DecisionBuilder;
import org.jberet.job.model.FlowBuilder;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.SplitBuilder;
import org.jberet.job.model.StepBuilder;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class JavaJSLIT extends AbstractIT {
    static final String batchlet1Name = "batchlet1";
    static final String deciderName = "decider2";

    /**
     * Creates a job with Java JSL:
     * add 2 job properties;
     * add 1 job listener that has 2 batch properties;
     * add 1 step that has
     * 2 step properties;
     * 1 batchlet that has 2 batch properties;
     * 1 step listener that has 2 batch properties;
     * stop transition element;
     * end transition element;
     * fail transition element;
     * next transition element;
     * <p/>
     * add another step that has
     * 1 batchlet.
     * <p/>
     * <p/>
     * Job or step properties can be set one by one, or set multiple properties together with either a series of String[]
     * or java.util.Properties.
     * <p/>
     * Batch artifacts can be created along with its batch properties in the form of either a series of String[], or
     * java.util.Properties.  When using String[] to specify a property, the first element is key and the second element
     * is its value.
     *
     * @throws Exception
     */
    @Test
    public void batchlet1() throws Exception {
        final String jobName = "javaJSL-batchlet1";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";

        final Properties stepListenerProps = new Properties();
        stepListenerProps.setProperty("stepListenerk1", "l");
        stepListenerProps.setProperty("stepListenerk2", "l");

        //used to test property resolution
        params.setProperty("jobListenerPropVal", "L");

        final Job job = new JobBuilder(jobName)
                .restartable(false)
                .property("jobk1", "J")
                .property("jobk2", "J")
                .listener("jobListener1", new String[]{"jobListenerk1", "#{jobParameters['jobListenerPropVal']}"},
                        new String[]{"jobListenerk2", "#{jobParameters['jobListenerPropVal']}"})
                .step(new StepBuilder(stepName)
                        .properties(new String[]{"stepk1", "S"},
                                new String[]{"stepk2", "S"})
                        .batchlet(batchlet1Name, new String[]{"batchletk1", "B"},
                                new String[]{"batchletk2", "B"})
                        .listener("stepListener1", stepListenerProps)
                        .stopOn("STOP").restartFrom(stepName).exitStatus()
                        .endOn("END").exitStatus("new status for end")
                        .failOn("FAIL").exitStatus()
                        .nextOn("*").to(step2Name)
                        .build())
                .step(new StepBuilder(step2Name)
                        .batchlet(batchlet1Name).build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals("LL", jobExecution.getExitStatus());
        Assert.assertEquals("JJSSBBll", stepExecution0.getExitStatus());
    }

    /**
     * Builds a job consisting of 1 flow, which consists of 2 steps.
     *
     * @throws Exception
     */
    @Test
    public void flow1() throws Exception {
        final String jobName = "javaJSL-flow1";
        final String flowName = jobName + "flow1";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";

        final Job job = new JobBuilder(jobName)
                .flow(new FlowBuilder(flowName)
                        .step(new StepBuilder(stepName).batchlet(batchlet1Name)
                                .next(step2Name)
                                .build())
                        .step(new StepBuilder(step2Name).batchlet(batchlet1Name)
                                .build())
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(2, stepExecutions.size());
        Assert.assertEquals(stepName, stepExecution0.getStepName());
        Assert.assertEquals(step2Name, stepExecutions.get(1).getStepName());
    }

    /**
     * Builds a job consisting of 1 step and  1 decision.
     *
     * @throws Exception
     */
    @Test
    public void decision1() throws Exception {
        final String jobName = "javaJSL-decision1";
        final String stepName = jobName + ".step1";
        final String decisionName = jobName + ".decision1";

        final Job job = new JobBuilder(jobName)
                .step(new StepBuilder(stepName).batchlet(batchlet1Name).next(decisionName)
                        .build())
                .decision(new DecisionBuilder(decisionName, deciderName)
                        .failOn("FAIL").exitStatus()
                        .stopOn("STOP").restartFrom(stepName).exitStatus()
                        .nextOn("NEXT").to(stepName)
                        .endOn("*").exitStatus(stepName)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(1, stepExecutions.size());
        Assert.assertEquals(stepName, stepExecution0.getStepName());
        Assert.assertEquals(stepName, jobExecution.getExitStatus());  //set by the decision element endOn("*").exitStatus(...)
    }

    /**
     * Builds a job consisting of 1 split and 1 step. The split consists of 2 flows, each of which consists of 1 step.
     * Altogether 3 steps.
     *
     * @throws Exception
     */
    @Test
    public void split1() throws Exception {
        final String jobName = "javaJSL-split1";
        final String splitName = jobName + ".split1";
        final String flowName = splitName + ".flow1";
        final String flow2Name = splitName + ".flow2";
        final String stepName = jobName + ".step1";
        final String step2Name = jobName + ".step2";
        final String step3Name = jobName + ".step3";

        final Job job = new JobBuilder(jobName)
                .split(new SplitBuilder(splitName)
                        .flow(new FlowBuilder(flowName)
                                .step(new StepBuilder(stepName).batchlet(batchlet1Name).build())
                                .build())
                        .flow(new FlowBuilder(flow2Name)
                                .step(new StepBuilder(step2Name).batchlet(batchlet1Name).build())
                                .build())
                        .next(step3Name)
                        .build())
                .step(new StepBuilder(step3Name).batchlet(batchlet1Name)
                        .endOn("*").exitStatus(step3Name)
                        .build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(3, stepExecutions.size());

        //step1 and step2 execution order may be random, so stepExecution0 may point to step1 or step2
        //Assert.assertEquals(stepName, stepExecution0.getStepName());
        //Assert.assertEquals(step2Name, stepExecutions.get(1).getStepName());
        Assert.assertEquals(step3Name, stepExecutions.get(2).getStepName());
    }


}
