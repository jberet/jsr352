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

import javax.batch.annotation.Batchlet;
import javax.batch.annotation.BeginStep;
import javax.batch.annotation.EndStep;
import javax.batch.annotation.Process;
import javax.batch.annotation.Stop;

@Batchlet("Batchlet1")
public class Batchlet1 {
    @BeginStep
    public void begin() throws Exception {
        System.out.printf("in @BeginStep, %s%n", Thread.currentThread());
    }

    @Process
    public String process() throws Exception {
        System.out.printf("in @Process, %s%n", Thread.currentThread());
        return "Processed";
    }

    @Stop
    public void stop() throws Exception {
        System.out.printf("in @Stop, %s%n", Thread.currentThread());
    }

    @EndStep
    public void end() throws Exception {
        System.out.printf("in @EndStep, %s%n", Thread.currentThread());
    }

}
