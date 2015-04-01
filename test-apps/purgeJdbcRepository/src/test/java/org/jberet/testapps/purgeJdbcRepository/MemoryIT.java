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

package org.jberet.testapps.purgeJdbcRepository;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("run it manually")
public class MemoryIT extends AbstractIT {
    static final int times = Integer.getInteger("times", 5000);
    static final String jobXml = "org.jberet.test.chunkPartition";

    @Test
    public void memoryTest() throws Exception {
        for (int i = 0; i < times; i++) {
            System.out.printf("================ %s ================ %n", i);

            params = new Properties();

            //add more job parameters to consume memory
            final String val = System.getProperty("user.dir");
            for (int n = 0; n < 20; n++) {
                params.setProperty(String.valueOf(n), val);
            }

            params.setProperty("thread.count", "10");
            params.setProperty("skip.thread.check", "true");
            params.setProperty("writer.sleep.time", "0");
            startJobAndWait(jobXml);
            Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        }
    }
}
