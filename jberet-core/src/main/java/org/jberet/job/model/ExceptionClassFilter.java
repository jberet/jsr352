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

package org.jberet.job.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jberet.util.BatchLogger;

public final class ExceptionClassFilter implements Serializable {
    private static final long serialVersionUID = -6174512038188933722L;

    private final List<String> include = new ArrayList<String>();
    private final List<String> exclude = new ArrayList<String>();

    ExceptionClassFilter() {
    }

    public List<String> getInclude() {
        return include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    void addInclude(final String includeClass) {
        if (includeClass != null && includeClass.length() > 0) {
            include.add(includeClass);
        }
    }

    void addExclude(final String excludeClass) {
        if (excludeClass != null && excludeClass.length() > 0) {
            exclude.add(excludeClass);
        }
    }

    /**
     * Checks if an exception should be included or excluded.
     *
     * @param clazz the exception to check
     * @param cl    the class loader to check for the exception on
     *
     * @return {@code true} if the exception should be include, otherwise {@code false}
     */
    public boolean matches(final Class<? extends Throwable> clazz, final ClassLoader cl) {
        if (include.isEmpty()) {  //nothing is included, and exclude is ignored
            return false;
        } else {
            //only <include> is present
            if (exclude.isEmpty()) {
                return matches(clazz, cl, include);
            }
            //both <include> and <exclude> are present
            //if not covered by include, then return false
            if (!matches(clazz, cl, include)) {
                return false;
            }

            //by now it is covered by include, if it is covered by exclude
            return !matches(clazz, cl, exclude);
        }
    }

    /**
     * Checks if an exception class is covered by a the filter list, which can be either include or exclude list.
     *
     * @param clazz         the exception class to check
     * @param cl            the class loader used to check for the class on
     * @param filterClasses either the include or exclude filter list
     *
     * @return true if the exception class is covered by the filter; false otherwise.
     */
    private boolean matches(final Class<? extends Throwable> clazz, final ClassLoader cl, final List<String> filterClasses) {
        for (final String s : filterClasses) {
            try {
                Class<?> c = Class.forName(s, true, cl);
                if (c.isAssignableFrom(clazz)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                BatchLogger.LOGGER.invalidExceptionClassFilter(e, s);
            }
        }

        return false;
    }
}
