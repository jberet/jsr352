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

import java.lang.reflect.Constructor;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

import org.jberet.support._private.SupportMessages;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.comment.CommentMatches;
import org.supercsv.encoder.CsvEncoder;
import org.supercsv.encoder.SelectiveCsvEncoder;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.quote.ColumnQuoteMode;
import org.supercsv.quote.QuoteMode;

import static org.jberet.support.io.CsvProperties.ALWAYS;
import static org.jberet.support.io.CsvProperties.COLUMN;
import static org.jberet.support.io.CsvProperties.COMMENT_MATCHER_KEY;
import static org.jberet.support.io.CsvProperties.DEFAULT;
import static org.jberet.support.io.CsvProperties.ENCODER_KEY;
import static org.jberet.support.io.CsvProperties.EXCEL_NORTH_EUROPE_PREFERENCE;
import static org.jberet.support.io.CsvProperties.EXCEL_PREFERENCE;
import static org.jberet.support.io.CsvProperties.MATCHES;
import static org.jberet.support.io.CsvProperties.MATCHES_FUZZY;
import static org.jberet.support.io.CsvProperties.PREFERENCE_KEY;
import static org.jberet.support.io.CsvProperties.SELECT;
import static org.jberet.support.io.CsvProperties.STANDARD_PREFERENCE;
import static org.jberet.support.io.CsvProperties.STARTS_WITH;
import static org.jberet.support.io.CsvProperties.STARTS_WITH_FUZZY;
import static org.jberet.support.io.CsvProperties.STARTS_WITH_FUZZY2;
import static org.jberet.support.io.CsvProperties.TAB_PREFERENCE;

/**
 * The base class for {@link org.jberet.support.io.CsvItemReader} and {@link org.jberet.support.io.CsvItemWriter}.
 * This class also holds common CSV-related batch artifact properties.
 *
 * @see     CsvItemReader
 * @see     CsvItemWriter
 * @since   1.0.0
 */
public abstract class CsvItemReaderWriterBase extends ItemReaderWriterBase {
    static final Class[] stringParameterTypes = {String.class};
    static final CellProcessor[] noCellProcessors = new CellProcessor[0];

    /**
     * Specify the bean fields or map keys corresponding to CSV columns. If the CSV columns exactly
     * match bean fields or map keys, then no need to specify this property.
     *
     * @see #getNameMapping()
     */
    @Inject
    @BatchProperty
    protected String[] nameMapping;

    /**
     * Specifies a fully-qualified class or interface name that maps to a row of the source CSV file.
     * For example,
     * <p>
     * <ul>
     * <li>{@code java.util.List}
     * <li>{@code java.util.Map}
     * <li>{@code org.jberet.support.io.Person}
     * <li>{@code my.own.BeanType}
     * </ul>
     */
    @Inject
    @BatchProperty
    protected Class beanType;

    /**
     * Specifies one of the 4 predefined CSV preferences:
     * <p>
     * <ul>
     * <li>{@code STANDARD_PREFERENCE}
     * <li>{@code EXCEL_PREFERENCE}
     * <li>{@code EXCEL_NORTH_EUROPE_PREFERENCE}
     * <li>{@code TAB_PREFERENCE}
     * </ul>
     *
     * @see #getCsvPreference()
     */
    @Inject
    @BatchProperty
    protected String preference;

    /**
     * The quote character (used when a cell contains special characters, such as the delimiter char, a quote char,
     * or spans multiple lines). See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     * The default quoteChar is double quote ("). If " is present in the CSV data cells, specify quoteChar to some
     * other characters, e.g., |.
     */
    @Inject
    @BatchProperty
    protected String quoteChar;

    /**
     * The delimiter character (separates each cell in a row).
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     */
    @Inject
    @BatchProperty
    protected String delimiterChar;

    /**
     * The end of line symbols to use when writing (Windows, Mac and Linux style line breaks are all supported when
     * reading, so this preference won't be used at all for reading).
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     */
    @Inject
    @BatchProperty
    protected String endOfLineSymbols;

    /**
     * Whether spaces surrounding a cell need quotes in order to be preserved (see below). The default value is
     * false (quotes aren't required).
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     */
    @Inject
    @BatchProperty
    protected String surroundingSpacesNeedQuotes;

    /**
     * Specifies a {@code CommentMatcher} for reading CSV resource. The {@code CommentMatcher}
     * determines whether a line should be considered a comment. For example,
     * <p>
     * <ul>
     *     <li>"startsWith #"
     *     <li>"matches 'regexp'"
     *     <li>"my.own.CommentMatcherImpl"
     * </ul>
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     * @see #getCommentMatcher(String)
     */
    @Inject
    @BatchProperty
    protected String commentMatcher;

    /**
     * Specifies a custom encoder when writing CSV. For example,
     * <p>
     * <ul>
     *     <li>default
     *     <li>select 1, 2, 3
     *     <li>select true, true, false
     *     <li>column 1, 2, 3
     *     <li>column true, true, false
     *     <li>my.own.MyCsvEncoder
     * </ul>
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     * @see #getEncoder(String)
     */
    @Inject
    @BatchProperty
    protected String encoder;

    /**
     * Allows you to enable surrounding quotes for writing (if a column wouldn't normally be quoted because
     * it doesn't contain special characters). For example,
     * <p>
     * <ul>
     *     <li>default
     *     <li>always
     *     <li>select 1, 2, 3
     *     <li>select true, true, false
     *     <li>column 1, 2, 3
     *     <li>column true, true, false
     *     <li>my.own.MyQuoteMode
     * </ul>
     *
     * @see <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>
     * @see #getQuoteMode(String)
     */
    @Inject
    @BatchProperty
    protected String quoteMode;

    /**
     * Specifies a list of cell processors, one for each column. See
     * <a href="http://supercsv.sourceforge.net/cell_processors.html">Super CSV docs</a> for supported cell processor
     * types. The rules and syntax are as follows:
     * <p>
     * <ul>
     * <li>The size of the resultant list must equal to the number of CSV columns.
     * <li>Cell processors appear in the same order as CSV columns.
     * <li>If no cell processor is needed for a column, enter null.
     * <li>Each column may have null, 1, 2, or multiple cell processors, separated by comma (,)
     * <li>Cell processors for different columns must be separated with semi-colon (;).
     * <li>Cell processors may contain parameters enclosed in parenthesis, and multiple parameters are separated with comma (,).
     * <li>string literals in cell processor parameters must be enclosed within single quotes, e.g., 'xxx'
     * </ul>
     * <p>
     * For example, to specify cell processors for 5-column CSV:
     * <pre>
     * value = "
     *      null;
     *      Optional, StrMinMax(1, 20);
     *      ParseLong;
     *      NotNull;
     *      Optional, ParseDate('dd/MM/yyyy')
     * "
     * </pre>
     *
     * @see <a href="http://supercsv.sourceforge.net/cell_processors.html">Super CSV docs</a>
     * @see #getCellProcessors()
     */
    @Inject
    @BatchProperty
    protected String cellProcessors;

    /**
     * The name of the character set to be used for reading and writing data, e.g., UTF-8. This property is optional,
     * and if not set, the platform default charset is used.
     */
    @Inject
    @BatchProperty
    protected String charset;

    protected CellProcessor[] cellProcessorInstances;

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
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, preference, PREFERENCE_KEY);
        }

        //do not trim quoteChar or delimiterChar. They can be tab (\t) and after trim, it will be just empty
        if (quoteChar != null || delimiterChar != null || endOfLineSymbols != null ||
                surroundingSpacesNeedQuotes != null || commentMatcher != null || encoder != null || quoteMode != null) {
            final CsvPreference.Builder builder = new CsvPreference.Builder(
                    quoteChar == null ? (char) csvPreference.getQuoteChar() : quoteChar.charAt(0),
                    delimiterChar == null ? csvPreference.getDelimiterChar() : (int) delimiterChar.charAt(0),
                    endOfLineSymbols == null ? csvPreference.getEndOfLineSymbols() : endOfLineSymbols.trim()
            );
            if (surroundingSpacesNeedQuotes != null) {
                builder.surroundingSpacesNeedQuotes(Boolean.parseBoolean(surroundingSpacesNeedQuotes.trim()));
            }
            if (commentMatcher != null) {
                builder.skipComments(getCommentMatcher(commentMatcher));
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
     * Gets the cell processors for reading CSV resource.  The default implementation returns an empty array,
     * and subclasses may override it to provide more meaningful cell processors.
     *
     * @return an array of cell processors
     */
    protected CellProcessor[] getCellProcessors() {
        if (this.cellProcessors == null) {
            return CsvItemReaderWriterBase.noCellProcessors;
        }
        return CellProcessorConfig.parseCellProcessors(this.cellProcessors.trim());
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
     *            <p>
     *            <ul>
     *            <li>default
     *            <li>always
     *            <li>select 1, 2, 3
     *            <li>select true, true, false
     *            <li>column 1, 2, 3
     *            <li>column true, true, false
     *            <li>my.own.MyQuoteMode
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
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, val, ENCODER_KEY);
            }
        }
    }

    /**
     * Gets the configured {@code org.supercsv.encoder.CsvEncoder}.
     *
     * @param val property value of encoder property in this batch artifact. For example,
     *            <p>
     *            <ul>
     *            <li>default
     *            <li>select 1, 2, 3
     *            <li>select true, true, false
     *            <li>column 1, 2, 3
     *            <li>column true, true, false
     *            <li>my.own.MyCsvEncoder
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
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, val, ENCODER_KEY);
            }
        }
    }

    /**
     * Gets the configured {@code org.supercsv.comment.CommentMatcher}.
     *
     * @param val property value of commentMatcher property in this batch artifact. For example,
     *            <p>
     *            <ul>
     *            <li>starts with '#'
     *            <li>startswith  '##'
     *            <li>startsWith  '##'
     *            <li>matches '#.*#'
     *            <li>my.own.MyCommentMatcher
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
                throw SupportMessages.MESSAGES.missingQuote(val);
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
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, val, COMMENT_MATCHER_KEY);
        }

        return commentMatcher;
    }

    private static <T> T loadAndInstantiate(final String className, final String contextVal, final String param) {
        try {
            final Class<?> aClass = CsvItemReaderWriterBase.class.getClassLoader().loadClass(className);
            if (param == null) {
                return (T) aClass.newInstance();
            } else {
                final Constructor<?> constructor = aClass.getConstructor(CsvItemReaderWriterBase.stringParameterTypes);
                return (T) constructor.newInstance(param);
            }
        } catch (final Exception e) {
            throw SupportMessages.MESSAGES.failToLoadOrCreateCustomType(e, contextVal);
        }
    }


    static int[] convertToIntParams(final String[] strings, final int start, final int count) {
        final int[] ints = new int[count];
        for (int i = start, j = 0; j < count && i < strings.length; i++, j++) {
            ints[j] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    private static boolean[] convertToBooleanParams(final String[] strings) {
        final boolean[] booleans = new boolean[strings.length - 1];
        for (int i = 1; i < strings.length; i++) {
            booleans[i - 1] = Boolean.parseBoolean(strings[i]);
        }
        return booleans;
    }
}
