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

package org.jberet.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.AlreadyConnectedException;
import java.util.List;

import org.jberet.job.model.Chunk;
import org.jberet.job.model.ExceptionClassFilter;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Step;
import org.jberet.metadata.ArchiveXmlLoader;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionClassFilterTest {
    @Test
    public void exceptionClassFilter2() throws Exception {
        Job job = ArchiveXmlLoader.loadJobXml("exception-class-filter.xml", Job.class);
        Chunk chunk = getChunk(job, "exception-class-filter-step");

        ExceptionClassFilter filter = chunk.getSkippableExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(IllegalStateException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(RuntimeException.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        chunk = getChunk(job, "exception-class-filter-step2");
        filter = chunk.getSkippableExceptionClasses();
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(IllegalArgumentException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(FileNotFoundException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assert.assertEquals(true, filter.matches(IllegalStateException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));
    }

    private void verifyExceptionClassesFilter(ExceptionClassFilter filter) {
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
        List<JobElement> steps = job.getJobElements();
        for (JobElement s : steps) {
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
