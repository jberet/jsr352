/*
 * Copyright (c) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.se._private;

import javax.batch.operations.BatchRuntimeException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 50000, max = 50499)
public interface SEBatchMessages {
    SEBatchMessages MESSAGES = Messages.getBundle(SEBatchMessages.class);

    @Message(id = 50000, value = "Failed to load configuration file %s")
    BatchRuntimeException failToLoadConfig(@Cause Throwable th, String configFile);

    @Message(id = 50002, value = "Failed to get a valid value for configuration property %s; current value is %s.")
    BatchRuntimeException failToGetConfigProperty(String propName, String value, @Cause Throwable throwable);

    @Message(id = 50003, value = "Unrecognized job repository type %s")
    BatchRuntimeException unrecognizedJobRepositoryType(String v);
}
