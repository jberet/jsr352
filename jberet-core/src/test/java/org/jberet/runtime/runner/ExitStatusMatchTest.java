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

package org.jberet.runtime.runner;

import org.junit.Assert;
import org.junit.Test;

import static org.jberet.runtime.runner.StepExecutionRunner.matches;

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
