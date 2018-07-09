/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.simple;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests using {@code arrayItemReader} and {@code mockItemWriter} in jberet-support.
 */
public class SimpleIT extends AbstractIT {
    private static final String simpleJob = "simple.xml";

    @Test
    public void readArrayWriteToConsole() throws Exception {
        startJobAndWait(simpleJob);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
