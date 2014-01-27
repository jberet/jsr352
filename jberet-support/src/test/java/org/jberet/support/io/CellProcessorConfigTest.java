/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

import java.util.Arrays;

import org.jberet.util.BatchUtil;
import org.junit.Test;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CellProcessorConfigTest {

    @Test
    public void testParseCellProcessors1() throws Exception {
        final String val = "StrMinMax(1, 20)";
        final CellProcessor[] cellProcessors = CellProcessorConfig.parseCellProcessors(val, createHeader(1));
        System.out.printf("Resolved cell processors: %s%n", Arrays.toString(cellProcessors));
    }

    @Test
    public void testParseCellProcessors7() throws Exception {
        final String val = "null;"
                + "Optional, StrMinMax(1, 20);"
                + "ParseLong();"
                + "NotNull, ParseInt;"
                + "ParseDate( 'dd/MM/yyyy' );"
                + "StrMinMax(1, 20);"
                + "Optional, StrMinMax(1, 20), ParseDate('dd/MM/yyyy')";
        final CellProcessor[] cellProcessors = CellProcessorConfig.parseCellProcessors(val, createHeader(7));
        System.out.printf("Resolved cell processors: %s%n", Arrays.toString(cellProcessors));
    }

    private static String[] createHeader(final int count) {
        final String[] header = new String[count];
        for (int i = 0; i < count; i++) {
            header[i] = "H";
        }
        return header;
    }
}
