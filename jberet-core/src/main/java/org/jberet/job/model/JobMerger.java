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

import java.util.List;
import javax.batch.operations.JobStartException;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;

/**
 * Responsible for merging a child job and its parent job, and resolving its JSL inheritance.
 *
 * @see AbstractMerger
 * @see StepMerger
 * @see FlowMerger
 *
 * @since 1.0.1
 */
public final class JobMerger extends AbstractMerger<Job> {

    /**
     * Constructs a job merger.
     *
     * @param child the job to resolve its JSL inheritance
     * @param classLoader the class loader to use for loading jobs and resources
     * @param loadedJobs list of already loaded jobs to avoid reloading them while resolving inheritance
     * @param jobXmlResolver job xml resolver, typically obtained from {@code org.jberet.spi.BatchEnvironment#getJobXmlResolver()}
     *
     * @throws JobStartException if failed to construct the job merger
     *
     * @see BatchEnvironment#getJobXmlResolver()
     */
    private JobMerger(final Job child, final ClassLoader classLoader, final List<Job> loadedJobs,  final JobXmlResolver jobXmlResolver) throws JobStartException {
        super(child, classLoader, loadedJobs, jobXmlResolver);
        final String parentName = child.getParent();
        if (parentName != null) {
            this.parent = ArchiveXmlLoader.loadJobXml(parentName, classLoader, loadedJobs, jobXmlResolver);
        }
        this.child = child;
    }

    /**
     * The main entry point to resolve JSL inheritance for a job, including any inheritance in its sub-elements.
     *
     * @param job the job to resolve its JSL inheritance
     * @param classLoader the class loader to use for loading jobs and resources
     * @param loadedJobs list of already loaded jobs to avoid reloading them while resolving inheritance
     * @param jobXmlResolver job xml resolver, typically obtained from {@code org.jberet.spi.BatchEnvironment#getJobXmlResolver()}
     *
     * @throws JobStartException if failed to resolve the job's inheritance
     */
    public static void resolveInheritance(final Job job, final ClassLoader classLoader, final List<Job> loadedJobs, final JobXmlResolver jobXmlResolver)
            throws JobStartException {
        for (final InheritableJobElement e : job.inheritingJobElements) {
            if (e instanceof Step) {
                if (e.getParent() != null) {
                    final StepMerger stepMerger = new StepMerger(job, (Step) e, classLoader, loadedJobs, jobXmlResolver);
                    stepMerger.merge();
                }
            } else if (e instanceof Job) {
                if (e.getParent() != null) {
                    final JobMerger jobMerger = new JobMerger((Job) e, classLoader, loadedJobs, jobXmlResolver);
                    jobMerger.merge();
                }
            } else if (e instanceof Flow) {
                if (e.getParent() != null) {
                    final FlowMerger flowMerger = new FlowMerger(job, (Flow) e, classLoader, loadedJobs, jobXmlResolver);
                    flowMerger.merge();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Performs the merge, and if the parent job has its own parent, it is also resolved.
     * This method does not process JSL inheritance in the job's sub-element, which is done in
     * {@link #resolveInheritance(Job, ClassLoader, List, JobXmlResolver)}
     *
     * @throws JobStartException if failed due to cyclic inheritance or other errors
     */
    public void merge() throws JobStartException {
        checkInheritingElements(parent, parent.getId());

        //check if parent has its own parent
        if (parent.getParent() != null) {
            final JobMerger merger2 = new JobMerger(parent, classLoader, loadedJobs, jobXmlResolver);
            recordInheritingElements(merger2);
            merger2.merge();
        }

        //merge job attributes
        if (child.getRestartable() == null) {
            child.setRestartable(parent.getRestartable());
        }

        mergeProperties(parent, child);
        mergeListeners(parent, child);
        child.setParentAndJslName(null, null);
    }
}
