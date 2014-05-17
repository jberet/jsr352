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

import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * The base class of Excel reader and writer classes: {@link ExcelUserModelItemReader} and
 * {@link ExcelUserModelItemWriter}.
 *
 * @see ExcelUserModelItemReader
 * @see ExcelUserModelItemWriter
 * @since 1.0.3
 */
public abstract class ExcelItemReaderWriterBase extends JsonItemReaderWriterBase {

    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * Specifies the header as an ordered string array. For reader, header information must be specified with either
     * this property or {@link ExcelUserModelItemReader#headerRow} property. This property is typically specified
     * when there is no header row in the Excel file. For example,
     * <p/>
     * "id, name, age" specifies 1st column is id, 2nd column is name and 3rd column is age.
     * <p/>
     * This is a required property for writer.
     */
    @Inject
    @BatchProperty
    protected String[] header;

    /**
     * The optional name of the target sheet. When specified for a reader, it has higher precedence over
     * {@link org.jberet.support.io.ExcelUserModelItemReader#sheetIndex}
     */
    @Inject
    @BatchProperty
    protected String sheetName;
    protected Workbook workbook;
    protected Sheet sheet;
    protected Row mostRecentRow;

    @Override
    protected void registerModule() throws Exception {
        //noop
    }
}
