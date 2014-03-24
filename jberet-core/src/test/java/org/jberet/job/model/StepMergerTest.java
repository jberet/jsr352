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

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Test;

public class StepMergerTest {
    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-properties-listeners-child.xml");
        final Step child = getStep(childJob, "step-properties-listeners-child-step");
        Assert.assertEquals(2, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
        Assert.assertEquals(2, child.getListeners().getListeners().size());
    }

    @Test
    public void mergeFalse() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-merge-false-child.xml");
        final Step child = getStep(childJob, "step-merge-false-child-step");
        Assert.assertEquals(0, child.getProperties().getPropertiesMapping().size());
        Assert.assertEquals(0, child.getListeners().getListeners().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-merge-true-child.xml");
        final Step child = getStep(childJob, "step-merge-true-child-step");
        Assert.assertEquals(2, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assert.assertEquals(2, child.getListeners().getListeners().size());
    }

    @Test
    public void stepSameFileParentChild() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-same-file-parent-child");
        final String[] child1And2 = new String[]{"step-same-file-child-1", "step-same-file-child-2"};
        Step child;
        for (final String c : child1And2) {
            child = getStep(childJob, c);
            Assert.assertEquals(2, child.getProperties().getPropertiesMapping().size());
            JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"}, true);
            //Assert.assertEquals(3, child.getListeners().getListeners().size());
            Assert.assertEquals(2, child.getListeners().getListeners().size());  //remove dup listeners
            JobMergerTest.listenersContain(child.getListeners(), new String[]{"parent-listener", "child-listener"});
            Assert.assertEquals("child-batchlet", child.getBatchlet().getRef());
        }

        child = getStep(childJob, "step-same-file-child-child");
        Assert.assertEquals(3, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child", "child-child"}, true);
        //Assert.assertEquals(4, child.getListeners().getListeners().size());
        Assert.assertEquals(3, child.getListeners().getListeners().size());  //remove dup listeners
        JobMergerTest.listenersContain(child.getListeners(), new String[]{"parent-listener", "child-listener", "child-child-listener"});
        Assert.assertEquals("child-child-batchlet", child.getBatchlet().getRef());
    }

    @Test
    public void emptyParent() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-same-file-parent-child");
        final Step child = getStep(childJob, "inheriting-empty-parent");
        emptyStep(child);
    }

    @Test
    public void emptyChild() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-same-file-parent-child");
        final Step child = getStep(childJob, "empty-child");
        emptyStep(child);
    }

    private void emptyStep(final Step child) throws Exception {
        Assert.assertEquals(1, child.getProperties().getPropertiesMapping().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"child"}, true);
        Assert.assertEquals(1, child.getListeners().getListeners().size());
        JobMergerTest.listenersContain(child.getListeners(), new String[]{"child-listener"});
        Assert.assertEquals("child-batchlet", child.getBatchlet().getRef());
    }

    protected static LinkedList<Step> getSteps(final Job job) {
        final LinkedList<Step> results = new LinkedList<Step>();
        for (final JobElement e : job.getJobElements()) {
            if (e instanceof Step) {
                results.add((Step) e);
            }
        }
        return results;
    }

    protected static Step getStep(final Job job, final String stepId) {
        for (final Step s : getSteps(job)) {
            if (s.getId().equals(stepId)) {
                return s;
            }
        }
        return null;
    }

}
