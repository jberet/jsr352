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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlowMergerTest {
    @Test
    public void parentSameFile() throws Exception {
        final Job childJob = JobMergerTest.loadJob("flow-child.xml");
        final Flow child = getFlow(childJob, "flow2");
        Assertions.assertEquals(null, child.getParent());
        Assertions.assertEquals(null, child.getJslName());
        Assertions.assertEquals(false, child.isAbstract());
        Assertions.assertEquals("step3", child.getAttributeNext());

        Assertions.assertEquals(3, child.getJobElements().size());
        final Step step1 = (Step) child.getJobElements().get(0);
        Assertions.assertEquals("flow1-step1", step1.id);

        final Step step2 = (Step) child.getJobElements().get(1);
        Assertions.assertEquals("flow1-step2", step2.id);

        final Decision decision1 = (Decision) child.getJobElements().get(2);
        Assertions.assertEquals("flow1-decision1", decision1.id);


        Assertions.assertEquals(2, child.getTransitionElements().size());
        Assertions.assertEquals(true, child.getTransitionElements().get(0) instanceof Transition.Fail);
        Assertions.assertEquals("FAIL", child.getTransitionElements().get(0).getOn());

        Assertions.assertEquals(true, child.getTransitionElements().get(1) instanceof Transition.Stop);
        Assertions.assertEquals("STOP", child.getTransitionElements().get(1).getOn());
    }

    @Test
    public void parentOtherFile() throws Exception {
        final Job childJob = JobMergerTest.loadJob("flow-child.xml");
        final Flow child = getFlow(childJob, "flow3");
        Assertions.assertEquals(null, child.getParent());
        Assertions.assertEquals(null, child.getJslName());
        Assertions.assertEquals(false, child.isAbstract());
        Assertions.assertEquals("step4", child.getAttributeNext());

        Assertions.assertEquals(3, child.getJobElements().size());
        final Step step1 = (Step) child.getJobElements().get(0);
        Assertions.assertEquals("flow-parent-flow1-step1", step1.id);

        final Step step2 = (Step) child.getJobElements().get(1);
        Assertions.assertEquals("flow-parent-flow1-step2", step2.id);

        final Decision decision1 = (Decision) child.getJobElements().get(2);
        Assertions.assertEquals("flow-parent-flow1-decision1", decision1.id);


        Assertions.assertEquals(2, child.getTransitionElements().size());
        Assertions.assertEquals(true, child.getTransitionElements().get(0) instanceof Transition.Stop);
        Assertions.assertEquals("STOP", child.getTransitionElements().get(0).getOn());

        Assertions.assertEquals(true, child.getTransitionElements().get(1) instanceof Transition.Fail);
        Assertions.assertEquals("FAIL", child.getTransitionElements().get(1).getOn());
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
