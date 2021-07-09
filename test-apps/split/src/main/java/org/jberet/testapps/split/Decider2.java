/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.split;

import java.util.Arrays;
import jakarta.batch.api.Decider;
import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Named;

import org.junit.Assert;

/**
 * This decider follows a split, so there should be multiple StepExecution.
 */
@Named
public class Decider2 implements Decider {
    @Override
    public String decide(final StepExecution[] executions) throws Exception {
        Assert.assertEquals(2, executions.length);
        System.out.printf("In decider2 StepExecution[]: %s%n", Arrays.toString(executions));
        for (final StepExecution e : executions) {
            System.out.printf("batch status: %s, exit status: %s%n", e.getBatchStatus(), e.getExitStatus());
        }
        return "next";
    }
}
