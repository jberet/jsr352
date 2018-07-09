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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import javax.batch.operations.JobStartException;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Responsible for merging a child flow and its parent job, and resolving its JSL inheritance.
 *
 * @see AbstractMerger
 * @see JobMerger
 * @see StepMerger
 *
 * @since 1.0.1
 */
public final class FlowMerger extends AbstractMerger<Flow> {

    /**
     * Constructs a flow merger.
     *
     * @param job the current job
     * @param child the flow to resolve its JSL inheritance
     * @param classLoader the class loader to use for loading jobs and resources
     * @param loadedJobs list of already loaded jobs to avoid reloading them while resolving inheritance
     * @param jobXmlResolver job xml resolver, typically obtained from {@code org.jberet.spi.BatchEnvironment#getJobXmlResolver()}
     *
     * @throws JobStartException if failed to construct the flow merger
     *
     * @see BatchEnvironment#getJobXmlResolver()
     */
    FlowMerger(final Job job, final Flow child, final ClassLoader classLoader, final List<Job> loadedJobs, final JobXmlResolver jobXmlResolver)
            throws JobStartException {
        super(job, classLoader, loadedJobs, jobXmlResolver);
        this.child = child;
        final String parentName = child.getParent();
        final String jslName = child.getJslName();
        if (jslName == null || jslName.equals("*") || jslName.equals(job.id)) {
            for (final JobElement e : job.getJobElements()) {
                if (parentName.equals(e.getId())) {
                    this.parent = (Flow) e;
                    break;
                }
            }
        } else { // jslName points to a different jsl document
            final Job jobOfParentFlow = ArchiveXmlLoader.loadJobXml(jslName, classLoader, loadedJobs, jobXmlResolver);
            for (final JobElement e : jobOfParentFlow.getJobElements()) {
                if (parentName.equals(e.getId())) {
                    this.parent = (Flow) e;
                }
            }
        }
    }

    /**
     * Performs the merge, and if the parent flow has its own parent, it is also resolved.
     * This method does not process JSL inheritance in the flow's sub-element, which is done in
     * {@link JobMerger#resolveInheritance(Job, ClassLoader, List, JobXmlResolver)}
     *
     * @throws JobStartException if failed due to cyclic inheritance or other errors
     */
    public void merge() throws JobStartException {
        checkInheritingElements(this.parent, this.parent.getId());

        //check if parent has its own parent, which may be in the same or different job xml document
        if (parent.getParent() != null) {
            final FlowMerger merger2 = new FlowMerger(currentJob, parent, classLoader, loadedJobs, jobXmlResolver);
            recordInheritingElements(merger2);
            merger2.merge();
        }

        if (child.getAttributeNext() == null) {
            child.next= parent.next;
        }
        child.jobElements.clear();
        if (WildFlySecurityManager.isChecking()) {
            child.jobElements = AccessController.doPrivileged(new PrivilegedAction<List<JobElement>>() {
                @Override
                public List<JobElement> run() {
                    return BatchUtil.clone(parent.jobElements);
                }
            });
        } else {
            child.jobElements = BatchUtil.clone(parent.jobElements);
        }

        child.setParentAndJslName(null, null);
    }
}
