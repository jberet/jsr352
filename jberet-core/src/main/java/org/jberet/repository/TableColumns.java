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

package org.jberet.repository;

import com.google.common.base.Throwables;
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
     */
    static final String SEQ = "seq";

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
        String asString = Throwables.getStackTraceAsString(exception);
        if (asString.length() <= TableColumns.EXECUTION_EXCEPTION_LENGTH_LIMIT) {
            return asString;
        }
        asString = exception + BatchUtil.NL + Throwables.getRootCause(exception);
        return asString.substring(0, Math.min(TableColumns.EXECUTION_EXCEPTION_LENGTH_LIMIT, asString.length()));
    }
}
