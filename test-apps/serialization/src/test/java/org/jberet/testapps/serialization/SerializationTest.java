/*
 * Copyright (c) 2020 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.testapps.serialization;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

import jakarta.json.bind.JsonbBuilder;

public class SerializationTest extends AbstractIT {
    @Test
    public void serialization() throws Exception {
        startJobAndWait("batchlet.xml");

        final String serialized = new String(stepExecution0.getPersistentUserDataSerialized());
        final SerializableBatchlet.User user =
            JsonbBuilder.newBuilder().build().fromJson(serialized, SerializableBatchlet.User.class);
        Assert.assertEquals("Naruto", user.getFirstName());
        Assert.assertEquals("Uzumaki", user.getLastName());
        Assert.assertEquals(17, user.getAge());
    }
}
