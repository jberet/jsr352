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

import org.junit.Assert;
import org.junit.Test;

public class FlowMergerTest {
    @Test
    public void parentSameFile() throws Exception {
        final Job childJob = JobMergerTest.loadJob("flow-child.xml");
        final Flow child = getFlow(childJob, "flow2");
        Assert.assertEquals(null, child.getParent());
        Assert.assertEquals(null, child.getJslName());
        Assert.assertEquals(false, child.isAbstract());
        Assert.assertEquals("step3", child.getAttributeNext());

        Assert.assertEquals(3, child.getJobElements().size());
        final Step step1 = (Step) child.getJobElements().get(0);
        Assert.assertEquals("flow1-step1", step1.id);

        final Step step2 = (Step) child.getJobElements().get(1);
        Assert.assertEquals("flow1-step2", step2.id);

        final Decision decision1 = (Decision) child.getJobElements().get(2);
        Assert.assertEquals("flow1-decision1", decision1.id);


        Assert.assertEquals(2, child.getTransitionElements().size());
        Assert.assertEquals(true, child.getTransitionElements().get(0) instanceof Transition.Fail);
        Assert.assertEquals("FAIL", child.getTransitionElements().get(0).getOn());

        Assert.assertEquals(true, child.getTransitionElements().get(1) instanceof Transition.Stop);
        Assert.assertEquals("STOP", child.getTransitionElements().get(1).getOn());
    }

    @Test
    public void parentOtherFile() throws Exception {
        final Job childJob = JobMergerTest.loadJob("flow-child.xml");
        final Flow child = getFlow(childJob, "flow3");
        Assert.assertEquals(null, child.getParent());
        Assert.assertEquals(null, child.getJslName());
        Assert.assertEquals(false, child.isAbstract());
        Assert.assertEquals("step4", child.getAttributeNext());

        Assert.assertEquals(3, child.getJobElements().size());
        final Step step1 = (Step) child.getJobElements().get(0);
        Assert.assertEquals("flow-parent-flow1-step1", step1.id);

        final Step step2 = (Step) child.getJobElements().get(1);
        Assert.assertEquals("flow-parent-flow1-step2", step2.id);

        final Decision decision1 = (Decision) child.getJobElements().get(2);
        Assert.assertEquals("flow-parent-flow1-decision1", decision1.id);


        Assert.assertEquals(2, child.getTransitionElements().size());
        Assert.assertEquals(true, child.getTransitionElements().get(0) instanceof Transition.Stop);
        Assert.assertEquals("STOP", child.getTransitionElements().get(0).getOn());

        Assert.assertEquals(true, child.getTransitionElements().get(1) instanceof Transition.Fail);
        Assert.assertEquals("FAIL", child.getTransitionElements().get(1).getOn());
    }

    protected static Flow getFlow(final Job job, final String flowId) {
        for (final JobElement e : job.getJobElements()) {
            if (e instanceof Flow) {
                final Flow flow = (Flow) e;
                if (flowId.equals(flow.getId())) {
                    return flow;
                }
            }
        }
        return null;
    }
}
