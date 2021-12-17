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

package org.jberet.repository;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import com.google.common.base.Throwables;
import org.jberet.util.BatchUtil;

/**
 * A class to hold all table names and column names.  Commented-out column names are already defined in other tables,
 * and are kept there as comment line for completeness.
 */
public final class TableColumns {
    private TableColumns() {
    }

    /**
     * _id field in MongoDB
     */
    public static final String _id = "_id";

    /**
     * The name of the sequence collection, and also the name of the sequence field within this collection.
     * The name of the sequenceCache in infinispan job repository.
     */
    public static final String SEQ = "seq";

    /**
     * Provides the next id (as a long) for JOB_INSTANCE in infinispan job repository.
     */
    public static final String JOB_INSTANCE_ID_SEQ = "JOB_INSTANCE_ID_SEQ";

    /**
     * Provides the next id (as a long) for JOB_EXECUTION in infinispan job repository.
     */
    public static final String JOB_EXECUTION_ID_SEQ = "JOB_EXECUTION_ID_SEQ";

    /**
     * Provides the next id (as a long) for STEP_EXECUTION in infinispan job repository.
     */
    public static final String STEP_EXECUTION_ID_SEQ = "STEP_EXECUTION_ID_SEQ";

    /**
     * Size of EXECUTIONEXCEPTION column in STEP_EXECUTION and PARTITION_EXECUTION table.  Values will be truncated
     * to fit this size before saving to database.
     */
    public static final int EXECUTION_EXCEPTION_LENGTH_LIMIT = 2048;

    //table name
    public static final String JOB_INSTANCE = "JOB_INSTANCE";
    //column names
    //private static final String JOBINSTANCEID = "JOBINSTANCEID";
    public static final String JOBNAME = "JOBNAME";
    public static final String APPLICATIONNAME = "APPLICATIONNAME";

    //table name
    public static final String JOB_EXECUTION = "JOB_EXECUTION";
    //column names
    //private static final String JOBEXECUTIONID = "JOBEXECUTIONID";
    public static final String JOBINSTANCEID = "JOBINSTANCEID";
    public static final String CREATETIME = "CREATETIME";
    //private static final String STARTTIME = "STARTTIME";
    //private static final String ENDTIME = "ENDTIME";
    public static final String LASTUPDATEDTIME = "LASTUPDATEDTIME";
    //private static final String BATCHSTATUS = "BATCHSTATUS";
    //private static final String EXITSTATUS = "EXITSTATUS";
    public static final String JOBPARAMETERS = "JOBPARAMETERS";
    public static final String RESTARTPOSITION = "RESTARTPOSITION";

    //table name
    public static final String STEP_EXECUTION = "STEP_EXECUTION";
    //column names
    public static final String STEPEXECUTIONID = "STEPEXECUTIONID";
    public static final String JOBEXECUTIONID = "JOBEXECUTIONID";
    public static final String STEPNAME = "STEPNAME";
    public static final String STARTTIME = "STARTTIME";
    public static final String ENDTIME = "ENDTIME";
    public static final String BATCHSTATUS = "BATCHSTATUS";
    public static final String EXITSTATUS = "EXITSTATUS";
    public static final String EXECUTIONEXCEPTION = "EXECUTIONEXCEPTION";
    public static final String PERSISTENTUSERDATA = "PERSISTENTUSERDATA";
    public static final String READCOUNT = "READCOUNT";
    public static final String WRITECOUNT = "WRITECOUNT";
    public static final String COMMITCOUNT = "COMMITCOUNT";
    public static final String ROLLBACKCOUNT = "ROLLBACKCOUNT";
    public static final String READSKIPCOUNT = "READSKIPCOUNT";
    public static final String PROCESSSKIPCOUNT = "PROCESSSKIPCOUNT";
    public static final String FILTERCOUNT = "FILTERCOUNT";
    public static final String WRITESKIPCOUNT = "WRITESKIPCOUNT";
    public static final String READERCHECKPOINTINFO = "READERCHECKPOINTINFO";
    public static final String WRITERCHECKPOINTINFO = "WRITERCHECKPOINTINFO";

    //table name
    public static final String PARTITION_EXECUTION = "PARTITION_EXECUTION";
    //column names.  Other column names are already declared in other tables
    public static final String PARTITIONEXECUTIONID = "PARTITIONEXECUTIONID";

    public static String formatException(final Exception exception) {
        if (exception == null) {
            return null;
        }

        String asString = Throwables.getStackTraceAsString(exception);
        final Charset charset = Charset.defaultCharset();
        byte[] asBytes = asString.getBytes(charset);
        if (asBytes.length <= EXECUTION_EXCEPTION_LENGTH_LIMIT) {
            return asString;
        }

        asString = exception + BatchUtil.NL + Throwables.getRootCause(exception);
        asBytes = asString.getBytes(charset);
        if (asBytes.length <= EXECUTION_EXCEPTION_LENGTH_LIMIT) {
            return asString;
        }

        final ByteBuffer bb = ByteBuffer.wrap(asBytes, 0, EXECUTION_EXCEPTION_LENGTH_LIMIT);
        final CharBuffer cb = CharBuffer.allocate(EXECUTION_EXCEPTION_LENGTH_LIMIT);
        final CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        return new String(cb.array(), 0, cb.position());
    }
}
