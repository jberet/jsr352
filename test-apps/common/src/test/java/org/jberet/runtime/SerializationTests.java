/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import jakarta.batch.runtime.JobInstance;

import org.jberet.testapps.common.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SerializationTests extends AbstractIT {

    @BeforeEach
    public void setup() throws Exception {
        params.clear();
    }

    @Test
    public void testJobInstance() throws Exception {
        // Start the job
        startJobAndWait("no-op-batchlet.xml");

        // Get the current job instance
        final JobInstance jobInstance = jobExecution.getJobInstance();
        // Serialize, then deserialize the JobInstance
        final JobInstance serializedJobInstance = doSerialization(jobInstance, JobInstance.class);

        /// The job instance checks equals checks the id which should always be equal
        Assertions.assertEquals(jobInstance, serializedJobInstance);
        // Check each field in the JobInstance only for equality, currently each field overrides equals which will work
        // for now. If this changes this test may break and this really could be removed.
        reflectiveEquals(jobInstance, serializedJobInstance, false);
    }

    protected <T> T doSerialization(final T instance, final Class<? extends T> type) throws IOException, ClassNotFoundException {
        // Serialize the instance
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
        objectOutputStream.writeObject(instance);

        // Deserialize the instance and return it
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream objectInputStream = new ObjectInputStream(bais);
        return type.cast(objectInputStream.readObject());
    }

    protected void reflectiveEquals(final Object one, final Object two, final boolean recursive) throws IllegalAccessException {
        final Class<?> type = one.getClass();
        Assertions.assertEquals(type, two.getClass(), String.format("Type for %s doesn't match type for %s", one, two));
        // Get all the fields
        final Field[] fields = one.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isTransient(field.getModifiers())) {
                if (!field.isAccessible()) field.setAccessible(true);
                final Class<?> fieldType = field.getType();
                final Object valueOne = field.get(one);
                final Object valueTwo = field.get(two);
                // Only recursively check non-jdk objects
                if (recursive && !(fieldType.isPrimitive() || fieldType.getName().startsWith("java"))) {
                    reflectiveEquals(valueOne, valueTwo, false);
                }
                Assertions.assertEquals(valueOne, valueTwo, String.format("Value %s not equal to %s for field %s in class %s", valueOne, valueTwo, field, type));
            }
        }
    }
}
