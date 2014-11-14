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

package org.jberet.testapps.infinispanRepository;

import com.google.common.io.Files;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;

import javax.batch.runtime.BatchStatus;
import java.io.File;

class InfinispanRepositoryTestBase extends AbstractIT {
    static final String jobXml = "org.jberet.test.infinispanRepository";
    static final String runtimeInfinispanXml = "infinispanRepository-infinispan.xml";

    static void moveInfinispanXml(final String src) throws Exception {
        String classesDir = System.getProperty("project.build.outputDirectory");
        File runtimeInfinispanXmlFile = new File(classesDir, runtimeInfinispanXml);
        File srcInfinispanXmlFile = new File(classesDir, src);
        Files.move(srcInfinispanXmlFile, runtimeInfinispanXmlFile);
        System.out.printf("Moved %s to %s%n", srcInfinispanXmlFile.getPath(), runtimeInfinispanXmlFile.getPath());
    }

    void partitionWithInfinispan0() throws Exception {
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }
}
