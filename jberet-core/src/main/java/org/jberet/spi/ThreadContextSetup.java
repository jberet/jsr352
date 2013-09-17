package org.jberet.spi;

/**
 * An interface to setup and tear down any local context needed for a thread.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ThreadContextSetup {

    /**
     * Sets up any local thread context needed.
     *
     * @return a context to tear down after the invocation has finished
     */
    TearDownHandle setup();

    /**
     * Tears down the context
     */
    public interface TearDownHandle {

        /**
         * Tears down any previously setup context
         */
        void tearDown();
    }
}
