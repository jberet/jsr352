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

import javax.batch.runtime.BatchStatus;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Ignore;
import org.junit.Test;
import org.jberet.samples.wildfly.common.BatchTestBase;

public class DeserializationIT extends BatchTestBase {
    static final String CONTEXT_PATH = "deserialization";
    static final String SERVLET_PATH = null;

    /**
     * Starts a job execution and expects it to fail.
     * This failed job execution can be restarted by {@link #restartJob()}, which should complete successfully.
     *
     * @throws Exception
     * @see #restartJob()
     */
    @Test
    @Ignore("run it manually")
    public void startJob() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start " + CONTEXT_PATH,
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
    }

    /**
     * Restarts a previously failed job execution.
     * This test may be run after {@link #startJob()}, either immediately after, or after
     * restarting WildFly server for more strict verification.
     *
     * @throws Exception
     */
    @Test
    @Ignore("run it manually")
    public void restartJob() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "restart " + CONTEXT_PATH,
                new NameValuePair("fail.on", "-1"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }



    /**
     * Starts a job execution configured to be not restartable via job parameter, and expects to fail.
     * This failed job execution can be restarted by {@link #restartNotRestartable()} ()}, which should fail,
     * since the original job execution was not restartable.
     *
     * @throws Exception
     * @see #restartNotRestartable()
     */
    @Test
    @Ignore("This test bans the job to be restarted, and any restart will fail")
    public void startNotRestartable() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start " + CONTEXT_PATH,
                new NameValuePair("restartable", Boolean.FALSE.toString()),
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
    }

    /**
     * Restarts a previously failed job execution.
     * This test may be run after {@link #startNotRestartable()}, either immediately after, or after
     * restarting WildFly server for more strict verification.
     *
     * @throws Exception
     * @see #startNotRestartable()
     */
    @Test(expected = FailingHttpStatusCodeException.class)
    @Ignore("run it manually")
    public void restartNotRestartable() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "restart " + CONTEXT_PATH,
                new NameValuePair("fail.on", "-1"));
//        final String content = page.getContent();
//        assertEquals(500, page.getWebResponse().getStatusCode());
//        assertTrue(content.contains("javax.batch.operations.JobRestartException"));
    }



    /**
     * Starts a job execution whose job.xml file name differs from job id.
     * The start operation takes the job.xml file base name, while the restart operation takes the job execution id,
     * which usually maps to job id.
     * This test is configured to fail, and can be restarted by {@link #restartJobNameDifferent()}, which should
     * complete successfully.
     *
     * @throws Exception
     * @see #restartJobNameDifferent()
     */
    @Test
    @Ignore("run it manually")
    public void startJobNameDifferent() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "start job-xml-name-different",
                new NameValuePair("fail.on", "8"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.FAILED);
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
    @Ignore("run it manually")
    public void restartJobNameDifferent() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, "restart jobXmlNameDifferent",
                new NameValuePair("fail.on", "-1"));
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }

}
