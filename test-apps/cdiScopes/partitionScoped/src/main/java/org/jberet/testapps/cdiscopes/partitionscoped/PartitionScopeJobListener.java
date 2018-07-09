/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
 
package org.jberet.testapps.cdiscopes.partitionscoped;

import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionScopeJobListener extends AbstractJobListener {
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
