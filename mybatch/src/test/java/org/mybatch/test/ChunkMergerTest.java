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

import java.io.Serializable;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mybatch.job.Chunk;
import org.mybatch.job.Job;
import org.mybatch.job.Step;
import org.mybatch.metadata.ChunkMerger;
import org.mybatch.util.JaxbUtil;

public class ChunkMergerTest {
    @Test
    public void fromParentJob() throws Exception {
        Job parentJob = JaxbUtil.getJob("chunk-parent.xml");
        Chunk parent = getChunk(parentJob, "chunk-parent") ;

        Job childJob = JaxbUtil.getJob("chunk-child.xml");
        Chunk child = getChunk(childJob, "chunk-child");

        Assert.assertEquals(true, child.getProperties() == null || child.getProperties().getProperty().size() == 0);
        Assert.assertNull(child.getCheckpointAlgorithm());
        Assert.assertNull(child.getSkippableExceptionClasses());
        Assert.assertNull(child.getRetryableExceptionClasses());
        Assert.assertNull(child.getNoRollbackExceptionClasses());

        Assert.assertEquals("item", child.getCheckpointPolicy());  //default
        Assert.assertNull(child.getSkipLimit());
        Assert.assertNull(child.getRetryLimit());

        ChunkMerger merger = new ChunkMerger(parent, child);
        merger.merge();

//        TODO chunk listeners not in xsd yet
        Assert.assertEquals(1, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent"});
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
        Job parentJob = JaxbUtil.getJob("chunk-mixed-parent.xml");
        Chunk parent = getChunk(parentJob, "chunk-mixed-parent") ;

        Job childJob = JaxbUtil.getJob("chunk-mixed-child.xml");
        Chunk child = getChunk(childJob, "chunk-mixed-child");

        Assert.assertEquals(1, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"child"});
        Assert.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        Assert.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().getExclude().getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().getExclude().getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().getExclude().getClazz());

        Assert.assertEquals("custom", child.getCheckpointPolicy());
        Assert.assertEquals("15", child.getSkipLimit()) ;
        Assert.assertEquals(null, child.getRetryLimit()) ;

        ChunkMerger merger = new ChunkMerger(parent, child);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assert.assertEquals("child", child.getCheckpointAlgorithm().getRef());
        Assert.assertEquals("java.lang.RuntimeException", child.getSkippableExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getSkippableExceptionClasses().getExclude().getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getRetryableExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getRetryableExceptionClasses().getExclude().getClazz());
        Assert.assertEquals("java.lang.RuntimeException", child.getNoRollbackExceptionClasses().getInclude().getClazz());
        Assert.assertEquals("java.lang.IllegalStateException", child.getNoRollbackExceptionClasses().getExclude().getClazz());
        Assert.assertEquals("custom", child.getCheckpointPolicy());
        Assert.assertEquals("15", child.getSkipLimit());
        Assert.assertEquals("5", child.getRetryLimit());
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
