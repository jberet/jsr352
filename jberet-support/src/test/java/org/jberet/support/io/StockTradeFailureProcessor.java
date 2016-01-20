/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * An item processor implementation that handles {@link StockTrade}, and can be configured to throw
 * {@code ArithmeticException} based on the value of {@link StockTrade#getTime()}.
 * If not configured to fail, or no match, this class simply passes the data through.
 */
@Named
@Dependent
public class StockTradeFailureProcessor implements ItemProcessor {
    /**
     * A comma-separated list of time values of {@link StockTrade}. If any of them matches against
     * the incoming item, an {@code ArithmeticException} is thrown.
     * <p>
     * For example, "09:30, 09:31"
     */
    @Inject
    @BatchProperty
    protected List<String> failOnTimes;

    @Override
    public Object processItem(final Object item) throws Exception {
        final StockTrade st = (StockTrade) item;
        if (failOnTimes != null) {
            for (final String e : failOnTimes) {
                if (e.equals(st.getTime())) {
                    throw new ArithmeticException("StockTrade: " + st +
                            " time value matches configured failOnTimes value: " + failOnTimes);
                }
            }
        }

        return item;
    }
}
