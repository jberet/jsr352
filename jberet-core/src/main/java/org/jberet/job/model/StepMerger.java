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

import org.jberet._private.BatchLogger;
import org.jberet.creation.ArchiveXmlLoader;

public final class StepMerger extends AbstractMerger<Step> {
    public StepMerger(final Job job, final Step child, final ClassLoader classLoader, final List<Job> loadedJobs)
            throws JobStartException {
        super(job, classLoader, loadedJobs);
        this.child = child;
        final String parentName = child.getParent();
        final String jslName = child.getJslName();
        if (jslName == null || jslName.equals("*") || jslName.equals(job.id)) {
            for (final JobElement e : job.getJobElements()) {
                if (parentName.equals(e.getId())) {
                    this.parent = (Step) e;
                    break;
                }
            }
        } else { // jslName points to a different jsl document
            final Job jobOfParentStep = ArchiveXmlLoader.loadJobXml(jslName, classLoader, loadedJobs);
            for (final JobElement e : jobOfParentStep.getJobElements()) {
                if (parentName.equals(e.getId())) {
                    this.parent = (Step) e;
                }
            }
        }
    }

    public void merge() throws JobStartException {
        checkInheritingElements(this.parent, this.parent.getId());
        if (parent.getParent() != null) {
            final StepMerger merger2 = new StepMerger(currentJob, parent, classLoader, loadedJobs);
            recordInheritingElements(merger2);
            merger2.merge();
        }

        if (child.getAttributeNext() == null) {
            child.setAttributeNext(parent.getAttributeNext());
        }
        if (child.getStartLimit() == null) {
            child.setStartLimit(parent.getStartLimit());
        }
        if (child.getAllowStartIfComplete() == null) {
            child.setAllowStartIfComplete(parent.getAllowStartIfComplete());
        }

        mergeProperties(parent, child);
        mergeListeners(parent, child);

        //merge batchlet or chunk element
        final RefArtifact parentBatchlet = parent.getBatchlet();
        final RefArtifact childBatchlet = child.getBatchlet();
        final Chunk parentChunk = parent.getChunk();
        final Chunk childChunk = child.getChunk();

        if (childChunk != null && childBatchlet == null) {  //child has chunk type
            if (parentChunk != null) {
                mergeChunk(parentChunk, childChunk);
            }
        } else if (childChunk == null && childBatchlet != null) {  //child has batchlet type
            //nothing to do. Parent and child batchlet elements may refer to different artifacts and they cannot be
            //merged in a meaninful way.
        } else if (childChunk == null) {  //if child has no batchlet or chunk, inherit from parent
            if (parentChunk != null) {
                child.setChunk(parentChunk.clone());
            } else if (parentBatchlet != null) {
                child.setBatchlet(parentBatchlet.clone());
            }
        } else {  //if child contains both chunk and batchlet
            BatchLogger.LOGGER.cannotContainBothChunkAndBatchlet(child.getId());
            throw new JobStartException();
        }

        //if child has no partition, use parent partition
        //if child has partition, always use child partition and ignore parent partition
        final Partition parentPartition = parent.getPartition();
        if (child.getPartition() == null && parentPartition != null) {
            child.setPartition(parentPartition.clone());
        }
        child.setParentAndJslName(null, null);
    }

    private void mergeChunk(final Chunk parentChunk, final Chunk childChunk) {
        if (childChunk.checkpointPolicy == null && parentChunk.checkpointPolicy != null) {
            childChunk.checkpointPolicy = parentChunk.checkpointPolicy;
        }
        if (childChunk.skipLimit == null && parentChunk.skipLimit != null) {
            childChunk.skipLimit = parentChunk.skipLimit;
        }
        if (childChunk.retryLimit == null && parentChunk.retryLimit != null) {
            childChunk.retryLimit = parentChunk.retryLimit;
        }
        if (childChunk.itemCount == null && parentChunk.itemCount != null) {
            childChunk.itemCount = parentChunk.itemCount;
        }
        if (childChunk.timeLimit == null && parentChunk.timeLimit != null) {
            childChunk.timeLimit = parentChunk.timeLimit;
        }

        //no properties or listeners directly under chunk
        if (childChunk.reader == null && parentChunk.reader != null) {
            childChunk.reader = parentChunk.reader.clone();
        }
        //if both parentChunk and childChunk have reader element, whether they refer to the same artifact or not,
        //do not attempt to merge them, assuming childChunk reader element overrides parentChunk reader element as a whole.

        if (childChunk.writer == null && parentChunk.writer != null) {
            childChunk.writer = parentChunk.writer.clone();
        }

        if (childChunk.processor == null && parentChunk.processor != null) {
            childChunk.processor = parentChunk.processor.clone();
        }

        if (childChunk.checkpointAlgorithm == null && parentChunk.checkpointAlgorithm != null) {
            childChunk.checkpointAlgorithm = parentChunk.checkpointAlgorithm.clone();
        }
        //childChunk and parentChunk checkpoint algorithm may refer to different artifacts that entail different properties,
        //so their properties cannot be merged in a meaningful way

        if (parentChunk.skippableExceptionClasses != null) {
            if (childChunk.skippableExceptionClasses == null) {
                //cannot assign parentSkippable directly to childChunk, since property resolution should happen independently
                childChunk.skippableExceptionClasses = parentChunk.skippableExceptionClasses.clone();
            } else if (childChunk.skippableExceptionClasses.isMerge()) {
                childChunk.skippableExceptionClasses.include.addAll(parentChunk.skippableExceptionClasses.include);
                childChunk.skippableExceptionClasses.exclude.addAll(parentChunk.skippableExceptionClasses.exclude);
            }
        }

        if (parentChunk.retryableExceptionClasses != null) {
            if (childChunk.retryableExceptionClasses == null) {
                childChunk.retryableExceptionClasses = parentChunk.retryableExceptionClasses.clone();
            } else if (childChunk.retryableExceptionClasses.isMerge()) {
                childChunk.retryableExceptionClasses.include.addAll(parentChunk.retryableExceptionClasses.include);
                childChunk.retryableExceptionClasses.exclude.addAll(parentChunk.retryableExceptionClasses.exclude);
            }
        }

        if (parentChunk.noRollbackExceptionClasses != null) {
            if (childChunk.noRollbackExceptionClasses == null) {
                childChunk.noRollbackExceptionClasses = parentChunk.noRollbackExceptionClasses.clone();
            } else if (childChunk.noRollbackExceptionClasses.isMerge()) {
                childChunk.noRollbackExceptionClasses.include.addAll(parentChunk.noRollbackExceptionClasses.include);
                childChunk.noRollbackExceptionClasses.exclude.addAll(parentChunk.noRollbackExceptionClasses.exclude);
            }
        }
    }
}
