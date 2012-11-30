/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.mybatch.metadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mybatch.util.BatchUtil;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

public class ApplicationMetaData {
    private static final Logger logger = Logger.getLogger(ApplicationMetaData.class.getName());

    private static final String batchArtifactFileName = "batch-artifacts.xml";

    private AnnotationDB annotationDB;

    private static String[] ignoredPkgs = {
//            "javax.batch.annotation"
    };

    private static Map<Class<? extends Annotation>, String> artifactAnnotations =
            new HashMap<Class<? extends Annotation>, String>();

    private static Map<String, ArtifactClassAndAnnotation> artifactCatalog = new HashMap<String, ArtifactClassAndAnnotation>();

    public ApplicationMetaData() throws IOException {
        annotationDB = new AnnotationDB();
        for (String s : ignoredPkgs) {
            annotationDB.addIgnoredPackages(s);
        }
        URL[] urls = ClasspathUrlFinder.findClassPaths();
//        System.out.println("classpath urls: ");
//        for (URL u : urls) {
//            System.out.println(u);
//        }

        annotationDB.setScanClassAnnotations(true);
        annotationDB.setScanMethodAnnotations(false);
        annotationDB.setScanFieldAnnotations(false);
        annotationDB.setScanParameterAnnotations(false);
        annotationDB.scanArchives(urls);

        initArtifactAnnotations();
        identifyArtifacts();
        outputBatchArtifactXml();
    }

    public Class<?> getClassForRef(String ref) {
        Class<?> cls = null;
        ArtifactClassAndAnnotation a = artifactCatalog.get(ref);
        if (a != null) {
            cls = a.artifactClass;
        }
        return cls;
    }

    private void outputBatchArtifactXml() {
        PrintWriter pw = null;
        try {
            File f = new File(batchArtifactFileName);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            pw = new PrintWriter(bw);
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<batch-artifacts>");

            for (Map.Entry<String, ArtifactClassAndAnnotation> entry : artifactCatalog.entrySet()) {
                String id = entry.getKey();
                ArtifactClassAndAnnotation val = entry.getValue();
                Class<?> cls = val.artifactClass;
                String tag = artifactAnnotations.get(val.annotationType);
                String line = String.format("    <%s id=\"%s\" class=\"%s\" />", tag, id, cls.getName());
                pw.println(line);
            }

            pw.println("</batch-artifacts>");
            pw.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private void identifyArtifacts() {
        Map<String, Set<String>> annotationIndex = annotationDB.getAnnotationIndex();
        for (Map.Entry<Class<? extends Annotation>, String> entry : artifactAnnotations.entrySet()) {
            Class<? extends Annotation> annotationToLook = entry.getKey();
            Set<String> matchingClasses = annotationIndex.get(annotationToLook.getName());
            if (matchingClasses != null) {
                for (String matchingClass : matchingClasses) {
                    String artifactName = null;
                    Class<?> cls = null;

                    try {
                        cls = BatchUtil.getBatchApplicationClassLoader().loadClass(matchingClass);
                        Annotation ann = cls.getAnnotation(annotationToLook);
                        Class<? extends Annotation> annType = ann.annotationType();
                        artifactName = (String) annType.getMethod("value").invoke(ann);
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.WARNING, e.getMessage());
                        continue;
                    } catch (NoSuchMethodException e) {
                        logger.log(Level.WARNING, e.getMessage());
                        continue;
                    } catch (IllegalAccessException e) {
                        logger.log(Level.WARNING, e.getMessage());
                        continue;
                    } catch (InvocationTargetException e) {
                        logger.log(Level.WARNING, e.getMessage());
                        continue;
                    }
                    if (artifactName == null || artifactName.isEmpty()) {
                        artifactName = cls.getSimpleName();
                    }
                    artifactCatalog.put(artifactName, new ArtifactClassAndAnnotation(cls, annotationToLook));
                }
            }
        }
    }

    private static void initArtifactAnnotations() {
        artifactAnnotations.put(javax.batch.annotation.Batchlet.class, "batchlet");
        artifactAnnotations.put(javax.batch.annotation.CheckpointAlgorithm.class, "checkpoint-algorithm");
        artifactAnnotations.put(javax.batch.annotation.CheckpointListener.class, "checkpoint-listener");
        artifactAnnotations.put(javax.batch.annotation.Decider.class, "decider");
        artifactAnnotations.put(javax.batch.annotation.ItemProcessListener.class, "item-processor-listener");
        artifactAnnotations.put(javax.batch.annotation.ItemProcessor.class, "item-processor");
        artifactAnnotations.put(javax.batch.annotation.ItemReader.class, "item-reader");
        artifactAnnotations.put(javax.batch.annotation.ItemReadListener.class, "item-reader-listener");
        artifactAnnotations.put(javax.batch.annotation.ItemWriteListener.class, "item-writer-listener");
        artifactAnnotations.put(javax.batch.annotation.ItemWriter.class, "item-writer");
        artifactAnnotations.put(javax.batch.annotation.JobListener.class, "job-listener");
        artifactAnnotations.put(javax.batch.annotation.PartitionAlgorithm.class, "partition-algorithm");
        artifactAnnotations.put(javax.batch.annotation.PartitionAnalyzer.class, "partition-analyzer");
        artifactAnnotations.put(javax.batch.annotation.PartitionCollector.class, "partition-collector");
        artifactAnnotations.put(javax.batch.annotation.RetryListener.class, "retry-listener");
        artifactAnnotations.put(javax.batch.annotation.SkipListener.class, "skip-listener");
        artifactAnnotations.put(javax.batch.annotation.StepListener.class, "step-listener");
    }

    public Map<String, Set<String>> getAnnotationIndex() {
        return annotationDB.getAnnotationIndex();
    }

    private final static class ArtifactClassAndAnnotation {
        public Class<?> artifactClass;
        public Class<? extends Annotation> annotationType;

        public ArtifactClassAndAnnotation(Class<?> artifactClass, Class<? extends Annotation> annotationType) {
            this.artifactClass = artifactClass;
            this.annotationType = annotationType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ArtifactClassAndAnnotation that = (ArtifactClassAndAnnotation) o;

            if (!annotationType.equals(that.annotationType)) return false;
            if (!artifactClass.equals(that.artifactClass)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = artifactClass.hashCode();
            result = 31 * result + annotationType.hashCode();
            return result;
        }
    }
}
