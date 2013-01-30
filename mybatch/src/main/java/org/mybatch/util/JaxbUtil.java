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
 
package org.mybatch.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.mybatch.job.Job;
import org.mybatch.metadata.JaxbObjectFactory;

import static org.mybatch.util.BatchLogger.LOGGER;

public class JaxbUtil {
    public final static String ARCHIVE_JOB_LOCATION = "META-INF/batch-jobs/";

    public static Job getJob(String jobName) {
        InputStream is;
        Job jobDefined;
        try {
            is = getJobXml(jobName);
        } catch (IOException e) {
            throw LOGGER.failToGetJobXml(e, jobName);
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Job.class);
            Unmarshaller um = jaxbContext.createUnmarshaller();
            try {
                um.setProperty("com.sun.xml.bind.ObjectFactory", new JaxbObjectFactory());
            } catch (PropertyException e) {
                um.setProperty("com.sun.xml.internal.bind.ObjectFactory", new JaxbObjectFactory());
            }
            JAXBElement<Job> root = um.unmarshal(new StreamSource(is), Job.class);
            jobDefined = root.getValue();
        } catch (JAXBException e) {
            throw LOGGER.failToParseBindJobXml(e, jobName);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return jobDefined;
    }

    private static InputStream getJobXml(String jobXml) throws IOException {
        // META-INF first
        String path = ARCHIVE_JOB_LOCATION + jobXml;
        InputStream is;
        is = BatchUtil.getBatchApplicationClassLoader().getResourceAsStream(path);
        if (is != null) {
            return is;
        }

        // javax.jobpath system property. jobpath format?
        File jobFile = null;
        String jobpath = System.getProperty("javax.jobpath");
        if (jobpath != null && !jobpath.isEmpty()) {
            String[] jobPathElements = jobpath.split(":");
            for (String p : jobPathElements) {
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
