/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.commons;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.jberet.cdi.JobScoped;
import org.jberet.cdi.PartitionScoped;
import org.jberet.cdi.StepScoped;

/**
 * A CDI producer class to produce injection resources to batch artifacts.
 *
 * @since 1.3.0.Final
 */
public class FooProducer {
    @Produces
    @JobScoped
    @Named("jobScopedField")
    private FooFieldTarget jobScopedFooFieldTarget = new FooFieldTarget();

    @Produces
    @StepScoped
    @Named("stepScopedField")
    private FooFieldTarget stepScopedFooFieldTarget = new FooFieldTarget();

    @Produces
    @PartitionScoped
    @Named("partitionScopedField")
    private FooFieldTarget partitionScopedFooFieldTarget = new FooFieldTarget();

    @Produces
    @JobScoped
    @Named("jobScopedMethod")
    public FooMethodTarget getJobScopedFooMethodTarget() {
        return new FooMethodTarget();
    }

    @Produces
    @StepScoped
    @Named("stepScopedMethod")
    public FooMethodTarget getStepScopedFooMethodTarget() {
        return new FooMethodTarget();
    }

    @Produces
    @PartitionScoped
    @Named("partitionScopedMethod")
    public FooMethodTarget getPartitionScopedFooMethodTarget() {
        return new FooMethodTarget();
    }
}
