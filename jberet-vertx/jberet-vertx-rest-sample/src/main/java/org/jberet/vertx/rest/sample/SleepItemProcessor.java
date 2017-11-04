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

package org.jberet.vertx.rest.sample;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * An item processor to add delay time to each item to be processed for testing purpose.
 *
 * @since 1.3.0.Beta7
 */
@Named
public class SleepItemProcessor implements ItemProcessor {
    /**
     * Number of seconds to sleep for each item to be processed. Defaults to 0 (no sleep).
     * Note that the total delay time may reach large amount, depending on the total number
     * of items to be processed.  So this property is typically set to a small number.
     */
    @Inject
    @BatchProperty
    private long sleepSeconds;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object processItem(final Object item) throws Exception {
        if (sleepSeconds > 0) {
            Thread.sleep(sleepSeconds * 1000);
        }
        return item;
    }
}
