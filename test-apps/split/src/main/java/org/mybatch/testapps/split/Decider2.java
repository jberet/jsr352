/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 
package org.mybatch.testapps.split;

import java.util.Arrays;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.inject.Named;

import org.junit.Assert;

/**
 * This decider follows a split, so there should be multiple StepExecution.
 */
@Named("Decider2")
public class Decider2 implements Decider {
    @Override
    public String decide(StepExecution[] executions) throws Exception {
        Assert.assertEquals(2, executions.length);
        System.out.printf("In decider2 StepExecution[]: %s%n", Arrays.toString(executions));
        for (StepExecution e : executions) {
            System.out.printf("batch status: %s, exit status: %s%n", e.getBatchStatus(), e.getExitStatus());
        }
        return "next";
    }
}
