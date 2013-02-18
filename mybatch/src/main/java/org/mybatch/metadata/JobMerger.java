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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.batch.operations.exception.JobStartException;

import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.job.Property;
import org.mybatch.job.Step;
import org.mybatch.util.BatchUtil;

public class JobMerger {
    private Job parent;
    private Job child;

    /**
     * When this.child is also a parent, do not process steps under it.
     */
    private boolean skipEnclosingSteps;

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
            //check if parent has its own parent
            String parentParent = parent.getParent();
            if (parentParent != null) {
                JobMerger merger = new JobMerger(parentParent, parent, true);
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
        //check if each step has its own parent step, whether parent is null or not.
        if (!skipEnclosingSteps) {
            List<Step> steps = new ArrayList<Step>();
            for (Serializable s : child.getDecisionOrFlowOrSplit()) {
                if (s instanceof Step) {
                    steps.add((Step) s);
                }
            }
            for (Step s : steps) {
                StepMerger stepMerger = new StepMerger(s, steps);
                stepMerger.merge();
            }
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
        JobMerger.mergeProperties(parentProps, childProps);
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
        JobMerger.mergeListeners(parentListeners, childListeners);
    }

    /**
     * Merges parent properties and child properties (both must not be null).
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
