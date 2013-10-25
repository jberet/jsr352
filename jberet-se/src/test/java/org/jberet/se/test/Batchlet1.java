/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se.test;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Batchlet1 extends AbstractBatchlet implements Batchlet {
    @Inject @BatchProperty(name = "Batchlet1")
    private String prop1;

    @Inject @BatchProperty  //default name
    private String defaultName;

    @Inject @BatchProperty(name = "no-such-property")
    private String noSuchProperty;

    @Inject @BatchProperty(name = "no-such-property")
    private String defaultValue = "defaultValue";

    @Inject @BatchProperty(name = "foo")
    private String foo;

    @Inject @BatchProperty(name = "job-param")
    String jobParam;

    @Inject @BatchProperty(name="int.prop")
    private int intProp;

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public String process() throws Exception {
        System.out.printf("Injected batchlet property: %s => %s%n", "Batchlet1", prop1);
        System.out.printf("Injected batch property with default name: %s => %s%n", "defaultName", defaultName);
        System.out.printf("Undeclared batch property: %s => %s%n", "no-such-property", noSuchProperty);
        System.out.printf("Undeclared batch property: %s => %s%n", "no-such-property", defaultValue);
        System.out.printf("Injected job param %s => %s%n", "job-param", jobParam);

        System.out.printf("Injected JobContext: %s%n", jobContext);
        System.out.printf("Injected StepContext: %s%n", stepContext);
        System.out.printf("Job properties from injected JobContext: %s%n", jobContext.getProperties());
        System.out.printf("Step properties from injected StepContext: %s%n", stepContext.getProperties());

        final String fooExpected = "foo";
        if (fooExpected.equals(foo)) {
            System.out.printf("Injected batchlet property foo: %s%n", foo);
        } else {
            throw new BatchRuntimeException(String.format("Expecting batchlet property foo to be %s, but got %s", fooExpected, foo));
        }
        if (intProp == 1) {
            System.out.printf("Injected int.prop: %s%n", intProp);
        } else {
            throw new BatchRuntimeException(String.format("Expecting int.prop %s, but got %s", 1, intProp));
        }
        //stepContext.setPersistentUserData(new Integer(1));  // integer works
        stepContext.setPersistentUserData("Persistent User Data");
        return BatchStatus.COMPLETED.name();
    }

    @Override
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
    }
}
