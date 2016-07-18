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
import javax.batch.runtime.JobInstance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jberet.camel.component.JBeretProducer;
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

    static final String leadingUri = "/camel/";
    static final long readerTimeoutMillis = 8000;
    static final String readerEndpoint = "direct:reader";
    static final String processorEndpoint = "direct:processor";

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

    @Path("jobs/{jobName}")
    @GET
    public long jobName(final @PathParam("jobName") String jobName,
                        final @Context UriInfo uriInfo) throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("param1 key", "param1 value");

        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(getJBeretComponentUri(uriInfo, null));
//        producerTemplate.sendBody(componentEndpoint, jobParams);
        final Long jobExecutionId = producerTemplate.requestBody(jobParams, long.class);
        producerTemplate.stop();
        return jobExecutionId;
    }

    @Path("jobs/{jobName}/start")
    @GET
    public long jobNameStart(final @PathParam("jobName") String jobName,
                        final @Context UriInfo uriInfo) throws Exception {
        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(getJBeretComponentUri(uriInfo, null));
        final Long jobExecutionId = producerTemplate.requestBody((Properties) null, long.class);
        producerTemplate.stop();
        return jobExecutionId;
    }

    @Path("jobs")
    @GET
    public String[] jobs(final @Context UriInfo uriInfo) throws Exception {
        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(getJBeretComponentUri(uriInfo, null));
        final List<String> jobs = producerTemplate.requestBody((Object) null, List.class);
        producerTemplate.stop();
        return jobs.toArray(new String[jobs.size()]);
    }

    @Path("jobinstances")
    @GET
    public long[] jobInstances(final @Context UriInfo uriInfo) throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty(JBeretProducer.JOB_NAME, componentJobName);

        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(getJBeretComponentUri(uriInfo, queryParams));
        final List<JobInstance> jobInstances = producerTemplate.requestBody((Object) null, List.class);
        producerTemplate.stop();

        final long[] jobInstanceIds = new long[jobInstances.size()];
        for(int i = 0; i < jobInstances.size(); i++) {
            jobInstanceIds[i] = jobInstances.get(i).getInstanceId();
        }
        return jobInstanceIds;
    }

    private static String getJBeretComponentUri(final UriInfo uriInfo, final Properties queryParams) {
        String jberetComponentUri = uriInfo.getPath().substring(leadingUri.length());
        if (queryParams != null) {
            jberetComponentUri += "?";
            for (final String k : queryParams.stringPropertyNames()) {
                jberetComponentUri += k + "=" + queryParams.getProperty(k) + "&";
            }
            jberetComponentUri = jberetComponentUri.substring(0, jberetComponentUri.length() - 1);
        }
        return "jberet:" + jberetComponentUri;
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
