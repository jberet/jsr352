/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.chunkskipretry;

import java.util.ArrayList;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;

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
 */
public class ChunkSkipRetryIT extends AbstractIT {
    protected int dataCount = 30;
    protected static final String chunkRetryXml = "chunkRetry.xml";
    protected static final String chunkSkipXml = "chunkSkip.xml";
    protected static final String chunkSkipRetryXml = "chunkSkipRetry.xml";

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
    }

    @Test
    public void retryReadExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "27, 28, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
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
    }

    @Test
    public void retryWriteExceedLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "1, 28, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
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
    }

    @Test
    public void retryProcessExceedLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 27, 29");
        params.setProperty("retry.limit", "2");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
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
    }

    @Test
    public void skipRead5() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10));  //skip 5
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipRead9() throws Exception {
        params.setProperty("reader.fail.on.values", "9");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10));  //skip 9
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipRead10() throws Exception {
        params.setProperty("reader.fail.on.values", "10");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));  //skip 10
        expected.add(asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipRead29() throws Exception {
        params.setProperty("reader.fail.on.values", "29");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28));  //skip 29

        runTest(chunkSkipXml, expected);
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
    }

    @Test
    public void skipReadExceedLimit() throws Exception {
        params.setProperty("reader.fail.on.values", "0, 19, 29");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void skipWrite0() throws Exception {
        params.setProperty("writer.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 0 is located (0-9)
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
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
        params.setProperty("skip.limit", "20");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        //skip the chunk where 0 is located (20-29)
        //skip the chunk where 29 is located (20-29)
        //expected.add(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));

        runTest(chunkSkipXml, expected);
    }

    @Test
    public void skipWriteExceedLimit() throws Exception {
        params.setProperty("writer.fail.on.values", "0, 29");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    @Test
    public void skipProcess0() throws Exception {
        params.setProperty("processor.fail.on.values", "0");
        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();

        expected.add(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));  //skip 0
        expected.add(asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        runTest(chunkSkipXml, expected);
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
    }

    @Test
    public void skipProcessExceedLimit() throws Exception {
        params.setProperty("processor.fail.on.values", "0, 1, 2");
        params.setProperty("skip.limit", "2");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
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
    }

    private void runTest(final String jobXml, final ArrayList<List<Integer>> expected) throws Exception {
        startJob(jobXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(expected, stepExecution0.getPersistentUserData());
    }
}
