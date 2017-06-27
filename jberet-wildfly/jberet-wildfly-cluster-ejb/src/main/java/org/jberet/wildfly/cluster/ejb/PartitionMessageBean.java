/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.wildfly.cluster.ejb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jberet.spi.PartitionInfo;
import org.jberet.wildfly.cluster.jms._private.ClusterJmsMessages;

@MessageDriven(name = "PartitionMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/partitionQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "type = 'P'")
})
@TransactionManagement(TransactionManagementType.BEAN)
public class PartitionMessageBean implements MessageListener {
    @Inject
    private PartitionSingletonBean partitionSingletonBean;

    @Override
    public void onMessage(final Message message) {
        final PartitionInfo partitionInfo;
        try {
            partitionInfo = message.getBody(PartitionInfo.class);
        } catch (JMSException e) {
            throw ClusterJmsMessages.MESSAGES.failedInJms(e);
        }
        partitionSingletonBean.runPartition(partitionInfo);
    }

}
