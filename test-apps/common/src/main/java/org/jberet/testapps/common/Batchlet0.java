/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

@Named
public class Batchlet0 extends BatchletNoNamed {
    @PostConstruct
    void ps() {
        System.out.printf("Batchlet0 PostConstruct of %s%n", this);
        addToJobExitStatus("Batchlet0.ps");
    }

    @PreDestroy
    void pd() {
        System.out.printf("Batchlet0 PreDestroy of %s%n", this);
        addToJobExitStatus("Batchlet0.pd");
    }
}
