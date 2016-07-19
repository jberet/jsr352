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

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jberet.camel.CamelItemProcessor;
import org.jberet.camel.CamelItemReader;
import org.jberet.camel.CamelItemWriter;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "JBERET")
@ValidIdRange(min = 73500, max = 73999)
public interface JBeretCamelLogger extends BasicLogger {
    JBeretCamelLogger LOGGER = Logger.getMessageLogger(
            JBeretCamelLogger.class, "org.jberet.camel");

    @Message(id = 73500, value = "Opening %s with endpoint: %s, CamelContext: %s, and ConsumerTemplate: %s")
    @LogMessage(level = Logger.Level.INFO)
    void openReader(CamelItemReader camelItemReader,
                    String endpoint,
                    CamelContext camelContext,
                    ConsumerTemplate consumerTemplate);

    @Message(id = 73501, value = "Opening %s with endpoint: %s, CamelContext: %s, and ProducerTemplate: %s")
    @LogMessage(level = Logger.Level.INFO)
    void openWriter(CamelItemWriter camelItemWriter,
                    String endpoint,
                    CamelContext camelContext,
                    ProducerTemplate producerTemplate);

    @Message(id = 73502, value = "Opening %s with endpoint: %s, CamelContext: %s, and ProducerTemplate: %s")
    @LogMessage(level = Logger.Level.INFO)
    void openProcessor(CamelItemProcessor camelItemProcessor,
                    String endpoint,
                    CamelContext camelContext,
                    ProducerTemplate producerTemplate);

    @Message(id = 73503, value = "Failed to stop Camel component: %s")
    @LogMessage(level = Logger.Level.WARN)
    void failToStop(@Cause Throwable throwable, Object camelComponent);

}