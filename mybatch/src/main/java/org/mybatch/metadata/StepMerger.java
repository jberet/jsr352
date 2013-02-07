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

import java.util.List;

import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.job.Step;

public class StepMerger {
    private Step parent;
    private Step child;
    private List<Step> siblings;

    public StepMerger(Step child, List<Step> siblings) {
        this.child = child;
        String parentName = child.getParent();
        if (parentName != null) {
            if (siblings != null) {
                for (Step s : siblings) {
                    if (parentName.equals(s.getId())) {
                        this.parent = s;
                        this.siblings = siblings;
                    }
                }
            }
            if (this.parent == null) {
                this.parent = JobXmlLoader.loadJobXml(parentName, Step.class);
            }
        }
    }

    public StepMerger(Step parent, Step child, List<Step> siblings) {
        this.parent = parent;
        this.child = child;
        this.siblings = siblings;
    }

    public StepMerger(Step parent, Step child) {
        this(parent, child, null);
    }

    public void merge() {
        if (parent != null) {
            //check if parent has its own parent, which may be in the same or different job xml document
            String parentParent = parent.getParent();
            if (parentParent != null) {
                StepMerger merger = new StepMerger(parent, this.siblings);
                merger.merge();
            }

            //merge step attributes
            merge(parent.getProperties(), child.getProperties());
            merge(parent.getListeners(), child.getListeners());

            //if child has no batchlet or chunk, inherit from parent
            if (child.getBatchlet() == null && child.getChunk() == null) {
                if (parent.getChunk() != null) {
                    child.setChunk(parent.getChunk());
                } else {
                    child.setBatchlet(parent.getBatchlet());
                }
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

}
