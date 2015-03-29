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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemReader} that reads data items from the source database.
 *
 * @see     JdbcItemWriter
 * @see     JdbcItemReaderWriterBase
 * @since   1.1.0
 */
@Named
@Dependent
public class JdbcItemReader extends JdbcItemReaderWriterBase implements ItemReader {
    /**
     * The row number in the {@code ResultSet} to start reading.  It's a positive integer starting from 1.
     */
    @Inject
    @BatchProperty
    protected int start;

    /**
     * The row number in the {@code ResultSet} to end reading (inclusive).  It's a positive integer starting from 1.
     */
    @Inject
    @BatchProperty
    protected int end;
    /**
     * String keys used in target data structure for database columns. Optional property, and if not specified, it
     * defaults to {@link #columnLabels} . This property should have the same length and order as {@link #columnTypes},
     * if the latter is specified.
     * <p>
     * For example, if {@link #sql} is
     * <p>
     * SELECT NAME, ADDRESS, AGE FROM PERSON
     * <p>
     * And you want to map the data to the following form:
     * <p>
     * {"fn" = "Jon", "addr" = "1 Main st", "age" = 30}
     * <p>
     * then {@code columnMapping} should be specified as follows in job xml:
     * <p>
     * "fn, addr, age"
     */
    @Inject
    @BatchProperty
    protected String[] columnMapping;

    /**
     * Tells this class which {@code java.sql.ResultSet} getter method to call to get {@code ResultSet} field value.
     * It should have the same length and order as {@link #columnMapping}. Optional property, and if not set,
     * this class calls {@link java.sql.ResultSet#getObject(java.lang.String)} for all columns. For example,
     * this property can be configured as follows in job xml:
     * <p>
     * "String, String, Int"
     * <p>
     * And this class will call {@link java.sql.ResultSet#getString(java.lang.String)},
     * {@link java.sql.ResultSet#getString(java.lang.String)}, and {@link java.sql.ResultSet#getInt(java.lang.String)}.
     */
    @Inject
    @BatchProperty
    protected String[] columnTypes;

    /**
     * The following {@code resultSetProperties} can be optionally configured in job xml:
     * <p>
     * <ul>
     *  <li>fetchSize (use driver default)</li>
     *  <p>
     *  <li>fetchDirection
     *      <ul>
     *          <li>FETCH_FORWARD (default)</li>
     *          <li>FETCH_REVERSE</li>
     *          <li>FETCH_UNKNOWN</li>
     *      </ul>
     *  </li>
     *  <p>
     *  <li>resultSetType:
     *      <ul>
     *          <li>TYPE_FORWARD_ONLY (default)</li>
     *          <li>TYPE_SCROLL_INSENSITIVE</li>
     *          <li>TYPE_SCROLL_SENSITIVE</li>
     *      </ul>
     *  </li>
     *  <p>
     *  <li>
     *      resultSetConcurrency:
     *      <ul>
     *          <li>CONCUR_READ_ONLY (default)</li>
     *          <li>CONCUR_UPDATABLE</li>
     *      </ul>
     *  </li>
     *  <p>
     *  <li>
     *      resultSetHoldability:
     *      <ul>
     *          <li>HOLD_CURSORS_OVER_COMMIT (default)</li>
     *          <li>CLOSE_CURSORS_AT_COMMIT</li>
     *      </ul>
     *  </li>
     * <p>
     * </ul>
     * For example:
     * <p>
     * &lt;property name="resultSetProperties" value="fetchSize=1000, resultSetConcurrency=CONCUR_UPDATABLE"/&gt;
     */
    @Inject
    @BatchProperty
    protected Map<String, String> resultSetProperties;

    protected String[] columnLabels;

    protected Connection connection;

    protected ResultSet resultSet;

    protected int currentRowNumber;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        init();
        connection = getConnection();

        if (resultSetProperties == null) {
            preparedStatement = connection.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        } else {
            int rsType = ResultSet.TYPE_FORWARD_ONLY;
            int rsConcur = ResultSet.CONCUR_READ_ONLY;
            int rsHold = ResultSet.HOLD_CURSORS_OVER_COMMIT;
            int fetchSize = 0;
            int fetchDirection = ResultSet.FETCH_FORWARD;
            for (final Map.Entry<String, String> e : resultSetProperties.entrySet()) {
                final String k = e.getKey();
                final String v = e.getValue();

                if ("fetchSize".equals(k)) {
                    fetchSize = Integer.parseInt(v.trim());
                } else if ("resultSetType".equals(k)) {
                    if ("TYPE_FORWARD_ONLY".equals(v)) {
                        rsType = ResultSet.TYPE_FORWARD_ONLY;
                    } else if ("TYPE_SCROLL_SENSITIVE".equals(v)) {
                        rsType = ResultSet.TYPE_SCROLL_SENSITIVE;
                    } else if ("TYPE_SCROLL_INSENSITIVE".equals(v)) {
                        rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
                    } else {
                        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, v, "resultSetType");
                    }
                } else if ("resultSetConcurrency".equals(k)) {
                    if ("CONCUR_READ_ONLY".equals(v)) {
                        rsConcur = ResultSet.CONCUR_READ_ONLY;
                    } else if ("CONCUR_UPDATABLE".equals(v)) {
                        rsConcur = ResultSet.CONCUR_UPDATABLE;
                    } else {
                        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, v, "resultSetConcurrency");
                    }
                } else if ("resultSetHoldability".equals(k)) {
                    if ("HOLD_CURSORS_OVER_COMMIT".equals(v)) {
                        rsHold = ResultSet.HOLD_CURSORS_OVER_COMMIT;
                    } else if ("CLOSE_CURSORS_AT_COMMIT".equals(v)) {
                        rsHold = ResultSet.CLOSE_CURSORS_AT_COMMIT;
                    } else {
                        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, v, "resultSetHoldability");
                    }
                } else if ("fetchDirection".equals(k)) {
                    if ("FETCH_FORWARD".equals(v)) {
                        fetchDirection = ResultSet.FETCH_FORWARD;
                    } else if ("FETCH_REVERSE".equals(v)) {
                        fetchDirection = ResultSet.FETCH_REVERSE;
                    } else if ("FETCH_UNKNOWN".equals(v)) {
                        fetchDirection = ResultSet.FETCH_UNKNOWN;
                    } else {
                        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, v, "fetchDirection");
                    }
                } else {
                    throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, v, k);
                }
            }
            preparedStatement = connection.prepareStatement(sql, rsType, rsConcur, rsHold);
            preparedStatement.setFetchDirection(fetchDirection);
            preparedStatement.setFetchSize(fetchSize);
        }

        resultSet = preparedStatement.executeQuery();

        if (columnMapping == null) {
            final ResultSetMetaData metaData = resultSet.getMetaData();
            final int columnCount = metaData.getColumnCount();

            if (columnTypes != null && columnTypes.length != columnCount) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, Arrays.toString(columnTypes), "columnTypes");
            }

            columnLabels = new String[columnCount];
            for (int i = 0; i < columnCount; ++i) {
                columnLabels[i] = metaData.getColumnLabel(i + 1);
            }
            columnMapping = columnLabels;
        }

        if (start == 0) {
            start = 1;
        }
        if (end == 0) {
            end = Integer.MAX_VALUE;
        }
        if (end < start) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, String.valueOf(end), "end");
        }
        int rowToStart = start;
        if (checkpoint != null) {
            rowToStart = Math.max(start, (Integer) checkpoint);
        }
        if (rowToStart > 1) {
            resultSet.absolute(rowToStart - 1);
        }
        currentRowNumber = rowToStart - 1;
    }

    @Override
    public void close() throws Exception {
        if (preparedStatement != null || connection != null || resultSet != null) {
            try {
                resultSet.close();
            } catch (final SQLException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close ResultSet");
            }
            JdbcItemReaderWriterBase.close(connection, preparedStatement);
            connection = null;
            preparedStatement = null;
            resultSet = null;
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (currentRowNumber >= end) {
            return null;
        }
        Object result = null;
        if (resultSet.next()) {
            if (beanType == List.class) {
                final List<Object> resultList = new ArrayList<Object>();
                for (int i = 0; i < columnMapping.length; ++i) {
                    resultList.add(getColumnValue(i));
                }
                result = resultList;
            } else {
                final Map<String, Object> resultMap = new HashMap<String, Object>();
                for (int i = 0; i < columnMapping.length; ++i) {
                    resultMap.put(columnMapping[i], getColumnValue(i));
                }

                if (beanType == Map.class) {
                    result = resultMap;
                } else {
                    final Object readValue = objectMapper.convertValue(resultMap, beanType);
                    if (!skipBeanValidation) {
                        ItemReaderWriterBase.validate(readValue);
                    }
                    result = readValue;
                }
            }
        }
        currentRowNumber = resultSet.getRow();
        return result;
    }

    /**
     * Gets the current row number in the {@code ResultSet} as the checkpoint info.
     *
     * @return the current row number in the {@code ResultSet}
     * @throws Exception any exception raised
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        return currentRowNumber;
    }

    private Object getColumnValue(final int i) throws Exception {
        Object val = null;
        final int pos = i + 1;
        if (columnTypes == null) {
            val = resultSet.getObject(pos);
        } else {
            final String type = columnTypes[i];
            if (type.equals("String")) {
                val = resultSet.getString(pos);
            } else if (type.equals("Date")) {
                val = resultSet.getDate(pos);
            } else if (type.equals("Timestamp")) {
                val = resultSet.getTimestamp(pos);
            } else if (type.equals("Time")) {
                val = resultSet.getTime(pos);
            } else if (type.equals("Object") || type.equals("null")) {
                val = resultSet.getObject(pos);
            } else if (type.equals("NString")) {
                val = resultSet.getNString(pos);
            } else if (type.equals("Boolean")) {
                val = resultSet.getBoolean(pos);
            } else if (type.equals("Int")) {
                val = resultSet.getInt(pos);
            } else if (type.equals("Long")) {
                val = resultSet.getLong(pos);
            } else if (type.equals("Double")) {
                val = resultSet.getDouble(pos);
            } else if (type.equals("Float")) {
                val = resultSet.getFloat(pos);
            } else if (type.equals("Short")) {
                val = resultSet.getShort(pos);
            } else if (type.equals("Byte")) {
                val = resultSet.getByte(pos);
            } else if (type.equals("Blob")) {
                val = resultSet.getBlob(pos);
            } else if (type.equals("Clob")) {
                val = resultSet.getClob(pos);
            } else if (type.equals("NClob")) {
                val = resultSet.getNClob(pos);
            } else if (type.equals("BigDecimal")) {
                val = resultSet.getBigDecimal(pos);
            } else if (type.equals("URL")) {
                val = resultSet.getURL(pos);
            } else if (type.equals("Bytes")) {
                val = resultSet.getBytes(pos);
            } else if (type.equals("BinaryStream")) {
                val = resultSet.getBinaryStream(pos);
            } else if (type.equals("CharacterStream")) {
                val = resultSet.getCharacterStream(pos);
            } else if (type.equals("NCharacterStream")) {
                val = resultSet.getNCharacterStream(pos);
            } else if (type.equals("AsciiStream")) {
                val = resultSet.getAsciiStream(pos);
            } else if (type.equals("Ref")) {
                val = resultSet.getRef(pos);
            } else if (type.equals("RowId")) {
                val = resultSet.getRowId(pos);
            } else if (type.equals("SQLXML")) {
                val = resultSet.getSQLXML(pos);
            } else if (type.equals("Array")) {
                val = resultSet.getArray(pos);
            }
            if (resultSet.wasNull()) {
                val = null;
            }
        }
        return val;
    }
}
