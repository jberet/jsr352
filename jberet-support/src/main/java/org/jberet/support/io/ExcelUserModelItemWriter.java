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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} for Excel files. This implementation is currently based
 * on Apache POI user model API, and in-memory content generation. For large data set that may cause memory issue,
 * consider using {@link ExcelStreamingItemWriter}.
 *
 * @see     ExcelUserModelItemReader
 * @see     org.jberet.support.io.ExcelItemReaderWriterBase
 * @see     org.jberet.support.io.ExcelStreamingItemWriter
 * @since   1.1.0
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

    /**
     * The resource of an existing Excel file or template file to be used as a base for generating output Excel. Its
     * format is similar to {@link #resource}.
     */
    @Inject
    @BatchProperty
    protected String templateResource;

    /**
     * The sheet name in the template file to be used for generating output Excel. If {@link #templateResource} is
     * specified but this property is not specified, {@link #templateSheetIndex} is used instead.
     */
    @Inject
    @BatchProperty
    protected String templateSheetName;

    /**
     * The sheet index (0-based) in the template file to be used for generating output Excel.
     */
    @Inject
    @BatchProperty
    protected int templateSheetIndex;

    /**
     * The row number (0-based) of the header in the template sheet. If {@link #header} property is provided in
     * job xml file, then this property is ignored. Otherwise, it is used to retrieve header values.
     */
    @Inject
    @BatchProperty
    protected Integer templateHeaderRow;

    protected OutputStream outputStream;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        //if template is used, create workbook based on template resource, and try to get header from template
        if (templateResource != null) {
            InputStream templateInputStream = null;
            try {
                templateInputStream = getInputStream(templateResource, false);

                //for SXSSF (streaming), the original templateWorkbook is wrapped inside this.workbook, and these 2
                // workbook instances are different.  For XSSF and HSSF, the two are the same.
                // SXSSF workbook does not support reading, so we have to use the original templateWorkbook to read
                // header, and then reassign sheet to that of this.workbook, which is SXSSFWorkbook
                final Workbook templateWorkbook = createWorkbook(templateInputStream);
                if (templateSheetName != null) {
                    sheet = templateWorkbook.getSheet(templateSheetName);
                }
                if (sheet == null) {
                    sheet = templateWorkbook.getSheetAt(templateSheetIndex);
                }
                //if header property is already injected from job.xml, use it and no need to check templateHeaderRow
                if (header == null) {
                    if (templateHeaderRow == null) {
                        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "templateHeaderRow");
                    }
                    final Row headerRow = sheet.getRow(templateHeaderRow);
                    if (headerRow == null) {
                        throw SupportMessages.MESSAGES.failToReadExcelHeader(templateResource, templateSheetName);
                    }
                    header = getCellStringValues(headerRow);
                }
                currentRowNum = sheet.getLastRowNum();
                if (workbook != templateWorkbook) {
                    sheet = workbook.getSheet(sheet.getSheetName());
                }
                workbook.setActiveSheet(workbook.getSheetIndex(sheet));
            } finally {
                if (templateInputStream != null) {
                    try {
                        templateInputStream.close();
                    } catch (final Exception e) {
                        SupportLogger.LOGGER.tracef(e, "Failed to close template InputStream %s for template resource %s%n",
                                templateInputStream, templateResource);
                    }
                }
            }
        } else {  // no template is specified
            createWorkbook(null);
            sheet = sheetName == null ? workbook.createSheet() :
                    workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetName));

            if (header == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "header");
            }
            //write header row
            final Row headerRow = sheet.createRow(0);
            for (int i = 0, j = header.length; i < j; ++i) {
                headerRow.createCell(i, Cell.CELL_TYPE_STRING).setCellValue(header[i]);
            }
            currentRowNum = 0;
        }
        outputStream = getOutputStream(writeMode);
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        int nextRowNum = currentRowNum + 1;
        Row row = null;
        if (List.class.isAssignableFrom(beanType)) {
            for (int i = 0, j = items.size(); i < j; ++i, ++nextRowNum) {
                @SuppressWarnings("unchecked")
                final List<Object> item = (List<Object>) items.get(i);
                row = sheet.createRow(nextRowNum);
                for (int x = 0, y = item.size(); x < y; ++x) {
                    createCell(row, x, item.get(x));
                }
            }
        } else if (Map.class.isAssignableFrom(beanType)) {
            for (int i = 0, j = items.size(); i < j; ++i, ++nextRowNum) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> item = (Map<String, Object>) items.get(i);
                row = sheet.createRow(nextRowNum);
                for (int x = 0, y = header.length; x < y; ++x) {
                    createCell(row, x, item.get(header[x]));
                }
            }
        } else {
            if (objectMapper == null) {
                initJsonFactoryAndObjectMapper();
            }
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
        currentRowNum = row.getRowNum();
        if (sheet instanceof SXSSFSheet) {
            ((SXSSFSheet) sheet).flushRows();
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {
        if (workbook != null) {
            if (outputStream != null) {
                try {
                    workbook.write(outputStream);
                } catch (final IOException e) {
                    SupportLogger.LOGGER.failToWriteWorkbook(e, workbook.toString(), resource);
                }

                try {
                    outputStream.close();
                } catch (final IOException e) {
                    SupportLogger.LOGGER.tracef(e, "Failed to close OutputStream %s for resource %s%n", outputStream, resource);
                }
                outputStream = null;
            }

            if (workbook instanceof SXSSFWorkbook) {
                ((SXSSFWorkbook) workbook).dispose();
            }
            workbook = null;
        }
    }

    /**
     * Creates the workbook for this writer.
     * @param templateInputStream    java.io.InputStream for the template excel file, if any
     * @return    the template workbook if a template is specified; otherwise null
     * @throws IOException
     * @throws InvalidFormatException
     */
    protected Workbook createWorkbook(final InputStream templateInputStream) throws IOException, InvalidFormatException {
        if (templateInputStream != null) {
            return workbook = WorkbookFactory.create(templateInputStream);
        } else {
            workbook = resource.endsWith("xls") ? new HSSFWorkbook() : new XSSFWorkbook();
            return null;
        }
    }

    protected void createCell(final Row row, final int columnIndex, final Object val) throws Exception {
        final Cell cell;
        if (val instanceof String) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue((String) val);
        } else if (val instanceof Number) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_NUMERIC);
            cell.setCellValue(((Number) val).doubleValue());
        } else if (val instanceof Boolean) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_BOOLEAN);
            cell.setCellValue((Boolean) val);
        } else if (val instanceof Character) {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue(val.toString());
        } else if (val == null) {
            row.createCell(columnIndex, Cell.CELL_TYPE_BLANK);
        } else {
            cell = row.createCell(columnIndex, Cell.CELL_TYPE_STRING);
            cell.setCellValue(val.toString());
        }
    }
}
