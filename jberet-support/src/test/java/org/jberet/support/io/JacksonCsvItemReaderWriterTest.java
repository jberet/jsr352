/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
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

import java.io.File;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.BatchStatus;

import com.fasterxml.jackson.databind.JsonNode;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public class JacksonCsvItemReaderWriterTest extends CsvItemReaderWriterTest {
    private static final String jobName = "org.jberet.support.io.JacksonCsvReaderTest";

    private String jsonParserFeatures;
    private String csvParserFeatures;
    private String deserializationProblemHandlers;
    private String inputDecorator;

    private String jsonGeneratorFeatures;
    private String csvGeneratorFeatures = "ALWAYS_QUOTE_STRINGS=false";
    private String outputDecorator;

    private String lineSeparator;
    private String escapeChar;
    private String skipFirstDataRow;
    private String nullValue;

    @Test
    public void testBeanType() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testReadWrite0(personResource, "testBeanType.out", Person2.class.getName(), true, Person2.class.getName(),
                null, "|",
                personResourceExpect, personResourceForbid);
    }

    @Test
    public void testBeanTypeTab() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testReadWrite0(personTabResource, "testBeanTypeTab.out", Person2.class.getName(), true, nameMapping,
                "\t", "|",
                null, null);
    }

    @Test
    public void testBeanTypePipe() throws Exception {
        //override the default quote char ", which is used in feetInches cell. | is already used as the delimiterChar
        //so cannot be used as quoteChar again.
        testReadWrite0(personPipeResource, "testBeanTypePipe.out", Person2.class.getName(), true, nameMapping,
                "|", "^",
                null, null);
    }

    @Test
    public void testListType() throws Exception {
        testReadWrite0(personResource, "testListType.out", JsonNode.class.getName(), true, nameMapping,
                null, "|",
                personResourceExpect, personResourceForbid);
    }

    @Test
    public void testMapType() throws Exception {
        testReadWrite0(personResource, "testMapType.out", java.util.Map.class.getName(), true, nameMapping,
                null, "|",
                personResourceExpect, personResourceForbid);
    }

    @Override
    public void testInvalidWriteResource() throws Exception {
    }

    @Override
    public void testStringsToInts() throws Exception {
    }

    //test will print out the path of output file from CsvItemWriter, which can then be verified.
    //e.g., CSV resource to read:
    //person.csv,
    //to write:
    //        /var/folders/s3/2m3bc7_n0550tp44h4bcgwtm0000gn/T/testMapType.out
    private void testReadWrite0(final String resource, final String writeResource,
                                final String beanType, final boolean useHeader, final String columns,
                                final String columnSeparator, final String quoteChar,
                                final String expect, final String forbid) throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, beanType);
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);

        if (useHeader) {
            params.setProperty("useHeader", String.valueOf(useHeader));
        }
        if (columns != null) {
            params.setProperty("columns", columns);
        }
        if (columnSeparator != null) {
            params.setProperty("columnSeparator", columnSeparator);
        }
        if (quoteChar != null) {
            params.setProperty("quoteChar", quoteChar);
        }

        if (lineSeparator != null) {
            params.setProperty("lineSeparator", lineSeparator);
        }
        if (escapeChar != null) {
            params.setProperty("escapeChar", escapeChar);
        }
        if (skipFirstDataRow != null) {
            params.setProperty("skipFirstDataRow", skipFirstDataRow);
        }
        if (nullValue != null) {
            params.setProperty("nullValue", nullValue);
        }

        if (jsonParserFeatures != null) {
            params.setProperty("jsonParserFeatures", jsonParserFeatures);
        }
        if (csvParserFeatures != null) {
            params.setProperty("csvParserFeatures", csvParserFeatures);
        }
        if (deserializationProblemHandlers != null) {
            params.setProperty("deserializationProblemHandlers", deserializationProblemHandlers);
        }
        if (inputDecorator != null) {
            params.setProperty("inputDecorator", inputDecorator);
        }

        if (jsonGeneratorFeatures != null) {
            params.setProperty("jsonGeneratorFeatures", jsonGeneratorFeatures);
        }
        if (csvGeneratorFeatures != null) {
            params.setProperty("csvGeneratorFeatures", csvGeneratorFeatures);
        }
        if (outputDecorator != null) {
            params.setProperty("outputDecorator", outputDecorator);
        }

        final File writeResourceFile = new File(tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());
        JacksonCsvItemReaderWriterTest.setRandomWriteMode(params);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        validate(writeResourceFile, expect, forbid);
    }
}
