/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.partitionscoped;

import javax.batch.api.listener.StepListener;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionScopeStepListener implements StepListener {
    @Inject
    private Foo foo;

    @Inject
    private JobScopedFoo jobScopedFoo;

    @Inject
    private StepScopedFoo stepScopedFoo;

    @Override
    public void beforeStep() throws Exception {
        System.out.printf("In %s, foo: %s%n", this, foo);
        System.out.printf("In %s, jobScopedFoo: %s, stepScopedFoo: %s%n", this, jobScopedFoo, stepScopedFoo);
    }

    @Override
    public void afterStep() throws Exception {
        System.out.printf("In %s, foo: %s%n", this, foo);
    }
}
