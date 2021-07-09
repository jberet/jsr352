/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.se.test;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import org.jberet.util.BatchUtil;
import org.junit.Assert;

@Named
public class Batchlet1 extends AbstractBatchlet implements Batchlet {
    static final String ACTION = "action";
    static final String ACTION_STOP = "stop";
    static final String ACTION_FAIL = "fail";
    static final String ACTION_END = "end";
    static final String ACTION_EXCEPTION = "exception";
    static final String ACTION_LONG_EXCEPTION = "longException";
    static final String ACTION_OTHER = "other";

    @Inject
    @BatchProperty(name = "Batchlet1")
    private String prop1;

    @Inject
    @BatchProperty  //default name
    private String defaultName;

    @Inject
    @BatchProperty(name = "no-such-property")
    private String noSuchProperty;

    @Inject
    @BatchProperty(name = "no-such-property")
    private String defaultValue = "defaultValue";

    @Inject
    @BatchProperty(name = "foo")
    private String foo;

    @Inject
    @BatchProperty(name = "job-param")
    String jobParam;

    @Inject
    @BatchProperty(name = "int.prop")
    private int intProp;

    @Inject
    @BatchProperty(name = "multi-level")
    private String multiLevel;

    @Inject
    @BatchProperty
    private String action;

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public String process() throws Exception {
        final String stepName = stepContext.getStepName();
        System.out.printf("For %s action in %s: %s%n", stepName, this, action);

        //batchlet1 in step1 does not have "action" property
        if (stepName.equals("step1")) {
            Assert.assertEquals("Batchlet1", prop1);
            Assert.assertEquals("defaultName", defaultName);
            Assert.assertEquals(null, noSuchProperty);
            //Assert.assertEquals("defaultValue", defaultValue);
            Assert.assertEquals("jobParamDefault", jobParam);
            Assert.assertEquals("foo", foo);
            Assert.assertEquals(1, intProp);
            Assert.assertEquals(6, jobContext.getProperties().size());
            Assert.assertEquals(4, stepContext.getProperties().size());
            Assert.assertEquals("JSL.STOP", multiLevel);
            //System.out.printf("Job properties from injected JobContext: %s%n", jobContext.getProperties());
            //System.out.printf("Step properties from injected StepContext: %s%n", stepContext.getProperties());
        } else if (stepName.equals("step2")) {
            Assert.assertNotNull(action);
        }

        //stepContext.setPersistentUserData(new Integer(1));  // integer works fine.
        stepContext.setPersistentUserData("Persistent User Data");
        if (ACTION_EXCEPTION.equals(action)) {
            stepContext.setExitStatus(ACTION_EXCEPTION);
            throw new RuntimeException("Exception from " + this.getClass().getName() + " to fail the job execution.");
        } else if (ACTION_LONG_EXCEPTION.equals(action)) {
            stepContext.setExitStatus(ACTION_LONG_EXCEPTION);
            throw generateLongNestedException();
        }
        return action;
    }

    @Override
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
    }

    private static Exception generateLongNestedException() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append(Long.MAX_VALUE).append(BatchUtil.NL);
        }
        return new IllegalStateException(ACTION_LONG_EXCEPTION, new ArithmeticException(sb.toString()));
    }
}
