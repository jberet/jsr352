/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ResultSet;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that inserts data items into Cassandra cluster.
 *
 * @see     CassandraItemReader
 * @see     CassandraReaderWriterBase
 * @since   1.3.0
 */
@Named
@Dependent
public class CassandraItemWriter extends CassandraReaderWriterBase implements ItemWriter {
    /**
     * String keys used to retrieve values from incoming data and apply to CQL insert statement parameters. It should
     * have the same length and order as CQL insert statement parameters. Optional property and if not set, it is
     * initialized from the columns part of CQL insert statement. This property is not used when {@link #beanType}
     * is {@code java.util.List}, which assumes that incoming data is already in the same order as CQL parameters.
     * <p>
     * If {@link #beanType} is {@code java.util.Map}, and any of its key is different than the target table column
     * names, {@code parameterNames} should be specified. For example, if an incoming data item is:
     * <p>
     * {"name" = "Jon", "address" = "1 Main st", "age" = 30}
     * <p>
     * And {@link #cql} is
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
     * It should have the same length and order as CQL insert statement parameters. Optional property, and if not set,
     * this class calls {@link BoundStatement#set(int, java.lang.Object, java.lang.Class)} for all parameters. For example,
     * this property can be configured as follows in job xml:
     * <p>
     * "String, String, Int"
     * <p>
     * And this class will call {@link BoundStatement#setString(int, java.lang.String)},
     * {@link BoundStatement#setString(int, java.lang.String)}, and {@link BoundStatement#setInt(int, int)}.
     * <p>
     * Note that the value of this property is case sensitive.
     */
    @Inject
    @BatchProperty
    protected String[] parameterTypes;

    protected BatchStatement batchStatement = new BatchStatement();

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        try {
            for (final Object item : items) {
                batchStatement.add(mapParameters(item));
            }
            final ResultSet resultSet = session.execute(batchStatement);
        } finally {
            batchStatement.clear();
        }
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if(session == null) {
            init();
        }

        if (preparedStatement == null) {
            preparedStatement = session.prepare(cql);
        }

        //if parameterNames is null, assume the cql string contains named parameters
        //and the parameter value will be bound with its name instead of the index.
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    private BoundStatement mapParameters(final Object item) throws Exception {
        final BoundStatement boundStatement;

        if (item instanceof List) {
            final List itemAsList = (List) item;
            final int itemSize = itemAsList.size();

            //the item is a list and should contain data of proper types, e.g., String, Integer, Date, etc,
            //and in the same order as CQL insert statement parameters.

            //the item list may contain more elements than the number of cql parameters
            //in the insert cql statement.

            int parameterCount = preparedStatement.getVariables().size();
            final Object[] itemAsArray = new Object[parameterCount];
            for (int i = 0; i < parameterCount && i < itemSize; i++) {
                itemAsArray[i] = itemAsList.get(i);
            }
            boundStatement = preparedStatement.bind(itemAsArray);
        } else {
            final Map itemAsMap;
            if (item instanceof Map) {
                itemAsMap = (Map) item;
            } else {
                itemAsMap = objectMapper.convertValue(item, Map.class);
            }
            boundStatement = preparedStatement.bind();
            for(ColumnDefinitions.Definition cd : preparedStatement.getVariables()) {
                final String name = cd.getName();
                final DataType type = cd.getType();
                System.out.printf("## parameter name:%s, type:%s%n", name, type);
                final Object val = itemAsMap.get(name);

                if (val == null) {
                    SupportLogger.LOGGER.queryParameterNotBound(name, cql);
                } else {
                    boundStatement.set(name, val, Object.class);
                }
            }
        }
        return boundStatement;
    }

//    private void setParameter(final int i, final Object val) throws Exception {
//        final int pos = i + 1;
//        if (parameterTypes == null) {
//            preparedStatement.setObject(i + 1, val);
//            return;
//        }
//        final String type = parameterTypes[i];
//        if (type.equals("String")) {
//            preparedStatement.setString(pos, val == null ? null : val.toString());
//        } else if (type.equals("Date")) {
//            if (val == null) {
//                preparedStatement.setDate(pos, null);
//            } else {
//                final java.sql.Date sqlDate;
//                if (val instanceof java.sql.Date) {
//                    sqlDate = (java.sql.Date) val;
//                } else if (val instanceof java.util.Date) {
//                    sqlDate = new java.sql.Date(((java.util.Date) val).getTime());
//                } else if (val instanceof Long) {
//                    sqlDate = new java.sql.Date((Long) val);
//                } else {
//                    sqlDate = new java.sql.Date(Long.parseLong(val.toString()));
//                }
//                preparedStatement.setDate(pos, sqlDate);
//            }
//        } else if (type.equals("Timestamp")) {
//            if (val == null) {
//                preparedStatement.setTimestamp(pos, null);
//            } else {
//                final Timestamp sqlTimestamp;
//                if (val instanceof Timestamp) {
//                    sqlTimestamp = (Timestamp) val;
//                } else if (val instanceof java.util.Date) {
//                    sqlTimestamp = new Timestamp(((java.util.Date) val).getTime());
//                } else if (val instanceof Long) {
//                    sqlTimestamp = new Timestamp((Long) val);
//                } else {
//                    sqlTimestamp = new Timestamp(Long.parseLong(val.toString()));
//                }
//                preparedStatement.setTimestamp(pos, sqlTimestamp);
//            }
//        } else if (type.equals("Time")) {
//            if (val == null) {
//                preparedStatement.setTime(pos, null);
//            } else {
//                final Time sqlTime;
//                if (val instanceof Time) {
//                    sqlTime = (Time) val;
//                } else if (val instanceof java.util.Date) {
//                    sqlTime = new Time(((java.util.Date) val).getTime());
//                } else if (val instanceof Long) {
//                    sqlTime = new Time((Long) val);
//                } else {
//                    sqlTime = new Time(Long.parseLong(val.toString()));
//                }
//                preparedStatement.setTime(pos, sqlTime);
//            }
//        } else if (type.equals("Object") || type.equals("null")) {
//            preparedStatement.setObject(pos, val);
//        } else if (type.equals("NString")) {
//            preparedStatement.setNString(pos, val == null ? null : val.toString());
//        } else if (type.equals("Boolean")) {
//            preparedStatement.setBoolean(pos, (val instanceof Boolean ? (Boolean) val :
//                    val != null && Boolean.parseBoolean(val.toString())));
//        } else if (type.equals("Int")) {
//            preparedStatement.setInt(pos, (val instanceof Integer ? (Integer) val :
//                    val == null ? 0 : Integer.parseInt(val.toString())));
//        } else if (type.equals("Long")) {
//            preparedStatement.setLong(pos, (val instanceof Long ? (Long) val :
//                    val == null ? 0 : Long.parseLong(val.toString())));
//        } else if (type.equals("Double")) {
//            preparedStatement.setDouble(pos, (val instanceof Double ? (Double) val :
//                    val == null ? 0 : Double.parseDouble(val.toString())));
//        } else if (type.equals("Float")) {
//            preparedStatement.setFloat(pos, (val instanceof Float ? (Float) val :
//                    val == null ? 0 : Float.parseFloat(val.toString())));
//        } else if (type.equals("Short")) {
//            preparedStatement.setShort(pos, (val instanceof Short ? (Short) val :
//                    val == null ? 0 : Short.parseShort(val.toString())));
//        } else if (type.equals("Byte")) {
//            preparedStatement.setByte(pos, (val instanceof Byte ? (Byte) val :
//                    val == null ? 0 : Byte.parseByte(val.toString())));
//        } else if (type.equals("Blob")) {
//            if (val == null) {
//                preparedStatement.setBlob(pos, (Blob) null);
//            } else if (val instanceof Blob) {
//                preparedStatement.setBlob(pos, (Blob) val);
//            } else if (val instanceof InputStream) {
//                preparedStatement.setBlob(pos, (InputStream) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("Blob | InputStream", val.getClass().getName(), val);
//            }
//        } else if (type.equals("Clob")) {
//            if (val == null) {
//                preparedStatement.setClob(pos, (Clob) null);
//            } else if (val instanceof Clob) {
//                preparedStatement.setClob(pos, (Clob) val);
//            } else if (val instanceof Reader) {
//                preparedStatement.setClob(pos, (Reader) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("Clob | Reader", val.getClass().getName(), val);
//            }
//        } else if (type.equals("NClob")) {
//            if (val == null) {
//                preparedStatement.setNClob(pos, (NClob) null);
//            } else if (val instanceof NClob) {
//                preparedStatement.setNClob(pos, (NClob) val);
//            } else if (val instanceof Reader) {
//                preparedStatement.setNClob(pos, (Reader) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("NClob | Reader", val.getClass().getName(), val);
//            }
//        } else if (type.equals("BigDecimal")) {
//            preparedStatement.setBigDecimal(pos, (val instanceof BigDecimal ? (BigDecimal) val :
//                    val == null ? null : new BigDecimal(val.toString())));
//        } else if (type.equals("URL")) {
//            preparedStatement.setURL(pos, (val instanceof URL ? (URL) val :
//                    val == null ? null : (new URI(val.toString())).toURL()));
//        } else if (type.equals("Bytes")) {
//            preparedStatement.setBytes(pos, (val instanceof byte[] ? (byte[]) val :
//                    val == null ? null : val.toString().getBytes()));
//        } else if (type.equals("BinaryStream")) {
//            if (val == null) {
//                preparedStatement.setBinaryStream(pos, null);
//            } else if (val instanceof InputStream) {
//                preparedStatement.setBinaryStream(pos, (InputStream) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("InputStream", val.getClass().getName(), val);
//            }
//        } else if (type.equals("CharacterStream")) {
//            if (val == null) {
//                preparedStatement.setCharacterStream(pos, null);
//            } else if (val instanceof Reader) {
//                preparedStatement.setCharacterStream(pos, (Reader) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("Reader", val.getClass().getName(), val);
//            }
//        } else if (type.equals("NCharacterStream")) {
//            if (val == null) {
//                preparedStatement.setNCharacterStream(pos, null);
//            } else if (val instanceof Reader) {
//                preparedStatement.setNCharacterStream(pos, (Reader) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("Reader", val.getClass().getName(), val);
//            }
//        } else if (type.equals("AsciiStream")) {
//            if (val == null) {
//                preparedStatement.setAsciiStream(pos, null);
//            } else if (val instanceof InputStream) {
//                preparedStatement.setAsciiStream(pos, (InputStream) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("InputStream", val.getClass().getName(), val);
//            }
//        } else if (type.equals("Ref")) {
//            if (val == null) {
//                preparedStatement.setRef(pos, null);
//            } else if (val instanceof Ref) {
//                preparedStatement.setRef(pos, (Ref) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.Ref", val.getClass().getName(), val);
//            }
//        } else if (type.equals("RowId")) {
//            if (val == null) {
//                preparedStatement.setRowId(pos, null);
//            } else if (val instanceof RowId) {
//                preparedStatement.setRowId(pos, (RowId) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.RowId", val.getClass().getName(), val);
//            }
//        } else if (type.equals("SQLXML")) {
//            if (val == null) {
//                preparedStatement.setSQLXML(pos, null);
//            } else if (val instanceof SQLXML) {
//                preparedStatement.setSQLXML(pos, (SQLXML) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.SQLXML", val.getClass().getName(), val);
//            }
//        } else if (type.equals("Array")) {
//            if (val == null) {
//                preparedStatement.setArray(pos, null);
//            } else if (val instanceof Array) {
//                preparedStatement.setArray(pos, (Array) val);
//            } else {
//                throw SupportMessages.MESSAGES.unexpectedDataType("java.sql.Array", val.getClass().getName(), val);
//            }
//        } else {
//            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(
//                    null, Arrays.toString(parameterTypes), "parameterTypes");
//        }
//    }

}
