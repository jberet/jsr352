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

import org.supercsv.cellprocessor.ift.CellProcessor;

/**
 * This class is responsible for parsing the cellProcessors configuration property value into an array of
 * {@code org.supercsv.cellprocessor.ift.CellProcessor}, which can be consumed by
 * {@code org.jberet.support.io.CsvItemReader}.
 */
class CellProcessorConfig {

    static CellProcessor[] parseCellProcessors(final String val, final String[] header) {
        CellProcessor[] result = null;
        final String[] parts = val.split("\\r?\\n");


        return result;
    }
}
