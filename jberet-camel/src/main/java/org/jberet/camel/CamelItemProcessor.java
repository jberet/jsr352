/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.camel;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

/**
 * Implementation of {@code javax.batch.api.chunk.ItemProcessor} that processes
 * batch data using Apache Camel component.
 *
 * @since 1.3.0
 */
@Named
public class CamelItemProcessor extends CamelArtifactBase implements ItemProcessor {

    @Override
    public Object processItem(final Object item) throws Exception {
        return null;
    }
}
