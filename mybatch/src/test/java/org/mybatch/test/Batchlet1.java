/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.test;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named("Batchlet1")
public class Batchlet1 extends AbstractBatchlet implements Batchlet {
    @Inject @BatchProperty(name = "Batchlet1")
    private String prop1;

    @Inject @BatchProperty  //default name
    private String defaultName;

    @Inject @BatchProperty(name = "no-such-property")
    private String noSuchProperty;

    @Inject @BatchProperty(name = "no-such-property")
    private String defaultValue = "defaultValue";

    @Inject @BatchProperty(name = "foo")
    private String foo;

    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public String process() throws Exception {
        System.out.printf("Injected batchlet property: %s => %s%n", "Batchlet1", prop1);
        System.out.printf("Injected batch property with default name: %s => %s%n", "defaultName", defaultName);
        System.out.printf("Undeclared batch property: %s => %s%n", "no-such-property", noSuchProperty);
        System.out.printf("Undeclared batch property: %s => %s%n", "no-such-property", defaultValue);

        System.out.printf("Injected JobContext: %s%n", jobContext);
        System.out.printf("Injected StepContext: %s%n", stepContext);
        System.out.printf("Job properties from injected JobContext: %s%n", jobContext.getProperties());
        System.out.printf("Step properties from injected StepContext: %s%n", stepContext.getProperties());

        String fooExpected = "foo";
        if (fooExpected.equals(foo)) {
            System.out.printf("Injected batchlet property foo: %s%n", foo);
        } else {
            throw new BatchRuntimeException(String.format("Expecting batchlet property foo to be %s, but got %s", fooExpected, foo));
        }
        return "Processed";
    }

    @Override
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
    }
}
