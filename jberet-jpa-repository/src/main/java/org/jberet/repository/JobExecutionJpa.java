package org.jberet.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import static org.jberet.repository.TableColumns.BATCHSTATUS;
import static org.jberet.repository.TableColumns.CREATETIME;
import static org.jberet.repository.TableColumns.ENDTIME;
import static org.jberet.repository.TableColumns.EXITSTATUS;
import static org.jberet.repository.TableColumns.JOBEXECUTIONID;
import static org.jberet.repository.TableColumns.JOBINSTANCEID;
import static org.jberet.repository.TableColumns.JOBPARAMETERS;
import static org.jberet.repository.TableColumns.JOB_EXECUTION;
import static org.jberet.repository.TableColumns.LASTUPDATEDTIME;
import static org.jberet.repository.TableColumns.RESTARTPOSITION;
import static org.jberet.repository.TableColumns.STARTTIME;
import static org.jberet.repository.TableColumnsJpa.VERSION;

/**
 *
 * @author a.moscatelli
 */
@Entity
@Table(name = JOB_EXECUTION)
public class JobExecutionJpa implements Serializable, JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = JOBEXECUTIONID)
    private Long id;

    @Column(name = VERSION)
    private Long version;

    @ManyToOne()
    @JoinColumn(name = JOBINSTANCEID, nullable = false)
    private JobInstanceJpa jobInstance;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = CREATETIME)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = STARTTIME)
    private Date startTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = ENDTIME)
    private Date endTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = LASTUPDATEDTIME)
    private Date lastUpdatedTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = BATCHSTATUS)
    private BatchStatus batchStatus;
    
    @Column(name = EXITSTATUS)
    private String exitStatus;
    
    @Convert(converter = PropertiesConverter.class)
    @Column(name = JOBPARAMETERS)
    private Properties jobParameters;
    
    @Column(name = RESTARTPOSITION)
    private String restartPosition;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = StepExecutionJpa_.JOB_EXECUTION, cascade = CascadeType.REMOVE , orphanRemoval = true)
    private Collection<StepExecutionJpa> stepExecutions;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public JobInstanceJpa getJobInstance() {
        return jobInstance;
    }

    public void setJobInstance(JobInstanceJpa jobInstance) {
        this.jobInstance = jobInstance;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    @Override
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = jobParameters;
    }

    public String getRestartPosition() {
        return restartPosition;
    }

    public void setRestartPosition(String restartPosition) {
        this.restartPosition = restartPosition;
    }

    @Override
    public long getExecutionId() {
        return this.getId();
    }

    @Override
    public String getJobName() {
        return this.jobInstance.getJobName();
    }

}
