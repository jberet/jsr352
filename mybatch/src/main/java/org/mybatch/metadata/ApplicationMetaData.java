/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012-2013, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mybatch.util.BatchUtil;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import static org.mybatch.util.BatchLogger.LOGGER;

public class ApplicationMetaData {
    private AnnotationDB annotationDB;

    //current default is {"javax", "java", "sun", "com.sun", "javassist"}
    private static String[] ignoredPkgs = {
    };

    private static Map<String, String> artifactCatalog = new HashMap<String, String>();

    public ApplicationMetaData() throws IOException {
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
    }

    public String getClassNameForRef(String ref) {
        return artifactCatalog.get(ref);
    }

    private void identifyArtifacts() {
        Map<String, Set<String>> annotationIndex = annotationDB.getAnnotationIndex();
        Set<String> namedClasses = annotationIndex.get("javax.inject.Named");
        if (namedClasses != null) {
            for (String matchingClass : namedClasses) {
                String refName;
                Class<?> cls;
                try {
                    cls = BatchUtil.getBatchApplicationClassLoader().loadClass(matchingClass);
                    refName = cls.getAnnotation(javax.inject.Named.class).value();
                } catch (ClassNotFoundException e) {
                    LOGGER.failToIdentifyArtifact(e);
                    continue;
                }
                if (refName.isEmpty()) {
                    refName = cls.getSimpleName();
                }
                artifactCatalog.put(refName, matchingClass);
            }
        }
    }
}
