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
public class CsvProperties {
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
     * The property key to specify a fully-qualified class or interface name that maps to a row of the source CSV file
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
     * The property key to specify target bean fields.
     */
    public static final String NAME_MAPPING_KEY = "nameMapping";
}
