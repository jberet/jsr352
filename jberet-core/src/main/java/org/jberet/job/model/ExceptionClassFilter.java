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

/**
 * Corresponds to {@code jsl:ExceptionClassFilter} job XML element type.
 * It represents job XML elements such as {@code skippable-exception-classes}, {@code retryable-exception-classes},
 * and {@code no-rollback-exception-classes}.
 */
public final class ExceptionClassFilter implements Serializable, Cloneable {
    private static final long serialVersionUID = -6174512038188933722L;

    final List<String> include = new ArrayList<String>();
    final List<String> exclude = new ArrayList<String>();

    /**
     * Indicates whether to merge with the counterpart element from the parent job element. Only applicable when the
     * current job element (e.g., a step) has a parent. The default merge value is true.
     */
    private boolean merge = true;

    /**
     * Adds an exception class fully-qualified name to either the include or exclude list.
     *
     * @param exceptionClass fully-qualified exception class name
     * @param includeOrExcludeList include or exclude list to add to
     */
    static void addExceptionClassTo(final String exceptionClass, final List<String> includeOrExcludeList) {
        final String trimmed = exceptionClass.trim();
        if (!trimmed.isEmpty()) {
            includeOrExcludeList.add(trimmed);
        }
    }

    /**
     * Checks whether to merge with the counterpart element from the parent job element.
     *
     * @return true (default) or false
     */
    boolean isMerge() {
        return merge;
    }

    /**
     * Sets the {@code merge} attribute when the current job element inherits from a parent.
     *
     * @param mergeVal {@code merge} attribute value ("true" or "false") as string
     */
    void setMerge(final String mergeVal) {
        if (mergeVal != null && !mergeVal.toLowerCase().equals("true")) {
            this.merge = false;
        }
    }

    /**
     * Checks if an exception should be included or excluded.
     *
     * @param clazz the exception to check
     * @return {@code true} if the exception should be include, otherwise {@code false}
     */
    public boolean matches(final Class<? extends Throwable> clazz) {
        if (include.isEmpty()) {  //nothing is included, and exclude is ignored
            return false;
        }
        final String clazzName = clazz.getName();
        if (include.contains(clazzName)) {
            return true;
        }
        if (exclude.contains(clazzName)) {
            return false;
        }
        //by now the exception class itself is not contained in either include or exclude list.
        //check its superclass against each element in include and exclude list.
        final int shortestDistanceToInclude = getShortestDistance(clazz, include);
        if (shortestDistanceToInclude == Integer.MAX_VALUE) {
            return false;
        } else if (shortestDistanceToInclude == 1) {
            return true;
        } else if (exclude.isEmpty()) {
            return true;
        }
        return shortestDistanceToInclude < getShortestDistance(clazz, exclude);
    }

    /**
     * Calculates the distance from clazz to each element of filterClasses, and return the min.
     *
     * @param clazz         the exception class to check all of its super classes
     * @param filterClasses the list of filter classes
     * @return the shortest distance
     */
    private int getShortestDistance(final Class<? extends Throwable> clazz, final List<String> filterClasses) {
        int result = Integer.MAX_VALUE;
        for (final String filterClass : filterClasses) {
            int distance = 0;
            boolean found = false;
            Class<?> superclass = clazz.getSuperclass();
            while (superclass != Throwable.class && superclass != Object.class) {
                distance++;
                if (superclass.getName().equals(filterClass)) {
                    found = true;
                    break;
                }
                superclass = superclass.getSuperclass();
            }

            if (found) {
                if (distance == 1) {
                    return distance;
                }
                if (distance < result) {
                    result = distance;
                }
            }
        }
        return result;
    }

    @Override
    protected ExceptionClassFilter clone() {
        final ExceptionClassFilter c = new ExceptionClassFilter();
        c.include.addAll(this.include);
        c.exclude.addAll(this.exclude);
        return c;
    }
}
