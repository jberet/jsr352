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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.batch.operations.exception.JobStartException;

import org.mybatch.job.Flow;
import org.mybatch.job.Job;
import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.job.Split;
import org.mybatch.job.Step;

public final class JobMerger extends AbstractMerger<Job> {
    Set<Step> mergedSteps = new HashSet<Step>();

    /**
     * When this.child is also a parent, do not process steps under it.
     */
    private boolean skipEnclosingSteps;

    /**
     * no-arg constructor, used by tests.
     */
    public JobMerger() {
    }

    public JobMerger(Job child) throws JobStartException {
        String parentName = child.getParent();
        if (parentName != null) {
            this.parent = ArchiveXmlLoader.loadJobXml(parentName, Job.class);
        }
        this.child = child;
    }

    private JobMerger(String parentName, Job child, boolean skipSteps) throws JobStartException {
        this(ArchiveXmlLoader.loadJobXml(parentName, Job.class), child);
        this.skipEnclosingSteps = skipSteps;
    }

    public JobMerger(Job parent, Job child) {
        this.parent = parent;
        this.child = child;
    }

    public void merge() throws JobStartException {
        if (parent != null) {
            checkInheritingElements(parent, parent.getId());

            //check if parent has its own parent
            String parentParent = parent.getParent();
            if (parentParent != null) {
                JobMerger merger = new JobMerger(parentParent, parent, true);
                recordInheritingElements(merger);
                merger.merge();
            }

            //merge job attributes
            if (child.getRestartable() == null && parent.getRestartable() != null) {
                child.setRestartable(parent.getRestartable());
            }

            merge(parent.getProperties(), child.getProperties());
            merge(parent.getListeners(), child.getListeners());
        }

        //job steps, flows, and splits are not inherited.
        //check if each step has its own parent step
        if (!skipEnclosingSteps) {
            LinkedList<Step> steps = new LinkedList<Step>();
            List<?> elements = child.getDecisionOrFlowOrSplit();
            for (Object s : elements) {
                if (s instanceof Step) {
                    steps.add((Step) s);
                }
            }
            for (Step s : steps) {
                if (!mergedSteps.contains(s)) {
                    StepMerger stepMerger = new StepMerger(s, steps, this);
                    stepMerger.merge();
                    mergedSteps.add(s);
                }
            }
            for (Object e : elements) {
                if (e instanceof Flow) {
                    merge((Flow) e, steps);
                } else if (e instanceof Split) {
                    merge((Split) e, steps);
                }
            }
        }
    }

    /**
     * Merges flow steps.  A flow does not have a parent, but its steps may have parent.
     *
     * @param flow  the flow whose steps need to be merged
     * @param steps the steps at the same level with the flow, which may contain parent steps for flow step
     */
    private void merge(Flow flow, LinkedList<Step> steps) throws JobStartException {
        List<?> elements = flow.getDecisionOrStepOrSplit();
        for (Object e : elements) {
            if (e instanceof Step) {
                steps.add((Step) e);
            }
        }
        for (Step s : steps) {
            if (!mergedSteps.contains(s)) {
                StepMerger stepMerger = new StepMerger(s, steps, this);
                stepMerger.merge();
                mergedSteps.add(s);
            }
        }
        for (Object e : elements) {
            if (e instanceof Split) {
                merge((Split) e, steps);
            }
        }
    }

    /**
     * Merges split steps.  A split does not have a parent, but its steps may have parent.
     *
     * @param split the split whose steps need to be merged
     * @param steps the steps at the same level with the split, which may contain parent steps for split step
     */
    private void merge(Split split, LinkedList<Step> steps) throws JobStartException {
        List<Flow> flows = split.getFlow();
        for (Flow f : flows) {
            merge(f, steps);
        }
    }

    private void merge(Properties parentProps, Properties childProps) {
        if (parentProps == null) {
            return;
        }
        if (childProps == null) {
            child.setProperties(parentProps);
            return;
        }
        AbstractMerger.mergeProperties(parentProps, childProps);
        //for job-level properties, ignore Properties partition attribute
    }

    private void merge(Listeners parentListeners, Listeners childListeners) {
        if (parentListeners == null) {
            return;
        }
        if (childListeners == null) {
            child.setListeners(parentListeners);
            return;
        }
        AbstractMerger.mergeListeners(parentListeners, childListeners);
    }

}
