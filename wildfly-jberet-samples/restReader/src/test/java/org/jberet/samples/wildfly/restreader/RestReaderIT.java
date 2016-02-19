/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.restreader;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link org.jberet.support.io.RestItemReader}, which reads data
 * by calling REST GET operations on the configured collection resource
 * {@link MoviesResource}.
 * <p>
 * {@code MoviesResource} returns resources in either of the 3 forms:
 * <ul>
 *     <li>{@code Movie[]}
 *     <li>{@code java.util.List<Movie>}
 *     <li>{@code java.util.Collection<Movie>}
 * </ul>
 */
public final class RestReaderIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/restReader.xml}
     */
    private static final String jobName = "restReader";

    /**
     * {@link org.jberet.support.io.RestItemReader} will call the REST resource method
     * that returns {@code Movie[]}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestReader() throws Exception {
        //set restUrl job parameters, which is referenced by restItemReader in job xml
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", getRestUrl() + "/movies");
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
    }

    /**
     * {@link org.jberet.support.io.RestItemReader} will call the REST resource method
     * that returns {@code List<Movie>}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestReaderList() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", getRestUrl() + "/movies/list");
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
    }

    /**
     * {@link org.jberet.support.io.RestItemReader} will call the REST resource method
     * that returns {@code Collection<Movie>}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestReaderCollection() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", getRestUrl() + "/movies/collection");
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
    }

    /**
     * Verifies that the message of the original exception the caused the step
     * and the job to fail is preserved in step exception (i.e., not lost or
     * replaced by some other exception message).
     * <p>
     * Even if the step exception is not implemented to be fully serializable,
     * it should still be preserved ({@code java.io.NotSerializableException}
     * shouldn't take place of the original step exception).
     *
     * @throws Exception if test verification fails
     */
    @Test
    public void testError() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("testName", "testError");
        jobParams.setProperty("restUrl", getRestUrl() + "/movies/error");
        final JobExecutionEntity jobExecutionEntity =
                startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.FAILED);
        Assert.assertEquals("HTTP 500 Internal Server Error", jobExecutionEntity.getExitStatus());
    }

    @Override
    protected String getRestUrl() {
        return BASE_URL + "restReader/api";
    }
}
