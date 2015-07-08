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

package org.jberet.testapps.javajsl;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Named;

import org.jberet.cdi.PartitionScoped;
import org.jberet.cdi.StepScoped;

@Named
@StepScoped
//@PartitionScoped
public class StepScopedBean {
    private final AtomicInteger sequence = new AtomicInteger();

    public AtomicInteger getSequence() {
        return sequence;
    }
}
