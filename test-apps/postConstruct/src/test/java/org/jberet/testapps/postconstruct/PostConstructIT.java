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

package org.jberet.testapps.postconstruct;

import javax.batch.runtime.BatchStatus;

import junit.framework.Assert;
import org.jberet.testapps.common.AbstractIT;
import org.junit.Test;

public class PostConstructIT extends AbstractIT {
    @Test
    public void postConstructAndPreDestroy() throws Exception {
        String expected = "PostConstructPreDestroyBase.ps JobListener1.ps JobListener1.beforeJob PostConstructPreDestroyBase.ps StepListener1.ps StepListener1.beforeStep PostConstructPreDestroyBase.ps Batchlet0.ps Batchlet1.ps Batchlet1.process Batchlet1.pd Batchlet0.pd PostConstructPreDestroyBase.pd StepListener1.afterStep StepListener1.pd PostConstructPreDestroyBase.pd PostConstructPreDestroyBase.ps Decider1.ps Decider1.decide Decider1.pd PostConstructPreDestroyBase.pd JobListener1.afterJob JobListener1.pd PostConstructPreDestroyBase.pd";
        startJobAndWait("postConstruct.xml");
        String jobExitStatus = jobExecution.getExitStatus();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(expected, jobExitStatus);
    }
}
