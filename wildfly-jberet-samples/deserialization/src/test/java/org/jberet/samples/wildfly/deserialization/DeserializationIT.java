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

package org.jberet.samples.wildfly.deserialization;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Test;

public class DeserializationIT extends BatchTestBase {
    /**
     * Job id and job xml name for deserialization tests (deserialization.xml)
     */
    private static final String deserializationJobName = "deserialization";

    /**
     * Job xml name for tests with differing job xml name and job id
     */
    private static final String jobXmlNameDifferentFromId = "job-xml-name-different";

    /**
     * Job id for tests with differing job xml name and job id
     */
    private static final String jobIdDifferentFromXmlName = "jobXmlNameDifferent";

    /**
     * Job id and job xml name for tests where restartable attribute may be set to false
     */
    private static final String notRestartableJobName = "not-restartable";


    @Override
    protected String getRestUrl() {
        return BASE_URL + "deserialization/api";
    }

    /**
     * Starts a job execution and expects it to fail.
     * This failed job execution can be restarted by {@link #deserializationJobRestart()},
     * which should complete successfully.
     *
     * @throws Exception
     * @see #deserializationJobRestart()
     */
    @Test
    public void deserializationJob() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(8));
        startJobCheckStatus(deserializationJobName, params, 500, BatchStatus.FAILED);
    }

    /**
     * Restarts a previously failed job execution.
     * This test may be run after {@link #deserializationJob()}, either immediately after, or after
     * restarting WildFly server for more strict verification.
     *
     * @throws Exception
     */
    @Test
    public void deserializationJobRestart() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(-1));
        restartJobCheckStatus(deserializationJobName, params, 500, BatchStatus.COMPLETED);
    }

    /**
     * Starts a job execution configured to be not restartable via job parameter, and expects to fail.
     * This failed job execution can be restarted by {@link #startNotRestartableRestart()} ()}, which should fail,
     * since the original job execution was not restartable.
     *
     * @throws Exception
     * @see #startNotRestartableRestart()
     */
    @Test
    public void startNotRestartable() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(8));
        params.setProperty("restartable", Boolean.FALSE.toString());
        startJobCheckStatus(notRestartableJobName, params, 500, BatchStatus.FAILED);
    }

    /**
     * Restarts a previously failed job execution.
     * This test may be run after {@link #startNotRestartable()}, either immediately after, or after
     * restarting WildFly server for more strict verification.
     * <p/>
     * The restart operation will cause internal server error, and no new restart job execution is created or
     * executed. So the test expects {@code javax.ws.rs.InternalServerErrorException}.
     *
     * @throws Exception
     * @see #startNotRestartable()
     */
    @Test(expected = javax.ws.rs.WebApplicationException.class)
    public void startNotRestartableRestart() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(-1));

        //the expected BatchStatus.FAILED will not be used here, since the restart operation will cause
        //internal server error, and no new restart job execution is ever created or run.
        restartJobCheckStatus(notRestartableJobName, params, 500, BatchStatus.FAILED);
    }


    /**
     * Starts a job execution whose job.xml file name differs from job id.
     * The start operation takes the job.xml file base name, while the restart operation takes the job execution id,
     * which usually maps to job id.
     * This test is configured to fail, and can be restarted by {@link #startJobNameDifferentRestart()}, which should
     * complete successfully.
     *
     * @throws Exception
     * @see #startJobNameDifferentRestart()
     */
    @Test
    public void startJobNameDifferent() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(8));
        startJobCheckStatus(jobXmlNameDifferentFromId, params, 500, BatchStatus.FAILED);
    }

    /**
     * Restarts a failed job execution in {@link #startJobNameDifferent()}, and the restart should complete successfully.
     * The restart can follow immediately after {@link #startJobNameDifferent()}, or after restarting WildFly for more
     * strict verification.
     *
     * @throws Exception
     * @see #startJobNameDifferent()
     */
    @Test
    public void startJobNameDifferentRestart() throws Exception {
        final Properties params = new Properties();
        params.setProperty("fail.on", String.valueOf(-1));
        restartJobCheckStatus(jobIdDifferentFromXmlName, params, 500, BatchStatus.COMPLETED);
    }
}
