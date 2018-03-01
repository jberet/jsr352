/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.listener.AbstractItemReadListener;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;

/**
 * An {@code ItemReadListener} that converts {@code java.util.Date} date fields into
 * {org.joda.time.DateTime}, to test custom codec in {@link CassandraItemWriter}.
 *
 * The custom codec should be able to map between CQL data type {@code timestamp} and
 * java type {@code org.joda.time.DateTime}.
 *
 * @since 1.3.0.Final
 */
@Named
@Dependent
public class StockTradeItemReadListener extends AbstractItemReadListener {

    @Inject
    @BatchProperty
    private boolean convertToJodaDate;

    @Override
    public void afterRead(final Object item) throws Exception {
        if (convertToJodaDate && item != null) {
            if (item instanceof java.util.List) {
                //the first element is tradedate of type java.util.Date
                //convert it to org.joda.time.DateTime

                final List itemAsList = (List) item;
                final Date tradedate = (Date) itemAsList.get(0);
                itemAsList.set(0, new DateTime(tradedate.getTime()));
            } else if (item instanceof java.util.Map) {
                //convert java.util.Date to org.joda.time.DateTime

                final Map itemAsMap = (Map) item;
                for (Object k : itemAsMap.keySet()) {
                    final Object v = itemAsMap.get(k);
                    if (v instanceof Date) {
                        final Date tradedate = (Date) v;
                        itemAsMap.put(k, new DateTime(tradedate.getTime()));
                    }
                }
            }
        }
    }
}
