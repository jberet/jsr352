/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.javajsl;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Named;

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
