/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.comment.CommentMatches;
import org.supercsv.encoder.CsvEncoder;
import org.supercsv.encoder.SelectiveCsvEncoder;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.quote.ColumnQuoteMode;
import org.supercsv.quote.QuoteMode;

import static org.jberet.support.io.CsvProperties.ALWAYS;
import static org.jberet.support.io.CsvProperties.BEAN_TYPE_KEY;
import static org.jberet.support.io.CsvProperties.COLUMN;
import static org.jberet.support.io.CsvProperties.DEFAULT;
import static org.jberet.support.io.CsvProperties.ENCODER_KEY;
import static org.jberet.support.io.CsvProperties.EXCEL_NORTH_EUROPE_PREFERENCE;
import static org.jberet.support.io.CsvProperties.EXCEL_PREFERENCE;
import static org.jberet.support.io.CsvProperties.MATCHES;
import static org.jberet.support.io.CsvProperties.MATCHES_FUZZY;
import static org.jberet.support.io.CsvProperties.NAME_MAPPING_KEY;
import static org.jberet.support.io.CsvProperties.PREFERENCE_KEY;
import static org.jberet.support.io.CsvProperties.RESOURCE_KEY;
import static org.jberet.support.io.CsvProperties.SELECT;
import static org.jberet.support.io.CsvProperties.SKIP_COMMENTS_KEY;
import static org.jberet.support.io.CsvProperties.STANDARD_PREFERENCE;
import static org.jberet.support.io.CsvProperties.STARTS_WITH;
import static org.jberet.support.io.CsvProperties.STARTS_WITH_FUZZY;
import static org.jberet.support.io.CsvProperties.STARTS_WITH_FUZZY2;
import static org.jberet.support.io.CsvProperties.TAB_PREFERENCE;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads from a CSV resource into a user-defined
 * bean, java.util.List&lt;String&gt;, or java.util.Map&lt;String, String&gt;. Data files delimited with characters
 * other than comma (e.g., tab, |) are also supported by configuring {@code preference}, {@code delimiterChar},
 * or {@code quoteChar} properties in job xml.
 */
@Named
public class CsvItemReader implements ItemReader {
    private static final Class[] stringParameterTypes = {String.class};
    private static final CellProcessor[] noCellProcessors = new CellProcessor[0];
    static final String[] EMPTY_STRING_ARRAY = new String[0];

    @Inject
    @BatchProperty
    protected int start;

    @Inject
    @BatchProperty
    protected int end;

    @Inject
    @BatchProperty
    protected Class beanType;

    @Inject
    @BatchProperty
    protected String[] nameMapping;

    @Inject
    @BatchProperty
    protected String resource;

    @Inject
    @BatchProperty
    protected String preference;

    @Inject
    @BatchProperty
    protected String quoteChar;

    @Inject
    @BatchProperty
    protected String delimiterChar;

    @Inject
    @BatchProperty
    protected String endOfLineSymbols;

    @Inject
    @BatchProperty
    protected String surroundingSpacesNeedQuotes;

    @Inject
    @BatchProperty
    protected String skipComments;

    @Inject
    @BatchProperty
    protected String encoder;

    @Inject
    @BatchProperty
    protected String quoteMode;

    @Inject
    @BatchProperty
    protected String cellProcessors;

    protected ICsvReader delegateReader;

    private CellProcessor[] cellProcessorInstances;

    @Override
    public void open(final Serializable checkpoint) {
        /**
         * The row number to start reading.  It may be different from the injected field start. During a restart,
         * we would start reading from where it ended during the last run.
         */
        final int startRowNumber;
        if (checkpoint == null) {
            startRowNumber = this.start;
        } else {
            startRowNumber = (Integer) checkpoint;
        }
        if (startRowNumber < this.start || startRowNumber > this.end) {
            throw SupportLogger.LOGGER.invalidStartPosition(startRowNumber, this.start, this.end);
        }
        if (beanType == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(null, BEAN_TYPE_KEY);
        } else if (java.util.List.class.isAssignableFrom(beanType)) {
            delegateReader = new FastForwardCsvListReader(getInputReader(), getCsvPreference(), startRowNumber);
        } else if (java.util.Map.class.isAssignableFrom(beanType)) {
            delegateReader = new FastForwardCsvMapReader(getInputReader(), getCsvPreference(), startRowNumber);
        } else {
            delegateReader = new FastForwardCsvBeanReader(getInputReader(), getCsvPreference(), startRowNumber);
        }

        final String[] header;
        try {
            header = delegateReader.getHeader(true);//first line check true
            if (this.nameMapping == null) {
                this.nameMapping = header;
            }
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToReadCsvHeader(e, resource);
        }
        if (this.end == 0) {
            this.end = Integer.MAX_VALUE;
        }
        if (nameMapping == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(null, NAME_MAPPING_KEY);
        }
        this.cellProcessorInstances = getCellProcessors(header);
    }

    @Override
    public void close() throws Exception {
        delegateReader.close();
    }

    @Override
    public Object readItem() throws Exception {
        if (delegateReader.getRowNumber() > this.end) {
            return null;
        }
        final Object result;
        if (delegateReader instanceof org.supercsv.io.ICsvBeanReader) {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvBeanReader) delegateReader).read(beanType, getNameMapping());
            } else {
                result = ((ICsvBeanReader) delegateReader).read(beanType, getNameMapping(), cellProcessorInstances);
            }
        } else if (delegateReader instanceof ICsvListReader) {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvListReader) delegateReader).read();
            } else {
                result = ((ICsvListReader) delegateReader).read(cellProcessorInstances);
            }
        } else {
            if (cellProcessorInstances.length == 0) {
                result = ((ICsvMapReader) delegateReader).read(getNameMapping());
            } else {
                result = ((ICsvMapReader) delegateReader).read(getNameMapping(), cellProcessorInstances);
            }
        }
        return result;
    }

    @Override
    public Integer checkpointInfo() throws Exception {
        return delegateReader.getRowNumber();
    }

    /**
     * Creates or obtains {@code org.supercsv.prefs.CsvPreference} according to the configuration in JSL document.
     *
     * @return CsvPreference
     */
    protected CsvPreference getCsvPreference() {
        CsvPreference csvPreference;
        if (preference == null || STANDARD_PREFERENCE.equals(preference)) {
            csvPreference = CsvPreference.STANDARD_PREFERENCE;
        } else if (EXCEL_PREFERENCE.equals(preference)) {
            csvPreference = CsvPreference.EXCEL_PREFERENCE;
        } else if (EXCEL_NORTH_EUROPE_PREFERENCE.equals(preference)) {
            csvPreference = CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE;
        } else if (TAB_PREFERENCE.equals(preference)) {
            csvPreference = CsvPreference.TAB_PREFERENCE;
        } else {
            throw SupportLogger.LOGGER.invalidCsvPreference(preference, PREFERENCE_KEY);
        }

        //do not trim quoteChar or delimiterChar. They can be tab (\t) and after trim, it will be just empty
        if (quoteChar != null || delimiterChar != null || endOfLineSymbols != null ||
                surroundingSpacesNeedQuotes != null || skipComments != null || encoder != null || quoteMode != null) {
            final CsvPreference.Builder builder = new CsvPreference.Builder(
                    quoteChar == null ? (char) csvPreference.getQuoteChar() : quoteChar.charAt(0),
                    delimiterChar == null ? csvPreference.getDelimiterChar() : (int) delimiterChar.charAt(0),
                    endOfLineSymbols == null ? csvPreference.getEndOfLineSymbols() : endOfLineSymbols.trim()
            );
            if (surroundingSpacesNeedQuotes != null) {
                builder.surroundingSpacesNeedQuotes(Boolean.parseBoolean(surroundingSpacesNeedQuotes.trim()));
            }
            if (skipComments != null) {
                builder.skipComments(getCommentMatcher(skipComments));
            }
            if (encoder != null) {
                final CsvEncoder encoder1 = getEncoder(encoder);
                if (encoder1 != null) {
                    builder.useEncoder(encoder1);
                }
            }
            if (quoteMode != null) {
                final QuoteMode quoteMode1 = getQuoteMode(quoteMode);
                if (quoteMode1 != null) {
                    builder.useQuoteMode(quoteMode1);
                }
            }
            csvPreference = builder.build();
        }
        return csvPreference;
    }

    /**
     * Gets an instance of {@code java.io.Reader} that represents the CSV resource.
     *
     * @return {@code java.io.Reader} that represents the CSV resource
     */
    protected Reader getInputReader() {
        if (resource == null) {
            throw SupportLogger.LOGGER.invalidCsvPreference(resource, RESOURCE_KEY);
        }
        final UnicodeBOMInputStream bomin;
        try {
            InputStream inputStream;
            try {
                final URL url = new URL(resource);
                inputStream = url.openStream();
            } catch (final MalformedURLException e) {
                SupportLogger.LOGGER.notUrl(e, resource);
                final File file = new File(resource);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    SupportLogger.LOGGER.notFile(resource);
                    inputStream = CsvItemReader.class.getClassLoader().getResourceAsStream(resource);
                }
            }
            bomin = new UnicodeBOMInputStream(inputStream);
            bomin.skipBOM();
        } catch (final IOException e) {
            throw SupportLogger.LOGGER.failToOpenStream(e, resource);
        }
        return new InputStreamReader(bomin);
    }

    /**
     * Gets the cell processors for reading CSV resource.  The default implementation returns an empty array,
     * and subclasses may override it to provide more meaningful cell processors.
     *
     * @return an array of cell processors
     */
    protected CellProcessor[] getCellProcessors(final String[] header) {
        if (this.cellProcessors == null) {
            return noCellProcessors;
        }
        return CellProcessorConfig.parseCellProcessors(this.cellProcessors.trim(), header);
    }

    /**
     * Gets the field names of the target bean, if they differ from the CSV header, or if there is no CSV header.
     *
     * @return an string array of field names of the target bean. Return null if CSV header exactly match the bean
     * field.
     */
    protected String[] getNameMapping() {
        return this.nameMapping;
    }

    /**
     * Gets the configured {@code org.supercsv.quote.QuoteMode}.
     *
     * @param val property value of quoteMode property in this batch artifact. For example,
     *            <ul>
     *            <li>default</li>
     *            <li>always</li>
     *            <li>select 1, 2, 3</li>
     *            <li>select true, true, false</li>
     *            <li>column 1, 2, 3</li>
     *            <li>column true, true, false</li>
     *            <li>my.own.MyQuoteMode</li>
     *            </ul>
     * @return a QuoteMode
     */
    protected QuoteMode getQuoteMode(final String val) {
        final String[] parts = val.split("[,\\s]+");
        final String quoteModeName;
        if (parts.length == 1) {
            //there is only 1 chunk, either default, always, or custom encoder
            quoteModeName = parts[0];
            if (quoteModeName.equalsIgnoreCase(DEFAULT)) {
                return null;
            } else if (quoteModeName.equalsIgnoreCase(ALWAYS)) {
                return new AlwaysQuoteMode();
            } else {
                return loadAndInstantiate(quoteModeName, val, null);
            }
        } else {
            quoteModeName = parts[0];
            final String encoderNameLowerCase = quoteModeName.toLowerCase();
            if (encoderNameLowerCase.startsWith(SELECT) || encoderNameLowerCase.startsWith(COLUMN)) {
                try {
                    Integer.parseInt(parts[1]);
                    return new ColumnQuoteMode(convertToIntParams(parts, 1, parts.length - 1));
                } catch (final NumberFormatException e) {
                    return new ColumnQuoteMode(convertToBooleanParams(parts));
                }
            } else {
                throw SupportLogger.LOGGER.invalidCsvPreference(val, ENCODER_KEY);
            }
        }
    }

    /**
     * Gets the configured {@code org.supercsv.encoder.CsvEncoder}.
     *
     * @param val property value of encoder property in this batch artifact. For example,
     *            <ul>
     *            <li>default</li>
     *            <li>select 1, 2, 3</li>
     *            <li>select true, true, false</li>
     *            <li>column 1, 2, 3</li>
     *            <li>column true, true, false</li>
     *            <li>my.own.MyCsvEncoder</li>
     *            </ul>
     * @return a {@code CsvEncoder}
     */
    protected CsvEncoder getEncoder(final String val) {
        final String[] parts = val.split("[,\\s]+");
        final String encoderName;
        if (parts.length == 1) {
            //there is only 1 chunk, either default, or custom encoder
            encoderName = parts[0];
            if (encoderName.equalsIgnoreCase(DEFAULT)) {
                return null;
            } else {
                return loadAndInstantiate(encoderName, val, null);
            }
        } else {
            encoderName = parts[0];
            final String encoderNameLowerCase = encoderName.toLowerCase();
            if (encoderNameLowerCase.startsWith(SELECT) || encoderNameLowerCase.startsWith(COLUMN)) {
                try {
                    Integer.parseInt(parts[1]);
                    return new SelectiveCsvEncoder(convertToIntParams(parts, 1, parts.length - 1));
                } catch (final NumberFormatException e) {
                    return new SelectiveCsvEncoder(convertToBooleanParams(parts));
                }
            } else {
                throw SupportLogger.LOGGER.invalidCsvPreference(val, ENCODER_KEY);
            }
        }
    }

    /**
     * Gets the configured {@code org.supercsv.comment.CommentMatcher}.
     *
     * @param val property value of skipComments property in this batch artifact. For example,
     *            <ul>
     *            <li>starts with '#'</li>
     *            <li>startswith  '##'</li>
     *            <li>startsWith  '##'</li>
     *            <li>matches '#.*#'</li>
     *            <li>my.own.MyCommentMatcher</li>
     *            </ul>
     * @return a {@code CommentMatcher}
     */
    protected CommentMatcher getCommentMatcher(String val) {
        val = val.trim();
        final char paramQuoteChar = '\'';
        final int singleQuote1 = val.indexOf(paramQuoteChar);
        String matcherName = null;
        String matcherParam = null;
        if (singleQuote1 < 0) {
            final String[] parts = val.split("\\s");
            if (parts.length == 1) {
                //there is only 1 chunk, assume it's the custom CommentMatcher type
                return loadAndInstantiate(parts[0], val, null);
            } else if (parts.length == 2) {
                matcherName = parts[0];
                matcherParam = parts[1];
            } else {
                throw SupportLogger.LOGGER.missingQuote(val);
            }
        }
        if (matcherName == null) {
            matcherName = val.substring(0, singleQuote1 - 1).trim();
            final int paramQuoteCharEnd = val.lastIndexOf(paramQuoteChar);
            matcherParam = val.substring(singleQuote1 + 1, paramQuoteCharEnd);
            matcherName = matcherName.split("\\s")[0];
        }

        final CommentMatcher commentMatcher;
        if (matcherName.equalsIgnoreCase(STARTS_WITH) || matcherName.equalsIgnoreCase(STARTS_WITH_FUZZY)
                || matcherName.equalsIgnoreCase(STARTS_WITH_FUZZY2)) {
            commentMatcher = new org.supercsv.comment.CommentStartsWith(matcherParam);
        } else if (matcherName.equalsIgnoreCase(MATCHES) || matcherName.equalsIgnoreCase(MATCHES_FUZZY)) {
            commentMatcher = new CommentMatches(matcherParam);
        } else {
            throw SupportLogger.LOGGER.invalidCsvPreference(val, SKIP_COMMENTS_KEY);
        }

        return commentMatcher;
    }

    private <T> T loadAndInstantiate(final String className, final String contextVal, final String param) {
        try {
            final Class<?> aClass = CsvItemReader.class.getClassLoader().loadClass(className);
            if (param == null) {
                return (T) aClass.newInstance();
            } else {
                final Constructor<?> constructor = aClass.getConstructor(stringParameterTypes);
                return (T) constructor.newInstance(param);
            }
        } catch (final Exception e) {
            throw SupportLogger.LOGGER.failToLoadOrCreateCustomType(e, contextVal);
        }
    }

    static int[] convertToIntParams(final String[] strings, final int start, final int count) {
        final int[] ints = new int[count];
        for (int i = start, j = 0; j < count && i < strings.length; i++, j++) {
            ints[j] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    private boolean[] convertToBooleanParams(final String[] strings) {
        final boolean[] booleans = new boolean[strings.length - 1];
        for (int i = 1; i < strings.length; i++) {
            booleans[i - 1] = Boolean.parseBoolean(strings[i]);
        }
        return booleans;
    }
}
