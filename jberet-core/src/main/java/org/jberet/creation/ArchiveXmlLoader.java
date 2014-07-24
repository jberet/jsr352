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

package org.jberet.creation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.batch.operations.JobStartException;

import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobMerger;
import org.jberet.job.model.JobParser;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jberet._private.BatchMessages.MESSAGES;

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
     * @param jobName base name of the job xml document
     * @return the job root element
     */
    public static Job loadJobXml(final String jobName, final ClassLoader classLoader, final List<Job> loadedJobs)
            throws JobStartException {
        for (final Job j : loadedJobs) {
            if (jobName.equals(j.getId())) {
                return j;
            }
        }

        Job job = null;
        final InputStream is;
        try {
            is = getJobXml(jobName, classLoader);
        } catch (final IOException e) {
            throw MESSAGES.failToGetJobXml(e, jobName);
        }

        try {
            job = JobParser.parseJob(is, classLoader);
            loadedJobs.add(job);
            if (!job.getInheritingJobElements().isEmpty()) {
                JobMerger.resolveInheritance(job, classLoader, loadedJobs);
            }
        } catch (final Exception e) {
            throw MESSAGES.failToParseJobXml(e, jobName);
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

    private static InputStream getJobXml(String jobXml, final ClassLoader classLoader) throws IOException {
        if (!jobXml.endsWith(".xml")) {
            jobXml += ".xml";
        }
        // META-INF first
        final String path = ARCHIVE_JOB_XML_DIR + jobXml;
        InputStream is;
        is = classLoader.getResourceAsStream(path);
        if (is != null) {
            return is;
        }

        // javax.jobpath system property. jobpath format?
        File jobFile = null;
        final String jobpath = WildFlySecurityManager.getPropertyPrivileged("javax.jobpath", null);
        if (jobpath != null && !jobpath.isEmpty()) {
            final String[] jobPathElements = jobpath.split(":");
            for (final String p : jobPathElements) {
                jobFile = new File(p, jobXml);
                if (jobFile.exists() && jobFile.isFile()) {
                    break;
                }
            }
        }

        // default location: current directory
        if (jobFile == null) {
            jobFile = new File(WildFlySecurityManager.getPropertyPrivileged("user.dir", "."), jobXml);
            if (!jobFile.exists() || !jobFile.isFile()) {
                //may be an absolute path to the job file
                jobFile = new File(jobXml);
            }
        }

        is = new BufferedInputStream(new FileInputStream(jobFile));
        return is;
    }
}
