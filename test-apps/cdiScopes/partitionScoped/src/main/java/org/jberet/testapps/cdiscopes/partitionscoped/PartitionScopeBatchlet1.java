/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.partitionscoped;

import java.util.List;
import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;

@Named
public class PartitionScopeBatchlet1 extends AbstractBatchlet {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("partitionScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("partitionScopedField")
    private FooFieldTarget fooFieldTarget;


    @Inject
    private JobScopedFoo jobScopedFoo;

    @Inject
    private StepScopedFoo stepScopedFoo;

    @Inject
    @BatchProperty
    private String stepName;

    @Override
    public String process() throws Exception {
        final List<String> typeStepNames = fooTypeTarget.getStepNames();
        typeStepNames.add(stepName);
        System.out.printf("In %s, fooTypeTarget.stepNames: %s%n", this, fooTypeTarget.getStepNames());
        System.out.printf("In %s, jobScopedFoo: %s, stepScopedFoo: %s%n", this, jobScopedFoo, stepScopedFoo);

        final List<String> methodStepNames = fooMethodTarget.getStepNames();
        methodStepNames.add(stepName);

        final List<String> fieldStepNames = fooFieldTarget.getStepNames();
        fieldStepNames.add(stepName);

        return null;
    }
}
