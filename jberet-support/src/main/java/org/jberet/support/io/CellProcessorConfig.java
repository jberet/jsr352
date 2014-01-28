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
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jberet.support._private.SupportLogger;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.FmtBool;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.FmtNumber;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseChar;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.StrReplace;
import org.supercsv.cellprocessor.Token;
import org.supercsv.cellprocessor.Trim;
import org.supercsv.cellprocessor.Truncate;
import org.supercsv.cellprocessor.constraint.DMinMax;
import org.supercsv.cellprocessor.constraint.Equals;
import org.supercsv.cellprocessor.constraint.ForbidSubStr;
import org.supercsv.cellprocessor.constraint.IsElementOf;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.LMinMax;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.RequireHashCode;
import org.supercsv.cellprocessor.constraint.RequireSubStr;
import org.supercsv.cellprocessor.constraint.StrMinMax;
import org.supercsv.cellprocessor.constraint.StrNotNullOrEmpty;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.constraint.Strlen;
import org.supercsv.cellprocessor.constraint.Unique;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.BoolCellProcessor;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.cellprocessor.ift.DoubleCellProcessor;
import org.supercsv.cellprocessor.ift.LongCellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;

/**
 * This class is responsible for parsing the cellProcessors configuration property value into an array of
 * {@code org.supercsv.cellprocessor.ift.CellProcessor}, which can be consumed by
 * {@code org.jberet.support.io.CsvItemReader}.
 */
class CellProcessorConfig {

    /**
     * Parses the property value for cellProcessors into an array of {@code CellProcessor}. The number of the
     * returned {@code CellProcessor} must equal to the number of headers.
     *
     * @param val    the raw property value. For example,
     *               value = "
     *               null
     *               Optional, StrMinMax(1, 20)
     *               ParseLong()
     *               NotNull
     *               ParseDate( 'dd/MM/yyyy' )
     *               StrMinMax(1, 20)
     *               Optional, StrMinMax(1, 20), ParseDate('dd/MM/yyyy')
     *               "
     * @param header the headers for the CSV
     * @return an array of {@code CellProcessor}, one for each line in the raw property value
     */
    static CellProcessor[] parseCellProcessors(final String val, final String[] header) {
        //final String[] parts = val.split("\\r?\\n");  //new line
        final String[] parts = val.split(";");
        if (parts.length != header.length) {
            throw SupportLogger.LOGGER.numberOfCellProcessorsAndHeaderDiff(parts.length, header.length);
        }
        final CellProcessor[] result = new CellProcessor[parts.length];

        for (int x = 0; x < parts.length; x++) { // start parsing all lines
            final String line = parts[x].trim();
            final char[] chars = line.toCharArray();
            final List<List<String>> processorValuesInThisLine = new ArrayList<List<String>>();
            List<String> oneProcessorValue = new ArrayList<String>();
            int processorStartPosition = 0;
            int paramStartPosition = 0;
            byte insideParams = 0;
            byte insideQuote = 0;

            // start parsing a line
            int i;
            for (i = 0; i < chars.length; i++) {
                final char ch = chars[i];
                switch (ch) {
                    case '(':
                        if (insideQuote == 0) {
                            if (insideParams == 0) {
                                insideParams++;
                                // add the processor name as the first element
                                final String s = line.substring(processorStartPosition, i).trim();
                                if (!s.isEmpty()) {
                                    oneProcessorValue.add(s);
                                }
                                paramStartPosition = i + 1;
                            } else {
                                throw SupportLogger.LOGGER.unexpectedChar(ch, i, line);
                            }
                        }
                        break;
                    case ')':
                        if (insideQuote == 0) {
                            if (insideParams == 1) {
                                insideParams--;
                                //end of param
                                addParam(line, paramStartPosition, i, oneProcessorValue);
                                //end of current processor
                                endCurrentProcessor(line, processorStartPosition, i, oneProcessorValue, processorValuesInThisLine, true);
                                processorStartPosition = i + 1;
                                oneProcessorValue = new ArrayList<String>();
                            } else {
                                throw SupportLogger.LOGGER.unexpectedChar(ch, i, line);
                            }
                        }
                        break;
                    case '\'':
                        if (insideQuote == 0) {
                            insideQuote++;
                        } else if (insideQuote == 1) {
                            insideQuote--;
                        } else {
                            throw SupportLogger.LOGGER.unexpectedChar(ch, i, line);
                        }
                        break;
                    case ',':
                        if (insideQuote == 0) {
                            if (insideParams == 0) {
                                //end of current processor
                                endCurrentProcessor(line, processorStartPosition, i, oneProcessorValue, processorValuesInThisLine, false);
                                processorStartPosition = i + 1;
                                oneProcessorValue = new ArrayList<String>();
                            } else if (insideParams == 1) {
                                // add the param to the current processor
                                addParam(line, paramStartPosition, i, oneProcessorValue);
                                paramStartPosition = i + 1;
                            } else {
                                throw SupportLogger.LOGGER.unexpectedChar(ch, i, line);
                            }
                        }
                        break;
                }
            } //end parsing a line
            if (processorStartPosition < i) {
                oneProcessorValue.add(line.substring(processorStartPosition, i).trim());
            }
            if (!oneProcessorValue.isEmpty() && !processorValuesInThisLine.contains(oneProcessorValue)) {
                processorValuesInThisLine.add(oneProcessorValue);
            }
            result[x] = createCellProcessorForOneLine(processorValuesInThisLine);
        } //end parsing all lines

        return result;
    }

    private static void endCurrentProcessor(final String line,
                                            final int processorStartPosition,
                                            final int currentPosition,
                                            final List<String> oneProcessorValue,
                                            final List<List<String>> processorValuesInThisLine,
                                            final boolean endsWithParenthesis) {
        //if endsWithParenthesis, the processor name and params have already been recorded when ) is encountered, so
        //skip the following step.
        if (!endsWithParenthesis) {
            final String s = line.substring(processorStartPosition, currentPosition).trim();
            if (!s.isEmpty()) {
                oneProcessorValue.add(s);
            }
        }
        if (!oneProcessorValue.isEmpty()) {
            processorValuesInThisLine.add(oneProcessorValue);
        }
    }

    private static void addParam(final String line,
                                 final int paramStartPosition,
                                 final int currentPosition,
                                 final List<String> oneProcessorValue) {
        String s = line.substring(paramStartPosition, currentPosition).trim();
        if (!s.isEmpty()) {
            if (s.startsWith("'")) {
                if (s.endsWith("'")) {
                    s = s.substring(1, s.length() - 1);
                } else {
                    throw SupportLogger.LOGGER.missingQuote(line);
                }
            }
            oneProcessorValue.add(s);
        }
    }

    static CellProcessor createCellProcessorForOneLine(final List<List<String>> processorValuesInThisLine) {
        CellProcessor previous;
        CellProcessor current = null;
        for (int x = processorValuesInThisLine.size() - 1; x >= 0; x--) {
            previous = current;
            current = null;
            final List<String> oneProcessorValue = processorValuesInThisLine.get(x);
            SupportLogger.LOGGER.createCellProcessor(oneProcessorValue);
            final String name = oneProcessorValue.get(0);
            final String[] params;
            if (oneProcessorValue.size() == 1) {
                params = CsvItemReader.EMPTY_STRING_ARRAY;
            } else {
                params = new String[oneProcessorValue.size() - 1];
                for (int i = 1; i < oneProcessorValue.size(); i++) {
                    params[i - 1] = oneProcessorValue.get(i);
                }
            }

            //not supported CellProcessor: Collector, HashMapper,

            if (name.equalsIgnoreCase("null")) {
                current = null;
                break;
            } else if (name.equalsIgnoreCase("NotNull")) {
                if (params.length > 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                current = previous == null ? new NotNull() : new NotNull(previous);
            } else if (name.equalsIgnoreCase("Optional")) {
                if (params.length > 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                current = previous == null ? new Optional() : new Optional(previous);
            } else if (name.equalsIgnoreCase("ParseBool")) {
                if (params.length == 0) {
                    //use the default true and false string values in org.supercsv.cellprocessor.ParseBool
                    current = previous == null ? new ParseBool() : new ParseBool((BoolCellProcessor) previous);
                } else if (params.length == 2) {
                    //use custom true and false values: can be either single value or multiple values
                    final String[] trueValues = params[0].trim().split("\\s*,\\s*");
                    final String[] falseValues = params[1].trim().split("\\s*,\\s*");
                    current = previous == null ? new ParseBool(trueValues, falseValues) :
                            new ParseBool(trueValues, falseValues, (BoolCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ParseChar")) {
                if (params.length > 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                current = previous == null ? new ParseChar() : new ParseChar((DoubleCellProcessor) previous);
            } else if (name.equalsIgnoreCase("ParseDate")) {
                if (params.length == 1) {  //dateFormat
                    current = previous == null ? new ParseDate(params[0]) : new ParseDate(params[0], (DateCellProcessor) previous);
                } else if (params.length == 2) { //dateFormat, lenient
                    current = previous == null ? new ParseDate(params[0], Boolean.parseBoolean(params[1])) :
                            new ParseDate(params[0], Boolean.parseBoolean(params[1]), (DateCellProcessor) previous);
                } else if (params.length == 3) { //dateFormat, lenient, locale
                    current = previous == null ? new ParseDate(params[0], Boolean.parseBoolean(params[1]), new Locale(params[2])) :
                            new ParseDate(params[0], Boolean.parseBoolean(params[1]), new Locale(params[2]), (DateCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ParseDouble")) {
                if (params.length == 0) {
                    current = previous == null ? new ParseDouble() : new ParseDouble((DoubleCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ParseInt")) {
                if (params.length == 0) {
                    current = previous == null ? new ParseInt() : new ParseInt((LongCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ParseLong")) {
                if (params.length == 0) {
                    current = previous == null ? new ParseLong() : new ParseLong((LongCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ParseBigDecimal")) {
                if (params.length == 0) {
                    current = previous == null ? new ParseBigDecimal() : new ParseBigDecimal(previous);
                } else if (params.length == 1) {  //locale for getting DecimalFormatSymbols
                    current = previous == null ? new ParseBigDecimal(new DecimalFormatSymbols(new Locale(params[0]))) :
                            new ParseBigDecimal(new DecimalFormatSymbols(new Locale(params[0])), previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Truncate")) {
                if (params.length == 1) {  //max length
                    current = previous == null ? new Truncate(Integer.parseInt(params[0])) :
                            new Truncate(Integer.parseInt(params[0]), (StringCellProcessor) previous);
                } else if (params.length == 2) {  //max length, suffix
                    current = previous == null ? new Truncate(Integer.parseInt(params[0]), params[1]) :
                            new Truncate(Integer.parseInt(params[0]), params[1], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Trim")) {
                if (params.length == 0) {
                    current = previous == null ? new Trim() : new Trim((StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Token")) {
                if (params.length == 2) {  //token, replacement
                    current = previous == null ? new Token(params[0], params[1]) :
                            new Token(params[0], params[1], previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("StrReplace")) {
                if (params.length == 2) {  //regex, replacement
                    current = previous == null ? new StrReplace(params[0], params[1]) :
                            new StrReplace(params[0], params[1], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("ConvertNullTo")) {
                if (params.length == 1) {
                    current = previous == null ? new ConvertNullTo(params[0]) : new ConvertNullTo(params[0], previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("FmtNumber")) {
                if (params.length == 1) {  //decimalFormat
                    current = previous == null ? new FmtNumber(params[0]) :
                            new FmtNumber(params[0], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("FmtDate")) {
                if (params.length == 1) {  //dateFormat
                    current = previous == null ? new FmtDate(params[0]) :
                            new FmtDate(params[0], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("FmtBool")) {
                if (params.length == 2) {  //trueValue, falseValue
                    current = previous == null ? new FmtBool(params[0], params[1]) :
                            new FmtBool(params[0], params[1], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("DMinMax")) {
                if (params.length == 2) {  //min, max
                    current = previous == null ? new DMinMax(Double.parseDouble(params[0]), Double.parseDouble(params[1])) :
                            new DMinMax(Double.parseDouble(params[0]), Double.parseDouble(params[1]), (DoubleCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Equals")) {
                if (params.length == 0) {  //all input date equal to each other
                    current = previous == null ? new Equals() : new Equals(previous);
                } else if (params.length == 1) {  // equals to a supplied constantValue
                    current = previous == null ? new Equals(params[0]) : new Equals(params[0], previous);
                }
            } else if (name.equalsIgnoreCase("ForbidSubStr")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                current = previous == null ? new ForbidSubStr(params) : new ForbidSubStr(params, previous);
            } else if (name.equalsIgnoreCase("IsElementOf")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                final Collection<Object> coll = new ArrayList<Object>();
                Collections.addAll(coll, params);
                current = previous == null ? new IsElementOf(coll) : new IsElementOf(coll, previous);
            } else if (name.equalsIgnoreCase("IsIncludedIn")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                current = previous == null ? new IsIncludedIn(params) : new IsIncludedIn(params, previous);
            } else if (name.equalsIgnoreCase("LMinMax")) {
                if (params.length == 2) {
                    current = previous == null ? new LMinMax(Long.parseLong(params[0]), Long.parseLong(params[1])) :
                            new LMinMax(Long.parseLong(params[0]), Long.parseLong(params[1]), (LongCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("RequireHashCode")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                } else if (params.length == 1) {
                    current = previous == null ? new RequireHashCode(Integer.parseInt(params[0])) :
                            new RequireHashCode(Integer.parseInt(params[0]), previous);
                } else {
                    current = previous == null ? new RequireHashCode(CsvItemReader.convertToIntParams(params, 0, params.length)) :
                            new RequireHashCode(CsvItemReader.convertToIntParams(params, 0, params.length), previous);
                }
            } else if (name.equalsIgnoreCase("RequireSubStr")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                if (params.length == 1) {
                    current = previous == null ? new RequireSubStr(params[0]) : new RequireSubStr(params[0], previous);
                } else {
                    current = previous == null ? new RequireSubStr(params) : new RequireSubStr(params, previous);
                }
            } else if (name.equalsIgnoreCase("Strlen")) {
                if (params.length == 0) {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
                if (params.length == 1) {
                    current = previous == null ? new Strlen(Integer.parseInt(params[0])) : new Strlen(Integer.parseInt(params[0]), previous);
                } else {
                    current = previous == null ? new Strlen(CsvItemReader.convertToIntParams(params, 0, params.length)) :
                            new Strlen(CsvItemReader.convertToIntParams(params, 0, params.length), previous);
                }
            } else if (name.equalsIgnoreCase("StrMinMax")) {
                if (params.length == 2) {  // min, max
                    current = previous == null ? new StrMinMax(Long.parseLong(params[0]), Long.parseLong(params[1])) :
                            new StrMinMax(Long.parseLong(params[0]), Long.parseLong(params[1]), previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("StrNotNullOrEmpty")) {
                if (params.length == 0) {
                    current = previous == null ? new StrNotNullOrEmpty() : new StrNotNullOrEmpty(previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("StrRegEx")) {
                if (params.length == 1) {
                    current = previous == null ? new StrRegEx(params[0]) : new StrRegEx(params[0], (StringCellProcessor) previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Unique")) {
                if (params.length == 0) {
                    current = previous == null ? new Unique() : new Unique(previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("UniqueHashCode")) {
                if (params.length == 0) {
                    current = previous == null ? new UniqueHashCode() : new UniqueHashCode(previous);
                } else {
                    throw SupportLogger.LOGGER.invalidParamsForCellProcessor(name, params);
                }
            } else if (name.equalsIgnoreCase("Collector") || name.equalsIgnoreCase("HashMapper")) {
                throw SupportLogger.LOGGER.unsupportedCellProcessor(name, params);
            } else {  //custom cell processor
                current = createCustomCellProcessor(name, params, oneProcessorValue, previous);
            }
        }
        return current;
    }

    private static CellProcessor createCustomCellProcessor(final String name,
                                                           final String[] params,
                                                           final List<String> oneProcessorValue,
                                                           final CellProcessor previous) {
        final Class[] constructorParamTypes;
        final CellProcessor result;
        try {
            final Class<?> cellProcessorClass = CellProcessorConfig.class.getClassLoader().loadClass(name);
            if (params.length == 0) {
                if (previous == null) {
                    result = (CellProcessor) cellProcessorClass.newInstance();
                } else {
                    constructorParamTypes = new Class[] {CellProcessor.class};
                    final Constructor<?> constructor = cellProcessorClass.getConstructor(constructorParamTypes);
                    result = (CellProcessor) constructor.newInstance(previous);
                }
            } else if (params.length == 1) {
                if (previous == null) {
                    constructorParamTypes = new Class[]{String.class};
                    final Constructor<?> constructor = cellProcessorClass.getConstructor(constructorParamTypes);
                    result = (CellProcessor) constructor.newInstance(params[0]);
                } else {
                    constructorParamTypes = new Class[]{String.class, CellProcessor.class};
                    final Constructor<?> constructor = cellProcessorClass.getConstructor(constructorParamTypes);
                    result = (CellProcessor) constructor.newInstance(params[0], previous);
                }
            } else {
                if (previous == null) {
                    constructorParamTypes = new Class[]{String[].class};
                    final Constructor<?> constructor = cellProcessorClass.getConstructor(constructorParamTypes);
                    result = (CellProcessor) constructor.newInstance(params);
                } else {
                    constructorParamTypes = new Class[]{String[].class, CellProcessor.class};
                    final Constructor<?> constructor = cellProcessorClass.getConstructor(constructorParamTypes);
                    result = (CellProcessor) constructor.newInstance(params, previous);
                }
            }
        } catch (final Exception e) {
            throw SupportLogger.LOGGER.failToLoadOrCreateCustomType(e, oneProcessorValue.toString());
        }
        return result;
    }
}