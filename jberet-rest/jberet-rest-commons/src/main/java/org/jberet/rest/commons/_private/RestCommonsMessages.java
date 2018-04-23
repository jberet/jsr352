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

package org.jberet.rest.commons._private;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 71000, max = 71999)
public interface RestCommonsMessages {
    RestCommonsMessages MESSAGES = Messages.getBundle(RestCommonsMessages.class);

    @Message(id = 71000, value = "Expecting JSON element '%s' within '%s'")
    IllegalStateException expectingJsonElement(String name, String... parents);


}