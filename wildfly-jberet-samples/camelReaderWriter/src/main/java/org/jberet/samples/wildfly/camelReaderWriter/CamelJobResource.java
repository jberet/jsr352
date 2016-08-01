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
import java.util.Set;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.util.UnitOfWorkHelper;
import org.jberet.camel.ChunkExecutionInfo;
import org.jberet.camel.EventType;
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
    static final String jobListenerJobName = "camelJobListenerTest";
    static final String stepListenerJobName = "camelStepListenerTest";
    static final String chunkListenerJobName = "camelChunkListenerTest";

    static final String saveTo = "file:" + System.getProperty("java.io.tmpdir");

    static final String writerEndpoint = "direct:writer";

    static final String leadingUri = "/camel/";
    static final long readerTimeoutMillis = 8000;
    static final String readerEndpoint = "direct:reader";
    static final String processorEndpoint = "direct:processor";
    static final String jobListenerEndpoint = "direct:jobListener";
    static final String stepListenerEndpoint = "direct:stepListener";
    static final String chunkListenerEndpoint = "direct:chunkListener";

    static final String NL = System.getProperty("line.separator");

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
        return requestBody(getJBeretComponentUri(uriInfo, null), jobParams, long.class);
    }

    @Path("jobs/{jobName}/start")
    @GET
    public long jobNameStart(final @PathParam("jobName") String jobName,
                             final @Context UriInfo uriInfo) throws Exception {
        return requestBody(getJBeretComponentUri(uriInfo, null), null, long.class);
    }

    @Path("jobs")
    @GET
    public String[] jobs(final @Context UriInfo uriInfo) throws Exception {
        @SuppressWarnings("unchecked")
        final Set<String> jobs = requestBody(getJBeretComponentUri(uriInfo, null), null, Set.class);
        return jobs.toArray(new String[jobs.size()]);
    }

    @Path("jobinstances")
    @GET
    public long[] jobInstances(final @Context UriInfo uriInfo,
                               final @QueryParam(JBeretProducer.START) String start,
                               final @QueryParam(JBeretProducer.COUNT) String count) throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty(JBeretProducer.JOB_NAME, componentJobName);
        if (start != null) {
            queryParams.setProperty(JBeretProducer.START, start);
        }
        if (count != null) {
            queryParams.setProperty(JBeretProducer.COUNT, count);
        }

        @SuppressWarnings("unchecked")
        final List<JobInstance> jobInstances =
                requestBody(getJBeretComponentUri(uriInfo, queryParams), null, List.class);

        final long[] jobInstanceIds = new long[jobInstances.size()];
        for (int i = 0; i < jobInstances.size(); i++) {
            jobInstanceIds[i] = jobInstances.get(i).getInstanceId();
        }
        return jobInstanceIds;
    }

    @Path("jobinstances/count")
    @GET
    public int jobInstancesCount(final @Context UriInfo uriInfo) throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty(JBeretProducer.JOB_NAME, componentJobName);
        return requestBody(getJBeretComponentUri(uriInfo, queryParams), null, int.class);
    }

    @Path("jobexecutions/running")
    @GET
    public long[] jobExecutionsRunning(final @Context UriInfo uriInfo) throws Exception {
        final Properties queryParams = new Properties();
        queryParams.setProperty(JBeretProducer.JOB_NAME, componentJobName);

        @SuppressWarnings("unchecked")
        final List<Long> jobExecutionsRunning =
                requestBody(getJBeretComponentUri(uriInfo, queryParams), null, List.class);

        final long[] jobExecutionIds = new long[jobExecutionsRunning.size()];
        for (int i = 0; i < jobExecutionsRunning.size(); i++) {
            jobExecutionIds[i] = jobExecutionsRunning.get(i);
        }
        return jobExecutionIds;
    }

    @Path("jobexecutions/{jobExecutionId}")
    @GET
    public long jobExecutionId(final @Context UriInfo uriInfo,
                               final @PathParam("jobExecutionId") String jobExecutionId) throws Exception {
        final JobExecution jobExecution = requestBody(getJBeretComponentUri(uriInfo, null), null, JobExecution.class);
        return jobExecution.getExecutionId();
    }

    @Path("jobexecutions/{jobExecutionId}/restart")
    @GET
    public long jobExecutionRestart(final @Context UriInfo uriInfo,
                                    final @PathParam("jobExecutionId") String jobExecutionId) throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("fail", "false");
        return requestBody(getJBeretComponentUri(uriInfo, null), jobParams, long.class);
    }

    @Path("jobexecutions/{jobExecutionId}/abandon")
    @GET
    public boolean jobExecutionAbandon(final @Context UriInfo uriInfo,
                                       final @PathParam("jobExecutionId") String jobExecutionId) throws Exception {
        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(getJBeretComponentUri(uriInfo, null));
        producerTemplate.requestBody(null);
        producerTemplate.stop();
        return true;
    }

    @Path("joblistener")
    @GET
    public String jobListener() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", jobListenerEndpoint);

        final DirectEndpoint endpoint = camelContext.getEndpoint(jobListenerEndpoint, DirectEndpoint.class);
        endpoint.start();
        final PollingConsumer pollingConsumer = endpoint.createPollingConsumer();
        pollingConsumer.start();

        jobOperator.start(jobListenerJobName, jobParams);
        final StringBuilder sb = new StringBuilder();
        Exchange exchange;
        do {
            exchange = pollingConsumer.receive(readerTimeoutMillis);
            if (exchange != null) {
                JobExecution jobExecution = exchange.getIn().getBody(JobExecution.class);
                final Object header = exchange.getIn().getHeader(EventType.KEY);
                sb.append(header).append('\t')
                        .append(jobExecution.getExecutionId()).append('\t')
                        .append(jobExecution.getBatchStatus()).append('\t');
                UnitOfWorkHelper.doneSynchronizations(exchange, null, null);
            }
        } while (exchange != null);

        return sb.toString();
    }

    @Path("steplistener")
    @GET
    public String stepListener() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", stepListenerEndpoint);

        final DirectEndpoint endpoint = camelContext.getEndpoint(stepListenerEndpoint, DirectEndpoint.class);
        endpoint.start();
        final PollingConsumer pollingConsumer = endpoint.createPollingConsumer();
        pollingConsumer.start();

        jobOperator.start(stepListenerJobName, jobParams);
        final StringBuilder sb = new StringBuilder();
        Exchange exchange;
        do {
            exchange = pollingConsumer.receive(readerTimeoutMillis);
            if (exchange != null) {
                StepExecution stepExecution = exchange.getIn().getBody(StepExecution.class);
                final Object header = exchange.getIn().getHeader(EventType.KEY);
                sb.append(header).append('\t')
                        .append(stepExecution.getStepExecutionId()).append('\t')
                        .append(stepExecution.getStepName()).append('\t')
                        .append(stepExecution.getBatchStatus()).append('\t');
                UnitOfWorkHelper.doneSynchronizations(exchange, null, null);
            }
        } while (exchange != null);

        return sb.toString();
    }

    @Path("chunklistener")
    @GET
    public String chunkListener() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.setProperty("endpoint", chunkListenerEndpoint);

        final DirectEndpoint endpoint = camelContext.getEndpoint(chunkListenerEndpoint, DirectEndpoint.class);
        endpoint.start();
        final PollingConsumer pollingConsumer = endpoint.createPollingConsumer();
        pollingConsumer.start();

        jobOperator.start(chunkListenerJobName, jobParams);
        final StringBuilder sb = new StringBuilder();
        Exchange exchange;
        do {
            exchange = pollingConsumer.receive(readerTimeoutMillis);
            if (exchange != null) {
                ChunkExecutionInfo chunkExecutionInfo = exchange.getIn().getBody(ChunkExecutionInfo.class);
                final Object header = exchange.getIn().getHeader(EventType.KEY);
                sb.append(header).append('\t').append(String.valueOf(chunkExecutionInfo)).append(NL).append(NL);
                UnitOfWorkHelper.doneSynchronizations(exchange, null, null);
            }
        } while (exchange != null);

        return sb.toString();
    }

    private <T> T requestBody(final String jberetComponentUri,
                              final Object camelMessageBody,
                              final Class<T> resultType) throws Exception {
        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        producerTemplate.setDefaultEndpointUri(jberetComponentUri);

        // producerTemplate.sendBody(componentEndpoint, jobParams);
        final T result = producerTemplate.requestBody(camelMessageBody, resultType);
        producerTemplate.stop();
        return result;
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
