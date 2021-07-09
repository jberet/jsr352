/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.common;

import jakarta.annotation.PreDestroy;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;

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
