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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} for Excel files.
 *
 * @see ExcelUserModelItemReader
 * @since 1.0.3
 */
@Named
@Dependent
public class ExcelUserModelItemWriter extends ExcelItemReaderWriterBase implements ItemWriter {
    /**
     * Valid writeMode for this writer class is {@link CsvProperties#OVERWRITE} and {@link CsvProperties#FAIL_IF_EXISTS}.
     */
    @Inject
    @BatchProperty
    protected String writeMode;

    protected OutputStream outputStream;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        outputStream = getOutputStream(writeMode);
        workbook = resource.endsWith("xls") ? new HSSFWorkbook() : new XSSFWorkbook();
        sheet = sheetName == null ? workbook.createSheet() :
                workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetName));

        if (header == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, null, "header");
        }
        //write header row
        final Row headerRow = sheet.createRow(0);
        for (int i = 0, j = header.length; i < j; ++i) {
            headerRow.createCell(i, Cell.CELL_TYPE_STRING).setCellValue(header[i]);
        }
        mostRecentRow = headerRow;
        initJsonFactoryAndObjectMapper();
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        int nextRowNum = mostRecentRow.getRowNum() + 1;
        Row row = null;
        if (List.class.isAssignableFrom(beanType)) {
            for (int i = 0, j = items.size(); i < j; ++i, ++nextRowNum) {
                final List<Object> item = (List<Object>) items.get(i);
                row = sheet.createRow(nextRowNum);
                for (int x = 0, y = item.size(); x < y; ++x) {
                    createCell(row, x, item.get(x));
                }
            }
        } else if (Map.class.isAssignableFrom(beanType)) {
            for (int i = 0, j = items.size(); i < j; ++i, ++nextRowNum) {
                final Map<String, Object> item = (Map<String, Object>) items.get(i);
                row = sheet.createRow(nextRowNum);
                for (int x = 0, y = header.length; x < y; ++x) {
                    createCell(row, x, item.get(header[x]));
                }
            }
        } else {
            for (int i = 0, j = items.size(); i < j; ++i, ++nextRowNum) {
                final Object item = items.get(i);

                @SuppressWarnings("unchecked")
                final Map<String, Object> itemAsMap = objectMapper.convertValue(item, Map.class);

                row = sheet.createRow(nextRowNum);
                for (int x = 0, y = header.length; x < y; ++x) {
                    createCell(row, x, itemAsMap.get(header[x]));
                }
            }
        }
        mostRecentRow = row;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {
        if (outputStream != null) {
            try {
                workbook.write(outputStream);
                outputStream.close();
            } catch (final IOException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close OutputStream %s for resource %s%n", outputStream, resource);
            }
        }
        outputStream = null;
    }

    protected void createCell(final Row row, final int columnIndex, final Object val) throws Exception {
        final Cell cell;
        if (val instanceof String) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue((String) val);
        } else if (val instanceof Number) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_NUMERIC);
            Number number = (Number) val;
            cell.setCellValue(number.doubleValue());
        } else if (val instanceof Boolean) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_BOOLEAN);
            cell.setCellValue((Boolean) val);
        } else if (val instanceof Character) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue(val.toString());
        } else if (val == null) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_BLANK);
        } else {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue(val.toString());
        }
    }
}
