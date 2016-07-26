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

import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;
import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CamelReaderWriterIT extends BatchTestBase {

    /**
     * The full REST API URL, including scheme, hostname, port number, context path, servlet path for REST API.
     * For example, "http://localhost:8080/testApp/api"
     */
    private static final String restUrl = BASE_URL + "camelReaderWriter/api";

    private Client client = ClientBuilder.newClient();

    private BatchClient batchClient = new BatchClient(client, restUrl);

    @Override
    protected BatchClient getBatchClient() {
        return batchClient;
    }

    @Test
    public void testCamelWriter() throws Exception {
        runTest("/camel/writer");
    }

    @Test
    public void testCamelReader() throws Exception {
        runTest("/camel/reader");
    }

    @Test
    public void testCamelProcessor() throws Exception {
        runTest("/camel/processor");
    }

    @Test
    public void testCamelComponentJobs() throws Exception {
        final String[] result = runTestResult("/camel/jobs", String[].class);
        System.out.printf("Got jobs: %s%n", Arrays.toString(result));
    }

    @Test
    public void testCamelComponentJobName() throws Exception {
        runTest("/camel/jobs/" + CamelJobResource.componentJobName);
    }

    @Test
    public void testCamelComponentJobName2() throws Exception {
        runTest("/camel/jobs/" + CamelJobResource.componentJobName + "/");
    }

    @Test
    public void testCamelComponentJobNameStart() throws Exception {
        runTest("/camel/jobs/" + CamelJobResource.componentJobName + "/start");
    }

    @Test
    public void testCamelComponentJobInstances() throws Exception {
        final long[] jobInstanceIds = runTestResult("/camel/jobinstances", long[].class);
        System.out.printf("Got job instances: %s%n", Arrays.toString(jobInstanceIds));
    }

    @Test
    public void testCamelComponentJobInstancesStartCount() throws Exception {
        final long[] jobInstanceIds = runTestResult("/camel/jobinstances?start=1&count=3", long[].class);
        System.out.printf("Got job instances: %s%n", Arrays.toString(jobInstanceIds));
    }

    @Test
    public void testCamelComponentJobInstancesCount() throws Exception {
        final int jobInstancesCount = runTestResult("/camel/jobinstances/count", int.class);
        System.out.printf("Got job instances count: %s%n", jobInstancesCount);
    }

    @Test
    public void testCamelComponentJobExecutionsRunning() throws Exception {
        final long[] jobExecutionIds = runTestResult("/camel/jobexecutions/running", long[].class);
        System.out.printf("Got job executions running: %s%n", Arrays.toString(jobExecutionIds));
    }

    @Test
    public void testCamelComponentJobExecutionId() throws Exception {
        //first run a batch job so we can have a valid job execution id
        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(CamelJobResource.componentJobName, null);

        final long jobExecutionId = runTestResult("/camel/jobexecutions/" + jobExecutionEntity.getExecutionId(), long.class);
        System.out.printf("Got job execution id: %s%n", jobExecutionId);
        assertEquals(jobExecutionEntity.getExecutionId(), jobExecutionId);
    }

    @Test
    public void testCamelComponentJobExecutionRestart() throws Exception {
        //first run a failed batch job so we can restart it
        final Properties jobParams = new Properties();
        jobParams.setProperty("fail", "true");

        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(CamelJobResource.componentJobName, jobParams);
        Assert.assertEquals(BatchStatus.FAILED, waitForJobExecutionDone(jobExecutionEntity.getExecutionId()));

        final long restartJobExecutionId = runTestResult("/camel/jobexecutions/" +
                jobExecutionEntity.getExecutionId() + "/restart", long.class);
        System.out.printf("Got restart job execution id: %s%n", restartJobExecutionId);
        assertEquals(BatchStatus.COMPLETED, waitForJobExecutionDone(restartJobExecutionId));
    }

    @Test
    public void testCamelComponentJobExecutionAbandon() throws Exception {
        //first run a failed batch job so we can abandon it
        final Properties jobParams = new Properties();
        jobParams.setProperty("fail", "true");

        final JobExecutionEntity jobExecutionEntity = batchClient.startJob(CamelJobResource.componentJobName, jobParams);
        final long jobExecutionId = jobExecutionEntity.getExecutionId();
        Assert.assertEquals(BatchStatus.FAILED, waitForJobExecutionDone(jobExecutionId));

        final boolean abandoned = runTestResult("/camel/jobexecutions/" +
                jobExecutionId + "/abandon", boolean.class);
        System.out.printf("job execution id: %s abandoned: %s%n", jobExecutionId, abandoned);
        assertEquals(BatchStatus.ABANDONED, waitForJobExecutionDone(jobExecutionId));
    }

    @Test
    public void testCamelJobListener() throws Exception {
        final String events = runTestResult("/camel/joblistener", String.class);
        System.out.printf("Got job listener events: %s%n", events);
    }

    @Test
    public void testCamelStepListener() throws Exception {
        final String events = runTestResult("/camel/steplistener", String.class);
        System.out.printf("Got step listener events: %s%n", events);
    }

    private void runTest(final String resourceUrl) throws Exception {
        final WebTarget target = client.target(new URI(restUrl + resourceUrl));
        final long jobExecutionId = target.request().get(long.class);
        System.out.printf("Job execution id in response: %s%n", jobExecutionId);
        assertEquals(BatchStatus.COMPLETED, waitForJobExecutionDone(jobExecutionId));
    }

    private <T> T runTestResult(final String resourceUrl, final Class<T> resultType) throws Exception {
        final WebTarget target = client.target(new URI(restUrl + resourceUrl));
        return target.request().get(resultType);
    }

    private BatchStatus waitForJobExecutionDone(final long jobExecutionId) throws Exception {
        BatchStatus batchStatus;
        int numberOfSeconds = 0;
        int maxNumberOfSeconds = 200;
        do {
            Thread.sleep(1000);
            numberOfSeconds++;
            batchStatus = batchClient.getJobExecution(jobExecutionId).getBatchStatus();
        } while ((batchStatus == BatchStatus.STARTED || batchStatus == BatchStatus.STARTING || batchStatus == null)
                && numberOfSeconds < maxNumberOfSeconds);
        return batchStatus;
    }
}
