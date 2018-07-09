/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        Assert.assertFalse(matches(null, "*"));

        Assert.assertTrue(matches("pass", "*a*"));
        Assert.assertTrue(matches("pass", "*s"));
        Assert.assertTrue(matches("pass", "???*"));
    }
}
