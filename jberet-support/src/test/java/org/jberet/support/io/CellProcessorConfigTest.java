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

import org.junit.Assert;
import org.junit.Test;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.StrMinMax;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CellProcessorConfigTest {

    @Test
    public void testParseCellProcessors1() throws Exception {
        final String val = "StrMinMax(1, 20)";
        final CellProcessor[] cellProcessors = CellProcessorConfig.parseCellProcessors(val);
        System.out.printf("Resolved cell processors: %s%n", Arrays.toString(cellProcessors));
        Assert.assertEquals(1, cellProcessors.length);
        Assert.assertEquals(StrMinMax.class, cellProcessors[0].getClass());
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
        final CellProcessor[] cellProcessors = CellProcessorConfig.parseCellProcessors(val);
        System.out.printf("Resolved cell processors: %s%n", Arrays.toString(cellProcessors));
        Assert.assertEquals(7, cellProcessors.length);
        Assert.assertEquals(null, cellProcessors[0]);
        Assert.assertEquals(Optional.class, cellProcessors[1].getClass());
        Assert.assertEquals(ParseLong.class, cellProcessors[2].getClass());
        Assert.assertEquals(NotNull.class, cellProcessors[3].getClass());
        Assert.assertEquals(ParseDate.class, cellProcessors[4].getClass());
        Assert.assertEquals(StrMinMax.class, cellProcessors[5].getClass());
        Assert.assertEquals(Optional.class, cellProcessors[6].getClass());
    }
}
