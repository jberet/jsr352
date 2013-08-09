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
