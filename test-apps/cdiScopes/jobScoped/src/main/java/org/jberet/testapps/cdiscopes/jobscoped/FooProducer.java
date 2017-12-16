/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
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

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.jberet.cdi.JobScoped;

/**
 * A CDI producer class to produce injection resources to batch artifacts.
 *
 * @since 1.3.0.Final
 */
public class FooProducer {
    @Produces
    @JobScoped
    @Named("FIELD")
    private FooFieldTarget fooFieldTargetreturn = new FooFieldTarget();

    @Produces
    @JobScoped
    @Named("METHOD")
    public FooMethodTarget getFooMethodTarget() {
        return new FooMethodTarget();
    }

}
