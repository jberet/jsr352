/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.io.IOException;
import java.nio.channels.AlreadyConnectedException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChunkMergerTest {
    @Test
    public void fromParentJob() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-child.xml");
        final Chunk child = getChunk(childJob, "chunk-child-step");

        Assertions.assertEquals("parent", child.getCheckpointAlgorithm().getRef());
        Assertions.assertNotNull(child.getSkippableExceptionClasses());
        Assertions.assertNotNull(child.getRetryableExceptionClasses());
        Assertions.assertNotNull(child.getNoRollbackExceptionClasses());

        Assertions.assertEquals("item", child.getCheckpointPolicy());
        Assertions.assertEquals("5", child.getSkipLimit());
        Assertions.assertEquals("5", child.getRetryLimit());
    }

    @Test
    public void mixed() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "chunk-mixed-child-step");

        Assertions.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        //Assertions.assertEquals(2, child.getCheckpointAlgorithm().getProperties().getPropertiesMapping().size());
        //JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child", "parent"});
        Assertions.assertEquals(1, child.getCheckpointAlgorithm().getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child"});

        Assertions.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().include.get(0));
        Assertions.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().exclude.get(0));
        Assertions.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().include.get(0));
        Assertions.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().exclude.get(0));
        Assertions.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().include.get(0));
        Assertions.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().exclude.get(0));
        Assertions.assertEquals("custom", child.getCheckpointPolicy());
        Assertions.assertEquals("15", child.getSkipLimit());
        Assertions.assertEquals("5", child.getRetryLimit());
    }

    @Test
    public void readerProcessorWriter() throws Exception {
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "chunk-mixed-child-step");

        Assertions.assertEquals(1, child.getReader().getProperties().getNameValues().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);

        //Assertions.assertEquals(2, child.getProcessor().getProperties().getPropertiesMapping().size());
        Assertions.assertEquals(1, child.getProcessor().getProperties().getNameValues().size());
        //JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child", "parent"}, true);
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child"}, true);

        Assertions.assertEquals(1, child.getWriter().getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getWriter().getProperties(), new String[]{"child"}, true);
    }

    @Test
    public void parentHasChunk() throws Exception {  //child step is empty and the chunk is declared in parent
        final Job childJob = JobMergerTest.loadJob("chunk-mixed-child.xml");
        final Chunk child = getChunk(childJob, "parent-has-chunk-child");

        Assertions.assertEquals("R1", child.getReader().getRef());
        Assertions.assertEquals("P1", child.getProcessor().getRef());
        Assertions.assertEquals("W1", child.getWriter().getRef());
        Assertions.assertEquals(1, child.getReader().getProperties().getNameValues().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);
        Assertions.assertEquals(1, child.getProcessor().getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child"}, true);
        Assertions.assertEquals(1, child.getWriter().getProperties().getNameValues().size());
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
        Assertions.assertEquals(true, filter.matches(RuntimeException.class));  //included
        Assertions.assertEquals(false, filter.matches(IllegalStateException.class));  //excluded
        Assertions.assertEquals(true, filter.matches(IllegalArgumentException.class));  // superclass included
        Assertions.assertEquals(false, filter.matches(AlreadyConnectedException.class));  //superclass excluded
        Assertions.assertEquals(false, filter.matches(IOException.class));  // excluded in parent step
        Assertions.assertEquals(true, filter.matches(Exception.class));  // included in parent step
        Assertions.assertEquals(false, filter.matches(Throwable.class));
        Assertions.assertEquals(false, filter.matches(Error.class));
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
