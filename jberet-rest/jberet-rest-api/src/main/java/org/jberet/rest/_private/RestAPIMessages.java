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

package org.jberet.rest._private;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 70000, max = 70499)
public interface RestAPIMessages {
    RestAPIMessages MESSAGES = Messages.getBundle(RestAPIMessages.class);

    @Message(id = 70000, value = "Missing request query parameters: %s")
    BadRequestException missingQueryParams(String params);

    @Message(id = 70001, value = "Invalid request query parameter value: %s = %s")
    BadRequestException invalidQueryParamValue(String paramKey, String paramValue);

    @Message(id = 70002, value = "The resource identified is not found: %s = %s")
    NotFoundException notFoundException(String key, String value);

}