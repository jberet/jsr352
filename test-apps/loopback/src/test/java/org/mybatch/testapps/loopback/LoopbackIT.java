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

package org.mybatch.testapps.loopback;

import org.junit.Test;
import org.mybatch.testapps.common.AbstractIT;

/**
 * Verifies step loopbacks are detected and failed.
 */
public class LoopbackIT extends AbstractIT {
    /**
     * step1's next attribute is itself.
     */
    @Test
    public void selfNextAttribute() throws Exception {
        startJob("self-next-attribute.xml");
    }

    /**
     * step1's next element points to itself.
     */
    @Test
    public void selfNextElement() throws Exception {
        startJob("self-next-element.xml");
    }

    /**
     * step1->step2->step3, transitioning with either next attribute or next element.
     */
    @Test
    public void loopbackAttributeElement() throws Exception {
        startJob("loopback-attribute-element.xml");
    }

    /**
     * same as loopbackAttributeElement, but within a flow, still a loopback error.
     */
    @Test
    public void loopbackInFlow() throws Exception {
        startJob("loopback-in-flow.xml");
    }

    /**
     * flow1 (step1 -> step2) => step1 is not loopback.  The job should run successfully.
     * @throws Exception
     */
    @Test
    public void notLoopbackAcrossFlow() throws Exception {
        startJob("not-loopback-across-flow.xml");
    }

    /**
     * flow1 (step1) => flow2 (step1 -> step2) => flow1 is a loopback at the last transition,
     * not at flow1.step1 -> flow2.step1.
     * @throws Exception
     */
    @Test
    public void loopbackFlowToFlow() throws Exception {
        startJob("loopback-flow-to-flow.xml");
    }
}
