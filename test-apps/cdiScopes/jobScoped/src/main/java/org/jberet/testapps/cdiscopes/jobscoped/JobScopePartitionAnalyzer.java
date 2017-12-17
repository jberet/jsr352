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

package org.jberet.testapps.cdiscopes.jobscoped;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;

@Named
public class JobScopePartitionAnalyzer extends AbstractPartitionAnalyzer {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("jobScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("jobScopedField")
    private FooFieldTarget fooFieldTarget;

    @Inject
    private StepContext stepContext;

    private static final int numberOfPartitions = 2;
    private int numberOfPartitionsFinished;

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        if (++numberOfPartitionsFinished == numberOfPartitions) {
            final String stepName = stepContext.getStepName();
            if (stepName.equals("jobScopedPartitioned.step2") ||
                    stepName.equals("jobScoped2Partitioned.step2")) {

                // step2 has finished, and all partitions are finished
                final int fooTypeSize = fooTypeTarget.getStepNames().size();
                final int fooMethodSize = fooMethodTarget.getStepNames().size();
                final int fooFieldSize = fooFieldTarget.getStepNames().size();

                // 2 steps, each step has 2 partitions, each partition runs one batchlet
                final int expectedSize = 4;
                if (fooTypeSize != expectedSize || fooMethodSize != expectedSize || fooFieldSize != expectedSize) {
                    throw new IllegalStateException(String.format(
                            "Expecting %s elements, but got fooTypeTarget %s, fooMethodType %s, fooFieldTarget %s",
                            expectedSize, fooTypeSize, fooMethodSize, fooFieldSize));
                }
                final String exitStatus1 = String.join(" ", fooTypeTarget.getStepNames());
                final String exitStatus2 = String.join(" ", fooMethodTarget.getStepNames());
                final String exitStatus3 = String.join(" ", fooFieldTarget.getStepNames());
                stepContext.setExitStatus(String.join(" ", exitStatus1, exitStatus2, exitStatus3));
            }
        }
    }
}
