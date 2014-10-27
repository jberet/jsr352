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
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportMessages;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that inserts data items into the target database.
 *
 * @see     JdbcItemReader
 * @see     JdbcItemReaderWriterBase
 * @since   1.1.0
 */
@Named
@Dependent
public class JdbcItemWriter extends JdbcItemReaderWriterBase implements ItemWriter {
    /**
     * String keys used to retrieve values from incoming data and apply to SQL insert statement parameters. It should
     * have the same length and order as SQL insert statement parameters. Optional property and if not set, it is
     * initialized from the columns part of SQL insert statement. This property is not used when {@link #beanType}
     * is {@code java.util.List}, which assumes that incoming data is already in the same order as SQL parameters.
     * <p>
     * If {@link #beanType} is {@code java.util.Map}, and any of its key is different than the target table column
     * names, {@code parameterNames} should be specified. For example, if an incoming data item is:
     * <p>
     * {"name" = "Jon", "address" = "1 Main st", "age" = 30}
     * <p>
     * And {@link #sql} is
     * <p>
     * INSERT INTO PERSON(NAME, ADDRESS, AGE) VALUES(?, ?, ?)
     * <p>
     * then {@code parameterNames} should be specified as follows in job xml:
     * <p>
     * "name, address, age"
     * <p>
     * If {@link #beanType} is custom bean type, custom mapping may be achieved with either {@code parameterNames}, or
     * in bean class with annotations, e.g., JAXB or Jackson annotations. If the bean class does not contain field
     * mapping, or the field mapping is intended for other part of the application (e.g., {@code ItemReader}),
     * {@code parameterNames} can be used to customize mapping.
     */
    @Inject
    @BatchProperty
    protected String[] parameterNames;

    /**
     * Tells this class which {@code PreparedStatement} setter method to call to set insert statement parameters.
     * It should have the same length and order as SQL insert statement parameters. Optional property, and if not set,
     * this class calls {@link java.sql.PreparedStatement#setObject(int, Object)} for all parameters. For example,
     * this property can be configured as follows in job xml:
     * <p>
     * "String, String, Int"
     * <p>
     * And this class will call {@link java.sql.PreparedStatement#setString(int, String)},
     * {@link java.sql.PreparedStatement#setString(int, String)}, and {@link java.sql.PreparedStatement#setInt(int, int)}.
     */
    @Inject
    @BatchProperty
    protected String[] parameterTypes;

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        Connection connection = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            for (final Object item : items) {
                mapParameters(item);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } finally {
            JdbcItemReaderWriterBase.close(connection, preparedStatement);
        }
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        init();

        if (parameterNames == null) {
            final String sqlLowerCase = sql.toLowerCase();
            final int insertPos = sqlLowerCase.indexOf("insert");
            int leftParenthesisPos = sqlLowerCase.indexOf('(', insertPos + 7);
            int rightParenthesisPos = sqlLowerCase.indexOf(')', leftParenthesisPos + 1);
            final String[] columns = sql.substring(leftParenthesisPos + 1, rightParenthesisPos).split(",");
            final int valuesPos = sqlLowerCase.indexOf("values", rightParenthesisPos + 1);
            leftParenthesisPos = sqlLowerCase.indexOf('(', valuesPos + 1);
            rightParenthesisPos = sqlLowerCase.lastIndexOf(')');

            if (rightParenthesisPos <= leftParenthesisPos) {
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, sql, "sql");
            }

            final String[] values = sql.substring(leftParenthesisPos + 1, rightParenthesisPos).split(",");
            final List<String> parameterNamesList = new ArrayList<String>();
            for (int i = 0; i < values.length; ++i) {
                final String v = values[i].trim();
                if (v.equals("?")) {
                    parameterNamesList.add(columns[i].trim());
                }
            }
            parameterNames = parameterNamesList.toArray(new String[parameterNamesList.size()]);
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    private void mapParameters(final Object item) throws Exception {
        if (item instanceof List) {
            final List itemAsList = (List) item;
            //the item is a list and should contain data of proper types, e.g., String, Integer, Date, etc,
            //and in the same order as SQL insert statement parameters.
            for (int i = 0; i < parameterNames.length; ++i) {
                setParameter(i, itemAsList.get(i));
            }
        } else {
            final Map itemAsMap;
            if (item instanceof Map) {
                itemAsMap = (Map) item;
            } else {
                itemAsMap = objectMapper.convertValue(item, Map.class);
            }
            for (int i = 0; i < parameterNames.length; ++i) {
                setParameter(i, itemAsMap.get(parameterNames[i]));
            }
        }
    }

    private void setParameter(final int i, final Object val) throws Exception {
        final int pos = i + 1;
        if (parameterTypes == null) {
            preparedStatement.setObject(i + 1, val);
            return;
        }
        final String type = parameterTypes[i];
        if (type.equals("String")) {
            preparedStatement.setString(pos, val == null ? null : val.toString());
        } else if (type.equals("Date")) {
            if (val == null) {
                preparedStatement.setDate(pos, null);
            } else {
                final java.sql.Date sqlDate;
                if (val instanceof java.sql.Date) {
                    sqlDate = (java.sql.Date) val;
                } else if (val instanceof java.util.Date) {
                    sqlDate = new java.sql.Date(((java.util.Date) val).getTime());
                } else if (val instanceof Long) {
                    sqlDate = new java.sql.Date((Long) val);
                } else {
                    sqlDate = new java.sql.Date(Long.parseLong(val.toString()));
                }
                preparedStatement.setDate(pos, sqlDate);
            }
        } else if (type.equals("Timestamp")) {
            if (val == null) {
                preparedStatement.setTimestamp(pos, null);
            } else {
                final Timestamp sqlTimestamp;
                if (val instanceof Timestamp) {
                    sqlTimestamp = (Timestamp) val;
                } else if (val instanceof java.util.Date) {
                    sqlTimestamp = new Timestamp(((java.util.Date) val).getTime());
                } else if (val instanceof Long) {
                    sqlTimestamp = new Timestamp((Long) val);
                } else {
                    sqlTimestamp = new Timestamp(Long.parseLong(val.toString()));
                }
                preparedStatement.setTimestamp(pos, sqlTimestamp);
            }
        } else if (type.equals("Time")) {
            if (val == null) {
                preparedStatement.setTime(pos, null);
            } else {
                final Time sqlTime;
                if (val instanceof Time) {
                    sqlTime = (Time) val;
                } else if (val instanceof java.util.Date) {
                    sqlTime = new Time(((java.util.Date) val).getTime());
                } else if (val instanceof Long) {
                    sqlTime = new Time((Long) val);
                } else {
                    sqlTime = new Time(Long.parseLong(val.toString()));
                }
                preparedStatement.setTime(pos, sqlTime);
            }
        } else if (type.equals("Object") || type.equals("null")) {
            preparedStatement.setObject(pos, val);
        } else if (type.equals("NString")) {
            preparedStatement.setNString(pos, val == null ? null : val.toString());
        } else if (type.equals("Boolean")) {
            preparedStatement.setBoolean(pos, (val instanceof Boolean ? (Boolean) val :
                    val != null && Boolean.parseBoolean(val.toString())));
        } else if (type.equals("Int")) {
            preparedStatement.setInt(pos, (val instanceof Integer ? (Integer) val :
                    val == null ? 0 : Integer.parseInt(val.toString())));
        } else if (type.equals("Long")) {
            preparedStatement.setLong(pos, (val instanceof Long ? (Long) val :
                    val == null ? 0 : Long.parseLong(val.toString())));
        } else if (type.equals("Double")) {
            preparedStatement.setDouble(pos, (val instanceof Double ? (Double) val :
                    val == null ? 0 : Double.parseDouble(val.toString())));
        } else if (type.equals("Float")) {
            preparedStatement.setFloat(pos, (val instanceof Float ? (Float) val :
                    val == null ? 0 : Float.parseFloat(val.toString())));
        } else if (type.equals("Short")) {
            preparedStatement.setShort(pos, (val instanceof Short ? (Short) val :
                    val == null ? 0 : Short.parseShort(val.toString())));
        } else if (type.equals("Byte")) {
            preparedStatement.setByte(pos, (val instanceof Byte ? (Byte) val :
                    val == null ? 0 : Byte.parseByte(val.toString())));
        } else if (type.equals("Blob")) {
            if (val == null) {
                preparedStatement.setBlob(pos, (Blob) null);
            } else if (val instanceof Blob) {
                preparedStatement.setBlob(pos, (Blob) val);
            } else if (val instanceof InputStream) {
                preparedStatement.setBlob(pos, (InputStream) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("Blob | InputStream", val.getClass().getName(), val);
            }
        } else if (type.equals("Clob")) {
            if (val == null) {
                preparedStatement.setClob(pos, (Clob) null);
            } else if (val instanceof Clob) {
                preparedStatement.setClob(pos, (Clob) val);
            } else if (val instanceof Reader) {
                preparedStatement.setClob(pos, (Reader) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("Clob | Reader", val.getClass().getName(), val);
            }
        } else if (type.equals("NClob")) {
            if (val == null) {
                preparedStatement.setNClob(pos, (NClob) null);
            } else if (val instanceof NClob) {
                preparedStatement.setNClob(pos, (NClob) val);
            } else if (val instanceof Reader) {
                preparedStatement.setNClob(pos, (Reader) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("NClob | Reader", val.getClass().getName(), val);
            }
        } else if (type.equals("BigDecimal")) {
            preparedStatement.setBigDecimal(pos, (val instanceof BigDecimal ? (BigDecimal) val :
                    val == null ? null : new BigDecimal(val.toString())));
        } else if (type.equals("URL")) {
            preparedStatement.setURL(pos, (val instanceof URL ? (URL) val :
                    val == null ? null : (new URI(val.toString())).toURL()));
        } else if (type.equals("Bytes")) {
            preparedStatement.setBytes(pos, (val instanceof byte[] ? (byte[]) val :
                    val == null ? null : val.toString().getBytes()));
        } else if (type.equals("BinaryStream")) {
            if (val == null) {
                preparedStatement.setBinaryStream(pos, null);
            } else if (val instanceof InputStream) {
                preparedStatement.setBinaryStream(pos, (InputStream) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("InputStream", val.getClass().getName(), val);
            }
        } else if (type.equals("CharacterStream")) {
            if (val == null) {
                preparedStatement.setCharacterStream(pos, null);
            } else if (val instanceof Reader) {
                preparedStatement.setCharacterStream(pos, (Reader) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("Reader", val.getClass().getName(), val);
            }
        } else if (type.equals("NCharacterStream")) {
            if (val == null) {
                preparedStatement.setNCharacterStream(pos, null);
            } else if (val instanceof Reader) {
                preparedStatement.setNCharacterStream(pos, (Reader) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("Reader", val.getClass().getName(), val);
            }
        } else if (type.equals("AsciiStream")) {
            if (val == null) {
                preparedStatement.setAsciiStream(pos, null);
            } else if (val instanceof InputStream) {
                preparedStatement.setAsciiStream(pos, (InputStream) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("InputStream", val.getClass().getName(), val);
            }
        } else if (type.equals("Ref")) {
            if (val == null) {
                preparedStatement.setRef(pos, null);
            } else if (val instanceof Ref) {
                preparedStatement.setRef(pos, (Ref) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.Ref", val.getClass().getName(), val);
            }
        } else if (type.equals("RowId")) {
            if (val == null) {
                preparedStatement.setRowId(pos, null);
            } else if (val instanceof RowId) {
                preparedStatement.setRowId(pos, (RowId) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.RowId", val.getClass().getName(), val);
            }
        } else if (type.equals("SQLXML")) {
            if (val == null) {
                preparedStatement.setSQLXML(pos, null);
            } else if (val instanceof SQLXML) {
                preparedStatement.setSQLXML(pos, (SQLXML) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.SQLXML", val.getClass().getName(), val);
            }
        } else if (type.equals("Array")) {
            if (val == null) {
                preparedStatement.setArray(pos, null);
            } else if (val instanceof Array) {
                preparedStatement.setArray(pos, (Array) val);
            } else {
                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.Array", val.getClass().getName(), val);
            }
        }
    }
}
