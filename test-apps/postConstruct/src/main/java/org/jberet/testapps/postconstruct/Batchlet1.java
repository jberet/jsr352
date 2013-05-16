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

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.operations.BatchRuntimeException;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.testapps.common.Batchlet0;

@Named
public class Batchlet1 extends Batchlet0 {
    @Inject @BatchProperty(name = "date")
    private Date date;

    @Override
    public String process() throws Exception {
        return jobContext.getExitStatus();
    }

    @PostConstruct
    private void ps() throws Exception {
        System.out.printf("Batchlet1 PostConstruct of %s%n", this);
        if (jobContext == null || stepContext == null || date == null) {
            throw new BatchRuntimeException("Some fields are not initialized: jobContext=" + jobContext +
            ", stepContext=" + stepContext + ", date=" + date);
        }
        addToJobExitStatus("Batchlet1.ps");
    }

    @PreDestroy
    private void pd() throws Exception {
        System.out.printf("Batchlet1 PreDestroy of %s%n", this);
        addToJobExitStatus("Batchlet1.pd");
    }

}
