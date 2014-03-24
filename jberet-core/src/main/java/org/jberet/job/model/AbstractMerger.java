/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.batch.operations.JobStartException;

import org.jberet._private.BatchMessages;
import org.jberet.util.BatchUtil;

abstract class AbstractMerger<T> {
     T parent;
     T child;

    Job currentJob;
    ClassLoader classLoader;
    List<Job> loadedJobs;

    /**
     * For tracking cyclic inheritance
     */
    private List<T> inheritingElements;

    AbstractMerger(final Job currentJob, final ClassLoader classLoader, final List<Job> loadedJobs) {
        this.currentJob = currentJob;
        this.classLoader = classLoader;
        this.loadedJobs = loadedJobs;
    }

    /**
     * Checks if the element is already recorded in inheritingElements.  If yes, a JobStartException is thrown to
     * indicate cyclic inheritance.
     *
     * @param element   a job element of type Job, Step, or Flow
     * @param elementId the id of the element
     * @throws JobStartException if a inheritance cycle is detected
     */
    void checkInheritingElements(final T element, final String elementId) throws JobStartException {
        if (inheritingElements != null && inheritingElements.contains(element)) {
            final StringBuilder sb = BatchUtil.toElementSequence(inheritingElements);
            sb.append(elementId);
            throw BatchMessages.MESSAGES.cycleInheritance(sb.toString());
        }
    }

    /**
     * Records current parent and child elements in inheritingElements, and pass them along when creating a new
     * merger in order to resolve further inheritance.
     *
     * @param nextMerger the new merger for resolving the further inheritance
     */
    void recordInheritingElements(final AbstractMerger<T> nextMerger) {
        if (inheritingElements != null) {
            nextMerger.inheritingElements = inheritingElements;
        } else {
            nextMerger.inheritingElements = new ArrayList<T>();
        }
        if (!nextMerger.inheritingElements.contains(child)) {
            nextMerger.inheritingElements.add(child);
        }
        nextMerger.inheritingElements.add(parent);
    }

    /**
     * Merges parent properties and child properties.
     *
     * @param parent parent job element that can contain properties
     * @param child  child job element that can contain properties
     */
    static void mergeProperties(final PropertiesHolder parent, final PropertiesHolder child) {
        if (parent.getProperties() != null) {
            final Properties childProps = child.getProperties();
            final Properties parentProps = parent.getProperties();
            if (childProps == null) {
                child.setProperties(parentProps.clone());
            } else if (childProps.isMerge()) {
                for (final Map.Entry<String, String> e : parentProps.getPropertiesMapping().entrySet()) {
                    childProps.addIfAbsent(e.getKey(), e.getValue());
                }
            }
        }
    }

    static void mergeListeners(final InheritableJobElement parent, final InheritableJobElement child) {
        if (parent.getListeners() != null && !parent.getListeners().getListeners().isEmpty()) {
            final Listeners childListeners = child.getListeners();
            final List<RefArtifact> parentListenerList = parent.getListeners().getListeners();

            if (childListeners == null) {
                final Listeners newListeners = new Listeners();
                newListeners.getListeners().addAll(parentListenerList);
                child.setListeners(newListeners);
            } else if (childListeners.isMerge()) {
                final List<RefArtifact> childListenerList = childListeners.getListeners();
                for (final RefArtifact l : parentListenerList) {
                    final String pr = l.getRef();
                    boolean existsInChild = false;
                    for (final RefArtifact cr : childListenerList) {
                        if (cr.getRef().equals(pr)) {
                            existsInChild = true;
                            break;
                        }
                    }

                    if (!existsInChild) {
                        childListenerList.add(l);
                        //if the child already has the same-named listener, ignore the parent one,
                        //will not attempt to merge Properties under this listener
                    }
                }
            }
        }
    }
}
