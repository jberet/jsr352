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

package org.jberet.metadata;

import java.util.ArrayList;
import java.util.List;

import org.jberet.job.ExceptionClassFilter;
import org.jberet.util.BatchLogger;
import org.jberet.util.BatchUtil;

/**
 * An extension of the jaxb-generated class to add exception-class-matching logic.
 */
final public class ExceptionClassFilterImpl extends ExceptionClassFilter {
    public boolean matches(Class<? extends Throwable> th) {
        if (include == null) {  //nothing is included, and exluce is ignored
            return false;
        } else {
            if (exclude == null) {
                return matches(th, classesFromIncludeExclude(include));
            }
            //both <include> and <exclude> are present
            if (!matches(th, classesFromIncludeExclude(include))) {
                return false;
            }
            return !matches(th, classesFromIncludeExclude(exclude));
        }
    }

    private List<String> classesFromIncludeExclude(List<? extends Object> includeExclude) {
        List<String> result = new ArrayList<String>();
        String s;
        for (Object obj : includeExclude) {
            if (obj instanceof Include) {
                s = ((Include) obj).getClazz().trim();
            } else {
                s = ((Exclude) obj).getClazz().trim();
            }
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    private boolean matches(Class<? extends Throwable> th, List<String> filterClasses) {
        for (String s : filterClasses) {
            try {
                Class<?> clazz = Class.forName(s, true, th.getClassLoader());
                if (clazz.isAssignableFrom(th)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                BatchLogger.LOGGER.invalidExceptionClassFilter(e, s);
            }
        }

        return false;
    }
}
