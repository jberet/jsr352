/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
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
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionCollector;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;

@Named
public class PartitionScopePartitionCollector implements PartitionCollector {
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

    private boolean collected;

    @Override
    public Serializable collectPartitionData() throws Exception {
        if (!collected) {
            System.out.printf("In %s, jobScopedFoo: %s, stepScopedFoo: %s%n", this, jobScopedFoo, stepScopedFoo);

            final List<String> stepNames = fooTypeTarget.getStepNames();
            stepNames.add(stepName);
            System.out.printf("In %s, foo.stepNames: %s%n", this, stepNames);

            //by now, both the batchlet and collector already had the chance to add value to Foo, so verify it
            collected = true;

            //check injected fooMethodTarget and fooFieldTarget
            //but do not send these data to keep the collected data short
            if (fooMethodTarget.getStepNames().size() != 1 || fooFieldTarget.getStepNames().size() != 1) {
                return "Expecting fooMethodTarget.stepNames.size and fooFieldTarget.stepNames.size to be 1, but got " +
                        fooMethodTarget.getStepNames().size() + ", and " + fooFieldTarget.getStepNames().size();
            }

            //check injected fooTypeTarget
            if (stepNames.size() == 2 && stepNames.get(0).equals(stepNames.get(1))) {
                return stepNames.toString();
            } else {
                collected = true;
                return "Expecting 2 equal strings in stepNames, but got " + stepNames;
            }
        } else {
            return null;
        }
    }
}
