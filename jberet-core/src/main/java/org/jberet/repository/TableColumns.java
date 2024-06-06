/*
 * Copyright (c) 2014-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

//import com.google.common.base.Throwables;
import org.jberet.util.BatchUtil;

/**
 * A class to hold all table names and column names.  Commented-out column names are already defined in other tables,
 * and are kept there as comment line for completeness.
 */
final class TableColumns {
    private TableColumns() {
    }

    /**
     * _id field in MongoDB
     */
    static final String _id = "_id";

    /**
     * The name of the sequence collection, and also the name of the sequence field within this collection.
     * The name of the sequenceCache in infinispan job repository.
     */
    static final String SEQ = "seq";

    /**
     * Provides the next id (as a long) for JOB_INSTANCE in infinispan job repository.
     */
    static final String JOB_INSTANCE_ID_SEQ = "JOB_INSTANCE_ID_SEQ";

    /**
     * Provides the next id (as a long) for JOB_EXECUTION in infinispan job repository.
     */
    static final String JOB_EXECUTION_ID_SEQ = "JOB_EXECUTION_ID_SEQ";

    /**
     * Provides the next id (as a long) for STEP_EXECUTION in infinispan job repository.
     */
    static final String STEP_EXECUTION_ID_SEQ = "STEP_EXECUTION_ID_SEQ";

    /**
     * Size of EXECUTIONEXCEPTION column in STEP_EXECUTION and PARTITION_EXECUTION table.  Values will be truncated
     * to fit this size before saving to database.
     */
    static final int EXECUTION_EXCEPTION_LENGTH_LIMIT = 2048;

    //table name
    static final String JOB_INSTANCE = "JOB_INSTANCE";
    //column names
    //private static final String JOBINSTANCEID = "JOBINSTANCEID";
    static final String JOBNAME = "JOBNAME";
    static final String APPLICATIONNAME = "APPLICATIONNAME";

    //table name
    static final String JOB_EXECUTION = "JOB_EXECUTION";
    //column names
    //private static final String JOBEXECUTIONID = "JOBEXECUTIONID";
    static final String JOBINSTANCEID = "JOBINSTANCEID";
    static final String CREATETIME = "CREATETIME";
    //private static final String STARTTIME = "STARTTIME";
    //private static final String ENDTIME = "ENDTIME";
    static final String LASTUPDATEDTIME = "LASTUPDATEDTIME";
    //private static final String BATCHSTATUS = "BATCHSTATUS";
    //private static final String EXITSTATUS = "EXITSTATUS";
    static final String JOBPARAMETERS = "JOBPARAMETERS";
    static final String RESTARTPOSITION = "RESTARTPOSITION";

    //table name
    static final String STEP_EXECUTION = "STEP_EXECUTION";
    //column names
    static final String STEPEXECUTIONID = "STEPEXECUTIONID";
    static final String JOBEXECUTIONID = "JOBEXECUTIONID";
    static final String STEPNAME = "STEPNAME";
    static final String STARTTIME = "STARTTIME";
    static final String ENDTIME = "ENDTIME";
    static final String BATCHSTATUS = "BATCHSTATUS";
    static final String EXITSTATUS = "EXITSTATUS";
    static final String EXECUTIONEXCEPTION = "EXECUTIONEXCEPTION";
    static final String PERSISTENTUSERDATA = "PERSISTENTUSERDATA";
    static final String READCOUNT = "READCOUNT";
    static final String WRITECOUNT = "WRITECOUNT";
    static final String COMMITCOUNT = "COMMITCOUNT";
    static final String ROLLBACKCOUNT = "ROLLBACKCOUNT";
    static final String READSKIPCOUNT = "READSKIPCOUNT";
    static final String PROCESSSKIPCOUNT = "PROCESSSKIPCOUNT";
    static final String FILTERCOUNT = "FILTERCOUNT";
    static final String WRITESKIPCOUNT = "WRITESKIPCOUNT";
    static final String READERCHECKPOINTINFO = "READERCHECKPOINTINFO";
    static final String WRITERCHECKPOINTINFO = "WRITERCHECKPOINTINFO";

    //table name
    static final String PARTITION_EXECUTION = "PARTITION_EXECUTION";
    //column names.  Other column names are already declared in other tables
    static final String PARTITIONEXECUTIONID = "PARTITIONEXECUTIONID";

    static String formatException(final Exception exception) {
        if (exception == null) {
            return null;
        }

        String asString = getStackTraceAsString(exception);
        final Charset charset = Charset.defaultCharset();
        byte[] asBytes = asString.getBytes(charset);
        if (asBytes.length <= EXECUTION_EXCEPTION_LENGTH_LIMIT) {
            return asString;
        }

        asString = exception + BatchUtil.NL + getRootCause(exception);
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

    static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable slowPointer = throwable;

        Throwable cause;
        for(boolean advanceSlowPointer = false; (cause = throwable.getCause()) != null; advanceSlowPointer = !advanceSlowPointer) {
            throwable = cause;
            if (throwable == slowPointer) {
                throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
            }

            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
        }

        return throwable;
    }
}
