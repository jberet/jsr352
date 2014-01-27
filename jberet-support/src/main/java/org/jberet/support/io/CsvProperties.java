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

/**
 * A collection of property keys and values used to configure CSV-related batch artifacts in JSL document.
 */
public final class CsvProperties {
    /**
     * The property key to specify the start position in the data set
     */
    public static final String START_KEY = "start";

    /**
     * The property key to specify the end position in the data set (inclusive)
     */
    public static final String END_KEY = "end";

    /**
     * The property key to specify one of the 4 predefined CSV preferences: {@code STANDARD_PREFERENCE},
     * {@code EXCEL_PREFERENCE}, {@code EXCEL_NORTH_EUROPE_PREFERENCE}, {@code TAB_PREFERENCE}.
     */
    public static final String PREFERENCE_KEY = "preference";

    /**
     * The string property value to specify the use of {@code org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE}
     */
    public static final String STANDARD_PREFERENCE = "STANDARD_PREFERENCE";

    /**
     * The string property value to specify the use of {@code org.supercsv.prefs.CsvPreference.EXCEL_PREFERENCE}
     */
    public static final String EXCEL_PREFERENCE = "EXCEL_PREFERENCE";

    /**
     * The string property value to specify the use of {@code org.supercsv.prefs.CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE}
     */
    public static final String EXCEL_NORTH_EUROPE_PREFERENCE = "EXCEL_NORTH_EUROPE_PREFERENCE";

    /**
     * The string property value to specify the use of {@code org.supercsv.prefs.CsvPreference.TAB_PREFERENCE}
     */
    public static final String TAB_PREFERENCE = "TAB_PREFERENCE";

    /**
     * The property key to specify a fully-qualified class or interface name that maps to a row of the source CSV file.
     * For example, java.util.List, java.util.Map, org.jberet.support.io.Person, my.own.BeanType, etc.
     */
    public static final String BEAN_TYPE_KEY = "beanType";

    /**
     * The property key to specify the location of the CSV file.  The value can be file path, URL, or any resource that
     * can be loaded with application class loader.
     */
    public static final String RESOURCE_KEY = "resource";

    /**
     * The quote character (used when a cell contains special characters, such as the delimiter char, a quote char,
     * or spans multiple lines). See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String QUOTE_CHAR_KEY = "quoteChar";

    /**
     * The delimiter character (separates each cell in a row).
     * See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String DELIMITER_CHAR_KEY = "delimiterChar";

    /**
     * The end of line symbols to use when writing (Windows, Mac and Linux style line breaks are all supported when
     * reading, so this preference won't be used at all for reading).
     * See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String END_OF_LINE_SYMBOLS_KEY = "endOfLineSymbols";

    /**
     * Whether spaces surrounding a cell need quotes in order to be preserved (see below). The default value is
     * false (quotes aren't required). See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String SURROUNDING_SPACES_NEED_QUOTES_KEY = "surroundingSpacesNeedQuotes";

    /**
     * Skips comments (what makes up a comment is determined by the CommentMatcher you supply). See the section
     * on skipping comments below for more information.
     * See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String SKIP_COMMENTS_KEY = "skipComments";

    /**
     * The property value to indicate the use of a {@code org.supercsv.comment.CommentStartsWith} comment matcher.
     * For example,
     * <ul>
     * <li>"startsWith '#'"</li>
     * <li>"starts with '##'"</li>
     * <li>"start with '###'"</li>
     * </ul>
     */
    public static final String STARTS_WITH = "startsWith";

    /**
     * A fuzzy variant of STARTS_WITH.
     */
    public static final String STARTS_WITH_FUZZY = "starts";

    /**
     * A fuzzy variant of STARTS_WITH.
     */
    public static final String STARTS_WITH_FUZZY2 = "start";

    /**
     * The property value to indicate the use of a {@code org.supercsv.comment.CommentMatches} comment matcher.
     * For example,
     * <ul>
     * <li>"matches 'some regexp'"</li>
     * <li>"match 'some regexp'"</li>
     * </ul>
     */
    public static final String MATCHES = "matches";

    /**
     * A fuzzy variant of MATCHES.
     */
    public static final String MATCHES_FUZZY = "match";

    /**
     * Use your own encoder when writing CSV. See the section on custom encoders below.
     * See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String ENCODER_KEY = "encoder";

    /**
     * Allows you to enable surrounding quotes for writing (if a column wouldn't normally be quoted because
     * it doesn't contain special characters). See the section on quote modes below.
     * See <a href="http://supercsv.sourceforge.net/preferences.html">CSV Preferences</a>.
     */
    public static final String QUOTE_MODE_KEY = "quoteMode";

    /**
     * The property value to indicate the default behavior for the current property. For example, the default
     * quoteMode, or the default encoder.
     */
    public static final String DEFAULT = "default";

    /**
     * The property value to indicate always enable surrounding quotes for writing.
     */
    public static final String ALWAYS = "always";

    /**
     * The property value to indicate selected columns.  For example,
     * <ul>
     * <li>"select true, true, false, false"</li>
     * <li>"select 1, 2, 3, 4</li>
     * </ul>
     */
    public static final String SELECT = "select";

    /**
     * The property value to indicate selected columns.  For example,
     * <ul>
     * <li>"column true, true, false, false"</li>
     * <li>"column 1, 2, 3, 4</li>
     * </ul>
     */
    public static final String COLUMN = "column";

    /**
     * The property key to specify target bean fields.
     */
    public static final String NAME_MAPPING_KEY = "nameMapping";

    /**
     * The property key to specify a list of cell processors, one for each column. See
     * <a href="http://supercsv.sourceforge.net/cell_processors.html">Super CSV docs</a> for supported cell processor
     * types. The rules and syntax are as follows:
     * <ul>
     * <li>The size of the resultant list must equal to the number of CSV columns.</li>
     * <li>Cell processors appear in the same order as CSV columns.</li>
     * <li>If no cell processor is needed for a column, enter null.</li>
     * <li>Each column may have null, 1, 2, or multiple cell processors, separated by comma (,)</li>
     * <li>Cell processors for each column must appear in its own line, and cell processors for different
     * columns must be separated with new line characters.</li>
     * <li>Cell processors may contain parameters enclosed in parenthesis, and multiple parameters are separated with comma (,).</li>
     * <li>string literals in cell processor parameters must be enclosed within single quotes, e.g., 'xxx'</li>
     * </ul>
     * For example, to specify cell processors for 5-column CSV:
     * <pre>
     * value = "
     *      null
     *      Optional, StrMinMax(1, 20)
     *      ParseLong
     *      NotNull
     *      Optional, ParseDate('dd/MM/yyyy')
     * "
     * </pre>
     */
    public static final String CELL_PROCESSORS_KEY = "cellProcessors";
}
