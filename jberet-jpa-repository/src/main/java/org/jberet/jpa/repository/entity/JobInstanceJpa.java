package org.jberet.jpa.repository.entity;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import static org.jberet.repository.TableColumns.APPLICATIONNAME;
import static org.jberet.repository.TableColumns.JOBINSTANCEID;
import static org.jberet.repository.TableColumns.JOBNAME;
import static org.jberet.repository.TableColumns.JOB_INSTANCE;
import static org.jberet.jpa.repository.TableColumnsJpa.VERSION;

@Entity
@Table(name = JOB_INSTANCE)
public class JobInstanceJpa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = JOBINSTANCEID)
    private Long id;

    @Column(name = VERSION)
    private Long version;

    @Column(name = JOBNAME)
    private String jobName;

    @Column(name = APPLICATIONNAME)
    private String applicationName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = JobExecutionJpa_.JOB_INSTANCE, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<JobExecutionJpa> jobExecutions;

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

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Collection<JobExecutionJpa> getJobExecutions() {
        return jobExecutions;
    }

    public void setJobExecutions(Collection<JobExecutionJpa> jobExecutions) {
        this.jobExecutions = jobExecutions;
    }

}
