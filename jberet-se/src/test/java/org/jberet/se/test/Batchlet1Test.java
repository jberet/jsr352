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

package org.jberet.se.test;

import org.jberet.se.Main;
import org.junit.Test;

public class Batchlet1Test {
    private static final String jobXmlName = "batchlet1.xml";
    private static final String[] args = {jobXmlName, "param1.key=param1.val", "param2.key=param2.val"};

    @Test
    public void testMain() throws Exception {
        Main.main(args);
    }
}
