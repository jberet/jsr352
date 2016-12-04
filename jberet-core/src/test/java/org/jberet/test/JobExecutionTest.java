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

package org.jberet.test;

import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@code JobExecutionImpl}.
 *
 * @see JobExecutionImpl
 * @since 1.2.2
 * @since 1.3.0.Beta4
 */
public class JobExecutionTest {
    /**
     * Separator character placed in front of user when combining user and restart position.
     * @see JobExecutionImpl#RESTART_POSITION_USER_SEP
     */
    private static final char RESTART_POSITION_USER_SEP = '"';

    final String step1 = "step1";
    final String user1 = "user1";
    final String step2 = "step2";
    final String user2 = "user2";

    /**
     * Tests combined restart and user value format: step1{@value #RESTART_POSITION_USER_SEP}user1.
     * @throws Exception
     */
    @Test
    public void restartPositionAndUserTest1() throws Exception {
        final String restartPositionAndUser1 =  step1 + RESTART_POSITION_USER_SEP + user1;
        final String restartPositionAndUser2 =  step2 + RESTART_POSITION_USER_SEP + user2;
        final String restartPositionAndUser3 =  step2;

        JobExecutionImpl jobExecution1 = new JobExecutionImpl(null, 1, null, null, null, null, null,
                BatchStatus.STARTING.name(), null, restartPositionAndUser1);
        assertEquals(step1, jobExecution1.getRestartPosition());
        assertEquals(user1, jobExecution1.getUser());
        assertEquals(restartPositionAndUser1, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(user2);
        assertEquals(restartPositionAndUser2, jobExecution1.combineRestartPositionAndUser());
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(user2, jobExecution1.getUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(null);
        assertEquals(restartPositionAndUser3, jobExecution1.combineRestartPositionAndUser());
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(null, jobExecution1.getUser());
    }

    /**
     * Tests combined restart and user value format: step1.
     * @throws Exception
     */
    @Test
    public void restartPositionAndUserTest2() throws Exception {
        final String restartPositionAndUser1 =  step1;
        final String restartPositionAndUser2 =  step2 + RESTART_POSITION_USER_SEP + user2;
        final String restartPositionAndUser3 =  RESTART_POSITION_USER_SEP + user2;

        JobExecutionImpl jobExecution1 = new JobExecutionImpl(null, 1, null, null, null, null, null,
                BatchStatus.STARTING.name(), null, restartPositionAndUser1);
        assertEquals(step1, jobExecution1.getRestartPosition());
        assertEquals(null, jobExecution1.getUser());
        assertEquals(restartPositionAndUser1, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(user2);
        assertEquals(restartPositionAndUser2, jobExecution1.combineRestartPositionAndUser());
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(user2, jobExecution1.getUser());

        jobExecution1.setRestartPosition(null);
        jobExecution1.setUser(user2);
        assertEquals(restartPositionAndUser3, jobExecution1.combineRestartPositionAndUser());
        assertEquals(null, jobExecution1.getRestartPosition());
        assertEquals(user2, jobExecution1.getUser());
    }

    /**
     * Tests combined restart and user value format: {@value #RESTART_POSITION_USER_SEP}user1.
     * @throws Exception
     */
    @Test
    public void restartPositionAndUserTest3() throws Exception {
        final String restartPositionAndUser1 =  RESTART_POSITION_USER_SEP + user1;
        final String restartPositionAndUser2 =  step2 + RESTART_POSITION_USER_SEP + user2;
        final String restartPositionAndUser3 =  step2;

        JobExecutionImpl jobExecution1 = new JobExecutionImpl(null, 1, null, null, null, null, null,
                BatchStatus.STARTING.name(), null, restartPositionAndUser1);
        assertEquals(null, jobExecution1.getRestartPosition());
        assertEquals(user1, jobExecution1.getUser());
        assertEquals(restartPositionAndUser1, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(user2);
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(user2, jobExecution1.getUser());
        assertEquals(restartPositionAndUser2, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(null);
        assertEquals(restartPositionAndUser3, jobExecution1.combineRestartPositionAndUser());
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(null, jobExecution1.getUser());
    }

    /**
     * Tests combined restart and user value format: null.
     * @throws Exception
     */
    @Test
    public void restartPositionAndUserTest4() throws Exception {
        final String restartPositionAndUser2 =  step2 + RESTART_POSITION_USER_SEP + user2;
        final String restartPositionAndUser3 =  step2;

        JobExecutionImpl jobExecution1 = new JobExecutionImpl(null, 1, null, null, null, null, null,
                BatchStatus.STARTING.name(), null, null);
        assertEquals(null, jobExecution1.getRestartPosition());
        assertEquals(null, jobExecution1.getUser());
        assertEquals(null, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(user2);
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(user2, jobExecution1.getUser());
        assertEquals(restartPositionAndUser2, jobExecution1.combineRestartPositionAndUser());

        jobExecution1.setRestartPosition(step2);
        jobExecution1.setUser(null);
        assertEquals(restartPositionAndUser3, jobExecution1.combineRestartPositionAndUser());
        assertEquals(step2, jobExecution1.getRestartPosition());
        assertEquals(null, jobExecution1.getUser());
    }
}
