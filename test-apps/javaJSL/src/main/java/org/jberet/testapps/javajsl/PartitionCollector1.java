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

import java.io.Serializable;
import javax.batch.api.partition.PartitionCollector;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionCollector1 implements PartitionCollector {
    @Inject
    private StepScopedBean stepScopedBean;

    @Override
    public Serializable collectPartitionData() throws Exception {
        final int n = stepScopedBean.getSequence().getAndIncrement();
        System.out.printf("Sequence %s, in %s%n", n, this);
        return n;
    }
}
