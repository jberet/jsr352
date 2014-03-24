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

package org.jberet.job.model;

import java.io.IOException;
import java.nio.channels.AlreadyConnectedException;

import org.junit.Assert;
import org.junit.Test;

public class ChunkMergerTest {
    @Test
    public void fromParentJob() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-child.xml");
        final Chunk child = getChunk(childJob, "chunk-child-step");

        Assert.assertEquals("parent", child.getCheckpointAlgorithm().getRef());
        Assert.assertNotNull(child.getSkippableExceptionClasses());
        Assert.assertNotNull(child.getRetryableExceptionClasses());
        Assert.assertNotNull(child.getNoRollbackExceptionClasses());

        Assert.assertEquals("item", child.getCheckpointPolicy());
        Assert.assertEquals("5", child.getSkipLimit());
        Assert.assertEquals("5", child.getRetryLimit());
    }

    @Test
    public void mixed() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "chunk-mixed-child-step");

        Assert.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        //Assert.assertEquals(2, child.getCheckpointAlgorithm().getProperties().getPropertiesMapping().size());
        //JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child", "parent"});
        Assert.assertEquals(1, child.getCheckpointAlgorithm().getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child"});

        Assert.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().include.get(0));
        Assert.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().exclude.get(0));
        Assert.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().include.get(0));
        Assert.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().exclude.get(0));
        Assert.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().include.get(0));
        Assert.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().exclude.get(0));
        Assert.assertEquals("custom", child.getCheckpointPolicy());
        Assert.assertEquals("15", child.getSkipLimit());
        Assert.assertEquals("5", child.getRetryLimit());
    }

    @Test
    public void readerProcessorWriter() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "chunk-mixed-child-step");

        Assert.assertEquals(1, child.getReader().getProperties().getPropertiesMapping().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);

        //Assert.assertEquals(2, child.getProcessor().getProperties().getPropertiesMapping().size());
        Assert.assertEquals(1, child.getProcessor().getProperties().getPropertiesMapping().size());
        //JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child", "parent"}, true);
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child"}, true);

        Assert.assertEquals(1, child.getWriter().getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getWriter().getProperties(), new String[]{"child"}, true);
    }

    @Test
    public void parentHasChunk() throws Exception {  //child step is empty and the chunk is declared in parent
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "parent-has-chunk-child");

        Assert.assertEquals("R1", child.getReader().getRef());
        Assert.assertEquals("P1", child.getProcessor().getRef());
        Assert.assertEquals("W1", child.getWriter().getRef());
        Assert.assertEquals(1, child.getReader().getProperties().getPropertiesMapping().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(1, child.getProcessor().getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(1, child.getWriter().getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getWriter().getProperties(), new String[]{"child"}, true);
    }

    @Test
    public void exceptionClassFilter() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "chunk-mixed-child-step");

        verifyExceptionClassesFilter(child.getSkippableExceptionClasses());
        verifyExceptionClassesFilter(child.getRetryableExceptionClasses());
        verifyExceptionClassesFilter(child.getNoRollbackExceptionClasses());
    }

    private void verifyExceptionClassesFilter(final ExceptionClassFilter filter) {
        Assert.assertEquals(true, filter.matches(RuntimeException.class));  //included
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));  //excluded
        Assert.assertEquals(true, filter.matches(IllegalArgumentException.class));  // superclass included
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));  //superclass excluded
        Assert.assertEquals(false, filter.matches(IOException.class));  // excluded in parent step
        Assert.assertEquals(true, filter.matches(Exception.class));  // included in parent step
        Assert.assertEquals(false, filter.matches(Throwable.class));
        Assert.assertEquals(false, filter.matches(Error.class));
    }

    protected static Chunk getChunk(final Job job, final String stepId) {
        for (final JobElement e : job.getJobElements()) {
            if (e instanceof Step) {
                final Step step = (Step) e;
                if (stepId.equals(step.getId())) {
                    return step.getChunk();
                }
            }
        }
        return null;
    }
}
