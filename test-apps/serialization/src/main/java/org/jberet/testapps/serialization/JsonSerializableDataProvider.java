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

import org.jberet.spi.SerializableDataProvider;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.Serializable;

public class JsonSerializableDataProvider implements SerializableDataProvider {
    private static final Jsonb jsonb = JsonbBuilder.newBuilder().build();

    @Override
    public byte[] objectToBytes(final Object obj) {
        return jsonb.toJson(obj).getBytes();
    }

    @Override
    public Serializable bytesToObject(final byte[] bytes, final Class<?> klass, final ClassLoader classLoader) {
        return (Serializable) jsonb.fromJson(new String(bytes), klass);
    }
}
