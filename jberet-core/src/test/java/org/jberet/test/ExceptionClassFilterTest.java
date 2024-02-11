/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionClassFilterTest {
    @Test
    public void exceptionClassFilter2() throws Exception {
        final ClassLoader cl = this.getClass().getClassLoader();
        final Job job = ArchiveXmlLoader.loadJobXml("exception-class-filter.xml", cl, new ArrayList<Job>(), new MetaInfBatchJobsJobXmlResolver());
        Chunk chunk = getChunk(job, "exception-class-filter-step");

        ExceptionClassFilter filter = chunk.getSkippableExceptionClasses();
        Assertions.assertEquals(false, filter.matches(Exception.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assertions.assertEquals(true, filter.matches(RuntimeException.class));
        Assertions.assertEquals(true, filter.matches(IllegalStateException.class));
        Assertions.assertEquals(true, filter.matches(AlreadyConnectedException.class));
        Assertions.assertEquals(false, filter.matches(Exception.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assertions.assertEquals(false, filter.matches(Exception.class));
        Assertions.assertEquals(false, filter.matches(RuntimeException.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));
        Assertions.assertEquals(false, filter.matches(IllegalStateException.class));
        Assertions.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        chunk = getChunk(job, "exception-class-filter-step2");
        filter = chunk.getSkippableExceptionClasses();
        Assertions.assertEquals(false, filter.matches(Exception.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));
        Assertions.assertEquals(false, filter.matches(IllegalStateException.class));
        Assertions.assertEquals(false, filter.matches(AlreadyConnectedException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assertions.assertEquals(true, filter.matches(RuntimeException.class));
        Assertions.assertEquals(true, filter.matches(Exception.class));
        Assertions.assertEquals(true, filter.matches(IllegalArgumentException.class));
        Assertions.assertEquals(false, filter.matches(IllegalStateException.class));
        Assertions.assertEquals(false, filter.matches(AlreadyConnectedException.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));
        Assertions.assertEquals(true, filter.matches(FileNotFoundException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assertions.assertEquals(true, filter.matches(Exception.class));
        Assertions.assertEquals(true, filter.matches(RuntimeException.class));
        Assertions.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assertions.assertEquals(true, filter.matches(IllegalStateException.class));
        Assertions.assertEquals(true, filter.matches(AlreadyConnectedException.class));

        chunk = getChunk(job, "exception-class-filter-step3");
        filter = chunk.getSkippableExceptionClasses();
        Assertions.assertEquals(true, filter.matches(Exception.class));
        Assertions.assertEquals(true, filter.matches(java.io.EOFException.class));
        Assertions.assertEquals(false, filter.matches(IOException.class));
        Assertions.assertEquals(false, filter.matches(java.io.FileNotFoundException.class));
        Assertions.assertEquals(true, filter.matches(AlreadyConnectedException.class));

        filter = chunk.getRetryableExceptionClasses();
        Assertions.assertEquals(true, filter.matches(Exception.class));
        Assertions.assertEquals(true, filter.matches(jakarta.xml.ws.WebServiceException.class));
        Assertions.assertEquals(false, filter.matches(java.lang.RuntimeException.class));
        Assertions.assertEquals(true, filter.matches(IOException.class));
        Assertions.assertEquals(true, filter.matches(FileNotFoundException.class));
        Assertions.assertEquals(false, filter.matches(AlreadyConnectedException.class));
        Assertions.assertEquals(true, filter.matches(jakarta.xml.ws.ProtocolException.class));
        Assertions.assertEquals(true, filter.matches(jakarta.xml.ws.http.HTTPException.class));

        filter = chunk.getNoRollbackExceptionClasses();
        Assertions.assertEquals(true, filter.matches(IOException.class));
        Assertions.assertEquals(true, filter.matches(java.io.FileNotFoundException.class));
        Assertions.assertEquals(true, filter.matches(java.util.zip.ZipException.class));
        Assertions.assertEquals(true, filter.matches(java.util.jar.JarException.class));
        Assertions.assertEquals(false, filter.matches(Exception.class));
        Assertions.assertEquals(false, filter.matches(Throwable.class));
        Assertions.assertEquals(false, filter.matches(Error.class));
        Assertions.assertEquals(false, filter.matches(RuntimeException.class));
        Assertions.assertEquals(false, filter.matches(IllegalStateException.class));
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
