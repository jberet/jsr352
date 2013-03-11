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
import org.mybatch.job.ItemProcessor;
import org.mybatch.job.ItemReader;
import org.mybatch.job.ItemWriter;
import org.mybatch.job.Properties;

public final class ChunkMerger extends AbstractMerger<Chunk> {
    public ChunkMerger(Chunk parent, Chunk child) {
        this.parent = parent;
        this.child = child;
    }

    public void merge() {
        //merge chunk attributes
        if (child.getCheckpointPolicy() == null && parent.getCheckpointPolicy() != null) {
            child.setCheckpointPolicy(parent.getCheckpointPolicy());
        }
        if (child.getSkipLimit() == null && parent.getSkipLimit() != null) {
            child.setSkipLimit(parent.getSkipLimit());
        }
        if (child.getRetryLimit() == null && parent.getRetryLimit() != null) {
            child.setRetryLimit(parent.getRetryLimit());
        }
        if (child.getItemCount() == null && parent.getItemCount() != null) {
            child.setItemCount(parent.getItemCount());
        }
        if (child.getTimeLimit() == null && parent.getTimeLimit() != null) {
            child.setTimeLimit(parent.getTimeLimit());
        }

        //no properties or listeners directly under chunk
        merge(parent.getReader(), child.getReader());
        merge(parent.getWriter(), child.getWriter());
        merge(parent.getProcessor(), child.getProcessor());
        merge(parent.getCheckpointAlgorithm(), child.getCheckpointAlgorithm());
        mergeSkippableExceptionClasses(parent.getSkippableExceptionClasses(), child.getSkippableExceptionClasses());
        mergeRetryableExceptionClasses(parent.getRetryableExceptionClasses(), child.getRetryableExceptionClasses());
        mergeNoRollbackExceptionClasses(parent.getNoRollbackExceptionClasses(), child.getNoRollbackExceptionClasses());
    }

    private void merge(ItemReader parentReader, ItemReader childReader) {
        if (parentReader == null) {
            return;
        }
        if (childReader == null) {
            child.setReader(parentReader);
            return;
        }
        Properties parentProps = parentReader.getProperties();
        Properties childProps  = childReader.getProperties();
        if (parentProps == null) {
            return;
        }
        if (childProps == null) {
            childReader.setProperties(parentProps);
            return;
        }
        AbstractMerger.mergeProperties(parentProps, childProps);
    }

    private void merge(ItemWriter parentWriter, ItemWriter childWriter) {
        if (parentWriter == null) {
            return;
        }
        if (childWriter == null) {
            child.setWriter(parentWriter);
            return;
        }
        Properties parentProps = parentWriter.getProperties();
        Properties childProps  = childWriter.getProperties();
        if (parentProps == null) {
            return;
        }
        if (childProps == null) {
            childWriter.setProperties(parentProps);
            return;
        }
        AbstractMerger.mergeProperties(parentProps, childProps);
    }

    private void merge(ItemProcessor parentProcessor, ItemProcessor childProcessor) {
        if (parentProcessor == null) {
            return;
        }
        if (childProcessor == null) {
            child.setProcessor(parentProcessor);
            return;
        }
        Properties parentProps = parentProcessor.getProperties();
        Properties childProps  = childProcessor.getProperties();
        if (parentProps == null) {
            return;
        }
        if (childProps == null) {
            childProcessor.setProperties(parentProps);
            return;
        }
        AbstractMerger.mergeProperties(parentProps, childProps);
    }

    private void merge(CheckpointAlgorithm parentCheckPointAlgo, CheckpointAlgorithm childCheckPointAlgo) {
        if (parentCheckPointAlgo == null) {
            return;
        }
        if (childCheckPointAlgo == null) {
            child.setCheckpointAlgorithm(parentCheckPointAlgo);
            return;
        }
        AbstractMerger.mergeProperties(parentCheckPointAlgo.getProperties(), childCheckPointAlgo.getProperties());
    }

    private void mergeSkippableExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setSkippableExceptionClasses(parentExceptionClassFilter);
            return;
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
    }

    private void mergeRetryableExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setRetryableExceptionClasses(parentExceptionClassFilter);
            return;
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
    }

    private void mergeNoRollbackExceptionClasses(ExceptionClassFilter parentExceptionClassFilter, ExceptionClassFilter childExceptionClassFilter) {
        if (parentExceptionClassFilter == null) {
            return;
        }
        if (childExceptionClassFilter == null) {
            child.setNoRollbackExceptionClasses(parentExceptionClassFilter);
            return;
        }
        //<include> and <exclude> sub-elements from parent and child are not merged, kept as is
    }
}
