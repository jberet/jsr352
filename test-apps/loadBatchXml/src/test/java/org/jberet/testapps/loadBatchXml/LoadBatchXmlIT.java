/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.loadBatchXml;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Test;

/**
 * Verifies the following:
 * <p>
 * BatchletNoNamed is declared in batch.xml, loaded with portable archive loader;
 * job listener L1 is declared with @Named, loaded with non-portable job loader;
 * JobContext injected with @Inject into batchlet and job listener;
 * artifact properties for the job listener are injected into the target artifact;
 * job-level properties are not injected into the field of a job listener;
 * job-level listener property can reference job-level properties with #{jobProperties['']};
 * job-level listener property can reference system properties with #{systemProperties['']};
 * job-level listener property can reference job parameters with #{jobParameters['']};
 * can retrieve all job-level properties from JobContext injected into a job listener;
 * the beforeJob and afterJob methods are invoked in order;
 * multiple job listeners configured to a job are all invoked;
 * artifact properties of listener1 is not exposed to listener 2;
 * the above requirements applied to multiple step listeners;
 * step listener properties can reference job property from the enclosing step and the enclosing job w/ proper precedence;
 */
public class LoadBatchXmlIT extends AbstractIT {
    public LoadBatchXmlIT() {
        params.setProperty("job-param", "job-param");
    }

    @Test
    public void loadBatchXml() throws Exception {
        startJobAndWait("batchlet1.xml");
    }
}
