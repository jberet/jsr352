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
 
package org.mybatch.test;

import org.junit.Assert;
import org.junit.Test;
import org.mybatch.job.Job;
import org.mybatch.util.JaxbUtil;
import org.mybatch.metadata.JobMerger;
import org.junit.Assert.*;

public class JobMergerTest {
    @Test
    public void propertiesListenersFromParentJob() throws Exception {
        Job parent = JaxbUtil.getJob("properties-listeners-from-parent.xml");
        Job child = JaxbUtil.getJob("properties-listeners-from-parent-child.xml");

        Assert.assertNull(child.getRestartable());
        Assert.assertNull(child.getProperties());
        Assert.assertNull(child.getListeners());

        JobMerger merger = new JobMerger(parent, child);
        merger.merge();

        Assert.assertEquals("true", child.getRestartable());
        Assert.assertEquals(2, child.getProperties().getProperty().size());
        Assert.assertEquals(2, child.getListeners().getListener().size());
    }

    @Test
    public void mergeFalse() throws Exception {
        Job parent = JaxbUtil.getJob("merge-false-parent.xml");
        Job child = JaxbUtil.getJob( "merge-false-child.xml");

        Assert.assertEquals("false", child.getRestartable());
        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size()) ;

        JobMerger merger = new JobMerger(parent, child);
        merger.merge();

        Assert.assertEquals("false", child.getRestartable());
        Assert.assertEquals(0, child.getProperties().getProperty().size());
        Assert.assertEquals(0, child.getListeners().getListener().size());
    }

    @Test
    public void mergeTrue() throws Exception {
        Job parent = JaxbUtil.getJob("merge-true-parent.xml");
        Job child = JaxbUtil.getJob( "merge-true-child.xml");

        Assert.assertEquals(1, child.getProperties().getProperty().size());
        Assert.assertEquals(1, child.getListeners().getListener().size()) ;

        JobMerger merger = new JobMerger(parent, child);
        merger.merge();

        Assert.assertEquals(2, child.getProperties().getProperty().size());
        Assert.assertEquals(2, child.getListeners().getListener().size());
    }
}
