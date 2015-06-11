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

package org.jberet.testapps.cdiscopes.partitionscoped;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionScopePartitionAnalyzer implements PartitionAnalyzer {
    @Inject
    private Foo foo;

    @Inject
    private JobScopedFoo jobScopedFoo;

    @Inject
    private StepScopedFoo stepScopedFoo;

    @Inject
    private StepContext stepContext;

    static final int numberOfPartitions = 3;
    private int numberOfPartitionsFinished;
    private final Set<Serializable> collectedData = new HashSet<Serializable>();
    final static Set<String> expectedData = new HashSet<String>();

    static {
        expectedData.add(Arrays.asList("partitionScopedPartitioned.step1.A", "partitionScopedPartitioned.step1.A").toString());
        expectedData.add(Arrays.asList("partitionScopedPartitioned.step1.B", "partitionScopedPartitioned.step1.B").toString());
        expectedData.add(Arrays.asList("partitionScopedPartitioned.step1.C", "partitionScopedPartitioned.step1.C").toString());
    }

    @Override
    public void analyzeCollectorData(final Serializable data) throws Exception {
        if (data != null) {
            collectedData.add(data);
        }
    }

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        if (++numberOfPartitionsFinished == numberOfPartitions) {
            System.out.printf("In %s, jobScopedFoo: %s, stepScopedFoo: %s%n", this, jobScopedFoo, stepScopedFoo);

            if (expectedData.equals(collectedData)) {
                stepContext.setExitStatus(collectedData.toString());
            } else {
                throw new IllegalStateException("Expecting collected data: " + expectedData + ", but is " + collectedData);
            }
        }
    }
}
