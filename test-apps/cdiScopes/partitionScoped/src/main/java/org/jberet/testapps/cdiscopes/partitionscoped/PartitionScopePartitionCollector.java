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

package org.jberet.testapps.cdiscopes.partitionscoped;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionCollector;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionScopePartitionCollector implements PartitionCollector {
    @Inject
    private Foo foo;

    @Inject
    @BatchProperty
    private String stepName;

    private boolean collected;

    @Override
    public Serializable collectPartitionData() throws Exception {
        if (!collected) {
            final List<String> stepNames = foo.getStepNames();
            stepNames.add(stepName);
            System.out.printf("In %s, foo.stepNames: %s%n", this, stepNames);

            //by now, both the batchlet and collector already had the chance to add value to Foo, so verify it
            collected = true;
            if (stepNames.size() == 2 && stepNames.get(0).equals(stepNames.get(1))) {
                return stepNames.toString();
            } else {
                collected = true;
                return "Expecting 2 equal strings in stepNames, but got " + stepNames;
            }
        } else {
            return null;
        }
    }
}
