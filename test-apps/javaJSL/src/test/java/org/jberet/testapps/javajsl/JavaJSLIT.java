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

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class JavaJSLIT extends AbstractIT {

    /**
     * Creates a job with Java JSL:
     * add 2 job properties;
     * add 1 job listener that has 2 batch properties;
     * add 1 step that has
     *      2 step properties;
     *      1 batchlet that has 2 batch properties;
     *      1 step listener that has 2 batch properties;
     *      stop transition element;
     *      end transition element;
     *      next transition element;
     * add another step that has
     *      1 batchlet.
     *
     *<p>
     * Job or step properties can be set one by one, or set multiple properties together with either a series of String[]
     * or java.util.Properties.
     *
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

        final Job job = new JobBuilder(jobName)
                .restartable(false)
                .property("jobk1", "J")
                .property("jobk2", "J")
                .listener("jobListener1", new String[]{"jobListenerk1", "L"},
                        new String[]{"jobListenerk2", "L"})
                .step(new StepBuilder(stepName)
                        .properties(new String[]{"stepk1", "S"},
                                new String[]{"stepk2", "S"})
                        .batchlet("batchlet1", new String[]{"batchletk1", "B"},
                                new String[]{"batchletk2", "B"})
                        .listener("stepListener1", stepListenerProps)
                        .stopOn("STOP").restartFrom(stepName).exitStatus()
                        .endOn("END").exitStatus("new status for end")
                        .nextOn("*").to(step2Name)
                        .build())
                .step(new StepBuilder(step2Name)
                        .batchlet("batchlet1").build())
                .build();

        startJobAndWait(job);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals("LL", jobExecution.getExitStatus());
        Assert.assertEquals("JJSSBBll", stepExecution0.getExitStatus());
    }


}
