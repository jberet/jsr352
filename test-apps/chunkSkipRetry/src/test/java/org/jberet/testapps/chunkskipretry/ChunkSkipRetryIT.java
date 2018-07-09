/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.chunkskipretry;

import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.jberet.testapps.chunkskipretry.ChunkListener1.after;
import static org.jberet.testapps.chunkskipretry.ChunkListener1.before;
import static org.jberet.testapps.chunkskipretry.ChunkListener1.error;
import static org.junit.Assert.assertEquals;

/**
 * Tests to verify skip and retry behaviors by configuring {@code ArithmeticException} as {@code skippable-exception},
 * {@code retryable-exception}, and both {@code skippable-exception} and @{code retryable-exception}.
 * <p>
 * Tests compare the content and structure of written result to an expected values. Note that these comparison may be
 * too restrictive and may need to change as the jberet-core changes.
 * <p>
 * Items that will fail and be tested for retry and/or skip:
 * <ul>
 *     <li>0: the first item of the first chunk;
 *     <li>5: mid of the first chunk;
 *     <li>9: last item of the first chunk;
 *     <li>10: first item of the 2nd chunk;
 *     <li>29: last item of the last chunk.
 * </ul>
 * <p>
 * For rollback and no-rollback behavior, see spec section 11.9 Chunk with RetryListener.
 * <p>
 * For retryable and no-rollback exception from an {@code ItemReader}, when no-rollback retry happens, it will just
 * call the reader's {@code readItem()} method, which is likely to advance to the next item in the data source, and
 * the original item that caused the exception will be effectively skipped. See test {@link #retryRead0NoRollback()}.
 *<p>
 * For retryable and no-rollback exception from an {@code ItemProcessor}, when no-rollback retry happens, it will pass
 * the object that caused the exception in the {@code process()} method to the processor again. See test
 * {@link #retryProcess0NoRollback()}.
 *
 * <p>
 * For retryable and no-rollback exception from an {@code ItemWriter}, when no-rollback retry happens, it will pass the
 * list of objects that caused the exception in the {@code writeItems} method to the writer again. See test
 * {@link #retryWrite0NoRollback()}.
 * <p>
 * org.jberet.test.ExceptionClassFilterTest and exception-class-filter.xml contains tests for exception matching logic,
 * how to reconcile between exceptions and their super classes listed in both {@code <include>} and {@code <exclude>}
 * elements.
 *
 * @see "org.jberet.test.ExceptionClassFilterTest"
 * @see "exception-class-filter.xml"
 */
public class ChunkSkipRetryIT extends AbstractIT {
    protected int dataCount = 30;
    protected String numbers0_29 = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29";
    protected static final String arithmeticException = "java.lang.ArithmeticException";
    protected static final String chunkRetryXml = "chunkRetry.xml";
    protected static final String chunkSkipXml = "chunkSkip.xml";
    protected static final String chunkSkipRetryXml = "chunkSkipRetry.xml";
    protected static final String chunkListenerXml = "chunkListener.xml";

    @Before
    public void before() throws Exception {
        super.before();
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    @Test
    public void retryRead0() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        params.setProperty("retry.limit", "1");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 4);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    /**
     * This test expands {@link #retryRead0()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryRead0ChunkListener() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + error + before + after +
                before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 4);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryRead0NoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        params.setProperty("retry.limit", "1");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //no-rollback retry, calls readItem() again, which advances to the next item
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryRead5() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(asList(26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
        verifyMetric(Metric.MetricType.FILTER_COUNT, 0);
    }

    @Test
    public void retryRead5NoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
        verifyMetric(Metric.MetricType.FILTER_COUNT, 0);
    }

    /**
     * This test expands {@link #retryRead5NoRollback()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     * <p>
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryRead5NoRollbackChunkListener() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
        verifyMetric(Metric.MetricType.FILTER_COUNT, 0);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    /**
     * This test uses {@link ChunkListener1} to verify that when a step
     * that has no retryable exception classes fails, {@code onError}
     * method of the chunk listener is called.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void failRead5ChunkListener() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        params.setProperty("retryable.exception.classes", SecurityException.class.getName());
        final String expectedExitStatus = before + error;

        startJob(chunkListenerXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryRead9() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryRead9NoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryRead10() throws Exception {
        params.setProperty("reader.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryRead10NoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "10");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryRead29() throws Exception {
        params.setProperty("reader.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryRead29NoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "29");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryReadLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "28, 29");
        params.setProperty("retry.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 2);
    }

    @Test
    public void retryReadLimitNoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "28, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryReadProcessWriteLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "1");
        params.setProperty("processor.fail.on.values", "11");
        params.setProperty("writer.fail.on.values", "21");
        params.setProperty("retry.limit", "3");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //read 0, 1, reader failed on 1
        //retry 0
        //retry 1
        //read 2,3,4,5,6,7,8,9,10,11 (10 items), processor failed on 11, roll back 2-11, retry 2, 3...11
        //read and process 12,13,14,15,16,17,18,19,20,21 (10 items), write 12-21, writer failed on 21, roll back 12-21,
        //retry 12,13...21
        //read, process, and write 22,23,24,25,26,27,28,29 (remaining 8 items) in one chunk
        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10));
        expected.add(asList(11));
        expected.add(asList(12));
        expected.add(asList(13));
        expected.add(asList(14));
        expected.add(asList(15));
        expected.add(asList(16));
        expected.add(asList(17));
        expected.add(asList(18));
        expected.add(asList(19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 3);
    }

    @Test
    public void retryReadExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "27, 28, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 3);
    }

    @Test
    public void retryReadExceedLimitRepeatFailure() throws Exception {
        params.setProperty("reader.fail.on.values", "27, 28, 29");
        params.setProperty("retry.limit", "10");
        params.setProperty("repeat.failure", "true");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 11);
    }

    @Test
    public void retryReadExceedLimitNoRollback() throws Exception {
        params.setProperty("reader.fail.on.values", "27, 28, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryReadProcessWriteExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "1");
        params.setProperty("processor.fail.on.values", "11");
        params.setProperty("writer.fail.on.values", "21");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 3);
    }

    @Test
    public void retryWrite0() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    /**
     * This test expands {@link #retryWrite0()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryWrite0ChunkListener() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + error +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryWrite0NoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    /**
     * This test expands {@link #retryWrite0NoRollback()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryWrite0NoRollbackChunkListener() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + after +
                before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    /**
     * This test uses {@link ChunkListener1} to verify that when a step
     * that has no retryable exception classes fails, {@code onError}
     * method of the chunk listener is called.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void failWrite5ChunkListener() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        params.setProperty("retryable.exception.classes", SecurityException.class.getName());
        final String expectedExitStatus = before + error;

        startJob(chunkListenerXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryWrite5() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryWrite5NoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryWrite9() throws Exception {
        params.setProperty("writer.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryWrite9NoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "9");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryWrite10() throws Exception {
        params.setProperty("writer.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10));
        expected.add(asList(11));
        expected.add(asList(12));
        expected.add(asList(13));
        expected.add(asList(14));
        expected.add(asList(15));
        expected.add(asList(16));
        expected.add(asList(17));
        expected.add(asList(18));
        expected.add(asList(19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryWrite10NoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "10");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryWrite29() throws Exception {
        params.setProperty("writer.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryWrite29NoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "29");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryWriteLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "28, 29");
        params.setProperty("retry.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 2);
    }

    @Test
    public void retryWriteLimitNoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "2, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryWriteExceedLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "1, 28, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 3);
    }

    @Test
    public void retryWriteExceedLimitNoRollback() throws Exception {
        params.setProperty("writer.fail.on.values", "1, 18, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryWriteExceedLimitRepeatFailure() throws Exception {
        params.setProperty("writer.fail.on.values", "1, 28, 29");
        params.setProperty("retry.limit", "10");
        params.setProperty("repeat.failure", "true");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 11);
    }

    @Test
    public void retryProcess0() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        params.setProperty("retry.limit", "1");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    /**
     * This test expands {@link #retryProcess0()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryProcess0ChunkListener() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + error +
                before + after +
                before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryProcess0NoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        params.setProperty("retry.limit", "1");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryProcess5() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(asList(26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryProcess5NoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    /**
     * This test expands {@link #retryProcess5NoRollback()} by adding {@link ChunkListener1}
     * to the test job, and verifying job exit status, which should contain
     * values saved in chunk listener.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void retryProcess5NoRollbackChunkListener() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        final String expectedExitStatus = before + after +
                before + after +
                before + after +
                before + after;

        runTest(chunkListenerXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);

        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    /**
     * This test uses {@link ChunkListener1} to verify that when a step
     * that has no retryable exception classes fails, {@code onError}
     * method of the chunk listener is called.
     *
     * @throws Exception upon errors
     *
     * @since 1.3.0.Beta7, 1.2.5.Final
     */
    @Test
    public void failProcess5ChunkListener() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        params.setProperty("retryable.exception.classes", SecurityException.class.getName());
        final String expectedExitStatus = before + error;

        startJob(chunkListenerXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        assertEquals(expectedExitStatus, jobExecution.getExitStatus());
    }

    @Test
    public void retryProcess9() throws Exception {
        params.setProperty("processor.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryProcess9NoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "9");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryProcess10() throws Exception {
        params.setProperty("processor.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryProcess10NoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "10");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryProcess29() throws Exception {
        params.setProperty("processor.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryProcess29NoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "29");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryProcessLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "27, 29");
        params.setProperty("retry.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        expected.add(asList(29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 2);
    }

    @Test
    public void retryProcessLimitNoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "27, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void retryProcessExceedLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 27, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 3);
    }

    @Test
    public void retryProcessExceedLimitRepeatFailure() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 27, 29");
        params.setProperty("retry.limit", "10");
        params.setProperty("repeat.failure", "true");
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 11);
    }

    @Test
    public void retryProcessExceedLimitRepeatFailureNoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 27, 29");
        params.setProperty("retry.limit", "10");
        params.setProperty("repeat.failure", "true");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retryProcessExceedLimitNoRollback() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 27, 29");
        params.setProperty("retry.limit", "2");
        params.setProperty("no.rollback.exception.classes", arithmeticException);
        startJob(chunkRetryXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void skipRead0() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        params.setProperty("skip.limit", "1");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));  //skip 0
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipRead5() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10));  //skip 5
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipRead9() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10));  //skip 9
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipRead10() throws Exception {
        params.setProperty("reader.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));  //skip 10
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipRead29() throws Exception {
        params.setProperty("reader.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28));  //skip 29

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipReadLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "19, 29");
        params.setProperty("skip.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 20)); //skip 19
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28));  //skip 29

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 2);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void skipReadAll() throws Exception {
        params.setProperty("reader.fail.on.values", numbers0_29);

        runTest(chunkSkipXml, null);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 1);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 30);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void skipReadProcessWriteLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        params.setProperty("processor.fail.on.values", "19");
        params.setProperty("writer.fail.on.values", "29");
        params.setProperty("skip.limit", "3");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10));  //skip 9
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 20)); //skip 19
        //expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28));  //skip the chunk contained 29

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void skipReadExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "0, 19, 29");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.COMMIT_COUNT, 2);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 2);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void skipReadProcessWriteExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        params.setProperty("processor.fail.on.values", "10");
        params.setProperty("writer.fail.on.values", "20");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void skipWrite0() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 0 is located (0-9)
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
    }

    @Test
    public void skipWrite5() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 5 is located (0-9)
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipWrite9() throws Exception {
        params.setProperty("writer.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 9 is located (0-9)
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipWrite10() throws Exception {
        params.setProperty("writer.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 10 is located (10-19)
        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipWrite29() throws Exception {
        params.setProperty("writer.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 29 is located (20-29)
        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipWriteLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "0, 29");
        params.setProperty("skip.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 0 is located (20-29)
        //skip the chunk where 29 is located (20-29)
        //expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));

        runTest(chunkSkipXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 2);
    }

    @Test
    public void skipWriteExceedLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "0, 15, 29");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 2);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void skipProcess0() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));  //skip 0
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);

        //Note that commit count will be 4 instead of 3, because there is an empty-chunk at the end.
        //after the 10-item chunk (a perfect chunk), it doesn't know whether there is more items to read other than
        //start a new chunk that may just be empty.
        //verifyMetric(Metric.MetricType.COMMIT_COUNT, 4);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipProcess5() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 6, 7, 8, 9));  //skip 5
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipProcess9() throws Exception {
        params.setProperty("processor.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10));  //skip 9
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipProcess10() throws Exception {
        params.setProperty("processor.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19));  //skip 10
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipProcess29() throws Exception {
        params.setProperty("processor.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28));  //skip 29

        runTest(chunkSkipXml, expected);

        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipProcessLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 29");
        params.setProperty("skip.limit", "2");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));  //skip 0
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28));  //skip 29

        runTest(chunkSkipXml, expected);

        verifyMetric(Metric.MetricType.COMMIT_COUNT, 3);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 2);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
    }

    @Test
    public void skipProcessAll() throws Exception {
        params.setProperty("processor.fail.on.values", numbers0_29);

        runTest(chunkSkipXml, null);
        verifyMetric(Metric.MetricType.COMMIT_COUNT, 1);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 30);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 0);
    }

    @Test
    public void skipProcessExceedLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 1, 2");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        verifyMetric(Metric.MetricType.COMMIT_COUNT, 0);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 2);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipRead0() throws Exception {
        params.setProperty("reader.fail.on.values", "0");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //expected.add(Arrays.asList(0));  //failed, re-read and failed and skipped
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);

        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipRead5() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-read and failed and skipped
        expected.add(asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(asList(26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipRead9() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        //expected.add(Arrays.asList(9));  //failed, re-read and failed and skipped
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipRead10() throws Exception {
        params.setProperty("reader.fail.on.values", "10");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        //expected.add(Arrays.asList(10));  //failed, re-read and failed and skipped
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipRead29() throws Exception {
        params.setProperty("reader.fail.on.values", "29");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        //expected.add(Arrays.asList(29));  //failed, re-read and failed and skipped

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipWrite0() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //expected.add(Arrays.asList(0));  //failed, re-written and failed and skipped
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipWrite5() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-written and failed and skipped
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        expected.add(asList(9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipWrite9() throws Exception {
        params.setProperty("writer.fail.on.values", "9");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        //expected.add(Arrays.asList(9));  //failed, re-written and failed and skipped
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipWrite10() throws Exception {
        params.setProperty("writer.fail.on.values", "10");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        //expected.add(Arrays.asList(10));  //failed, re-written and failed and skipped
        expected.add(asList(11));
        expected.add(asList(12));
        expected.add(asList(13));
        expected.add(asList(14));
        expected.add(asList(15));
        expected.add(asList(16));
        expected.add(asList(17));
        expected.add(asList(18));
        expected.add(asList(19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipWrite29() throws Exception {
        params.setProperty("writer.fail.on.values", "29");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        //expected.add(Arrays.asList(29));  //failed, re-written and failed and skipped

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipProcess0() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //expected.add(Arrays.asList(0));  //failed, re-read and failed and skipped
        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipProcess5() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-read and failed and skipped
        expected.add(asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(asList(26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipProcess9() throws Exception {
        params.setProperty("processor.fail.on.values", "9");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0));
        expected.add(asList(1));
        expected.add(asList(2));
        expected.add(asList(3));
        expected.add(asList(4));
        expected.add(asList(5));
        expected.add(asList(6));
        expected.add(asList(7));
        expected.add(asList(8));
        //expected.add(Arrays.asList(9));  //failed, re-read and failed and skipped
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipProcess10() throws Exception {
        params.setProperty("processor.fail.on.values", "10");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        //expected.add(Arrays.asList(10));  //failed, re-read and re-processed and failed and skipped
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    @Test
    public void retrySkipProcess29() throws Exception {
        params.setProperty("processor.fail.on.values", "29");
        params.setProperty("repeat.failure", "true");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20));
        expected.add(asList(21));
        expected.add(asList(22));
        expected.add(asList(23));
        expected.add(asList(24));
        expected.add(asList(25));
        expected.add(asList(26));
        expected.add(asList(27));
        expected.add(asList(28));
        //expected.add(Arrays.asList(29));  //failed, re-read and re-processed and failed and skipped

        runTest(chunkSkipRetryXml, expected);
        verifyMetric(Metric.MetricType.READ_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.PROCESS_SKIP_COUNT, 1);
        verifyMetric(Metric.MetricType.WRITE_SKIP_COUNT, 0);
        verifyMetric(Metric.MetricType.ROLLBACK_COUNT, 1);
    }

    private void runTest(final String jobXml, final ArrayList<List<Integer>> expected) throws Exception {
        startJob(jobXml);
        awaitTermination();
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        assertEquals(expected, stepExecution0.getPersistentUserData());
    }
}
