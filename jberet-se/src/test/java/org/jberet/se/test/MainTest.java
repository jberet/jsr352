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

import java.util.HashMap;
import java.util.Map;

import org.jberet.se.Main;
import org.junit.Test;

public class MainTest {
    public Map<? extends Number, ?> integers = new HashMap<Integer, String>();

    private static final String jobXmlName = "batchlet1.xml";

    @Test
    public void testMain() throws Exception {
        final String[] args = {jobXmlName};
        Main.main(args);

    }

    @Override
    public String toString() {
        return super.toString();
    }
}
