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

package org.jberet.testapps.cdiscopes;

import java.util.List;
import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CdiScopePartitionAnalyzer extends AbstractPartitionAnalyzer {
    @Inject
    private Foo foo;

    @Inject
    private StepContext stepContext;

    static final int numberOfPartitions = 2;
    private int numberOfPartitionsFinished;

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        if (++numberOfPartitionsFinished == numberOfPartitions) {
            if (stepContext.getStepName().equals("jobScopedPartitioned.step2") ||
                    stepContext.getStepName().equals("jobScoped2Partitioned.step2")) {
                // step2 has finished, and all partitions are finished
                final List<String> stepNames = foo.getStepNames();
                if (stepNames.size() == 4) {
                    stepContext.setExitStatus(stepNames.toString());
                } else {
                    throw new IllegalStateException("Expecting 4 elements, but got " + stepNames);
                }
            }
        }
    }
}
