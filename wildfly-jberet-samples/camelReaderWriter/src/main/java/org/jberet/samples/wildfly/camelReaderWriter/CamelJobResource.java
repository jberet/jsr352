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
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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
    static final String processorJobName = "camelProcessorTest";
    static final String componentJobName = "camelComponentTest";

    static final String saveTo = "file:" + System.getProperty("java.io.tmpdir");

    static final String writerEndpoint = "direct:writer";

    static final long readerTimeoutMillis = 8000;
    static final String readerEndpoint = "direct:reader";
    static final String processorEndpoint = "direct:processor";
    static final String componentEndpoint = "jberet:" + componentJobName;

    @Inject
    private CamelContext camelContext;

    @Path("writer")
    @GET
    public long writer() throws Exception {
        camelContext.getTypeConverterRegistry().addTypeConverter(
                InputStream.class, Movie.class, new MovieTypeConverter());

        try {
            camelContext.addRoutes(new RouteBuilder() {
                public void configure() {
                    from(writerEndpoint).autoStartup(true).to(saveTo);
                }
            });
        } catch (final Exception e) {
            System.out.printf("Ignoring exception from adding route: %s%n", e);
        }

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
        producerTemplate.stop();
        return jobExecutionId;
    }

    @Path("processor")
    @GET
    public long processor() throws Exception {
        final Consumer consumer = camelContext.getEndpoint(processorEndpoint).createConsumer(new MovieProcessor());
        consumer.start();

        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", processorEndpoint);

        final long jobExecutionId = jobOperator.start(processorJobName, jobParams);

        do {
            Thread.sleep(1000);
        } while (jobOperator.getJobExecution(jobExecutionId).getBatchStatus() == BatchStatus.STARTED);

        consumer.stop();
        return jobExecutionId;
    }

    @Path("component")
    @GET
    public long component() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("param1 key", "param1 value");

        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(componentEndpoint);
//        producerTemplate.sendBody(componentEndpoint, jobParams);
        final Long jobExecutionId = producerTemplate.requestBody(jobParams, Long.class);
        producerTemplate.stop();
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

    /**
     * A camel processor that changes the movie type to uppercase.
     */
    private static class MovieProcessor implements Processor {
        @Override
        public void process(final Exchange exchange) throws Exception {
//            exchange.setPattern(ExchangePattern.InOut);
            final Movie in = (Movie) exchange.getIn().getBody();
            final String title = in.getTit();
            if (title != null) {
                in.setTit(title.toUpperCase());
            }
        }
    }

}
