/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.batchproperty;

import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Test;

public class BatchPropertyIT extends BatchTestBase {
    private static final String jobName = "batchproperty";

    @Test
    public void testBatchProperty() throws Exception {
        startJobShouldComplete(jobName, null, 500);
    }

    @Override
    protected String getRestUrl() {
        return BASE_URL + "batchproperty/api";
    }
}
