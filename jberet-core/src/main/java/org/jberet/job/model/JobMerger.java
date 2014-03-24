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

public final class JobMerger extends AbstractMerger<Job> {
    public JobMerger(final Job child, final ClassLoader classLoader, final List<Job> loadedJobs) throws JobStartException {
        super(child, classLoader, loadedJobs);
        final String parentName = child.getParent();
        if (parentName != null) {
            this.parent = ArchiveXmlLoader.loadJobXml(parentName, classLoader, loadedJobs);
        }
        this.child = child;
    }

    public static void resolveInheritance(final Job job, final ClassLoader classLoader, final List<Job> loadedJobs)
            throws JobStartException {
        for (final InheritableJobElement e : job.inheritingJobElements) {
            if (e instanceof Step) {
                if (e.getParent() != null) {
                    final StepMerger stepMerger = new StepMerger(job, (Step) e, classLoader, loadedJobs);
                    stepMerger.merge();
                }
            } else if (e instanceof Job) {
                if (e.getParent() != null) {
                    final JobMerger jobMerger = new JobMerger((Job) e, classLoader, loadedJobs);
                    jobMerger.merge();
                }
            } else if (e instanceof Flow) {
                if (e.getParent() != null) {
                    final FlowMerger flowMerger = new FlowMerger(job, (Flow) e, classLoader, loadedJobs);
                    flowMerger.merge();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public void merge() throws JobStartException {
        checkInheritingElements(parent, parent.getId());

        //check if parent has its own parent
        if (parent.getParent() != null) {
            final JobMerger merger2 = new JobMerger(parent, classLoader, loadedJobs);
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
