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

import org.junit.Assert;
import org.junit.Test;

public class BatchletMergerTest {
    @Test
    public void propertiesFromParentJob() throws Exception {
        final Job childJob = JobMergerTest.loadJob("batchlet-properties-child.xml");
        final RefArtifact child = getBatchlet(childJob, "batchlet-properties-child-step");
        //Assert.assertEquals(2, child.getProperties().getPropertiesMapping().size());
        //JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
        Assert.assertEquals(null, child.getProperties());
        Assert.assertEquals("batchlet-properties-child-batchlet", child.getRef());
    }

    @Test
    public void mergeFalse() throws Exception {
        final Job childJob = JobMergerTest.loadJob("batchlet-merge-false-child.xml");
        final RefArtifact child = getBatchlet(childJob, "batchlet-merge-false-child-step");
        Assert.assertEquals(0, child.getProperties().getPropertiesMapping().size());
        Assert.assertEquals("batchlet-merge-false-child-batchlet", child.getRef());
    }

    @Test
    public void mergeTrue() throws Exception {
        final Job childJob = JobMergerTest.loadJob("batchlet-merge-true-child.xml");
        final RefArtifact child = getBatchlet(childJob, "batchlet-merge-true-child-step");
        //Assert.assertEquals(2, child.getProperties().getPropertiesMapping().size());
        //JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assert.assertEquals(1, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"child"});
        Assert.assertEquals("batchlet-merge-true-child-batchlet", child.getRef());
    }

    @Test
    public void parentHasBatchlet() throws Exception {
        final Job childJob = JobMergerTest.loadJob("batchlet-merge-true-child.xml");
        final RefArtifact child = getBatchlet(childJob, "parent-has-batchlet-child");
        Assert.assertEquals(1, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent-has-batchlet-parent"}, true);
        Assert.assertEquals("parent-has-batchlet-parent-batchlet", child.getRef());
    }

    protected static RefArtifact getBatchlet(final Job job, final String stepId) {
        for (final JobElement e : job.getJobElements()) {
            if (e instanceof Step) {
                final Step step = (Step) e;
                if (stepId.equals(step.getId())) {
                    return step.getBatchlet();
                }
            }
        }
        return null;
    }
}
