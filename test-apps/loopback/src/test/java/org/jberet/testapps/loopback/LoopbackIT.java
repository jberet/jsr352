/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.loopback;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Test;

/**
 * Verifies step loopbacks are detected and failed.
 */
public class LoopbackIT extends AbstractIT {
    public LoopbackIT() {
        params.setProperty("job-param", "job-param");
    }

    /**
     * step1's next attribute is itself.
     */
    @Test
    public void selfNextAttribute() throws Exception {
        startJobAndWait("self-next-attribute.xml");
    }

    /**
     * step1's next element points to itself.
     */
    @Test
    public void selfNextElement() throws Exception {
        startJobAndWait("self-next-element.xml");
    }

    /**
     * step1->step2->step3, transitioning with either next attribute or next element.
     */
    @Test
    public void loopbackAttributeElement() throws Exception {
        startJobAndWait("loopback-attribute-element.xml");
    }

    /**
     * same as loopbackAttributeElement, but within a flow, still a loopback error.
     */
    @Test
    public void loopbackInFlow() throws Exception {
        startJobAndWait("loopback-in-flow.xml");
    }

    /**
     * flow1 (step1 -> step2) => step1 is not loopback.  The job should run successfully.
     * @throws Exception
     */
    @Test
    public void notLoopbackAcrossFlow() throws Exception {
        startJobAndWait("not-loopback-across-flow.xml");
    }

    /**
     * flow1 (step1) => flow2 (step1 -> step2) => flow1 is a loopback at the last transition,
     * not at flow1.step1 -> flow2.step1.
     * @throws Exception
     */
    @Test
    public void loopbackFlowToFlow() throws Exception {
        startJobAndWait("loopback-flow-to-flow.xml");
    }

    /**
     * split1 (flow1 (step1) | flow2 (step2)) => self is a loopback.
     */
    @Test
    public void loopbackSplitSelf() throws Exception {
        startJobAndWait("loopback-split-self.xml");
    }

    /**
     * step0 => split1 (flow1 (step1) | flow2 (step2)) => step0 is a loopback.
     */
    @Test
    public void loopbackStepSplit() throws Exception {
        startJobAndWait("loopback-step-split.xml");
    }
}
