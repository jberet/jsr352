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

package org.jberet.testapps.cdiscopes.jobscoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Named;

import org.jberet.cdi.JobScoped;

@Named
@JobScoped
public class Foo {
    private final List<String> stepNames = new CopyOnWriteArrayList<String>();

    public List<String> getStepNames() {
        return stepNames;
    }
}
