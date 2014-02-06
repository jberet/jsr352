/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.jberet.util.BatchUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ParseBoolTest {
    static final String jobName = "org.jberet.support.io.ParseBoolTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final int waitTimeoutMinutes = 0;
    public final String commentMatcher = "matches '\\(,.*,\\)'";

    @Test
    public void testParseBoolDefault() throws Exception {
        //use the default boolean true false values in org.supercsv.cellprocessor.ParseBool
        final String header = "boolTrueFalse,bool10,boolyn,booltf,boolYesNo,boolOnOff,description";
        final String data = header + BatchUtil.NL +
                        "(, THIS IS A COMMENT ,)" + BatchUtil.NL +
                        "true,      1,     y,     t,,," + BatchUtil.NL +
                        "false,     0,     n,     f,,,";

        final String readerCellProcessors =
                        "NotNull, ParseBool; " +
                        "NotNull, Trim, ParseBool; " +
                        "NotNull, Trim, ParseBool; " +
                        "NotNull, Trim, ParseBool;" +
                        "Optional, ParseBool;" +
                        "Optional, ParseBool;" +
                        "ConvertNullTo('This row contains booleans parsed from strings, (e.g., \'true\', 1, y, t).')";
        testParseBool0("testParseBoolDefault", data, readerCellProcessors, null, header,
                CsvItemReaderWriterTest.writeComments, false, CsvProperties.APPEND, null, null, BatchStatus.COMPLETED);
    }

    @Test
    public void testParseBoolSingleCustomValue() throws Exception {
        //use the SINGLE true or false string value to override the default boolean true false values in
        // org.supercsv.cellprocessor.ParseBool
        final String header = "boolTrueFalse,bool10,boolyn,booltf,boolYesNo,boolOnOff";
        final String data = header + BatchUtil.NL +
                        "true,      1,     y,     t,      yes,       on" + BatchUtil.NL +
                        "false,     0,     n,     f,       no,      off";

        final String readerCellProcessors =
                        "Optional, ParseBool('true', 'false');" +
                        "Optional, Trim, ParseBool('1', '0'); " +
                        "Optional, Trim, ParseBool('y', 'n'); " +
                        "Optional, Trim, ParseBool('t', 'f'); " +
                        "Optional, Trim, ParseBool('yes', 'no');" +
                        "Optional, Trim, ParseBool('on', 'off')";
        testParseBool0("testParseBoolSingleCustomValue", data, readerCellProcessors, null, header,
                CsvItemReaderWriterTest.writeComments, false, CsvProperties.OVERWRITE, null, null, BatchStatus.COMPLETED);
    }

    @Test
    public void testParseBoolMultipleCustomValues() throws Exception {
        //use multiple true or false string values to override the default boolean true false values in
        // org.supercsv.cellprocessor.ParseBool
        final String header = "boolTrueFalse,bool10,boolyn,booltf,boolYesNo,boolOnOff";
        final String data = header + BatchUtil.NL +
                "true,      1,     y,     t,      yes,       on" + BatchUtil.NL +
                "false,     0,     n,     f,       no,      off";

        final String readerCellProcessors =
                "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off');" +
                        "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off'); " +
                        "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off'); " +
                        "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off'); " +
                        "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off');" +
                        "Trim, ParseBool('true, 1, y, t, yes, on', 'false, 0, n, f, no, off')";
        testParseBool0("testParseBoolMultipleCustomValues", data, readerCellProcessors, null, header,
                CsvItemReaderWriterTest.writeComments, false, CsvProperties.OVERWRITE, null, null, BatchStatus.COMPLETED);
    }

    @Test
    public void testFmtBoolAndWriteToStepContext() throws Exception {
        final String header = "boolTrueFalse,bool10,boolyn,booltf";
        final String data = header + BatchUtil.NL +
                "true,      1,     y,     t" + BatchUtil.NL +
                "false,     0,     n,     f" + BatchUtil.NL +
                "true,      0,     y,     f" + BatchUtil.NL +
                "false,     1,     n,     t" + BatchUtil.NL +
                "true,      1,     n,     f" + BatchUtil.NL +
                "false,     0,     y,     t";

        final String readerCellProcessors =
                        "Optional, ParseBool('true', 'false');" +
                        "Optional, Trim, ParseBool('1', '0'); " +
                        "Optional, Trim, ParseBool('y', 'n'); " +
                        "Optional, Trim, ParseBool('t', 'f')";
        final String writerCellProcessors = "FmtBool(1, 0); FmtBool(1, 0); FmtBool(1, 0); FmtBool(1, 0)";

        //expect comment, each column name, 1, and 0
        final String expect = CsvItemReaderWriterTest.writeComments + ", " + header + ", 1, 0";

        //forbid true, false, since FmtBool formats true to 1 and false to 0
        final String forbid = "true, false";

        testParseBool0("testFmtBoolAndWriteToStepContext", data, readerCellProcessors, writerCellProcessors, header,
                CsvItemReaderWriterTest.writeComments, true, CsvProperties.FAIL_IF_EXISTS, expect, forbid, BatchStatus.COMPLETED);
    }

    @Test
    public void testInvalidWriteMode() throws Exception {
        final String header = "boolTrueFalse,bool10";
        final String data = header + BatchUtil.NL + "true, 1";
        final String invalidWriteMode = "invalidWriteMode";

        //write to file, invalid writeMode, will fail
        testParseBool0("testInvalidWriteMode", data, null, null, header,
                CsvItemReaderWriterTest.writeComments, false, invalidWriteMode, null, null, BatchStatus.FAILED);

        //write to StepContext, invalid writeMode, will fail
        testParseBool0("testInvalidWriteMode", data, null, null, header,
                CsvItemReaderWriterTest.writeComments, true, invalidWriteMode, null, null, BatchStatus.FAILED);
    }

    private void testParseBool0(final String fileName, final String data,
                                final String readerCellProcessors, final String writerCellProcessors,
                                final String header, final String writeComments,
                                final boolean writeToStepContext, final String writeMode,
                                final String expect, final String forbid,
                                final BatchStatus jobStatus) throws Exception {
        final String resource = saveFileToTmpdir(fileName, data).getPath();
        final String writeResource = writeToStepContext ? CsvProperties.RESOURCE_STEP_CONTEXT : resource + ".out";
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, BooleansBean.class.getName());
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);
        params.setProperty("writeResource", writeResource);

        if (readerCellProcessors != null) {
            params.setProperty("readerCellProcessors", readerCellProcessors);
        }
        if (writerCellProcessors != null) {
            params.setProperty("writerCellProcessors", writerCellProcessors);
        }
        if (writeMode != null) {
            params.setProperty(CsvProperties.WRITE_MODE_KEY, writeMode);
        }
        params.setProperty(CsvProperties.COMMENT_MATCHER_KEY, commentMatcher);
        params.setProperty(CsvProperties.WRITE_COMMENTS_KEY, writeComments);
        params.setProperty(CsvProperties.HEADER_KEY, header);
        params.setProperty("validate", String.valueOf(writeToStepContext));
        if (expect != null) {
            params.setProperty("expect", expect);
        }
        if (forbid != null) {
            params.setProperty("forbid", forbid);
        }

        System.out.printf("CSV resource to read: %n%s, to write: %n%s%n", resource, writeResource);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(jobStatus, jobExecution.getBatchStatus());
    }

    static File saveFileToTmpdir(final String fileName, final String content) throws Exception {
        final File file = new File(CsvItemReaderWriterTest.tmpdir, fileName);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(content, 0, content.length());
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
        return file;
    }
}
