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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} for reading OOXML Excel files, based on Apache POI
 * XSSF streaming reader API, and StAX.
 *
 * @see ExcelUserModelItemReader
 * @see ExcelStreamingItemWriter
 * @since 1.1.0
 */

@Named
@Dependent
public class ExcelStreamingItemReader extends ExcelUserModelItemReader implements ItemReader {
    private static final String schemaRelationships = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    /**
     * If set to true, the target sheet uses R1C1-style cell coordinates; if set to false, the target sheet uses the
     * more common A1-style cell coordinates; defaults to null (not set).
     */
    protected Boolean r1c1;

    protected InputStream sheetInputStream;
    protected XMLStreamReader sheetStreamReader;
    protected SharedStringsTable sharedStringsTable;

    @Override
    public Object readItem() throws Exception {
        if (currentRowNum == this.end) {
            return null;
        }

        Map<String, String> resultMap;
        while (sheetStreamReader.hasNext()) {
            final int event = sheetStreamReader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                currentRowNum = Integer.parseInt(sheetStreamReader.getAttributeValue(null, "r")) - 1;

                resultMap = new HashMap<String, String>();
                while (sheetStreamReader.hasNext()) {
                    final int event1 = sheetStreamReader.next();
                    if (event1 == XMLStreamConstants.START_ELEMENT && "c".equals(sheetStreamReader.getLocalName())) {
                        final String columnLabel = getColumnLabel(sheetStreamReader.getAttributeValue(null, "r"));
                        if (headerMapping == null) {
                            initHeaderMapping();
                        }
                        final String key = headerMapping.get(columnLabel);
                        resultMap.put(key, getCellStringValue());
                    } else if (event1 == XMLStreamConstants.END_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                        if (beanType == Map.class) {
                            return resultMap;
                        }
                        if (beanType == List.class) {
                            //blank cells have no trace in sheet xml file, so need to match any cell to its column letter
                            //and add null for blank cell to avoid accidental shift
                            final List<String> resultList = new ArrayList<String>();
                            for (final String h : header) {
                                resultList.add(resultMap.get(h));
                            }
                            return resultList;
                        }
                        initJsonFactoryAndObjectMapper();
                        final Object readValue = objectMapper.convertValue(resultMap, beanType);
                        if (!skipBeanValidation) {
                            ItemReaderWriterBase.validate(readValue);
                        }
                        return readValue;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (sheetStreamReader != null) {
            try {
                sheetStreamReader.close();
            } catch (final Exception e) {
                //ignore
            }
            sheetStreamReader = null;
        }
        if (sheetInputStream != null) {
            try {
                sheetInputStream.close();
            } catch (final Exception e) {
                //ignore
            }
            sheetInputStream = null;
        }
    }

    @Override
    protected void initWorkbookAndSheet(final int startRowNumber) throws Exception {
        InputStream workbookDataInputStream = null;
        XMLStreamReader workbookStreamReader = null;

        try {
            final OPCPackage opcPackage = OPCPackage.open(inputStream);
            final XSSFReader xssfReader = new XSSFReader(opcPackage);
            workbookDataInputStream = xssfReader.getWorkbookData();
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            workbookStreamReader = xmlInputFactory.createXMLStreamReader(workbookDataInputStream);
            sharedStringsTable = xssfReader.getSharedStringsTable();

            /*
            sample sheet element:
            <sheets>
                <sheet name="Movies" sheetId="1" state="visible" r:id="rId2"/>
                <sheet name="Person" sheetId="2" state="visible" r:id="rId3"/>
            </sheets>
             */
            while (workbookStreamReader.hasNext()) {
                if (workbookStreamReader.next() == XMLStreamConstants.START_ELEMENT && "sheet".equals(workbookStreamReader.getLocalName())) {
                    final String shn = workbookStreamReader.getAttributeValue(null, "name");
                    final String shId = workbookStreamReader.getAttributeValue(null, "sheetId");
                    if ((sheetName != null && sheetName.equals(shn)) ||
                            (sheetName == null && String.valueOf(this.sheetIndex + 1).equals(shId))) {
                        //this is the target sheet
                        final String relationshipId = workbookStreamReader.getAttributeValue(schemaRelationships, "id");
                        sheetInputStream = xssfReader.getSheet(relationshipId);
                        sheetStreamReader = xmlInputFactory.createXMLStreamReader(sheetInputStream);
                        break;
                    }
                }
            }
        } finally {
            if (workbookDataInputStream != null) {
                try {
                    workbookDataInputStream.close();

                } catch (final Exception e) {
                    //ignore
                }
            }
            if (workbookStreamReader != null) {
                try {
                    workbookStreamReader.close();
                } catch (final Exception e) {
                    //ignore
                }
            }
        }

        /*
        sample row element:
        <row r="1" customFormat="false" ht="15" hidden="false" customHeight="false" outlineLevel="0" collapsed="false">
            <c r="A1" s="0" t="s">
                <v>0</v>
            </c>
            <c r="B1" s="0" t="s">
                <v>1</v>
            </c>
            <c r="C1" s="0" t="s">
                <v>2</v>
            </c>
            <c r="D1" s="0" t="s">
                <v>3</v>
            </c>
        </row>

        For inlineStr:
        <c r="A1" t="inlineStr">
            <is>
                <t>Date</t>
            </is>
        </c>

        Note: a blank cell does not show up in xml at all. So for list type beanType, need to detect blank cell and add
        null; for map or custom beanType, need to link to the correct header column by r attribute.
         */
        if (header == null) {
            headerMapping = new HashMap<String, String>();
            outerLoop:
            while (sheetStreamReader.hasNext()) {
                if (sheetStreamReader.next() == XMLStreamConstants.START_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                    final int rowNum = Integer.parseInt(sheetStreamReader.getAttributeValue(null, "r"));

                    if (headerRow + 1 == rowNum) {
                        // got the header row, next loop through header row cells
                        final List<String> headerVals = new ArrayList<String>();
                        while (sheetStreamReader.hasNext()) {
                            final int event = sheetStreamReader.next();
                            if (event == XMLStreamConstants.START_ELEMENT && "c".equals(sheetStreamReader.getLocalName())) {
                                final String label = getColumnLabel(sheetStreamReader.getAttributeValue(null, "r"));
                                final String value = getCellStringValue();
                                headerVals.add(value);
                                headerMapping.put(label, value);
                            } else if (event == XMLStreamConstants.END_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                                header = headerVals.toArray(new String[headerVals.size()]);
                                currentRowNum = rowNum - 1;
                                break outerLoop;
                            }
                        }
                    }
                }
            }
        }

        //fast forward to the start row, which may not immediately follow header row
        while (currentRowNum < startRowNumber - 1 && sheetStreamReader.hasNext()) {
            if (sheetStreamReader.next() == XMLStreamConstants.START_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                currentRowNum = Integer.parseInt(sheetStreamReader.getAttributeValue(null, "r")) - 1;
            } else if (sheetStreamReader.next() == XMLStreamConstants.END_ELEMENT && "row".equals(sheetStreamReader.getLocalName())) {
                if (currentRowNum >= startRowNumber - 1) {
                    break;
                }
            }
        }
    }

    private String getCellStringValue() throws Exception {
        String result = null;
        final String cellType = sheetStreamReader.getAttributeValue(null, "t");
        while (sheetStreamReader.hasNext()) {
            final int event = sheetStreamReader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "v".equals(sheetStreamReader.getLocalName())) {
                result = sheetStreamReader.getElementText();
                if ("s".equals(cellType)) {
                    final int idx = Integer.parseInt(result);
                    result = new XSSFRichTextString(sharedStringsTable.getEntryAt(idx)).toString();
                }
            } else if (event == XMLStreamConstants.START_ELEMENT && "t".equals(sheetStreamReader.getLocalName())) {
                result = sheetStreamReader.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && "c".equals(sheetStreamReader.getLocalName())) {
                break;
            }
        }
        return result;
    }

    // A1 -> A, B1 -> B
    //R1C1 -> 1, R2C3 -> 3
    private String getColumnLabel(final String cellPoint) {
        // a R1C1 notation: R1234C567
        if (r1c1 == null) {
            final char[] chars = cellPoint.toCharArray();
            r1c1 = chars[0] == 'R' && Character.isDigit(chars[1]);
            return getColumnLabel(cellPoint);
        } else if (r1c1) {
            return cellPoint.substring(cellPoint.indexOf('C') + 1);
        } else {
            for (int i = 0; i < cellPoint.length(); ++i) {
                if (Character.isDigit(cellPoint.charAt(i))) {
                    return cellPoint.substring(0, i);
                }
            }
            return cellPoint;
        }
    }

    private void initHeaderMapping() {
        if (r1c1) {
            for (int index = 0; index < header.length; ++index) {
                headerMapping.put(String.valueOf(index + 1), header[index]);
            }
        } else {
            for (int index = 0; index < header.length; ++index) {
                headerMapping.put(getColumnLabelByPosition(index), header[index]);
            }
        }
    }

    //utility borrowed from
    //http://stackoverflow.com/questions/837155/fastest-function-to-generate-excel-column-letters-in-c-sharp
    private static String getColumnLabelByPosition(final long index) {
        final char[] ret = new char[64];
        for (int i = 0; i < ret.length; ++i) {
            final int digit = ret.length - i - 1;
            final long test = index - powerDown(i + 1);
            if (test < 0)
                break;
            ret[digit] = toChar(test / (long) (Math.pow(26, i)));
        }
        return new String(ret);
    }

    private static char toChar(final long num) {
        return (char) ((num % 26) + 65);
    }

    private static long powerDown(int limit) {
        long acc = 0;
        while (limit > 1) {
            acc += Math.pow(26, limit-- - 1);
        }
        return acc;
    }
}
