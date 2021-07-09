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

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("batchlet")
@Dependent
public class SerializableBatchlet implements Batchlet {
    @Inject
    StepContext stepExecution;

    @Override
    public String process() {
        stepExecution.setPersistentUserData(new User("Naruto", "Uzumaki", 17));
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() {

    }

    public static class User implements Serializable {
        private String firstName;
        private String lastName;
        private int age;

        public User() {
        }

        public User(final String firstName, final String lastName, final int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(final String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }
}
