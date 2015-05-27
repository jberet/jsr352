/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.jberet.support.io;

import org.junit.Test;

import javax.batch.operations.BatchRuntimeException;

import static org.junit.Assert.*;
import static org.jberet.support.io.JdbcItemWriter.determineParameterNamesList;

@SuppressWarnings("SpellCheckingInspection")
public class JdbcItemWriterTest {

    @Test
    public void normal() throws Exception {
        final String sql = "INSERT INTO forex (symbol, ts, bid_open, bid_high, bid_low, bid_close, volume) " +
                "values ('USDJPY', ?, ?, ?, ?, ?, ?)";
        final String[] actual = determineParameterNamesList(sql);
        final String[] expected = {"ts", "bid_open", "bid_high", "bid_low", "bid_close", "volume"};
        assertArrayEquals(expected, actual);
    }

    @Test(expected = BatchRuntimeException.class)
    public void canNotDetermine() throws Exception {
        final String sql = "INSERT INTO forex (symbol, ts, bid_open, bid_high, bid_low, bid_close, volume) " +
                "values ('USDJPY', parsedatetime('yyyyMMdd HHmmss', ?), ?, ?, ?, ?, ?)";
        determineParameterNamesList(sql);
    }
}
