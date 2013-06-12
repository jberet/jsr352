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

package org.jberet.testapps.split;

import org.junit.Test;
import org.jberet.testapps.common.AbstractIT;

/**
 * Verifies split properties referencing, job element transition, and decision following split.
 *
 * step within a flow within a split can have step-parent, which is a top-level job element;
 */
public class SplitIT extends AbstractIT {
    public SplitIT() {
        params.setProperty("job-param", "job-param");
    }

    @Test
    public void split() throws Exception {
        startJobAndWait("split.xml");
    }
}
