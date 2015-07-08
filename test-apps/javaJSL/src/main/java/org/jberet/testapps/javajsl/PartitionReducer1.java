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

import javax.batch.api.partition.AbstractPartitionReducer;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PartitionReducer1 extends AbstractPartitionReducer {
    @Inject
    private StepScopedBean stepScopedBean;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforePartitionedStepCompletion() throws Exception {
        stepContext.setExitStatus(String.valueOf(stepScopedBean.getSequence().intValue()));
    }
}
