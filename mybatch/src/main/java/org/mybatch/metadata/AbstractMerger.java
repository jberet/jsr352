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

import java.util.ArrayList;
import java.util.List;
import javax.batch.operations.JobStartException;

import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.job.Property;
import org.mybatch.util.BatchLogger;
import org.mybatch.util.BatchUtil;

public abstract class AbstractMerger<T> {
    protected T parent;
    protected T child;

    /**
     * For tracking cyclic inheritance
     */
    private List<T> inheritingElements;

    /**
     * Checks if the element is already recorded in inheritingElements.  If yes, a JobStartException is thrown to
     * indicate cyclic inheritance.
     * @param element a job element of type Job, Step, or Flow
     * @param elementId the id of the element
     * @throws JobStartException if a inheritance cycle is detected
     */
    protected void checkInheritingElements(T element, String elementId) throws JobStartException {
        if (inheritingElements != null && inheritingElements.contains(element)) {
            StringBuilder sb = BatchUtil.toElementSequence(inheritingElements);
            sb.append(elementId);
            throw BatchLogger.LOGGER.cycleInheritance(sb.toString());
        }
    }

    /**
     * Records current parent and child elements in inheritingElements, and pass them along when creating a new
     * merger in order to resolve further inheritance.
     * @param nextMerger the new merger for resolving the further inheritance
     */
    protected void recordInheritingElements(AbstractMerger<T> nextMerger) {
        if (inheritingElements != null) {
            nextMerger.inheritingElements = inheritingElements;
        } else {
            nextMerger.inheritingElements = new ArrayList<T>();
        }
        if(!nextMerger.inheritingElements.contains(child)) {
            nextMerger.inheritingElements.add(child);
        }
        nextMerger.inheritingElements.add(parent);
    }

    /**
     * Merges parent properties and child properties (both must not be null).
     *
     * @param parentProps properties from parent element
     * @param childProps  properties from child element
     */
    public static void mergeProperties(Properties parentProps, Properties childProps) {
        String merge = childProps.getMerge();
        if (merge != null && !Boolean.parseBoolean(merge)) {
            return;
        }

        List<Property> childPropList = childProps.getProperty();
        for (Property p : parentProps.getProperty()) {
            if (!BatchUtil.propertiesContains(childProps, p.getName())) {
                childPropList.add(p);
            }
        }
    }

    public static void mergeListeners(Listeners parentListeners, Listeners childListeners) {
        String merge = childListeners.getMerge();
        if (merge != null && !Boolean.parseBoolean(merge)) {
            return;
        }

        List<Listener> childListenerList = childListeners.getListener();
        for (Listener l : parentListeners.getListener()) {
            if (!BatchUtil.listenersContains(childListeners, l)) {
                childListenerList.add(l);
            }
        }
    }
}
