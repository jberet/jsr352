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

package org.jberet.samples.wildfly.xml2json;

import javax.batch.runtime.BatchStatus;

import org.jberet.samples.wildfly.common.BatchTestBase;
import org.junit.Test;

public final class Xml2JsonIT extends BatchTestBase {
    private static final String jobName = "xml2json";

    @Test
    public void testCsv2Json() throws Exception {
        startJobCheckStatus(jobName, null, 5000, BatchStatus.COMPLETED);
    }

    @Override
    protected String getRestUrl() {
        return BASE_URL + "xml2json/api";
    }
}
