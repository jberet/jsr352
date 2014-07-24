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

import java.security.PrivilegedAction;
import java.util.List;
import javax.batch.operations.JobStartException;

import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.util.BatchUtil;
import org.wildfly.security.manager.WildFlySecurityManager;

public final class FlowMerger extends AbstractMerger<Flow> {
    public FlowMerger(final Job job, final Flow child, final ClassLoader classLoader, final List<Job> loadedJobs)
            throws JobStartException {
        super(job, classLoader, loadedJobs);
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
            final Job jobOfParentFlow = ArchiveXmlLoader.loadJobXml(jslName, classLoader, loadedJobs);
            for (final JobElement e : jobOfParentFlow.getJobElements()) {
                if (parentName.equals(e.getId())) {
                    this.parent = (Flow) e;
                }
            }
        }
    }

    public void merge() throws JobStartException {
        checkInheritingElements(this.parent, this.parent.getId());

        //check if parent has its own parent, which may be in the same or different job xml document
        if (parent.getParent() != null) {
            final FlowMerger merger2 = new FlowMerger(currentJob, parent, classLoader, loadedJobs);
            recordInheritingElements(merger2);
            merger2.merge();
        }

        if (child.getAttributeNext() == null) {
            child.next= parent.next;
        }
        child.jobElements.clear();
        if (WildFlySecurityManager.isChecking()) {
            child.jobElements = WildFlySecurityManager.doUnchecked(new PrivilegedAction<List<JobElement>>() {
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
