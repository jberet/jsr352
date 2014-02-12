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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import com.fasterxml.jackson.databind.JsonNode;
import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

import static org.jberet.support.io.CsvProperties.RESOURCE_STEP_CONTEXT;

/**
 * A test class that reads json resource into java object and write out to json format.
 */
public final class JsonItemReaderTest {
    static final String jobName = "org.jberet.support.io.JsonItemReaderTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final String movieJson = "movies-2012.json";
    static final String widgetJson = "widget.json";
    static final String widgetExpect = "\"widget\", \"debug\", \"window\", \"title\", " +
            "\"Sample Konfabulator Widget\", \"name\", \"main_window\", \"width\"," +
            "500, \"height\", \"image\", \"src\", \"Images/Sun.png\", \"sun1\", \"hOffset\", 250," +
            "\"vOffset\", \"alignment\", \"center\", \"text\", \"data\", \"Click Here\", \"size\"," +
            "36, \"style\", \"bold\", \"text1\", \"onMouseUp\", " +
            "\"sun1.opacity = (sun1.opacity / 100) * 90;\"";

    private String jsonGeneratorFeatures;

    @Test
    public void testBeanType2_4() throws Exception {
        jsonGeneratorFeatures = " QUOTE_FIELD_NAMES = false , STRICT_DUPLICATE_DETECTION = false";

        //rating enum is written out as index, as configured in job xml serializationFeatures property
        final String forbid = MovieTest.forbid2_4 + ", \"rank\", \"tit\", \"grs\", \"opn\", PG13";
        testReadWrite0(movieJson, RESOURCE_STEP_CONTEXT, "2", "4", Movie.class, MovieTest.expect2_4, forbid);
    }

    @Test
    public void testJsonNodeType2_4() throws Exception {
        testReadWrite0(movieJson, RESOURCE_STEP_CONTEXT, "2", "4", JsonNode.class, MovieTest.expect2_4, MovieTest.forbid2_4);
    }

    @Test
    public void testBeanTypeFull() throws Exception {
        testReadWrite0(movieJson, RESOURCE_STEP_CONTEXT, null, null, Movie.class, MovieTest.expectFull, null);
    }

    @Test
    public void testMapTypeFull1_100() throws Exception {
        testReadWrite0(movieJson, RESOURCE_STEP_CONTEXT, "1", "100", Map.class, MovieTest.expectFull, null);
    }

    @Test
    public void testMapType1_2() throws Exception {
        //write json output to file
        testReadWrite0(movieJson, movieJson + "1_2.out", "1", "2", Map.class, null, null);
        //save json output to StepContext transient user data
        testReadWrite0(movieJson, RESOURCE_STEP_CONTEXT, "1", "2", Map.class, MovieTest.expect1_2, MovieTest.forbid1_2);
    }

    @Test
    public void testJsonNodeTypeWidget() throws Exception {
        //write json output to file
        testReadWrite0(widgetJson, widgetJson + ".out", null, null, JsonNode.class, null, null);
        //save json output to StepContext transient user data
        testReadWrite0(widgetJson, RESOURCE_STEP_CONTEXT, null, null, JsonNode.class, widgetExpect, MovieTest.expectFull);
    }

    @Test
    public void testMapTypeWidget() throws Exception {
        testReadWrite0(widgetJson, RESOURCE_STEP_CONTEXT, null, null, Map.class, widgetExpect, MovieTest.expectFull);
    }

    private void testReadWrite0(final String resource, final String writeResource,
                                final String start, final String end, final Class<?> beanType,
                                final String expect, final String forbid) throws Exception {
        final Properties params = CsvItemReaderWriterTest.createParams(CsvProperties.BEAN_TYPE_KEY, beanType.getName());
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);

        if (writeResource == null || writeResource.equalsIgnoreCase(RESOURCE_STEP_CONTEXT)) {
            params.setProperty("writeResource", RESOURCE_STEP_CONTEXT);
        } else {
            final String path = (new File(CsvItemReaderWriterTest.tmpdir, writeResource)).getPath();
            params.setProperty("writeResource", path);
            System.out.printf("Json resource to read: %n%s, %nto write: %n%s%n", resource, path);
        }

        if (expect != null) {
            params.setProperty("validate", String.valueOf(true));
            params.setProperty("expect", expect);
        }
        if (forbid != null) {
            params.setProperty("validate", String.valueOf(true));
            params.setProperty("forbid", forbid);
        }

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }
        if (jsonGeneratorFeatures != null) {
            params.setProperty("jsonGeneratorFeatures", jsonGeneratorFeatures);
        }

        params.setProperty(CsvProperties.HEADER_KEY, MovieTest.header);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
