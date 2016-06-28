/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.camelReaderWriter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jberet.samples.wildfly.common.Movie;

/**
 * REST resource class for batch jobs with Camel.
 */
@Path("/camel")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class CamelJobResource {
    static final JobOperator jobOperator = BatchRuntime.getJobOperator();

    static final String writerJobName = "camelWriterTest";
    static final String readerJobName = "camelReaderTest";

    static final String saveTo = "file:" + System.getProperty("java.io.tmpdir");

    static final String writerEndpoint = "direct:writer";

    static final long readerTimeoutMillis = 8000;
    static final String readerEndpoint = "direct:reader";

    @Inject
    private CamelContext camelContext;

    @Path("writer")
    @GET
    public long writer() throws Exception {
        camelContext.getTypeConverterRegistry().addTypeConverter(
                InputStream.class, Movie.class, new MovieTypeConverter());

        camelContext.addRoutes(new RouteBuilder() {
            public void configure() {
                from(writerEndpoint).autoStartup(true).to(saveTo);
            }
        });

        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", writerEndpoint);
        return jobOperator.start(writerJobName, jobParams);
    }

    @Path("reader")
    @GET
    public long reader() throws Exception {
        camelContext.getTypeConverterRegistry().addTypeConverter(
                InputStream.class, Movie.class, new MovieTypeConverter());

        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();

        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", readerEndpoint);
        jobParams.setProperty("timeout", String.valueOf(readerTimeoutMillis));

        final long jobExecutionId = jobOperator.start(readerJobName, jobParams);
        Thread.sleep(readerTimeoutMillis / 2);

        for (final Movie m : getMovies()) {
            producerTemplate.sendBody(readerEndpoint, m);
        }

        return jobExecutionId;
    }

    private static List<Movie> getMovies() {
        final List<Movie> movies = new ArrayList<Movie>();
        for (int i = 0; i < 3; i++) {
            final Movie m = new Movie();
            m.setRank(i);
            m.setGrs(i * 1000);
            m.setOpn(new Date());
            m.setRating(Movie.Rating.G);
            m.setTit("Season " + i);
            movies.add(m);
        }
        return movies;
    }

}
