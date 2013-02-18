/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.mybatch.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.AlreadyConnectedException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mybatch.job.Chunk;
import org.mybatch.job.Job;
import org.mybatch.job.Step;
import org.mybatch.metadata.ExceptionClassFilterImpl;
import org.mybatch.metadata.JobMerger;
import org.mybatch.metadata.JobXmlLoader;

public class ChunkMergerTest {
    @Test
    public void fromParentJob() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("chunk-child.xml", Job.class);
        JobMerger merger = new JobMerger(childJob);
        merger.merge();
        Chunk child = getChunk(childJob, "chunk-child");

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
        Job childJob = JobXmlLoader.loadJobXml("chunk-mixed-child.xml", Job.class);
        Chunk child = getChunk(childJob, "chunk-mixed-child");

        Assert.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        Assert.assertEquals(1, child.getCheckpointAlgorithm().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child"});

        Assert.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().getExclude().get(0).getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().getExclude().get(0).getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().getExclude().get(0).getClazz());

        Assert.assertEquals("custom", child.getCheckpointPolicy());
        Assert.assertEquals("15", child.getSkipLimit()) ;
        Assert.assertEquals(null, child.getRetryLimit()) ;

        JobMerger merger = new JobMerger(childJob);
        merger.merge();

        Assert.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        Assert.assertEquals(2, child.getCheckpointAlgorithm().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getCheckpointAlgorithm().getProperties(), new String[]{"child", "parent"});

        Assert.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().getExclude().get(0).getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().getExclude().get(0).getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().getInclude().get(0).getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().getExclude().get(0).getClazz());
        Assert.assertEquals("custom", child.getCheckpointPolicy());
        Assert.assertEquals("15", child.getSkipLimit());
        Assert.assertEquals("5", child.getRetryLimit());
    }

    @Test
    public void readerProcessorWriter() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("chunk-mixed-child.xml", Job.class);
        JobMerger merger = new JobMerger(childJob);
        merger.merge();
        Chunk child = getChunk(childJob, "chunk-mixed-child");

        Assert.assertEquals(1, child.getReader().getProperties().getProperty().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(2, child.getProcessor().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child", "parent"}, true);
        Assert.assertEquals(1, child.getWriter().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getWriter().getProperties(), new String[]{"child"}, true);
    }

    @Test
    public void parentHasChunk() throws Exception {  //child step is empty and the chunk is declared in parent
        Job childJob = JobXmlLoader.loadJobXml("chunk-mixed-child.xml", Job.class);
        JobMerger merger = new JobMerger(childJob);
        merger.merge();
        Chunk child = getChunk(childJob, "parent-has-chunk-child");

        Assert.assertEquals("R1", child.getReader().getRef());
        Assert.assertEquals("P1", child.getProcessor().getRef());
        Assert.assertEquals("W1", child.getWriter().getRef());
        Assert.assertEquals(1, child.getReader().getProperties().getProperty().size());  //properties merge is false
        JobMergerTest.propertiesContain(child.getReader().getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(1, child.getProcessor().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProcessor().getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(1, child.getWriter().getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getWriter().getProperties(), new String[]{"child"}, true);
    }

    @Test
    public void exceptionClassFilter() throws Exception {
        Job childJob = JobXmlLoader.loadJobXml("chunk-mixed-child.xml", Job.class);
        JobMerger merger = new JobMerger(childJob);
        merger.merge();
        Chunk child = getChunk(childJob, "chunk-mixed-child");

        verifyExceptionClassesFilter((ExceptionClassFilterImpl) child.getSkippableExceptionClasses());
        verifyExceptionClassesFilter((ExceptionClassFilterImpl) child.getRetryableExceptionClasses());
        verifyExceptionClassesFilter((ExceptionClassFilterImpl) child.getNoRollbackExceptionClasses());
    }

    @Test
    public void exceptionClassFilter2() throws Exception {
        Job job = JobXmlLoader.loadJobXml("exception-class-filter.xml", Job.class);
        Chunk chunk = getChunk(job, "exception-class-filter") ;

        ExceptionClassFilterImpl filter = (ExceptionClassFilterImpl) chunk.getSkippableExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));

        filter = (ExceptionClassFilterImpl) chunk.getRetryableExceptionClasses();
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(IllegalStateException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));

        filter = (ExceptionClassFilterImpl) chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(RuntimeException.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        chunk = getChunk(job, "exception-class-filter2");
        filter = (ExceptionClassFilterImpl) chunk.getSkippableExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        filter = (ExceptionClassFilterImpl) chunk.getRetryableExceptionClasses();
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(IllegalArgumentException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(FileNotFoundException.class));

        filter = (ExceptionClassFilterImpl) chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assert.assertEquals(true, filter.matches(IllegalStateException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));
    }

    private void verifyExceptionClassesFilter(ExceptionClassFilterImpl filter) {
        Assert.assertEquals(true, filter.matches(RuntimeException.class));  //included
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));  //excluded
        Assert.assertEquals(true, filter.matches(IllegalArgumentException.class));  // superclass included
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));  //superclass excluded
        Assert.assertEquals(false, filter.matches(IOException.class));  //not included
        Assert.assertEquals(false, filter.matches(Exception.class));  //not included
        Assert.assertEquals(false, filter.matches(Throwable.class));
        Assert.assertEquals(false, filter.matches(Error.class));
    }

    protected static Chunk getChunk(Job job, String stepId) {
        List<Serializable> steps = job.getDecisionOrFlowOrSplit();
        for (Serializable s : steps) {
            if (s instanceof Step) {
                Step step = (Step) s;
                if (stepId.equals(step.getId())) {
                    return step.getChunk();
                }
            }
        }
        return null;
    }
}
