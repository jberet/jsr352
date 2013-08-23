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
import javax.batch.operations.JobStartException;

import org.jberet.job.model.BatchArtifacts;
import org.jberet.job.model.JobParser;
import org.jberet.util.BatchUtil;

import static org.jberet.util.BatchLogger.LOGGER;

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
            throw LOGGER.failToParseBatchXml(e, ARCHIVE_BATCH_XML);
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
     * Gets the root element, either of type Job or Step, for a given job or step name.
     *
     * @param jobName  base name of the job xml document
     * @param rootType Job.class or Step.class
     * @param <T>      Job or Step
     * @param cl       the application classloader used to load job xml
     * @return the job or step root element
     */
    public static <T> T loadJobXml(final String jobName, final Class<T> rootType, final ClassLoader... cl) throws JobStartException {
        Object jobOrStep = null;
        final ClassLoader classLoader = cl.length > 0 ? cl[0] : BatchUtil.getBatchApplicationClassLoader();
        final InputStream is;
        try {
            is = getJobXml(jobName, classLoader);
        } catch (IOException e) {
            throw LOGGER.failToGetJobXml(e, jobName);
        }

        try {
            jobOrStep = JobParser.parseJob(is);
        } catch (Exception e) {
            throw LOGGER.failToParseJobXml(e, jobName);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return (T) jobOrStep;
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
        final String jobpath = System.getProperty("javax.jobpath");
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
            jobFile = new File(System.getProperty("user.dir"), jobXml);
        }

        is = new BufferedInputStream(new FileInputStream(jobFile));
        return is;
    }
}
