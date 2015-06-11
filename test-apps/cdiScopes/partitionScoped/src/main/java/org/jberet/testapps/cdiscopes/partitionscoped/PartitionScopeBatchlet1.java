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

import java.util.List;
import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionScopeBatchlet1 extends AbstractBatchlet {
    @Inject
    private Foo foo;

    @Inject
    private JobScopedFoo jobScopedFoo;

    @Inject
    private StepScopedFoo stepScopedFoo;

    @Inject
    @BatchProperty
    private String stepName;

    @Override
    public String process() throws Exception {
        final List<String> stepNames = foo.getStepNames();
        stepNames.add(stepName);
        System.out.printf("In %s, foo.stepNames: %s%n", this, foo.getStepNames());
        System.out.printf("In %s, jobScopedFoo: %s, stepScopedFoo: %s%n", this, jobScopedFoo, stepScopedFoo);

        return null;
    }
}
