/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.scripting;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ScriptingIT extends AbstractIT {
    private static String testNameKey = "testName";

    public ScriptingIT() {
        //params.setProperty("job-param", "job-param");
    }

    @Test
    public void batchletJavascriptInlineCDATA() throws Exception {
        test0("batchletJavascriptInlineCDATA");
    }

    @Test
    public void batchletJavascriptInline() throws Exception {
        test0("batchletJavascriptInline");
    }

    @Test
    public void batchletJavascriptSrc() throws Exception {
        test0("batchletJavascriptSrc");
    }

    @Test
    public void batchletGroovyInline() throws Exception {
        test0("batchletGroovyInline");
    }

    @Test
    public void batchletGroovySrc() throws Exception {
        test0("batchletGroovySrc");
    }

    @Test
    public void batchletRubySrc() throws Exception {
        test0("batchletRubySrc");
    }

    @Test
    public void batchletRubyInline() throws Exception {
        test0("batchletRubyInline");
    }

    @Test
    public void batchletPythonSrc() throws Exception {
        test0("batchletPythonSrc");
    }

    @Test
    public void batchletPythonInline() throws Exception {
        test0("batchletPythonInline");
    }

    @Test
    public void batchletScalaSrc() throws Exception {
        System.setProperty("scala.usejavacp", "true");
        test0("batchletScalaSrc");
    }

    @Test
    public void batchletScalaInline() throws Exception {
        System.setProperty("scala.usejavacp", "true");
        test0("batchletScalaInline");
    }

    @Test
    public void batchletPhpInlineCDATA() throws Exception {
        test0("batchletPhpInlineCDATA");
    }

    @Test
    public void batchletPhpSrc() throws Exception {
        test0("batchletPhpSrc");
    }

    @Test
    public void chunkJavascript() throws Exception {
        test0("chunkJavascript");
    }

    void test0(final String testName) throws Exception {
        params.setProperty(testNameKey, testName);
        startJobAndWait(testName);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertTrue(jobExecution.getExitStatus().equals(testName) || jobExecution.getExitStatus().equals(BatchStatus.COMPLETED.toString()));
        Assert.assertTrue(stepExecution0.getExitStatus().equals(testName) || stepExecution0.getExitStatus().equals(BatchStatus.COMPLETED.toString()));
    }
}
