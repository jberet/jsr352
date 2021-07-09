/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.cdiscopes.stepscoped;

import jakarta.batch.api.Batchlet;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;
import org.jberet.testapps.cdiscopes.commons.ScopeArtifactBase;

@Named
public class StepScopeBatchlet2 extends ScopeArtifactBase implements Batchlet {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("stepScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("stepScopedField")
    private FooFieldTarget fooFieldTarget;

    @Override
    public String process() throws Exception {
        return addStepNames(fooTypeTarget, fooMethodTarget, fooFieldTarget);
    }
}
