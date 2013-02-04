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

import org.mybatch.job.CheckpointAlgorithm;
import org.mybatch.job.Chunk;
import org.mybatch.job.ExceptionClassFilter;
import org.mybatch.job.Listeners;
import org.mybatch.job.Properties;
import org.mybatch.job.Step;

public class ChunkMerger {
    private Chunk parent;
    private Chunk child;

    public ChunkMerger(Chunk parent, Chunk child) {
        this.parent = parent;
        this.child = child;
    }

    public void merge() {
        //merge chunk attributes
        //TODO:
        //item-count
        //time-limit
        //buffer-items
        if (child.getCheckpointPolicy() == null && parent.getCheckpointPolicy() != null) {
            child.setCheckpointPolicy(parent.getCheckpointPolicy());
        }
        if (child.getSkipLimit()== null && parent.getSkipLimit() != null) {
            child.setSkipLimit(parent.getSkipLimit());
        }
        if (child.getRetryLimit() == null && parent.getRetryLimit() != null) {
            child.setRetryLimit(parent.getRetryLimit());
        }

        merge(parent.getProperties(), child.getProperties());
        //TODO wait for xsd update
        //merge reader, writer, processor, listeners
//        merge(parent.getListeners(), child.getListeners());

        merge(parent.getCheckpointAlgorithm(), child.getCheckpointAlgorithm());
        mergeSkippableExceptionClasses(parent.getSkippableExceptionClasses(), child.getSkippableExceptionClasses());
        mergeRetryableExceptionClasses(parent.getRetryableExceptionClasses(), child.getRetryableExceptionClasses());
        mergeNoRollbackExceptionClasses(parent.getNoRollbackExceptionClasses(), child.getNoRollbackExceptionClasses());
    }

    private void merge(CheckpointAlgorithm parentCheckPointAlgo, CheckpointAlgorithm childCheckPointAlgo) {
        if (parentCheckPointAlgo == null) {
            return;
        }
        if (childCheckPointAlgo == null) {
            child.setCheckpointAlgorithm(parentCheckPointAlgo);
            return;
        }
        JobMerger.mergeProperties(parentCheckPointAlgo.getProperties(), childCheckPointAlgo.getProperties());
    }

    private void mergeSkippableExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setSkippableExceptionClasses(parentExceptionClassFilter);
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
    }

    private void mergeRetryableExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setRetryableExceptionClasses(parentExceptionClassFilter);
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
    }

    private void mergeNoRollbackExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setNoRollbackExceptionClasses(parentExceptionClassFilter);
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
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


//    private void merge(Listeners parentListeners, Listeners childListeners) {
//        if (parentListeners == null) {
//            return;
//        }
//        if (childListeners == null) {
//            child.setListeners(parentListeners);
//            return;
//        }
//        JobMerger.mergeListeners(parentListeners, childListeners);
//    }

}
