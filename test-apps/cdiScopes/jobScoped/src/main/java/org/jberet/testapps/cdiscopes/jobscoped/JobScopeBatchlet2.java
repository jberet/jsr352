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

package org.jberet.testapps.cdiscopes.jobscoped;

import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.cdiscopes.commons.FooFieldTarget;
import org.jberet.testapps.cdiscopes.commons.FooMethodTarget;
import org.jberet.testapps.cdiscopes.commons.ScopeBatchletBase;

@Named
public class JobScopeBatchlet2 extends ScopeBatchletBase {
    @Inject
    private Foo fooTypeTarget;

    @Inject
    @Named("jobScopedMethod")
    private FooMethodTarget fooMethodTarget;

    @Inject
    @Named("jobScopedField")
    private FooFieldTarget fooFieldTarget;

    @Override
    public String process() throws Exception {
        return addStepNames(fooTypeTarget, fooMethodTarget, fooFieldTarget);
    }
}
