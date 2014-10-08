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

import java.io.IOException;
import java.io.InputStream;
import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} for Excel 2007 OOXML (.xlsx) file format.
 * This implementation is currently based on Apache POI SXSSF (buffered streaming) API, and is suitable for handling
 * large data set. Note that different versions of Excel have different limits for row number and column numbers.
 * As of Excel 2010, the row number limit is 1048576. For more details, see
 * http://office.microsoft.com/en-us/excel-help/excel-specifications-and-limits-HP010342495.aspx
 *
 * @since 1.1.0
 * @see ExcelUserModelItemWriter
 */
@Named
@Dependent
public class ExcelStreamingItemWriter extends ExcelUserModelItemWriter {
    /**
     * Whether to compress the temp files in the course of generating Excel file, defaults to false.
     */
    @Inject
    @BatchProperty
    protected Boolean compressTempFiles;

    @Override
    protected Workbook createWorkbook(final InputStream templateInputStream) throws IOException, InvalidFormatException {
        if (templateInputStream != null) {
            final Workbook template = WorkbookFactory.create(templateInputStream);
            if (template instanceof XSSFWorkbook) {
                workbook = compressTempFiles == Boolean.TRUE ?
                        new SXSSFWorkbook((XSSFWorkbook) template, -1, compressTempFiles) :
                        new SXSSFWorkbook((XSSFWorkbook) template, -1);
                return template;
            } else {
                throw SupportMessages.MESSAGES.incompatibleExcelFileFormat(templateResource);
            }
        } else if (resource.endsWith("xlsx")) {
            final SXSSFWorkbook workbook1 = new SXSSFWorkbook(-1);
            if (compressTempFiles == Boolean.TRUE) {
                workbook1.setCompressTempFiles(true);
            }
            workbook = workbook1;
        } else {
            throw SupportMessages.MESSAGES.incompatibleExcelFileFormat(resource);
        }
        return null;
    }
}
