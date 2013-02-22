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
 
package org.mybatch.runtime.runner;

import junit.framework.Assert;
import org.junit.Test;
import static org.mybatch.runtime.runner.StepExecutionRunner.matches;

public class ExitStatusMatchTest {
    @Test
    public void testMatches() throws Exception {
        Assert.assertTrue(matches("pass", "pass"));
        Assert.assertTrue(matches("pass", "*"));
        Assert.assertTrue(matches("pass", "????"));

        Assert.assertFalse(matches("pass", "p"));
        Assert.assertFalse(matches("pass", "passed"));
        Assert.assertFalse(matches("pass", "P???"));
        Assert.assertFalse(matches("PASS", "p???"));
        Assert.assertFalse(matches("pass", "p??S"));

        Assert.assertTrue(matches("pass", "*a*"));
        Assert.assertTrue(matches("pass", "*s"));
        Assert.assertTrue(matches("pass", "???*"));
    }
}
