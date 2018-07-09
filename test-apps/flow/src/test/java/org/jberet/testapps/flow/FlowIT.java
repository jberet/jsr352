/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.flow;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Test;

/**
 * Verifies the following:
 * injections into super class are processed (Batchlet1 extends Batchlet0), including @Inject and @BatchProperty
 * flow property resolution, runtime execution, and transition inside flow and outwards;
 */
public class FlowIT extends AbstractIT {
    public FlowIT() {
        params.setProperty("job-param", "job-param");
    }

    @Test
    public void flow() throws Exception {
        startJobAndWait("flow.xml");
    }
}
