package org.jberet.util;

import org.jberet.repository.JobExecutionJpa;
import org.jberet.repository.PartitionExecutionJpa;
import org.jberet.repository.StepExecutionJpa;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;

/**
 *
 * @author a.moscatelli
 */
public class BatchUtilJpa {

    public static JobExecutionImpl from(JobExecutionJpa from) {
        JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(
                null,
                from.getJobInstance().getApplicationName(),
                from.getJobInstance().getJobName()
        );
        jobInstanceImpl.setId(from.getJobInstance().getId());
        return new JobExecutionImpl(
                jobInstanceImpl,
                from.getId(),
                from.getJobParameters(),
                from.getCreateTime(),
                from.getStartTime(),
                from.getEndTime(),
                from.getLastUpdatedTime(),
                from.getBatchStatus().toString(),
                from.getExitStatus(),
                from.getRestartPosition()
        );
    }

    public static StepExecutionImpl from(StepExecutionJpa from) {
        return new StepExecutionImpl(
                from.getId(),
                from.getStepName(),
                from.getStartTime(),
                from.getEndTime(),
                from.getBatchStatus().toString(),
                from.getExitStatus(),
                from.getPersistenUserData(),
                from.getReadCount(),
                from.getWriteCount(),
                from.getCommitCount(),
                from.getRollbackCount(),
                from.getReadSkipCount(),
                from.getProcessSkipCount(),
                from.getFilterCount(),
                from.getWriteSkipCount(),
                from.getReaderCheckPointInfo(),
                from.getWriterCheckPointInfo()
        );
    }

    public static PartitionExecutionImpl from(PartitionExecutionJpa from) {
        return new PartitionExecutionImpl(
                from.getId().intValue(),
                from.getStepExecution().getId(),
                from.getStepExecution().getStepName(),
                from.getBatchStatus(),
                from.getExitStatus(),
                from.getPersistenUserData(),
                from.getReaderCheckPointInfo(),
                from.getWriterCheckPointInfo()
        );
    }

}
