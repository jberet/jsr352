/*
 * Copyright (c) 2014-2018 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.repository;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

public final class MongoRepository extends AbstractPersistentRepository {
    /**
     * Matches any document whose {@code BATCHSTATUS} has not been set to {@code STOPPING}.
     */
    private static final BasicDBObject NOT_STOPPING =
            new BasicDBObject("$ne", BatchStatus.STOPPING.name());

    private String dataSourceName;
    private String dbUrl;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private MongoCollection<DBObject> seqCollection;

    /**
     * Whether the connected MongoDB deployment can run multi-document transactions. Transactions require a
     * replica set or a sharded cluster running MongoDB 4.0 or later; standalone servers and servers older than
     * 4.0 reject them. The capability is discovered on first use and remembered, so that a deployment without
     * transaction support is not probed on every checkpoint.
     *
     * @see #saveStepAndJobExecution(StepExecutionImpl, long)
     */
    private volatile boolean transactionsSupported = true;

    public static MongoRepository create(final Properties configProperties) {
        return new MongoRepository(configProperties);
    }

    public MongoRepository(final Properties configProperties) {
        dataSourceName = configProperties.getProperty(JdbcRepository.DATASOURCE_JNDI_KEY);
        dbUrl = configProperties.getProperty(JdbcRepository.DB_URL_KEY);

        //if dataSourceName is configured, use dataSourceName;
        //else if dbUrl is specified, use dbUrl;
        if (dataSourceName != null) {
            dataSourceName = dataSourceName.trim();
        }
        if (dataSourceName != null && !dataSourceName.isEmpty()) {
            try {
                mongoClient = InitialContext.doLookup(dataSourceName);
                for (final String s : mongoClient.listDatabaseNames()) {
                    db = mongoClient.getDatabase(s);
                    break;
                }
            } catch (final NamingException e) {
                throw BatchMessages.MESSAGES.failToLookupDataSource(e, dataSourceName);
            }
        } else {
            if (dbUrl != null) {
                dbUrl = dbUrl.trim();
                try {
                    final MongoClientURI uri = new MongoClientURI(dbUrl);
                    mongoClient = (MongoClient) Mongo.Holder.singleton().connect(uri);
                    db = mongoClient.getDatabase(uri.getDatabase());
                } catch (final Exception e) {
                    throw BatchMessages.MESSAGES.invalidConfigProperty(e, JdbcRepository.DB_URL_KEY, dbUrl);
                }
            }
        }
        seqCollection = db.getCollection(TableColumns.SEQ, DBObject.class);
        if (seqCollection.countDocuments() < 3) {
            final DBObject jobInstanceDbo = new BasicDBObject(TableColumns._id, TableColumns.JOBINSTANCEID);
            jobInstanceDbo.put(TableColumns.SEQ, 1L);
            final DBObject jobExecutionDbo = new BasicDBObject(TableColumns._id, TableColumns.JOBEXECUTIONID);
            jobExecutionDbo.put(TableColumns.SEQ, 1L);
            final DBObject stepExecutionDbo = new BasicDBObject(TableColumns._id, TableColumns.STEPEXECUTIONID);
            stepExecutionDbo.put(TableColumns.SEQ, 1L);
            seqCollection.insertOne(jobInstanceDbo);
            seqCollection.insertOne(jobExecutionDbo);
            seqCollection.insertOne(stepExecutionDbo);
        }
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final Long nextId = incrementAndGetSequence(TableColumns.JOBINSTANCEID);
        jobInstance.setId(nextId);
        final DBObject dbObject = new BasicDBObject(TableColumns.JOBINSTANCEID, nextId);
        dbObject.put(TableColumns.JOBNAME, jobInstance.getJobName());
        dbObject.put(TableColumns.APPLICATIONNAME, jobInstance.getApplicationName());
        db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class).insertOne(dbObject);
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        final boolean selectAll = jobName == null || jobName.equals("*");
        final FindIterable<DBObject> findIterable = selectAll ?
                db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class).find() :
                db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class).find(new BasicDBObject(TableColumns.JOBNAME, jobName)).sort(
                        new BasicDBObject(TableColumns.JOBINSTANCEID, -1));

        final MongoCursor<DBObject> cursor = findIterable.iterator();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            final Long i = (Long) next.get(TableColumns.JOBINSTANCEID);
            final SoftReference<JobInstanceImpl, Long> ref = jobInstances.get(i);
            JobInstanceImpl jobInstance1 = (ref != null) ? ref.get() : null;
            if (jobInstance1 == null) {
                final String appName = (String) next.get(TableColumns.APPLICATIONNAME);
                if (selectAll) {
                    final String goodJobName = (String) next.get(TableColumns.JOBNAME);
                    jobInstance1 = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, goodJobName)), appName, goodJobName);
                } else {
                    jobInstance1 = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, jobName)), appName, jobName);
                }
                jobInstance1.setId(i);
                jobInstances.put(i,
                        new SoftReference<JobInstanceImpl, Long>(jobInstance1, jobInstanceReferenceQueue, i));
            }
            //this job instance is already in the cache, so get it from the cache
            result.add(jobInstance1);
        }
        return result;
    }

    @Override
    public JobInstanceImpl getJobInstance(final long jobInstanceId) {
        JobInstanceImpl result = super.getJobInstance(jobInstanceId);
        if (result != null) {
            return result;
        }

        final DBObject one = db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class).find(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstanceId)).first();
        if (one == null) {
            return null;
        }
        final SoftReference<JobInstanceImpl, Long> ref = jobInstances.get(jobInstanceId);
        result = (ref != null) ? ref.get() : null;
        if (result == null) {
            final String appName = (String) one.get(TableColumns.APPLICATIONNAME);
            final String goodJobName = (String) one.get(TableColumns.JOBNAME);
            result = new JobInstanceImpl(getJob(new ApplicationAndJobName(appName, goodJobName)), appName, goodJobName);
            result.setId(jobInstanceId);
            jobInstances.put(jobInstanceId,
                    new SoftReference<JobInstanceImpl, Long>(result, jobInstanceReferenceQueue, jobInstanceId));
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        return (int) db.getCollection(TableColumns.JOB_INSTANCE).countDocuments(new BasicDBObject(TableColumns.JOBNAME, jobName));
    }

    @Override
    void insertJobExecution(final JobExecutionImpl jobExecution) {
        final Long nextId = incrementAndGetSequence(TableColumns.JOBEXECUTIONID);
        jobExecution.setId(nextId);
        final DBObject dbObject = new BasicDBObject(TableColumns.JOBEXECUTIONID, nextId);
        dbObject.put(TableColumns.JOBINSTANCEID, jobExecution.getJobInstance().getInstanceId());
        dbObject.put(TableColumns.CREATETIME, jobExecution.getCreateTime());
        dbObject.put(TableColumns.BATCHSTATUS, jobExecution.getBatchStatus().name());
        dbObject.put(TableColumns.JOBPARAMETERS, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
        db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).insertOne(dbObject);
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate, final boolean saveJobParameters) {
        super.updateJobExecution(jobExecution, fullUpdate, saveJobParameters);
        final DBObject update = new BasicDBObject(TableColumns.LASTUPDATEDTIME, jobExecution.getLastUpdatedTime());
        update.put(TableColumns.STARTTIME, jobExecution.getStartTime());
        update.put(TableColumns.BATCHSTATUS, jobExecution.getBatchStatus().name());

        if (fullUpdate) {
            update.put(TableColumns.ENDTIME, jobExecution.getEndTime());
            update.put(TableColumns.EXITSTATUS, jobExecution.getExitStatus());
            update.put(TableColumns.RESTARTPOSITION, jobExecution.combineRestartPositionAndUser());
            if (saveJobParameters) {
                update.put(TableColumns.JOBPARAMETERS, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
            }
        }

        db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).updateOne(
                new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecution.getExecutionId()),
                new BasicDBObject("$set", update));
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        JobExecutionImpl result = super.getJobExecution(jobExecutionId);
        if (result != null) {
            return result;
        }
        final DBObject one = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find(
                new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecutionId)).first();
        if (one == null) {
            return null;
        }
        final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(jobExecutionId);
        result = (ref != null) ? ref.get() : null;
        if (result == null) {
            final Long jobInstanceId = (Long) one.get(TableColumns.JOBINSTANCEID);
            result = new JobExecutionImpl(getJobInstance(jobInstanceId),
                    jobExecutionId,
                    BatchUtil.stringToProperties((String) one.get(TableColumns.JOBPARAMETERS)),
                    (Date) one.get(TableColumns.CREATETIME),
                    (Date) one.get(TableColumns.STARTTIME),
                    (Date) one.get(TableColumns.ENDTIME),
                    (Date) one.get(TableColumns.LASTUPDATEDTIME),
                    (String) one.get(TableColumns.BATCHSTATUS),
                    (String) one.get(TableColumns.EXITSTATUS),
                    (String) one.get(TableColumns.RESTARTPOSITION));
            jobExecutions.put(jobExecutionId,
                    new SoftReference<JobExecutionImpl, Long>(result, jobExecutionReferenceQueue, jobExecutionId));
        }
        return result;
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        long jobInstanceId = jobInstance == null ? 0 : jobInstance.getInstanceId();

        FindIterable<DBObject> findIterable = jobInstance == null ?
                db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find():
                db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find(
                        new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstance.getInstanceId()));

        MongoCursor<DBObject> cursor = findIterable.sort(new BasicDBObject(TableColumns.JOBEXECUTIONID, 1)).iterator();
        final List<JobExecution> result = new ArrayList<JobExecution>();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            final Long i = (Long) next.get(TableColumns.JOBEXECUTIONID);
            final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(i);
            JobExecutionImpl jobExecution1 = (ref != null) ? ref.get() : null;
            if (jobExecution1 == null) {
                if (jobInstance == null) {
                    jobInstanceId = (Long) next.get(TableColumns.JOBINSTANCEID);
                }
                final Properties jobParameters1 = BatchUtil.stringToProperties((String) next.get(TableColumns.JOBPARAMETERS));
                jobExecution1 =
                        new JobExecutionImpl(getJobInstance(jobInstanceId), i, jobParameters1,
                                (Date) next.get(TableColumns.CREATETIME),
                                (Date) next.get(TableColumns.STARTTIME),
                                (Date) next.get(TableColumns.ENDTIME),
                                (Date) next.get(TableColumns.LASTUPDATEDTIME),
                                (String) next.get(TableColumns.BATCHSTATUS),
                                (String) next.get(TableColumns.EXITSTATUS),
                                (String) next.get(TableColumns.RESTARTPOSITION));
                jobExecutions.put(i,
                        new SoftReference<JobExecutionImpl, Long>(jobExecution1, jobExecutionReferenceQueue, i));
            }
            // jobExecution1 is either got from the cache, or created, now add it to the result list
            result.add(jobExecution1);
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        final List<Long> result = new ArrayList<Long>();

        try {
            //find all job instance ids belonging to the jobName
            BasicDBObject keys = new BasicDBObject(TableColumns.JOBINSTANCEID, 1);
            FindIterable<DBObject> findIterable = db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class).find(
                    new BasicDBObject(TableColumns.JOBNAME, jobName));
            MongoCursor<DBObject> cursor = findIterable.projection(keys).iterator();

            if (!cursor.hasNext()) {
                throw BatchMessages.MESSAGES.noSuchJobException(jobName);
            }

            //add matching job instance ids to the "jobinstanceid in" list
            BasicDBList basicDBList = new BasicDBList();
            while (cursor.hasNext()) {
                final DBObject next = cursor.next();
                basicDBList.add(next.get(TableColumns.JOBINSTANCEID));
            }
            final DBObject inJobInstanceIdsClause = new BasicDBObject("$in", basicDBList);
            final BasicDBObject query = new BasicDBObject(TableColumns.JOBINSTANCEID, inJobInstanceIdsClause);

            //create "batchstatus in" list
            basicDBList = new BasicDBList();
            basicDBList.add(BatchStatus.STARTED.name());
            basicDBList.add(BatchStatus.STARTING.name());
            final DBObject inBatchStatusClause = new BasicDBObject("$in", basicDBList);

            //combine batchstatus in clause into jobinstanceid in clause
            query.put(TableColumns.BATCHSTATUS, inBatchStatusClause);
            keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
            cursor = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find(query).projection(keys).iterator();

            while (cursor.hasNext()) {
                final DBObject next = cursor.next();
                result.add((Long) next.get(TableColumns.JOBEXECUTIONID));
            }
        } catch (Exception e) {
            result.addAll(getCachedJobExecutions(jobName, true));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        return getJobExecutionsByJob(jobName, null);
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName, Integer limit) {
        final List<Long> result = new ArrayList<>();

        // jobInstances result is already in desc order
        final List<JobInstance> jobInstances = getJobInstances(jobName);
        for (JobInstance jobInstance : jobInstances) {
            final List<JobExecution> jobExecutions = getJobExecutions(jobInstance);
            for (int i = jobExecutions.size() - 1; i >= 0; i--) {
                result.add(jobExecutions.get(i).getExecutionId());
            }
        }
        if (limit != null && result.size() >= limit) {
            BatchLogger.LOGGER.jobExecutionRecordsLimited(limit);
            result.sort(Comparator.reverseOrder());
            return result.subList(0, limit);
        } else {
            return result;
        }
    }

    @Override
    void insertStepExecution(final StepExecutionImpl stepExecution, final JobExecutionImpl jobExecution) {
        final Long nextId = incrementAndGetSequence(TableColumns.STEPEXECUTIONID);
        stepExecution.setId(nextId);
        final DBObject dbObject = new BasicDBObject(TableColumns.STEPEXECUTIONID, nextId);
        dbObject.put(TableColumns.JOBEXECUTIONID, jobExecution.getExecutionId());
        dbObject.put(TableColumns.STEPNAME, stepExecution.getStepName());
        dbObject.put(TableColumns.STARTTIME, stepExecution.getStartTime());
        dbObject.put(TableColumns.BATCHSTATUS, stepExecution.getBatchStatus().name());
        db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class).insertOne(dbObject);
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        try {
            db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class).updateOne(
                    new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecution.getStepExecutionId()),
                    new BasicDBObject("$set", stepExecutionUpdate((StepExecutionImpl) stepExecution)));

        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "updateStepExecution");
        }
    }

    /**
     * Builds the {@code $set} document that persists the mutable state of a step execution.
     */
    private static DBObject stepExecutionUpdate(final StepExecutionImpl stepExecutionImpl) {
        final DBObject update = new BasicDBObject(TableColumns.ENDTIME, stepExecutionImpl.getEndTime());
        update.put(TableColumns.BATCHSTATUS, stepExecutionImpl.getBatchStatus().name());
        update.put(TableColumns.EXITSTATUS, stepExecutionImpl.getExitStatus());
        update.put(TableColumns.EXECUTIONEXCEPTION, TableColumns.formatException(stepExecutionImpl.getException()));
        update.put(TableColumns.PERSISTENTUSERDATA, stepExecutionImpl.getPersistentUserDataSerialized());
        update.put(TableColumns.READCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_COUNT));
        update.put(TableColumns.WRITECOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_COUNT));
        update.put(TableColumns.COMMITCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.COMMIT_COUNT));
        update.put(TableColumns.ROLLBACKCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.ROLLBACK_COUNT));
        update.put(TableColumns.READSKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_SKIP_COUNT));
        update.put(TableColumns.PROCESSSKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.PROCESS_SKIP_COUNT));
        update.put(TableColumns.FILTERCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.FILTER_COUNT));
        update.put(TableColumns.WRITESKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_SKIP_COUNT));
        update.put(TableColumns.READERCHECKPOINTINFO, stepExecutionImpl.getReaderCheckpointInfoSerialized());
        update.put(TableColumns.WRITERCHECKPOINTINFO, stepExecutionImpl.getWriterCheckpointInfoSerialized());
        return update;
    }

    /**
     * Builds the {@code $set} document that persists the mutable state of a partition execution.
     */
    private static DBObject partitionExecutionUpdate(final PartitionExecutionImpl partitionExecution) {
        final DBObject update = new BasicDBObject(TableColumns.BATCHSTATUS, partitionExecution.getBatchStatus().name());
        update.put(TableColumns.EXITSTATUS, partitionExecution.getExitStatus());
        update.put(TableColumns.EXECUTIONEXCEPTION, TableColumns.formatException(partitionExecution.getException()));
        update.put(TableColumns.PERSISTENTUSERDATA, partitionExecution.getPersistentUserDataSerialized());
        update.put(TableColumns.READERCHECKPOINTINFO, partitionExecution.getReaderCheckpointInfoSerialized());
        update.put(TableColumns.WRITERCHECKPOINTINFO, partitionExecution.getWriterCheckpointInfoSerialized());
        return update;
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution,
                                   final AbstractStepExecution stepOrPartitionExecution) {
        //super.savePersistentData() serialize persistent data and checkpoint info to avoid further modification
        super.savePersistentData(jobExecution, stepOrPartitionExecution);
        if (stepOrPartitionExecution instanceof StepExecutionImpl) {
            //stepExecution is for the main step, and should map to the STEP_EXECUTIOIN table
            updateStepExecution(stepOrPartitionExecution);
        } else {
            //stepExecutionId is for a partition execution, and should map to the PARTITION_EXECUTION table
            //need to update PARTITION_EXECUTION
            final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) stepOrPartitionExecution;

            try {
                final BasicDBObject query = new BasicDBObject(TableColumns.STEPEXECUTIONID, partitionExecution.getStepExecutionId());
                query.put(TableColumns.PARTITIONEXECUTIONID, partitionExecution.getPartitionId());

                db.getCollection(TableColumns.PARTITION_EXECUTION, DBObject.class).updateOne(query,
                        new BasicDBObject("$set", partitionExecutionUpdate(partitionExecution)));
            } catch (final Exception e) {
                throw BatchMessages.MESSAGES.failToRunQuery(e, "savePersistentData");
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * In addition to saving the step execution, this method also refreshes the enclosing job execution's
     * {@code LASTUPDATEDTIME}, so that the progress of a long-running job is visible after every checkpoint
     * instead of only when the job ends. This mirrors the behaviour of
     * {@link JdbcRepository#savePersistentDataIfNotStopping}.
     * <p>
     * A partition execution only updates its own document; the enclosing job execution is left alone, since the
     * partitioned step itself reports the job's progress.
     *
     * @return 1 if the execution data was saved; 0 if the stored step or partition execution is already
     *         {@code STOPPING}, in which case nothing is written
     */
    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution,
                                               final AbstractStepExecution stepOrPartitionExecution) {
        //super.savePersistentData() serialize persistent data and checkpoint info to avoid further modification
        super.savePersistentData(jobExecution, stepOrPartitionExecution);

        try {
            if (stepOrPartitionExecution instanceof StepExecutionImpl) {
                //stepExecution is for the main step, and should map to the STEP_EXECUTION collection
                return saveStepAndJobExecution((StepExecutionImpl) stepOrPartitionExecution,
                        jobExecution.getExecutionId());
            }

            //stepOrPartitionExecution is a partition execution, and should map to the PARTITION_EXECUTION collection
            final PartitionExecutionImpl partitionExecution = (PartitionExecutionImpl) stepOrPartitionExecution;
            final BasicDBObject query = new BasicDBObject(TableColumns.STEPEXECUTIONID, partitionExecution.getStepExecutionId());
            query.put(TableColumns.PARTITIONEXECUTIONID, partitionExecution.getPartitionId());
            query.put(TableColumns.BATCHSTATUS, NOT_STOPPING);

            final UpdateResult result = db.getCollection(TableColumns.PARTITION_EXECUTION, DBObject.class)
                    .updateOne(query, new BasicDBObject("$set", partitionExecutionUpdate(partitionExecution)));
            return result.getMatchedCount() > 0 ? 1 : 0;
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "savePersistentDataIfNotStopping");
        }
    }

    /**
     * Saves a step execution checkpoint and refreshes the enclosing job execution's {@code LASTUPDATEDTIME}.
     * <p>
     * Both writes are performed in a single multi-document transaction so that the job execution's
     * {@code LASTUPDATEDTIME} can never advance without the matching step execution checkpoint having been
     * saved. Transactions require a replica set or sharded cluster on MongoDB 4.0 or later; against a standalone
     * server, or a server older than 4.0, the driver rejects the transaction and the two updates are applied
     * sequentially instead. In that degraded mode the writes are not atomic, which is harmless because
     * {@code LASTUPDATEDTIME} is progress metadata only, and the step execution is written first so the job
     * execution never claims progress that was not saved.
     *
     * @param stepExecution  the step execution to save
     * @param jobExecutionId the id of the enclosing job execution
     * @return 1 if the step execution was saved, 0 if the stored step execution is already {@code STOPPING}
     */
    private int saveStepAndJobExecution(final StepExecutionImpl stepExecution, final long jobExecutionId) {
        if (transactionsSupported) {
            try (ClientSession session = mongoClient.startSession()) {
                return session.withTransaction(() -> updateStepAndJobExecution(session, stepExecution, jobExecutionId));
            } catch (final RuntimeException e) {
                if (!isTransactionUnsupported(e)) {
                    throw e;
                }
                //remember that this deployment cannot run transactions, so later checkpoints do not retry
                transactionsSupported = false;
                BatchLogger.LOGGER.tracef(e,
                        "MongoDB deployment does not support transactions; " +
                                "step execution and job execution checkpoints will be saved non-atomically%n");
            }
        }
        return updateStepAndJobExecution(null, stepExecution, jobExecutionId);
    }

    /**
     * Updates the {@code STEP_EXECUTION} document and, only if that document was not already {@code STOPPING},
     * the {@code LASTUPDATEDTIME} of the enclosing {@code JOB_EXECUTION} document.
     *
     * @param session        the session to run both updates in, or {@code null} to run them without a session
     * @param stepExecution  the step execution to save
     * @param jobExecutionId the id of the enclosing job execution
     * @return 1 if the step execution was saved, 0 if the stored step execution is already {@code STOPPING}
     */
    private int updateStepAndJobExecution(final ClientSession session,
                                          final StepExecutionImpl stepExecution,
                                          final long jobExecutionId) {
        final BasicDBObject stepQuery = new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecution.getStepExecutionId());
        stepQuery.put(TableColumns.BATCHSTATUS, NOT_STOPPING);
        final BasicDBObject stepUpdate = new BasicDBObject("$set", stepExecutionUpdate(stepExecution));

        final MongoCollection<DBObject> stepExecutions = db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class);
        final UpdateResult stepResult = session == null ?
                stepExecutions.updateOne(stepQuery, stepUpdate) :
                stepExecutions.updateOne(session, stepQuery, stepUpdate);

        //only touch the job execution when the step execution row was actually written;
        //a matched count of 0 means the step execution is STOPPING and nothing was persisted
        if (stepResult.getMatchedCount() == 0) {
            return 0;
        }

        final BasicDBObject jobQuery = new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecutionId);
        final BasicDBObject jobUpdate = new BasicDBObject("$set",
                new BasicDBObject(TableColumns.LASTUPDATEDTIME, new Date()));

        final MongoCollection<DBObject> jobExecutions = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class);
        if (session == null) {
            jobExecutions.updateOne(jobQuery, jobUpdate);
        } else {
            jobExecutions.updateOne(session, jobQuery, jobUpdate);
        }
        return 1;
    }

    /**
     * Tells whether {@code e} was raised because the connected MongoDB deployment cannot run multi-document
     * transactions, as opposed to a genuine failure of the checkpoint itself.
     */
    private static boolean isTransactionUnsupported(final Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof MongoClientException) {
                //"Sessions are not supported by ..." / "Transactions are not supported by ..."
                return true;
            }
            if (t instanceof MongoCommandException) {
                final int code = ((MongoCommandException) t).getErrorCode();
                //20 IllegalOperation: "Transaction numbers are only allowed on a replica set member or mongos"
                //59 CommandNotFound: the server predates the transaction commands
                //263 OperationNotSupportedInTransaction
                if (code == 20 || code == 59 || code == 263) {
                    return true;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /*
    StepExecution selectStepExecution(final long stepExecutionId, final ClassLoader classLoader) {
        final DBCollection collection = db.getCollection(TableColumns.STEP_EXECUTION);
        final DBObject dbObject = collection.findOne(new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecutionId));
        return createStepExecutionFromDBObject(dbObject, classLoader);
    }
    */

    /**
     * Retrieves a list of StepExecution from database by JobExecution id.  This method does not check the cache, so it
     * should only be called after the cache has been searched without a match.
     *
     * @param jobExecutionId if null, retrieves all StepExecutions; otherwise, retrieves all StepExecutions belongs to the JobExecution id
     * @return a list of StepExecutions
     */
    @Override
    List<StepExecution> selectStepExecutions(final Long jobExecutionId, final ClassLoader classLoader) {
        final MongoCollection<DBObject> collection = db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class);
        final FindIterable<DBObject> findIterable = jobExecutionId == null ?
                collection.find(DBObject.class) :
                collection.find(new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecutionId), DBObject.class);
        final MongoCursor<DBObject> cursor = findIterable.sort(new BasicDBObject(TableColumns.STEPEXECUTIONID, 1)).iterator();
        final List<StepExecution> result = new ArrayList<StepExecution>();
        createStepExecutionsFromDBCursor(cursor, result, classLoader);
        return result;
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution,
                                      final PartitionExecutionImpl partitionExecution) {
        super.addPartitionExecution(enclosingStepExecution, partitionExecution);
        final DBObject dbObject = new BasicDBObject(TableColumns.PARTITIONEXECUTIONID, partitionExecution.getPartitionId());
        dbObject.put(TableColumns.STEPEXECUTIONID, partitionExecution.getStepExecutionId());
        dbObject.put(TableColumns.BATCHSTATUS, partitionExecution.getBatchStatus().name());
        db.getCollection(TableColumns.PARTITION_EXECUTION, DBObject.class).insertOne(dbObject);
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        final StepExecutionImpl result = super.findOriginalStepExecutionForRestart(stepName, jobExecutionToRestart, classLoader);
        if (result != null) {
            return result;
        }

        final BasicDBObject keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
        keys.put(TableColumns._id, 0);
        final MongoCursor<DBObject> cursor = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobExecutionToRestart.getJobInstance().getInstanceId()))
                .projection(keys).iterator();
        final BasicDBList basicDBList = new BasicDBList();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            basicDBList.add(next.get(TableColumns.JOBEXECUTIONID));
        }
        final DBObject inClause = new BasicDBObject("$in", basicDBList);
        final BasicDBObject query = new BasicDBObject(TableColumns.JOBEXECUTIONID, inClause);
        query.put(TableColumns.STEPNAME, stepName);

        final MongoCursor<DBObject> cursor1 = db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class).find(query).sort(
                new BasicDBObject(TableColumns.STEPEXECUTIONID, -1)).iterator();

        return createStepExecutionFromDBObject(cursor1.next(), classLoader);
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
                                                               final StepExecutionImpl stepExecution,
                                                               final boolean notCompletedOnly,
                                                               final ClassLoader classLoader) {
        List<PartitionExecutionImpl> result = super.getPartitionExecutions(stepExecutionId, stepExecution, notCompletedOnly, classLoader);
        if (result != null && !result.isEmpty()) {
            return result;
        }
        result = new ArrayList<PartitionExecutionImpl>();
        final MongoCursor<DBObject> cursor = db.getCollection(TableColumns.PARTITION_EXECUTION, DBObject.class).find(
                new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecutionId)).sort(
                new BasicDBObject(TableColumns.PARTITIONEXECUTIONID, 1)).iterator();

        try {
            while (cursor.hasNext()) {
                final DBObject next = cursor.next();
                final String batchStatusValue = (String) next.get(TableColumns.BATCHSTATUS);
                if (!notCompletedOnly ||
                        !BatchStatus.COMPLETED.name().equals(batchStatusValue)) {
                    result.add(new PartitionExecutionImpl(
                            (Integer) next.get(TableColumns.PARTITIONEXECUTIONID),
                            (Long) next.get(TableColumns.STEPEXECUTIONID),
                            stepExecution.getStepName(),
                            BatchStatus.valueOf(batchStatusValue),
                            (String) next.get(TableColumns.EXITSTATUS),
                            (byte[]) next.get(TableColumns.PERSISTENTUSERDATA),
                            (byte[]) next.get(TableColumns.READERCHECKPOINTINFO),
                            (byte[]) next.get(TableColumns.WRITERCHECKPOINTINFO)
                    ));
                }
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "getPartitionExecutions");
        }
        return result;
    }

    private void createStepExecutionsFromDBCursor(final MongoCursor<DBObject> cursor, final List<StepExecution> result, final ClassLoader classLoader) {
        while (cursor.hasNext()) {
            result.add(createStepExecutionFromDBObject(cursor.next(), classLoader));
        }
    }

    private StepExecutionImpl createStepExecutionFromDBObject(final DBObject dbObject, final ClassLoader classLoader) {
        if (dbObject == null) {
            return null;
        }
        try {
            return new StepExecutionImpl(
                    ((Number) dbObject.get(TableColumns.STEPEXECUTIONID)).longValue(),
                    (String) dbObject.get(TableColumns.STEPNAME),
                    (Date) dbObject.get(TableColumns.STARTTIME),
                    (Date) dbObject.get(TableColumns.ENDTIME),
                    (String) dbObject.get(TableColumns.BATCHSTATUS),
                    (String) dbObject.get(TableColumns.EXITSTATUS),
                    (byte[]) dbObject.get(TableColumns.PERSISTENTUSERDATA),
                    numberObjectToLong(dbObject.get(TableColumns.READCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.WRITECOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.COMMITCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.ROLLBACKCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.READSKIPCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.PROCESSSKIPCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.FILTERCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.WRITESKIPCOUNT)),
                    (byte[]) dbObject.get(TableColumns.READERCHECKPOINTINFO),
                    (byte[]) dbObject.get(TableColumns.WRITERCHECKPOINTINFO)
            );
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "createStepExecutionFromDBObject");
        }
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        final BasicDBObject keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
        keys.put(TableColumns._id, 0);
        final MongoCursor<DBObject> cursor = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class).find(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstanceId)).projection(keys).iterator();
        final BasicDBList basicDBList = new BasicDBList();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            basicDBList.add(next.get(TableColumns.JOBEXECUTIONID));
        }
        final DBObject inClause = new BasicDBObject("$in", basicDBList);
        final DBObject query = new BasicDBObject(TableColumns.JOBEXECUTIONID, inClause);
        query.put(TableColumns.STEPNAME, stepName);
        return (int) db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class).countDocuments(keys);
    }

    /**
     * Executes MongoDB remove queries.
     *
     * @param removeQueries a series of remove queries delimited by semi-colon (;). For example,
     *                      db.PARTITION_EXECUTION.remove({ STEPEXECUTIONID: { $gt: 100 } });
     *                      db.STEP_EXECUTION.remove({ STEPEXECUTIONID: { $gt: 100 } });
     *                      db.JOB_EXECUTION.remove({ JOBEXECUTIONID: { $gt: 10 } });
     *                      db.JOB_INSTANCE.remove({ JOBINSTANCEID: { $gt: 10 } })
     */
    public void executeRemoveQueries(final String removeQueries) {
        //db.getCollection(TableColumns.PARTITION_EXECUTION).remove(null);
        final String[] queries = removeQueries.split(";");
        final List<AbstractMap.SimpleEntry<BasicDBObject, MongoCollection>> dbObjectsAndCollections = new ArrayList<>();
        String queryConditionJson;

        for (String q : queries) {
            q = q.trim();
            if (q.isEmpty()) {
                continue;
            }
            final int dot1Pos = q.indexOf('.', 2);
            final int removePos = q.indexOf("remove", dot1Pos + 3);
            final int dot2Pos = q.lastIndexOf('.', removePos);
            final int leftParenthesisPos = q.indexOf('(', removePos + 6);
            if (dot1Pos <= 0 || dot2Pos <= 0 || removePos <= 0 || leftParenthesisPos <= 0 ||
                    leftParenthesisPos < removePos || leftParenthesisPos < dot2Pos || leftParenthesisPos < dot1Pos ||
                    removePos < dot2Pos || removePos < dot1Pos || dot2Pos <= dot1Pos) {
                throw BatchMessages.MESSAGES.failToRunQuery(null, q);
            }

            final String collectionName = q.substring(dot1Pos + 1, dot2Pos).trim();
            queryConditionJson = q.substring(leftParenthesisPos + 1, q.length() - 1).trim();
            BasicDBObject parsedDBObject = null;
            if (!queryConditionJson.isEmpty()) {
                parsedDBObject = BasicDBObject.parse(queryConditionJson);
            }
            if (parsedDBObject == null) {
                parsedDBObject = new BasicDBObject();
            }

            final MongoCollection<DBObject> coll;

            if (collectionName.equalsIgnoreCase(TableColumns.JOB_EXECUTION)) {
                coll = db.getCollection(TableColumns.JOB_EXECUTION, DBObject.class);
            } else if (collectionName.equalsIgnoreCase(TableColumns.STEP_EXECUTION)) {
                coll = db.getCollection(TableColumns.STEP_EXECUTION, DBObject.class);
            } else if (collectionName.equalsIgnoreCase(TableColumns.JOB_INSTANCE)) {
                coll = db.getCollection(TableColumns.JOB_INSTANCE, DBObject.class);
            } else if (collectionName.equalsIgnoreCase(TableColumns.PARTITION_EXECUTION)) {
                coll = db.getCollection(TableColumns.PARTITION_EXECUTION, DBObject.class);
            } else {
                throw BatchMessages.MESSAGES.failToRunQuery(null, q);
            }

            dbObjectsAndCollections.add(new AbstractMap.SimpleEntry<>(parsedDBObject, coll));
            BatchLogger.LOGGER.tracef("About to remove from collection: %s, with query: %s%n", coll, parsedDBObject);
        }

        for (final AbstractMap.SimpleEntry<BasicDBObject, MongoCollection> e : dbObjectsAndCollections) {
            //use deleteMany since one query condition (e.g., with in clause) can match multiple documents
            e.getValue().deleteMany(e.getKey());
        }
    }

    private Long incrementAndGetSequence(final String whichId) {
        final BasicDBObject query = new BasicDBObject(TableColumns._id, whichId);
        final BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject(TableColumns.SEQ, 1));
        final DBObject result = seqCollection.findOneAndUpdate(query, update);
        return (Long) result.get(TableColumns.SEQ);
    }

    private static long numberObjectToLong(final Object obj) {
        return obj == null ? 0 : ((Number) obj).longValue();
    }
}
