/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.loopback;

import java.io.File;
import java.nio.charset.Charset;
import javax.batch.runtime.BatchStatus;

import com.google.common.io.Files;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies step loopbacks are detected and failed.
 */
public class LoopbackIT extends AbstractIT {
    public LoopbackIT() {
        params.setProperty("job-param", "job-param");
    }


    /**
     * This test job has 2 steps: step1 scans a directory for matching files,
     * and each file is sent to step2 for processing, and then back to step1
     * to scan again for more files.  If there is no more files in step1,
     * the job execution ends.
     *
     * step1 -> step2 -> step1 -> step2 ... END
     *
     * @throws Exception if error
     */
    @Test
    @Ignore("restore it after loopback is conditionally allowed")
    public void allowLoopback() throws Exception {
        final String allowLoopbackJob = "allow-loopback.xml";
        final int numOfFiles = 10;
        final String fileBaseName = "allowLoopbackTestData";
        final String fileExt = ".txt";
        final String pattern = fileBaseName + "[0-9]*" + fileExt;
        String tmpDir = System.getProperty("jberet.tmp.dir");
        if (tmpDir == null || tmpDir.isEmpty()) {
            tmpDir = System.getProperty("java.io.tmpdir");
        }

        //create test files
        final File[] files = new File[numOfFiles];
        for (int i = 0; i < numOfFiles; i++) {
            String fileName = fileBaseName + i + fileExt;
            files[i] = new File(tmpDir, fileName);
            Files.write(fileName, files[i], Charset.defaultCharset());
        }

        try {
            params.setProperty("directory", tmpDir);
            params.setProperty("pattern", pattern);

            startJobAndWait(allowLoopbackJob);
            assertEquals(numOfFiles * 2 + 1, stepExecutions.size());
            assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        } finally {
            for (final File f : files) {
                f.delete();
            }
        }
    }

    /**
     * step1's next attribute is itself.
     */
    @Test
    public void selfNextAttribute() throws Exception {
        startJobAndWait("self-next-attribute.xml");
        assertEquals(1, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * step1's next element points to itself.
     */
    @Test
    public void selfNextElement() throws Exception {
        startJobAndWait("self-next-element.xml");
        assertEquals(1, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * step1->step2->step3, transitioning with either next attribute or next element.
     */
    @Test
    public void loopbackAttributeElement() throws Exception {
        startJobAndWait("loopback-attribute-element.xml");
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * same as loopbackAttributeElement, but within a flow, still a loopback error.
     */
    @Test
    public void loopbackInFlow() throws Exception {
        startJobAndWait("loopback-in-flow.xml");
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * flow1 (step1 -> step2) => step1 is not loopback.  The job should run successfully.
     *
     * @throws Exception
     */
    @Test
    public void notLoopbackAcrossFlow() throws Exception {
        startJobAndWait("not-loopback-across-flow.xml");
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    /**
     * flow1 (step1) => flow2 (step1 -> step2) => flow1 is a loopback at the last transition,
     * not at flow1.step1 -> flow2.step1.
     *
     * @throws Exception
     */
    @Test
    public void loopbackFlowToFlow() throws Exception {
        startJobAndWait("loopback-flow-to-flow.xml");
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * split1 (flow1 (step1) | flow2 (step2)) => self is a loopback.
     */
    @Test
    public void loopbackSplitSelf() throws Exception {
        startJobAndWait("loopback-split-self.xml");
        assertEquals(2, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }

    /**
     * step0 => split1 (flow1 (step1) | flow2 (step2)) => step0 is a loopback.
     */
    @Test
    public void loopbackStepSplit() throws Exception {
        startJobAndWait("loopback-step-split.xml");
        assertEquals(3, stepExecutions.size());
        assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
    }
}
