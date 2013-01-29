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

import org.mybatch.job.Job;
import org.mybatch.job.Listeners;
import org.mybatch.job.Listener;
import org.mybatch.job.Properties;
import org.mybatch.job.Property;

public class JobMerger {
    public JobMerger(Job parent, Job child) {
        this.parent = parent;
        this.child = child;
    }

    private Job parent;
    private Job child;

    public void merge() {
        //merge job attributes
        if (child.getRestartable() == null && parent.getRestartable() != null) {
            child.setRestartable(parent.getRestartable());
        }

        merge(parent.getProperties(), child.getProperties());
        merge(parent.getListeners(), child.getListeners());

        //job steps, flows, and splits are not inherited
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

    public static void mergeProperties(Properties parentProps, Properties childProps) {
        String merge = childProps.getMerge();
        if(merge != null && !Boolean.parseBoolean(merge)) {
            return;
        }

        List<Property> childPropList = childProps.getProperty();
        for (Property p : parentProps.getProperty()) {
            childPropList.add(p);
        }
    }

    public static void mergeListeners(Listeners parentListeners, Listeners childListeners) {
        String merge = childListeners.getMerge();
        if(merge != null && !Boolean.parseBoolean(merge)) {
            return;
        }

        List<Listener> childListenerList = childListeners.getListener();
        for (Listener p : parentListeners.getListener()) {
            childListenerList.add(p);
        }
    }

}
