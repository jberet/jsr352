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
        final String val = "null" + BatchUtil.NL
                + "Optional, StrMinMax(1, 20)" + BatchUtil.NL
                + "ParseLong()" + BatchUtil.NL
                + "NotNull, ParseInt" + BatchUtil.NL
                + "ParseDate( 'dd/MM/yyyy' )" + BatchUtil.NL
                + "StrMinMax(1, 20)" + BatchUtil.NL
                + "Optional, StrMinMax(1, 20), ParseDate('dd/MM/yyyy')" + BatchUtil.NL;
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
