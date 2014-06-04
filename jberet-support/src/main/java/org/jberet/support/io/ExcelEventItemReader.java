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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} for reading binary Excel files (.xls) based on
 * Apache POI event model API. Compared to {@link ExcelUserModelItemReader}, this reader implementation has smaller
 * memory footprint and is suitable for reading large binary excel files.
 *
 * @see ExcelUserModelItemReader
 * @see ExcelStreamingItemReader
 * @since 1.1.0
 */
@Named
@Dependent
public class ExcelEventItemReader extends ExcelUserModelItemReader implements ItemReader {
    /**
     * Maximum worksheet row numbers for Excel 2003: 65,536 (2 ** 16)
     * http://office.microsoft.com/en-us/excel-help/excel-specifications-and-limits-HP005199291.aspx
     */
    protected static final int MAX_WORKSHEET_ROWS = 65536;

    /**
     * the capacity of the queue used by {@code org.apache.poi.hssf.eventusermodel.HSSFListener} to hold pre-fetched
     * data rows. Optional property and defaults to {@link #MAX_WORKSHEET_ROWS} (65536).
     */
    @Inject
    @BatchProperty
    protected int queueCapacity;

    private BlockingQueue<Object> queue;
    private DocumentInputStream documentInputStream;
    private FormatTrackingHSSFListener formatListener;

    @Override
    public Object readItem() throws Exception {
        final Object result = queue.take();
        if (result instanceof Exception) {
            if (result instanceof ReadCompletedException) {
                return null;
            }
            throw (Exception) result;
        }
        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return currentRowNum;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (documentInputStream != null) {
            try {
                documentInputStream.close();
            } catch (final Exception e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close DocumentInputStream for %s%n", resource);
            }
        }
    }

    @Override
    protected void initWorkbookAndSheet(final int startRowNumber) throws Exception {
        queue = new ArrayBlockingQueue<Object>(queueCapacity == 0 ? MAX_WORKSHEET_ROWS : queueCapacity);
        final POIFSFileSystem poifs = new POIFSFileSystem(inputStream);
        // get the Workbook (excel part) stream in a InputStream
        documentInputStream = poifs.createDocumentInputStream("Workbook");
        final HSSFRequest req = new HSSFRequest();
        final MissingRecordAwareHSSFListener missingRecordAwareHSSFListener = new MissingRecordAwareHSSFListener(new HSSFListenerImpl(this));
        formatListener = new FormatTrackingHSSFListener(missingRecordAwareHSSFListener);
        req.addListenerForAllRecords(formatListener);
        final HSSFEventFactory factory = new HSSFEventFactory();

        if (objectMapper == null) {
            initJsonFactoryAndObjectMapper();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    factory.processEvents(req, documentInputStream);
                } catch (final ReadCompletedException e) {
                    SupportLogger.LOGGER.tracef("Completed reading %s%n", resource);
                }
            }
        }).start();
    }

    private static final class HSSFListenerImpl implements HSSFListener {
        private final ExcelEventItemReader itemReader;

        //to store 1 row of data
        Map<String, String> resultMap = new HashMap<String, String>();

        private SSTRecord sstrec;
        private String currentSheetName;
        private int currentSheetIndex = -1;
        private final ArrayList<BoundSheetRecord> boundSheetRecords = new ArrayList<BoundSheetRecord>();
        private BoundSheetRecord[] orderedBSRs;
        private final Map<Integer, String> headerIndexToLabelMapping = new HashMap<Integer, String>();
        private boolean readingHeaderRow;
        private boolean readingDataRow;

        /**
         * true if we are reading a sheet and this sheet is the target sheet
         */
        private boolean readingTargetSheet;

        private HSSFListenerImpl(final ExcelEventItemReader itemReader) {
            this.itemReader = itemReader;
            if (itemReader.header != null) {
                for (int i = 0; i < itemReader.header.length; ++i) {
                    headerIndexToLabelMapping.put(i, itemReader.header[i]);
                }
            }
        }

        @Override
        public void processRecord(final Record record) {
            String keyForNextStringRecord = null;
            try {
                if (currentSheetName == null || itemReader.sheetName.equals(currentSheetName)) {
                    switch (record.getSid()) {
                        case BoundSheetRecord.sid:
                            final BoundSheetRecord sheetRec = (BoundSheetRecord) record;
                            boundSheetRecords.add(sheetRec);
                            break;
                        case BOFRecord.sid:
                            final BOFRecord bofRecord = (BOFRecord) record;
                            if (bofRecord.getType() != BOFRecord.TYPE_WORKBOOK) {
                                currentSheetIndex++;
                            }
                            if (bofRecord.getType() == BOFRecord.TYPE_WORKSHEET) {
                                orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
                                currentSheetName = orderedBSRs[currentSheetIndex].getSheetname();
                                if (itemReader.sheetName != null) {
                                    readingTargetSheet = currentSheetName.equals(itemReader.sheetName);
                                } else {
                                    if (currentSheetIndex == itemReader.sheetIndex) {
                                        itemReader.sheetName = currentSheetName;
                                        readingTargetSheet = true;
                                    } else {
                                        readingTargetSheet = false;
                                    }
                                }

                            }
                            break;
                        case SSTRecord.sid:
                            sstrec = (SSTRecord) record;
                            break;

                        case BlankRecord.sid:
                            if (readingTargetSheet) {
                                final BlankRecord rec = (BlankRecord) record;
                                readCellValues(rec.getRow(), (int) rec.getColumn(), null);
                            }
                            break;
                        case BoolErrRecord.sid:
                            if (readingTargetSheet) {
                                final BoolErrRecord rec = (BoolErrRecord) record;
                                final String val = rec.isError() ? String.valueOf(rec.getErrorValue()) : String.valueOf(rec.getBooleanValue());
                                readCellValues(rec.getRow(), (int) rec.getColumn(), val);
                            }
                            break;
                        case FormulaRecord.sid:
                            if (readingTargetSheet) {
                                final FormulaRecord rec = (FormulaRecord) record;

                                final int row = rec.getRow();
                                final int column = rec.getColumn();
                                String val;
                                if (Double.isNaN(rec.getValue())) {
                                    // Formula result is a string This is stored in the next record
                                    keyForNextStringRecord = headerIndexToLabelMapping.get(column);
                                } else {
                                    val = itemReader.formatListener.formatNumberDateCell(rec);
                                    readCellValues(row, column, val);
                                }
                            }
                            break;
                        case StringRecord.sid:
                            if (readingTargetSheet) {
                                if (keyForNextStringRecord != null) {
                                    // String for formula
                                    final StringRecord rec = (StringRecord) record;
                                    final String val = rec.getString();
                                    resultMap.put(keyForNextStringRecord, val);
                                    keyForNextStringRecord = null;
                                }
                            }
                            break;
                        case LabelRecord.sid:
                            if (readingTargetSheet) {
                                final LabelRecord rec = (LabelRecord) record;
                                readCellValues(rec.getRow(), rec.getColumn(), rec.getValue());
                            }
                            break;
                        case LabelSSTRecord.sid:
                            if (readingTargetSheet) {
                                final LabelSSTRecord rec = (LabelSSTRecord) record;
                                final String val = sstrec.getString(rec.getSSTIndex()).toString();
                                readCellValues(rec.getRow(), rec.getColumn(), val);
                            }
                            break;
                        case NumberRecord.sid:
                            if (readingTargetSheet) {
                                final NumberRecord rec = (NumberRecord) record;
                                final double val = rec.getValue();
                                readCellValues(rec.getRow(), rec.getColumn(), String.valueOf(val));
                            }
                            break;
                        case EOFRecord.sid:
                            if (readingTargetSheet && readingDataRow) {
                                queueRowData(null, true);
                            }
                            break;
                        default:
                            break;
                    }
                }

                // Handle end of row
                if (readingTargetSheet && record instanceof LastCellOfRowDummyRecord) {
                    final LastCellOfRowDummyRecord lastCellOfRowDummyRecord = (LastCellOfRowDummyRecord) record;
                    final int row = lastCellOfRowDummyRecord.getRow();
                    if (readingHeaderRow) {
                        itemReader.headerMapping = new HashMap<String, String>();
                        for (final Map.Entry<Integer, String> e : headerIndexToLabelMapping.entrySet()) {
                            itemReader.headerMapping.put(String.valueOf(e.getKey()), e.getValue());
                        }
                        if (itemReader.header == null) {
                            final List<String> headerList = new ArrayList<String>();
                            final int headerColumnCount = headerIndexToLabelMapping.size();
                            for (int i = 0; headerList.size() < headerColumnCount; i++) {
                                final String val = headerIndexToLabelMapping.get(i);
                                if (val != null) {
                                    headerList.add(val);
                                }
                            }
                            itemReader.header = headerList.toArray(new String[headerColumnCount]);
                        }
                        readingHeaderRow = false;
                    } else if (readingDataRow) {
                        queueRowData(null, false);
                    }
                    if (row >= itemReader.end) {
                        queueRowData(null, true);
                    }
                    itemReader.currentRowNum = row;
                }
            } catch (final Exception e) {
                if (readingTargetSheet) {
                    queueRowData(e, false);
                }
            }

        }

        private void readCellValues(final int row, final int column, final String val) {
            if (itemReader.header == null && row == itemReader.headerRow) {
                readingHeaderRow = true;
                readingDataRow = false;
                headerIndexToLabelMapping.put(column, val);
            } else if (row >= itemReader.start) {
                readingDataRow = true;
                readingHeaderRow = false;
                resultMap.put(headerIndexToLabelMapping.get(column), val);

            }
        }

        /**
         * puts data, {@link #resultMap} for regular row data, and {@link ReadCompletedException} to indicate the end
         * of data stream. It also re-initialize {@link #resultMap}
         *
         * @param exception any exception occurred during event record processing
         * @param eof       true if reached the end of data stream
         */
        private void queueRowData(final Exception exception, final boolean eof) throws ReadCompletedException {
            try {
                if (eof) {
                    final ReadCompletedException readCompletedException = new ReadCompletedException();
                    itemReader.queue.put(readCompletedException);
                    throw readCompletedException;
                } else if (exception != null) {
                    itemReader.queue.put(exception);
                    resultMap = new HashMap<String, String>();
                } else {
                    final Object obj;
                    if (itemReader.beanType == List.class) {
                        final List<String> resultList = new ArrayList<String>();
                        for (int i = 0; i < itemReader.header.length; ++i) {
                            resultList.add(resultMap.get(itemReader.header[i]));
                        }
                        obj = resultList;
                    } else if (itemReader.beanType == Map.class) {
                        obj = resultMap;
                    } else {
                        obj = itemReader.objectMapper.convertValue(resultMap, itemReader.beanType);
                    }
                    itemReader.queue.put(obj);
                    resultMap = new HashMap<String, String>();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Exception to forcefully end excel parsing and reading. {@code org.apache.poi.hssf.eventusermodel.AbortableHSSFListener}
     * cannot be chained with {@code MissingRecordAwareHSSFListener}, so this exception has to be thrown from
     * {@link HSSFListenerImpl#processRecord(org.apache.poi.hssf.record.Record)} to indicate the end.
     */
    private static final class ReadCompletedException extends RuntimeException {
        private static final long serialVersionUID = -8693208957107027254L;
    }
}
