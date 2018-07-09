/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.javajsl;

import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionAnalyzer1 extends javax.batch.api.partition.AbstractPartitionAnalyzer {
    @Inject
    private StepScopedBean stepScopedBean;

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        System.out.printf("Sequence %s, in %s%n", stepScopedBean.getSequence().getAndIncrement(), this);
    }
}
