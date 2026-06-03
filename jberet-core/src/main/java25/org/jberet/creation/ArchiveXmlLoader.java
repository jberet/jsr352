/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import jakarta.batch.operations.JobStartException;
import org.jberet._private.BatchMessages;
import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobMerger;
import org.jberet.job.model.JobParser;
import org.jberet.spi.JobXmlResolver;

public class ArchiveXmlLoader {
    public final static String ARCHIVE_JOB_XML_DIR = "META-INF/batch-jobs/";
    public final static String ARCHIVE_BATCH_XML = "META-INF/batch.xml";

    //public static final String JOB_XML_SCHEMA = "jobXML_1_0.xsd";
    //public static final String BATCH_XML_SCHEMA = "batchXML_1_0.xsd";

//    since inheritance is deferred from 1.0, this cache is not needed.
//    private static ConcurrentMap<String, Object> loadedJobsByName = new ConcurrentHashMap<String, Object>();

    /**
     * Gets the batch artifacts definition object, loaded from the archive batch.xml if available.
     *
     * @param classLoader the application classloader used to load batch xml
     * @return the batch artifacts definition object
     */
    public static BatchArtifacts loadBatchXml(final ClassLoader classLoader) throws JobStartException {
        BatchArtifacts batchArtifacts = null;
        final InputStream is = classLoader.getResourceAsStream(ARCHIVE_BATCH_XML);
        if (is == null) {  //the app doesn't contain META-INF/batch.xml
            return null;
        }

        try {
            batchArtifacts = JobParser.parseBatchArtifacts(is);
        } catch (Exception e) {
            throw MESSAGES.failToParseBatchXml(e, ARCHIVE_BATCH_XML);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                //ignore
            }
        }
        return batchArtifacts;
    }

    /**
     * Gets the job root element for a given job name.
     *
     * @param jobXmlName      base name of the job xml document
     * @param classLoader  the class loader used to locate the job
     * @param loadedJobs   a collections of jobs that have already been loaded
     * @param jobXmlResolver the job XML resolver
     *
     * @return the job root element
     *
     * @throws jakarta.batch.operations.JobStartException if the job failed to start
     */
    public static Job loadJobXml(final String jobXmlName, final ClassLoader classLoader, final List<Job> loadedJobs, final JobXmlResolver jobXmlResolver)
            throws JobStartException {
        for (final Job j : loadedJobs) {
            if (jobXmlName.equals(j.getJobXmlName() != null ? j.getJobXmlName() : j.getId())) {
                return j;
            }
        }

        Job job = null;
        final InputStream is;
        try {
            is = getJobXml(jobXmlName, classLoader, jobXmlResolver);
        } catch (final IOException e) {
            throw MESSAGES.failToGetJobXml(e, jobXmlName);
        }

        try {
            job = JobParser.parseJob(is, classLoader, new JobXmlEntityResolver(classLoader, jobXmlResolver));
            if (!jobXmlName.equals(job.getId())) {
                job.setJobXmlName(jobXmlName);
            }
            loadedJobs.add(job);
            if (!job.getInheritingJobElements().isEmpty()) {
                JobMerger.resolveInheritance(job, classLoader, loadedJobs, jobXmlResolver);
            }
        } catch (final Exception e) {
            throw MESSAGES.failToParseJobXml(e, jobXmlName);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }
        return job;
    }

    private static InputStream getJobXml(String jobXmlName, final ClassLoader classLoader, final JobXmlResolver jobXmlResolver) throws IOException {
        if (!jobXmlName.endsWith(".xml")) {
            jobXmlName += ".xml";
        }

        // Use the SPI to locate the job XML
        final InputStream is = jobXmlResolver.resolveJobXml(jobXmlName, classLoader);
        if (is != null) {
            return is;
        }
        throw BatchMessages.MESSAGES.failToGetJobXml(jobXmlName);
    }

    private static class JobXmlEntityResolver implements XMLResolver {
        private final ClassLoader classLoader;
        private final JobXmlResolver jobXmlResolver;

        private JobXmlEntityResolver(final ClassLoader classLoader, final JobXmlResolver jobXmlResolver) {
            this.classLoader = classLoader;
            this.jobXmlResolver = jobXmlResolver;
        }

        @Override
        public Object resolveEntity(final String publicID, final String systemID, final String baseURI, final String namespace) throws XMLStreamException {
            try {
                return getJobXml(systemID, classLoader, jobXmlResolver);
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }
        }
    }
}
