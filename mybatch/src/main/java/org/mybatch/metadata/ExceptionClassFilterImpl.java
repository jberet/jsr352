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

package org.mybatch.metadata;

import org.jboss.logging.Logger;
import org.mybatch.job.ExceptionClassFilter;
import org.mybatch.util.BatchLogger;

/**
 * An extension of the jaxb-generated class to add exception-class-matching logic.
 */
public class ExceptionClassFilterImpl extends ExceptionClassFilter {
    public boolean matches(Class<? extends Throwable> th) {
        if (include == null) {
            if (exclude == null) {
                return false;
            }
            return !matchesExclude(th);
        } else {
            if (exclude == null) {
                return matchesInclude(th);
            }
            //both <include> and <exclude> are present
            if (matchesExclude(th)) {
                return false;
            }
            return matchesInclude(th);
        }
    }

    private boolean matchesInclude(Class<? extends Throwable> th) {
        return matches(th, include.getClazz());
    }

    private boolean matchesExclude(Class<? extends Throwable> th) {
        return matches(th, exclude.getClazz());
    }

    private boolean matches(Class<? extends Throwable> th, String filterClasses) {
        filterClasses = filterClasses.trim();
        if (filterClasses.isEmpty()) {
            return false;
        }
        for (String s : filterClasses.split(", ")) {
            try {
                Class<?> clazz = Class.forName(s);
                if (clazz.isAssignableFrom(th)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                BatchLogger.LOGGER.invalidExceptionClassFilter(filterClasses, s);
            }
        }

        return false;
    }
}
