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

import org.jberet.job.model.BatchArtifacts;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import static org.jberet.util.BatchLogger.LOGGER;

public class ApplicationMetaData {
    private final AnnotationDB annotationDB;

    //current build-in default ignoredPkgs is {"javax", "java", "sun", "com.sun", "javassist"}
    private static final String[] ignoredPkgs = {
    };

    private final Map<String, String> artifactCatalog = new HashMap<String, String>();

    private final BatchArtifacts batchArtifacts;

    private final ClassLoader classLoader;

    public ApplicationMetaData(final ClassLoader classLoader) throws IOException, JobStartException {
        this.classLoader = classLoader;
        annotationDB = new AnnotationDB();
        annotationDB.addIgnoredPackages(ignoredPkgs);
        final URL[] urls = ClasspathUrlFinder.findClassPaths();
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

    public String getClassNameForRef(final String ref) {
        String result = artifactCatalog.get(ref);
        if (result != null) {
            return result;
        }
        if (batchArtifacts != null) {
            result = batchArtifacts.getClassNameForRef(ref);
            if (result != null) {
                return result;
            }
        }
        return ref;
    }

    private void identifyArtifacts() {
        final Map<String, Set<String>> annotationIndex = annotationDB.getAnnotationIndex();
        final Set<String> namedClasses = annotationIndex.get("javax.inject.Named");
        if (namedClasses != null) {
            for (final String matchingClass : namedClasses) {
                String refName;
                final Class<?> cls;
                try {
                    cls = this.classLoader.loadClass(matchingClass);
                    refName = cls.getAnnotation(javax.inject.Named.class).value();
                } catch (ClassNotFoundException e) {
                    LOGGER.failToIdentifyArtifact(e);
                    continue;
                }
                if (refName.isEmpty()) {
                    final char[] chars = cls.getSimpleName().toCharArray();
                    chars[0] = Character.toLowerCase(chars[0]);
                    refName = new String(chars);
                }
                artifactCatalog.put(refName, matchingClass);
            }
        }
    }
}
