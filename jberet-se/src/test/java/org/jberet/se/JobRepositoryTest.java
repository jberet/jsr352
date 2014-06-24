package org.jberet.se;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(WeldRunner.class)
public class JobRepositoryTest {

	@Inject
	private JobRepository jobRepository;

	private JobOperator jobOperator;

	@Before
	public void setUp() throws Exception {
		this.jobOperator = BatchRuntime.getJobOperator();
	}

	@Test
	public void testInjection() throws Exception {
		assertNotNull(this.jobRepository);
		assertEquals(JdbcRepository.class, this.jobRepository.getClass());
	}

	@Test
	public void testRemoveJob() throws Exception {
		long executionId = this.jobOperator.start(
				"org.jberet.se.test.helloWorld", null);
		while (this.jobOperator.getJobExecution(executionId).getBatchStatus() != BatchStatus.COMPLETED) {
			Thread.sleep(100);
		}
		assertTrue(this.jobOperator.getJobNames().contains(
				"org.jberet.se.test.helloWorld"));
		assertNotNull(this.jobRepository
				.getJob("org.jberet.se.test.helloWorld"));
		long jobInstanceId = this.jobOperator.getJobInstance(executionId)
				.getInstanceId();
		this.jobRepository.removeJobInstance(jobInstanceId);
		JobExecution jobExecution = this.jobOperator.getJobExecution(executionId);
		assertNull(jobExecution);
	}
}
