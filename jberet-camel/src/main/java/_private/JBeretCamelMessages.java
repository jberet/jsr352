/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package _private;

import javax.batch.operations.BatchRuntimeException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 73000, max = 73499)
public interface JBeretCamelMessages {
    JBeretCamelMessages MESSAGES = Messages.getBundle(JBeretCamelMessages.class);

    @Message(id = 73000, value = "CamelContext not available in %s")
    BatchRuntimeException noCamelContext(Object requestingObject);

    @Message(id = 73001, value = "Invalid batch property value: '%s' = '%s'")
    BatchRuntimeException invalidPropertyValue(String name, String value);

}