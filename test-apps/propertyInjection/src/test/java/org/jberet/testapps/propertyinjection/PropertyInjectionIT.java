/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.propertyinjection;

import org.jberet.testapps.common.AbstractIT;
import org.junit.AfterClass;
import org.junit.jupiter.api.Assertions;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

public class PropertyInjectionIT extends AbstractIT {
    @BeforeClass
    public static void beforeClass() {
        switchToUSLocale();
    }

    @AfterClass
    public static void afterClass() {
        restoreDefaultLocale();
    }

    @Test
    public void propertyInjection() throws Exception {
        startJobAndWait("propertyInjection.xml");
        Assertions.assertEquals("ab 2ab2 2default2 defaultValue", stepExecution0.getExitStatus());
    }
}
