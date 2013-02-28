/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 
package org.mybatch.testapps.loadBatchXml;

import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

import org.junit.Test;

/**
 * Verifies the following:
 *
 * Batchlet1 is declared in batch.xml, loaded with portable archive loader;
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
public class LoadBatchXmlIT {
    private static final String jobXmlName = "batchlet1.xml";

    @Test
    public void loadBatchXml() throws Exception {
        Properties props = new Properties();
        props.setProperty("job-param", "job-param");

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        jobOperator.start(jobXmlName, props);
        Thread.sleep(10000);
    }
}
