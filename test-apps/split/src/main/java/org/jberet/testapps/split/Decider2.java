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

package org.jberet.testapps.split;

import java.util.Arrays;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.inject.Named;

import org.junit.Assert;

/**
 * This decider follows a split, so there should be multiple StepExecution.
 */
@Named
public class Decider2 implements Decider {
    @Override
    public String decide(StepExecution[] executions) throws Exception {
        Assert.assertEquals(2, executions.length);
        System.out.printf("In decider2 StepExecution[]: %s%n", Arrays.toString(executions));
        for (StepExecution e : executions) {
            System.out.printf("batch status: %s, exit status: %s%n", e.getBatchStatus(), e.getExitStatus());
        }
        return "next";
    }
}
