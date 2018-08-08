/*
 * Copyright (c) 2012-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.creation;

import org.junit.Before;
import org.junit.Test;

public class AbstractArtifactFactoryTest {

    private AbstractArtifactFactory factory = new AbstractArtifactFactory() {
        @Override
        public Object create(String ref, Class<?> cls, ClassLoader classLoader) throws Exception {
            return null;
        }

        @Override
        public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
            return null;
        }
    };

    private Class noPackageBatchletClass;
    private Object noPackageBatchlet;

    @Before
    public void setup() throws Exception {
        noPackageBatchletClass = Class.forName("NoPackageBatchlet");
        noPackageBatchlet = noPackageBatchletClass.getDeclaredConstructor().newInstance();
    }

    @Test
    public void testDestroyShouldNotFailForDefaultPackage()  {
        factory.destroy(noPackageBatchlet);
    }

    @Test
    public void testDoInjectionShouldNotFailForDefaultPackage() throws Exception {
        factory.doInjection(noPackageBatchlet, noPackageBatchletClass, null, null, null, null);
    }
}
