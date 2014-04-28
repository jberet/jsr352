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
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Test;

public final class BeanIOReaderWriterTest {
    static final String jobName = "org.jberet.support.io.BeanIOReaderWriterTest";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    // from http://star.cde.ca.gov/star2013/research_fixfileformat.aspx
    static final String starEntityData = "ca2013entities_ascii.txt";
    static final String starMapping = "star-entity-beanio-mapping.xml";
    static final String starEntitiesStream = "star-entities";
    static final String errorHandlerClassName = "org.beanio.BeanReaderErrorHandlerSupport";
    static final String mappingProperties = "zipCodeFieldName=zipCode, zipCodeFieldType=string";

    static final String starEntitiesExpect1_2 = "000000000000000000201304State of California, 010000000000000000201305Alameda";
    static final String starEntitiesForbid1_2 = "Alameda County Office of Education";

    static final String starEntitiesExpect2_4 =
            "010000000000000000201305Alameda, Alameda County Office of Education, FAME Public Charter, 94560";
    static final String starEntitiesForbid2_4 = "State of California, Envision Academy for Arts & T, 94612";

    static final String starEntitiesExpect86_87 = "Emery Secondary, 94608, Anna Yates Elementary";
    static final String starEntitiesForbid86_87 = "Fremont Unified";

    static final String starEntitiesExpectEnd = "Perry (Mary B.) High, 93010";


    static final String personMapping = "person-beanio-mapping.xml";
    static final String personsStream = "persons";

    @Test
    public void testPersonPipeDelimitedFull() throws Exception {
        testReadWrite0(CsvItemReaderWriterTest.personPipeResource, "testPersonPipeSeparatedFull.out", "8", "10",
                personMapping, personsStream, null,
                null, null);
    }

    @Test
    public void testStarEntitiesFixedLength1_2() throws Exception {
        testReadWrite0(starEntityData, "testStarEntities1_2.out", "1", "2",
                starMapping, starEntitiesStream, errorHandlerClassName,
                starEntitiesExpect1_2, starEntitiesForbid1_2);
    }

    @Test
    public void testStarEntitiesFixedLength2_4() throws Exception {
        testReadWrite0(starEntityData, "testStarEntities2_4.out", "2", "4",
                starMapping, starEntitiesStream, errorHandlerClassName,
                starEntitiesExpect2_4, starEntitiesForbid2_4);
    }

    @Test
    public void testStarEntitiesFixedLength86_87() throws Exception {
        testReadWrite0(starEntityData, "testStarEntities86_87.out", "86", "87",
                starMapping, starEntitiesStream, errorHandlerClassName,
                starEntitiesExpect86_87, starEntitiesForbid86_87);
    }

    @Test
    public void testStarEntitiesFixedLengthFull() throws Exception {
        testReadWrite0(starEntityData, "testStarEntitiesFull.out", null, null,
                starMapping, starEntitiesStream, errorHandlerClassName,
                starEntitiesExpect1_2 + ", " + starEntitiesExpect2_4 + ", " + starEntitiesExpect86_87 + ", " + starEntitiesExpectEnd,
                null);
    }

    private void testReadWrite0(final String resource, final String writeResource,
                                final String start, final String end,
                                final String streamMapping, final String streamName, final String errorHandler,
                                final String expect, final String forbid) throws Exception {
        final Properties params = new Properties();
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);

        final File file = new File(CsvItemReaderWriterTest.tmpdir, writeResource);
        params.setProperty("writeResource", file.getPath());

        if (start != null) {
            params.setProperty(CsvProperties.START_KEY, start);
        }
        if (end != null) {
            params.setProperty(CsvProperties.END_KEY, end);
        }

        params.setProperty("streamMapping", streamMapping);
        params.setProperty("streamName", streamName);
        params.setProperty("mappingProperties", mappingProperties);

        if (errorHandler != null) {
            params.setProperty("errorHandler", errorHandler);
        }

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(CsvItemReaderWriterTest.waitTimeoutMinutes * 100, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        CsvItemReaderWriterTest.validate(file, expect, forbid);
    }
}
