/*
 * Copyright (c) 2020 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.ee.bom.samples.batchletsingleton;

import java.util.logging.Level;
import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class BatchletProcess extends AbstractBatchlet {
    @Inject
    private StepContext stepContext;

    @Override
    public String process() {
        BatchletSingletonBean.LOGGER.log(Level.INFO, "Batchlet process() of {0}", stepContext.getStepName());
        return null;
    }
}
