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

package org.jberet.testapps.propertyinjection;

import org.jberet.testapps.common.AbstractIT;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Assert.assertEquals("ab 2ab2 2default2 defaultValue", stepExecution0.getExitStatus());
    }
}
