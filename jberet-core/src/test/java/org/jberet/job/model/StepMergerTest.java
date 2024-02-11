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

import java.util.LinkedList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StepMergerTest {
    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-properties-listeners-child.xml");
        final Step child = getStep(childJob, "step-properties-listeners-child-step");
        Assertions.assertEquals(2, child.getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "parent2"});
        Assertions.assertEquals(2, child.getListeners().getListeners().size());
    }

    @Test
    public void mergeFalse() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-merge-false-child.xml");
        final Step child = getStep(childJob, "step-merge-false-child-step");
        Assertions.assertEquals(0, child.getProperties().getNameValues().size());
        Assertions.assertEquals(0, child.getListeners().getListeners().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-merge-true-child.xml");
        final Step child = getStep(childJob, "step-merge-true-child-step");
        Assertions.assertEquals(2, child.getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"});
        Assertions.assertEquals(2, child.getListeners().getListeners().size());
    }

    @Test
    public void stepSameFileParentChild() throws Exception {
        final Job childJob = JobMergerTest.loadJob("step-same-file-parent-child");
        final String[] child1And2 = new String[]{"step-same-file-child-1", "step-same-file-child-2"};
        Step child;
        for (final String c : child1And2) {
            child = getStep(childJob, c);
            Assertions.assertEquals(2, child.getProperties().getNameValues().size());
            JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child"}, true);
            //Assertions.assertEquals(3, child.getListeners().getListeners().size());
            Assertions.assertEquals(2, child.getListeners().getListeners().size());  //remove dup listeners
            JobMergerTest.listenersContain(child.getListeners(), new String[]{"parent-listener", "child-listener"});
            Assertions.assertEquals("child-batchlet", child.getBatchlet().getRef());
        }

        child = getStep(childJob, "step-same-file-child-child");
        Assertions.assertEquals(3, child.getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"parent", "child", "child-child"}, true);
        //Assertions.assertEquals(4, child.getListeners().getListeners().size());
        Assertions.assertEquals(3, child.getListeners().getListeners().size());  //remove dup listeners
        JobMergerTest.listenersContain(child.getListeners(), new String[]{"parent-listener", "child-listener", "child-child-listener"});
        Assertions.assertEquals("child-child-batchlet", child.getBatchlet().getRef());
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
        Assertions.assertEquals(1, child.getProperties().getNameValues().size());
        JobMergerTest.propertiesContain(child.getProperties(), new String[]{"child"}, true);
        Assertions.assertEquals(1, child.getListeners().getListeners().size());
        JobMergerTest.listenersContain(child.getListeners(), new String[]{"child-listener"});
        Assertions.assertEquals("child-batchlet", child.getBatchlet().getRef());
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
