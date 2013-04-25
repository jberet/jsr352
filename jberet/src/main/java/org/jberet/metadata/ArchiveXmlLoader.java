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

package org.jberet.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import javax.batch.operations.JobStartException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jberet.job.BatchArtifacts;
import org.jberet.job.Job;
import org.jberet.util.BatchUtil;
import org.xml.sax.SAXException;

import static org.jberet.util.BatchLogger.LOGGER;

public class ArchiveXmlLoader {
    public final static String ARCHIVE_JOB_XML_DIR = "META-INF/batch-jobs/";
    public final static String ARCHIVE_BATCH_XML = "META-INF/batch.xml";
    public static final String JOB_XML_SCHEMA = "jobXML_1_0.xsd";
    public static final String BATCH_XML_SCHEMA = "batchXML_1_0.xsd";

//    since inheritance is deferred from 1.0, this cache is not needed.
//    private static ConcurrentMap<String, Object> loadedJobsByName = new ConcurrentHashMap<String, Object>();

    /**
     * Gets the batch artifacts definition object, loaded from the archive batch.xml if available.
     *
     * @param classLoader the applicaton classloader used to load batch xml
     * @return the batch artifacts definition object
     */
    public static BatchArtifacts loadBatchXml(ClassLoader classLoader) throws JobStartException {
        InputStream is;
        BatchArtifacts batchArtifacts = null;
        is = classLoader.getResourceAsStream(ARCHIVE_BATCH_XML);
        if (is == null) {  //the app doesn't contain META-INF/batch.xml
            return null;
        }

        InputStream schemaStream = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BatchArtifacts.class);
            Unmarshaller um = jaxbContext.createUnmarshaller();
            schemaStream = classLoader.getResourceAsStream(BATCH_XML_SCHEMA);
            um.setSchema(getSchema(BATCH_XML_SCHEMA, schemaStream));
            JAXBElement<BatchArtifacts> root = um.unmarshal(new StreamSource(is), BatchArtifacts.class);
            batchArtifacts = root.getValue();
        } catch (Exception e) {
            throw LOGGER.failToParseBindBatchXml(e, ARCHIVE_BATCH_XML);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (IOException e) {
                    //ignore
                }
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
     * @param cl       the applicaton classloader used to load job xml
     * @return the job or step root element
     */
    public static <T> T loadJobXml(String jobName, Class<T> rootType, ClassLoader... cl) throws JobStartException {
        Object jobOrStep = null;
        ClassLoader classLoader = cl.length > 0 ? cl[0] : BatchUtil.getBatchApplicationClassLoader();
        InputStream is;
        try {
            is = getJobXml(jobName, classLoader);
        } catch (IOException e) {
            throw LOGGER.failToGetJobXml(e, jobName);
        }

        InputStream schemaStream = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Job.class);
            Unmarshaller um = jaxbContext.createUnmarshaller();
            schemaStream = classLoader.getResourceAsStream(JOB_XML_SCHEMA);
            um.setSchema(getSchema(JOB_XML_SCHEMA, schemaStream));
            try {
                um.setProperty("com.sun.xml.bind.ObjectFactory", new JaxbObjectFactory());
            } catch (PropertyException e) {
                um.setProperty("com.sun.xml.internal.bind.ObjectFactory", new JaxbObjectFactory());
            }
            JAXBElement<T> root = um.unmarshal(new StreamSource(is), rootType);
            jobOrStep = root.getValue();
        } catch (Exception e) {
            throw LOGGER.failToParseBindJobXml(e, jobName);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return (T) jobOrStep;
    }

    private static Schema getSchema(String schemaLocation, InputStream is) throws URISyntaxException, SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(new StreamSource(is));
    }

    private static InputStream getJobXml(String jobXml, ClassLoader classLoader) throws IOException {
        if (!jobXml.endsWith(".xml")) {
            jobXml += ".xml";
        }
        // META-INF first
        String path = ARCHIVE_JOB_XML_DIR + jobXml;
        InputStream is;
        is = classLoader.getResourceAsStream(path);
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
