/*
 * Copyright (c) 2026 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import jakarta.batch.runtime.BatchStatus;
import org.h2.jdbcx.JdbcDataSource;
import org.jberet.creation.ArchiveXmlLoader;
import org.jberet.job.model.Job;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.tools.MetaInfBatchJobsJobXmlResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the checkpoint behaviour of {@link JdbcRepository#savePersistentDataIfNotStopping}.
 *
 * <p>Uses an in-process H2 database so no external infrastructure is required.
 * Each test class run gets a fresh schema via {@code DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}
 * and a unique in-memory database name to avoid cross-test interference.</p>
 */
public class JdbcRepositoryCheckpointTest {

    private static JdbcDataSource dataSource;
    private static JdbcRepository repository;
    private static Job job;

    @BeforeAll
    static void setUp() throws Exception {
        // Unique in-memory database per test class run; tables are auto-created by JdbcRepository.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jberet-checkpoint-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        repository = new JdbcRepository(dataSource, new Properties());

        job = ArchiveXmlLoader.loadJobXml(
                "exception-class-filter",
                JdbcRepositoryCheckpointTest.class.getClassLoader(),
                new ArrayList<>(),
                new MetaInfBatchJobsJobXmlResolver());
    }

    /**
     * Verifies that {@link JdbcRepository#savePersistentDataIfNotStopping} updates the
     * {@code LASTUPDATEDTIME} column on the {@code JOB_EXECUTION} row after a successful
     * chunk checkpoint.
     */
    @Test
    void updatesJobExecutionLastUpdatedTimeOnEveryCheckpoint() throws Exception {
        final JobInstanceImpl jobInstance = repository.createJobInstance(job, null, getClass().getClassLoader());
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, null);
        jobExecution.setBatchStatus(BatchStatus.STARTED);

        final StepExecutionImpl stepExecution = new StepExecutionImpl("step1");
        stepExecution.setBatchStatus(BatchStatus.STARTED);
        repository.insertStepExecution(stepExecution, jobExecution);

        final long initialTime = System.currentTimeMillis() - 1L;
        final int savedCount = repository.savePersistentDataIfNotStopping(jobExecution, stepExecution);

        assertEquals(1, savedCount, "savePersistentDataIfNotStopping should report 1 row saved for a STARTED step");

        final long dbLastUpdated = queryLastUpdatedTime(jobExecution.getExecutionId());
        assertNotNull(dbLastUpdated, "LASTUPDATEDTIME must not be null after a checkpoint");
        assertTrue(dbLastUpdated >= initialTime, "LASTUPDATEDTIME in JOB_EXECUTION (" + dbLastUpdated + ") must be >= intial time (" + initialTime + ")");
    }
    /* WIP */
    @Disabled 
    @Test
    void noUpdateJobExecutionLastUpdatedTimeOnCheckpointIfStopping() throws Exception {
        final JobInstanceImpl jobInstance = repository.createJobInstance(job, null, getClass().getClassLoader());
        final JobExecutionImpl jobExecution = repository.createJobExecution(jobInstance, null);
        jobExecution.setBatchStatus(BatchStatus.STARTED);

        final StepExecutionImpl stepExecution = new StepExecutionImpl("step2");
        stepExecution.setBatchStatus(BatchStatus.STOPPING);
        repository.insertStepExecution(stepExecution, jobExecution);

        final long startTime = jobExecution.getStartTime().getTime();
        final long dbLastUpdated = queryLastUpdatedTime(jobExecution.getExecutionId());
        assertNotNull(dbLastUpdated, "LASTUPDATEDTIME must not be null after a checkpoint");
        assertTrue(dbLastUpdated == startTime,"LASTUPDATEDTIME in JOB_EXECUTION (" + dbLastUpdated + ") must be equal to start time (" + startTime + ")");
    }

    private Long queryLastUpdatedTime(final long jobExecutionId) throws SQLException {
        final String sql = "SELECT LASTUPDATEDTIME FROM JOB_EXECUTION WHERE JOBEXECUTIONID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobExecutionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.getTime() : null;
                }
            }
        }
        throw new IllegalStateException("No JOB_EXECUTION row found for id=" + jobExecutionId);
    }
}
