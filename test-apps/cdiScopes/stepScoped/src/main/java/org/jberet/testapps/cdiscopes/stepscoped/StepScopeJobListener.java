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
 
package org.jberet.testapps.cdiscopes.stepscoped;

import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StepScopeJobListener extends AbstractJobListener {
    @Inject
    private Foo foo;

    @Override
    public void beforeJob() throws Exception {
        System.out.printf("In beforeJob of %s, foo: %s%n", this, foo);
    }

    @Override
    public void afterJob() throws Exception {
        System.out.printf("In afterJob of %s, foo: %s%n", this, foo);
    }
}
