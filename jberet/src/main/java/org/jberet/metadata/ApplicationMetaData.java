/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.batch.operations.JobStartException;

import org.jberet.job.BatchArtifactRef;
import org.jberet.job.BatchArtifacts;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import static org.jberet.util.BatchLogger.LOGGER;

public class ApplicationMetaData {
    private AnnotationDB annotationDB;

    //current build-in default ignoredPkgs is {"javax", "java", "sun", "com.sun", "javassist"}
    private static String[] ignoredPkgs = {
    };

    private Map<String, String> artifactCatalog = new HashMap<String, String>();

    private BatchArtifacts batchArtifacts;

    private ClassLoader classLoader;

    public ApplicationMetaData(ClassLoader classLoader) throws IOException, JobStartException {
        this.classLoader = classLoader;
        annotationDB = new AnnotationDB();
        annotationDB.addIgnoredPackages(ignoredPkgs);
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

        identifyArtifacts();
        batchArtifacts = ArchiveXmlLoader.loadBatchXml(classLoader);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getClassNameForRef(String ref) {
        String result = artifactCatalog.get(ref);
        if (result != null) {
            return result;
        }
        if (batchArtifacts != null) {
            for (BatchArtifactRef r : batchArtifacts.getRef()) {
                if (r.getId().equals(ref)) {
                    return r.getClazz();
                }
            }
        }
        return ref;
    }

    private void identifyArtifacts() {
        Map<String, Set<String>> annotationIndex = annotationDB.getAnnotationIndex();
        Set<String> namedClasses = annotationIndex.get("javax.inject.Named");
        if (namedClasses != null) {
            for (String matchingClass : namedClasses) {
                String refName;
                Class<?> cls;
                try {
                    cls = this.classLoader.loadClass(matchingClass);
                    refName = cls.getAnnotation(javax.inject.Named.class).value();
                } catch (ClassNotFoundException e) {
                    LOGGER.failToIdentifyArtifact(e);
                    continue;
                }
                if (refName.isEmpty()) {
                    char chars[] = cls.getSimpleName().toCharArray();
                    chars[0] = Character.toLowerCase(chars[0]);
                    refName = new String(chars);
                }
                artifactCatalog.put(refName, matchingClass);
            }
        }
    }
}
