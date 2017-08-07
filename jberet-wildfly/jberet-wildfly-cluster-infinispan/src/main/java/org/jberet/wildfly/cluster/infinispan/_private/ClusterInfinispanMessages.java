package org.jberet.wildfly.cluster.infinispan._private;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.ValidIdRange;

@MessageBundle(projectCode = "JBERET")
@ValidIdRange(min = 76000, max = 76499)
public interface ClusterInfinispanMessages {
    ClusterInfinispanMessages MESSAGES = Messages.getBundle(ClusterInfinispanMessages.class);

    @Message(id = 76000, value = "Failed to lookup %s")
    IllegalStateException failedToLookup(@Cause Throwable throwable, String name);

    @Message(id = 76001, value = "Failed instantiate naming context")
    IllegalStateException failedToNewNamingContext(@Cause Throwable throwable);

    @Message(id = 76002, value = "Failed in JMS operation")
    IllegalStateException failedInJms(@Cause Throwable throwable);

    @Message(id = 76003, value = "Failed to get job operator")
    IllegalStateException failedToGetJobOperator();

}
