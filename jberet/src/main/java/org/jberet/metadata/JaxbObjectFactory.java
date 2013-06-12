/*
 * Copyright (c) 2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.metadata;

import javax.xml.bind.annotation.XmlRegistry;

import org.jberet.job.ExceptionClassFilter;

@XmlRegistry
public class JaxbObjectFactory extends org.jberet.job.ObjectFactory {
    @Override
    public ExceptionClassFilter createExceptionClassFilter() {
        return new ExceptionClassFilterImpl();
    }
}