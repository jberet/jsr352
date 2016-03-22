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

package org.jberet.samples.wildfly.restwriter;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.WebTarget;

import org.jberet.rest.client.BatchClient;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.jberet.samples.wildfly.common.Movie;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link org.jberet.support.io.RestItemWriter}, which writes data
 * by calling REST GET operations on the configured resource {@link MoviesResource}.
 * <p>
 * {@code MoviesResource} takes data in either of the 3 forms:
 * <ul>
 *     <li>{@code Movie[]}
 *     <li>{@code java.util.List<Movie>}
 *     <li>{@code java.util.Collection<Movie>}
 * </ul>
 */
public final class RestWriterIT extends BatchTestBase {
    /**
     * The job name defined in {@code META-INF/batch-jobs/restWriter.xml}
     */
    private static final String jobName = "restWriter";

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "restWriter/api";

    private BatchClient batchClient = new BatchClient(client, restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    /**
     * {@link org.jberet.support.io.RestItemWriter} REST resource method takes
     * {@code Movie[]}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestWriter() throws Exception {
        final String testName = "testRestWriter";
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", restUrl + "/movies?testName=" + testName);
        removeMovies(testName);
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
        getAndVerifyMovies(testName);
    }

    /**
     * {@link org.jberet.support.io.RestItemWriter} REST resource method takes
     * {@code List<Movie>}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestWriterList() throws Exception {
        final String testName = "testRestWriterList";
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", restUrl + "/movies/list?testName=" + testName);
        removeMovies(testName);
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
        getAndVerifyMovies(testName);
    }

    /**
     * {@link org.jberet.support.io.RestItemWriter} REST resource method takes
     * {@code Collection<Movie>}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testRestWriterCollection() throws Exception {
        final String testName = "testRestWriterCollection";
        final Properties jobParams = new Properties();
        jobParams.setProperty("restUrl", restUrl + "/movies/collection?testName=" + testName);
        removeMovies(testName);
        startJobCheckStatus(jobName, jobParams, 5000, BatchStatus.COMPLETED);
        getAndVerifyMovies(testName);
    }

    private Movie[] getMovies(final String testName) {
        final WebTarget target = client.target(restUrl + "/movies")
                .queryParam("testName", testName);
        return target.request().get(Movie[].class);
    }

    private void removeMovies(final String testName) {
        final WebTarget target = client.target(restUrl + "/movies")
                .queryParam("testName", testName);
        target.request().delete();
    }

    private void getAndVerifyMovies(final String testName) {
        final Movie[] movies = getMovies(testName);
        Assert.assertEquals(100, movies.length);
        System.out.printf("Movie 1  : %s%nMovie 100: %s%n", movies[0], movies[99]);
    }
}
