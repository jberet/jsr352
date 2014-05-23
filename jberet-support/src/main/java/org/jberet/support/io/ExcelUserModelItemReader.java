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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} for reading Excel files. Current implementation is
 * based on Apache POI user model API.
 *
 * @see ExcelUserModelItemWriter
 * @since 1.1.0
 */
@Named
@Dependent
public class ExcelUserModelItemReader extends ExcelItemReaderWriterBase implements ItemReader {
    /**
     * A positive integer indicating the start position in the input resource. It is optional and defaults to 0
     * (starting from the 1st data item).  If a header row is present, the start point should be after the header row.
     */
    @Inject
    @BatchProperty
    protected int start;

    /**
     * A positive integer indicating the end position in the input resource. It is optional and defaults to
     * {@code Integer.MAX_VALUE}.
     */
    @Inject
    @BatchProperty
    protected int end;

    /**
     * The index (0-based) of the target sheet to read, defaults to 0.
     */
    @Inject
    @BatchProperty
    protected int sheetIndex;

    /**
     * The physical row number of the header.
     */
    @Inject
    @BatchProperty
    protected Integer headerRow;

    protected InputStream inputStream;
    protected FormulaEvaluator formulaEvaluator;
    protected Iterator<Row> rowIterator;
    protected int minColumnCount;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        /**
         * The row number to start reading.  It may be different from the injected field start. During a restart,
         * we would start reading from where it ended during the last run.
         */
        if (this.end == 0) {
            this.end = Integer.MAX_VALUE;
        }
        if (headerRow == null) {
            if (header == null) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "header | headerRow");
            }
            headerRow = -1;
        }
        if (start == headerRow) {
            start += 1;
        }

        int startRowNumber = checkpoint == null ? this.start : (Integer) checkpoint;
        if (startRowNumber < this.start || startRowNumber > this.end
                || startRowNumber < 0 || startRowNumber <= headerRow) {
            throw SupportMessages.MESSAGES.invalidStartPosition(startRowNumber, this.start, this.end);
        }

        inputStream = getInputStream(resource, false);
        workbook = WorkbookFactory.create(inputStream);
        if (sheetName != null) {
            sheet = workbook.getSheet(sheetName);
        }
        if (sheet == null) {
            sheet = workbook.getSheetAt(sheetIndex);
        }
        startRowNumber = Math.max(startRowNumber, sheet.getFirstRowNum());
        rowIterator = sheet.rowIterator();

        if (startRowNumber > 0) {
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                currentRowNum = row.getRowNum();
                if (header == null && headerRow == currentRowNum) {
                    header = getCellStringValues(row);
                }
                if (currentRowNum >= startRowNumber - 1) {
                    break;
                }
            }
        }
        if (header != null) {
            minColumnCount = header.length;
        } else {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, Arrays.toString(header), "header | headerRow");
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (currentRowNum == this.end) {
            return null;
        }
        Row row;
        while (rowIterator.hasNext()) {
            row = rowIterator.next();
            currentRowNum = row.getRowNum();
            final short lastCellNum = row.getLastCellNum();
            if (lastCellNum == -1) {  // no cell in the current row
                continue;
            }
            final int lastColumn = Math.max(lastCellNum, minColumnCount);
            if (java.util.List.class.isAssignableFrom(beanType)) {
                final List<Object> resultList = new ArrayList<Object>();
                for (int cn = 0; cn < lastColumn; cn++) {
                    final Cell c = row.getCell(cn, Row.RETURN_BLANK_AS_NULL);
                    if (c == null) {   // The spreadsheet is empty in this cell
                        resultList.add(null);
                    } else {
                        resultList.add(getCellValue(c, c.getCellType()));
                    }
                }
                return resultList;
            } else {
                final Map<String, Object> resultMap = new HashMap<String, Object>();
                for (int cn = 0; cn < header.length; cn++) {
                    final Cell c = row.getCell(cn, Row.RETURN_BLANK_AS_NULL);
                    if (c != null) {
                        resultMap.put(header[cn], getCellValue(c, c.getCellType()));
                    }
                }
                if (java.util.Map.class.isAssignableFrom(beanType)) {
                    return resultMap;
                } else {
                    if (objectMapper == null) {
                        initJsonFactoryAndObjectMapper();
                    }
                    return objectMapper.convertValue(resultMap, beanType);
                }
            }
        }

        return null;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return currentRowNum;
    }

    @Override
    public void close() throws Exception {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final IOException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close InputStream %s for resource %s", inputStream, resource);
            }
            inputStream = null;
        }
    }

    protected Object getCellValue(final Cell c, final int cellType) {
        final Object cellValue;
        switch (cellType) {
            case Cell.CELL_TYPE_STRING:
                cellValue = c.getStringCellValue();
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                cellValue = c.getBooleanCellValue();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                cellValue = DateUtil.isCellDateFormatted(c) ? c.getDateCellValue() : c.getNumericCellValue();
                break;
            case Cell.CELL_TYPE_BLANK:
                cellValue = null;
                break;
            case Cell.CELL_TYPE_FORMULA:
                if (formulaEvaluator == null) {
                    formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                }
                formulaEvaluator.evaluateFormulaCell(c);
                cellValue = getCellValue(c, c.getCachedFormulaResultType());
                break;
            default:
                cellValue = c.getStringCellValue();
                break;
        }
        return cellValue;
    }
}
