/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.infinispanRepository;

import org.junit.BeforeClass;
import org.junit.Test;

public class MemoryInfinispanRepositoryIT extends InfinispanRepositoryTestBase {
    @BeforeClass
    public static void beforeClass() throws Exception {
        moveInfinispanXml("memory-infinispan.xml");
    }

    @Test
    public void partitionWithInfinispanMemory() throws Exception {
        partitionWithInfinispan0();
    }
}
