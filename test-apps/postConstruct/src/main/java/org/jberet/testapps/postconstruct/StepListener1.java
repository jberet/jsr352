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
 
package org.jberet.testapps.postconstruct;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.common.PostConstructPreDestroyBase;

@Named
public class StepListener1 extends PostConstructPreDestroyBase implements StepListener {
    @Inject @BatchProperty(name = "os.name")
    private String osName;

    @Override
    public void beforeStep() throws Exception {
        addToJobExitStatus("StepListener1.beforeStep");
    }

    @Override
    public void afterStep() throws Exception {
        addToJobExitStatus("StepListener1.afterStep");
    }

    @PostConstruct
    public void ps() {
        System.out.printf("StepListener1 PostConstruct of %s%n", this);
        if (osName == null) {
            throw new BatchRuntimeException("osNmae field has not been initialized when checking from PostConstruct method.");
        }
        addToJobExitStatus("StepListener1.ps");
    }

    @PreDestroy
    public void pd() {
        System.out.printf("StepListener1 PreDestroy of %s%n", this);
        addToJobExitStatus("StepListener1.pd");
    }
}
