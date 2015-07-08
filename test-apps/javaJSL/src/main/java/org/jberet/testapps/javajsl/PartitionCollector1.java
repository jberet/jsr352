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
