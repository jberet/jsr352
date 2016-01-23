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
 
package org.samples.wildfly.jberet.xml2json;

import javax.batch.runtime.BatchStatus;

import com.gargoylesoftware.htmlunit.TextPage;
import org.junit.Test;
import org.samples.wildfly.jberet.common.BatchTestBase;

public final class Xml2JsonIT extends BatchTestBase {
    static final String CONTEXT_PATH = "xml2json";
    static final String SERVLET_PATH = null;
    static final String JOB_COMMAND = "start " + CONTEXT_PATH;

    @Test
    public void testCsv2Json() throws Exception {
        final TextPage page = runJob(CONTEXT_PATH, SERVLET_PATH, JOB_COMMAND);
        final String content = page.getContent();
        assertContainsBatchStatus(content, BatchStatus.COMPLETED);
    }
}
