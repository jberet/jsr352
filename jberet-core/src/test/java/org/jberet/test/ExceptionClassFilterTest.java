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
import java.util.ArrayList;
import java.util.List;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Chunk;
import org.jberet.job.model.ExceptionClassFilter;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.Step;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionClassFilterTest {
    @Test
    public void exceptionClassFilter2() throws Exception {
        final ClassLoader cl = this.getClass().getClassLoader();
        final Job job = ArchiveXmlLoader.loadJobXml("exception-class-filter.xml", cl, new ArrayList<Job>());
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
        Assert.assertEquals(true, filter.matches(FileNotFoundException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(RuntimeException.class));
        Assert.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assert.assertEquals(true, filter.matches(IllegalStateException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));

        chunk = getChunk(job, "exception-class-filter-step3");
        filter = chunk.getSkippableExceptionClasses();
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(java.io.EOFException.class));
        Assert.assertEquals(false, filter.matches(IOException.class));
        Assert.assertEquals(false, filter.matches(java.io.FileNotFoundException.class));
        Assert.assertEquals(true, filter.matches(AlreadyConnectedException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assert.assertEquals(true, filter.matches(Exception.class));
        Assert.assertEquals(true, filter.matches(javax.xml.ws.WebServiceException.class));
        Assert.assertEquals(false, filter.matches(java.lang.RuntimeException.class));
        Assert.assertEquals(true, filter.matches(IOException.class));
        Assert.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assert.assertEquals(false, filter.matches(AlreadyConnectedException.class));
        Assert.assertEquals(true, filter.matches(javax.xml.ws.ProtocolException.class));
        Assert.assertEquals(true, filter.matches(javax.xml.ws.http.HTTPException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assert.assertEquals(true, filter.matches(IOException.class));
        Assert.assertEquals(true, filter.matches(java.io.FileNotFoundException.class));
        Assert.assertEquals(true, filter.matches(java.util.zip.ZipException.class));
        Assert.assertEquals(true, filter.matches(java.util.jar.JarException.class));
        Assert.assertEquals(false, filter.matches(Exception.class));
        Assert.assertEquals(false, filter.matches(Throwable.class));
        Assert.assertEquals(false, filter.matches(Error.class));
        Assert.assertEquals(false, filter.matches(RuntimeException.class));
        Assert.assertEquals(false, filter.matches(IllegalStateException.class));
    }

    protected static Chunk getChunk(final Job job, final String stepId) {
        final List<JobElement> steps = job.getJobElements();
        for (final JobElement s : steps) {
            if (s instanceof Step) {
                final Step step = (Step) s;
                if (stepId.equals(step.getId())) {
                    return step.getChunk();
                }
            }
        }
        return null;
    }
}
