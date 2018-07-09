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

import javax.batch.api.listener.StepListener;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;
import org.jberet.testapps.cdiscopes.commons.ScopeArtifactBase;

@Named
public class StepScopedListener extends ScopeArtifactBase implements StepListener {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("stepScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("stepScopedField")
    private FooFieldTarget fooFieldTarget;

    @Override
    public void beforeStep() throws Exception {
        addStepNames(fooTypeTarget, fooMethodTarget, fooFieldTarget);
    }

    @Override
    public void afterStep() throws Exception {
        stepContext.setExitStatus(addStepNames(fooTypeTarget, fooMethodTarget, fooFieldTarget));
    }
}
