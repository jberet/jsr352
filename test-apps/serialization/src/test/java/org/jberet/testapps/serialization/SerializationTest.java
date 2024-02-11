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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.json.bind.JsonbBuilder;

public class SerializationTest extends AbstractIT {
    @Test
    public void serialization() throws Exception {
        startJobAndWait("batchlet.xml");

        final String serialized = new String(stepExecution0.getPersistentUserDataSerialized());
        final SerializableBatchlet.User user =
            JsonbBuilder.newBuilder().build().fromJson(serialized, SerializableBatchlet.User.class);
        Assertions.assertEquals("Naruto", user.getFirstName());
        Assertions.assertEquals("Uzumaki", user.getLastName());
        Assertions.assertEquals(17, user.getAge());
    }
}
