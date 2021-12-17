package org.jberet.jpa.util;

import java.util.Optional;
import javax.batch.runtime.BatchStatus;
import org.jberet.jpa.repository.entity.JobExecutionJpa;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa;
import org.jberet.jpa.repository.entity.StepExecutionJpa;
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
                Optional.ofNullable(from.getBatchStatus()).map(BatchStatus::toString).orElse(null),
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
                Optional.ofNullable(from.getBatchStatus()).orElse(BatchStatus.STARTING).toString(),
                from.getExitStatus(),
                from.getPersistenUserData(),
                Optional.ofNullable(from.getReadCount()).orElse(0L),
                Optional.ofNullable(from.getWriteCount()).orElse(0L),
                Optional.ofNullable(from.getCommitCount()).orElse(0L),
                Optional.ofNullable(from.getRollbackCount()).orElse(0L),
                Optional.ofNullable(from.getReadSkipCount()).orElse(0L),
                Optional.ofNullable(from.getProcessSkipCount()).orElse(0L),
                Optional.ofNullable(from.getFilterCount()).orElse(0L),
                Optional.ofNullable(from.getWriteSkipCount()).orElse(0L),
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
