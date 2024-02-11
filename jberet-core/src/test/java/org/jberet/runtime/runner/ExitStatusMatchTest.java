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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.jberet.runtime.runner.StepExecutionRunner.matches;

public class ExitStatusMatchTest {
    @Test
    public void testMatches() throws Exception {
        Assertions.assertTrue(matches("pass", "pass"));
        Assertions.assertTrue(matches("pass", "*"));
        Assertions.assertTrue(matches("pass", "????"));

        Assertions.assertFalse(matches("pass", "p"));
        Assertions.assertFalse(matches("pass", "passed"));
        Assertions.assertFalse(matches("pass", "P???"));
        Assertions.assertFalse(matches("PASS", "p???"));
        Assertions.assertFalse(matches("pass", "p??S"));
        Assertions.assertFalse(matches(null, "*"));

        Assertions.assertTrue(matches("pass", "*a*"));
        Assertions.assertTrue(matches("pass", "*s"));
        Assertions.assertTrue(matches("pass", "???*"));
    }
}
