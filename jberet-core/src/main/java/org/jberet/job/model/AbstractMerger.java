/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.job.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.batch.operations.JobStartException;

import org.jberet._private.BatchMessages;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;

/**
 * Abstract base class for various job element merger types, such as {@link JobMerger}, {@link StepMerger}, and
 * {@link FlowMerger}.
 *
 * @param <T> the job element type this merger class handles
 *
 * @see JobMerger
 * @see StepMerger
 * @see FlowMerger
 *
 * @since 1.0.1
 */
abstract class AbstractMerger<T extends AbstractJobElement> {
     T parent;
     T child;

    Job currentJob;
    ClassLoader classLoader;
    List<Job> loadedJobs;
    final JobXmlResolver jobXmlResolver;

    /**
     * For tracking cyclic inheritance
     */
    private List<T> inheritingElements;

    /**
     * Constructs common parts defined in this class for various concrete subclasses.
     *
     * @param currentJob the current job to be processed
     * @param classLoader the class loader to use for loading jobs and resources
     * @param loadedJobs list of already loaded jobs to avoid reloading them while resolving inheritance
     * @param jobXmlResolver job xml resolver, typically obtained from {@code org.jberet.spi.BatchEnvironment#getJobXmlResolver()}
     *
     * @see BatchEnvironment#getJobXmlResolver()
     */
    AbstractMerger(final Job currentJob, final ClassLoader classLoader, final List<Job> loadedJobs, final JobXmlResolver jobXmlResolver) {
        this.currentJob = currentJob;
        this.classLoader = classLoader;
        this.loadedJobs = loadedJobs;
        this.jobXmlResolver = jobXmlResolver;
    }

    /**
     * Checks if the element is already recorded in {@link #inheritingElements}.
     * If yes, a {@code JobStartException} is thrown to indicate cyclic inheritance.
     *
     * @param element   a job element of type {@code Job}, {@code Step}, or {@code Flow}
     * @param elementId the id of the element
     * @throws JobStartException if a inheritance cycle is detected
     */
    void checkInheritingElements(final T element, final String elementId) throws JobStartException {
        if (inheritingElements != null && inheritingElements.contains(element)) {
            final StringBuilder sb = new StringBuilder();
            for (final AbstractJobElement e : inheritingElements) {
                sb.append(e.id).append(" -> ");
            }
            sb.append(elementId);
            throw BatchMessages.MESSAGES.cycleInheritance(sb.toString());
        }
    }

    /**
     * Records current parent and child elements in {@link #inheritingElements}, and pass them along when creating a new
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

    /**
     * Merges listeners in parent with listeners in child element.
     *
     * @param parent parent job element that can contain listeners
     * @param child child job element that can contain listeners
     */
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
