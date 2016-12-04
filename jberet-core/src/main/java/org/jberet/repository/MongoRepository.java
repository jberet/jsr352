/*
 * Copyright (c) 2014-2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.repository;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.util.JSON;
import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

public final class MongoRepository extends AbstractPersistentRepository {
    private String dataSourceName;
    private String dbUrl;
    private MongoClient mongoClient;
    private DB db;
    private DBCollection seqCollection;

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
                for (final DB d : mongoClient.getUsedDatabases()) {
                    db = d;
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
                    db = mongoClient.getDB(uri.getDatabase());
                } catch (final Exception e) {
                    throw BatchMessages.MESSAGES.invalidConfigProperty(e, JdbcRepository.DB_URL_KEY, dbUrl);
                }
            }
        }
        if (!db.collectionExists(TableColumns.SEQ)) {
            seqCollection = db.createCollection(TableColumns.SEQ, new BasicDBObject());
            final DBObject jobInstanceDbo = new BasicDBObject(TableColumns._id, TableColumns.JOBINSTANCEID);
            jobInstanceDbo.put(TableColumns.SEQ, 1L);
            final DBObject jobExecutionDbo = new BasicDBObject(TableColumns._id, TableColumns.JOBEXECUTIONID);
            jobExecutionDbo.put(TableColumns.SEQ, 1L);
            final DBObject stepExecutionDbo = new BasicDBObject(TableColumns._id, TableColumns.STEPEXECUTIONID);
            stepExecutionDbo.put(TableColumns.SEQ, 1L);
            seqCollection.insert(jobInstanceDbo, jobExecutionDbo, stepExecutionDbo);
        } else {
            seqCollection = db.getCollection(TableColumns.SEQ);
        }
    }

    @Override
    void insertJobInstance(final JobInstanceImpl jobInstance) {
        final Long nextId = incrementAndGetSequence(TableColumns.JOBINSTANCEID);
        jobInstance.setId(nextId);
        final DBObject dbObject = new BasicDBObject(TableColumns.JOBINSTANCEID, nextId);
        dbObject.put(TableColumns.JOBNAME, jobInstance.getJobName());
        dbObject.put(TableColumns.APPLICATIONNAME, jobInstance.getApplicationName());
        db.getCollection(TableColumns.JOB_INSTANCE).insert(dbObject);
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        final List<JobInstance> result = new ArrayList<JobInstance>();
        final DBCursor cursor = jobName == null ? db.getCollection(TableColumns.JOB_INSTANCE).find() :
                db.getCollection(TableColumns.JOB_INSTANCE).find(new BasicDBObject(TableColumns.JOBNAME, jobName)).sort(
                        new BasicDBObject(TableColumns.JOBINSTANCEID, -1));

        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            final Long i = (Long) next.get(TableColumns.JOBINSTANCEID);
            final SoftReference<JobInstanceImpl, Long> ref = jobInstances.get(i);
            JobInstanceImpl jobInstance1 = (ref != null) ? ref.get() : null;
            if (jobInstance1 == null) {
                final String appName = (String) next.get(TableColumns.APPLICATIONNAME);
                if (jobName == null) {
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

        final DBObject one = db.getCollection(TableColumns.JOB_INSTANCE).findOne(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstanceId));
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
        return (int) db.getCollection(TableColumns.JOB_INSTANCE).count(new BasicDBObject(TableColumns.JOBNAME, jobName));
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
        db.getCollection(TableColumns.JOB_EXECUTION).insert(dbObject);
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

        db.getCollection(TableColumns.JOB_EXECUTION).update(
                new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecution.getExecutionId()),
                new BasicDBObject("$set", update));
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        JobExecutionImpl result = super.getJobExecution(jobExecutionId);
        if (result != null) {
            return result;
        }
        final DBObject one = db.getCollection(TableColumns.JOB_EXECUTION).findOne(
                new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecutionId));
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
        DBCursor cursor = jobInstance == null ? db.getCollection(TableColumns.JOB_EXECUTION).find() :
                db.getCollection(TableColumns.JOB_EXECUTION).find(
                        new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstance.getInstanceId()));
        cursor = cursor.sort(new BasicDBObject(TableColumns.JOBEXECUTIONID, 1));
        final List<JobExecution> result = new ArrayList<JobExecution>();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            final Long i = (Long) next.get(TableColumns.JOBEXECUTIONID);
            final SoftReference<JobExecutionImpl, Long> ref = jobExecutions.get(i);
            JobExecutionImpl jobExecution1 = (ref != null) ? ref.get() : null;
            if (jobExecution1 == null) {
                if (jobInstanceId == 0) {
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
        //find all job instance ids belonging to the jobName
        DBObject keys = new BasicDBObject(TableColumns.JOBINSTANCEID, 1);
        DBCursor cursor = db.getCollection(TableColumns.JOB_INSTANCE).find(
                new BasicDBObject(TableColumns.JOBNAME, jobName), keys);

        if (cursor.size() == 0) {
            throw BatchMessages.MESSAGES.noSuchJobException(jobName);
        }

        //add matching job instance ids to the "jobinstanceid in" list
        BasicDBList basicDBList = new BasicDBList();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            basicDBList.add(next.get(TableColumns.JOBINSTANCEID));
        }
        final DBObject inJobInstanceIdsClause = new BasicDBObject("$in", basicDBList);
        final DBObject query = new BasicDBObject(TableColumns.JOBINSTANCEID, inJobInstanceIdsClause);

        //create "batchstatus in" list
        basicDBList = new BasicDBList();
        basicDBList.add(BatchStatus.STARTED.name());
        basicDBList.add(BatchStatus.STARTING.name());
        final DBObject inBatchStatusClause = new BasicDBObject("$in", basicDBList);

        //combine batchstatus in clause into jobinstanceid in clause
        query.put(TableColumns.BATCHSTATUS, inBatchStatusClause);
        keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
        cursor = db.getCollection(TableColumns.JOB_EXECUTION).find(query, keys);

        final List<Long> result = new ArrayList<Long>();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            result.add((Long) next.get(TableColumns.JOBEXECUTIONID));
        }

        return result;
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
        db.getCollection(TableColumns.STEP_EXECUTION).insert(dbObject);
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        final StepExecutionImpl stepExecutionImpl = (StepExecutionImpl) stepExecution;
        try {
            final DBObject update = new BasicDBObject(TableColumns.ENDTIME, stepExecution.getEndTime());
            update.put(TableColumns.BATCHSTATUS, stepExecution.getBatchStatus().name());
            update.put(TableColumns.EXITSTATUS, stepExecution.getExitStatus());
            update.put(TableColumns.EXECUTIONEXCEPTION, TableColumns.formatException(stepExecutionImpl.getException()));
            update.put(TableColumns.PERSISTENTUSERDATA, BatchUtil.objectToBytes(stepExecution.getPersistentUserData()));
            update.put(TableColumns.READCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_COUNT));
            update.put(TableColumns.WRITECOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_COUNT));
            update.put(TableColumns.COMMITCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.COMMIT_COUNT));
            update.put(TableColumns.ROLLBACKCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.ROLLBACK_COUNT));
            update.put(TableColumns.READSKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.READ_SKIP_COUNT));
            update.put(TableColumns.PROCESSSKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.PROCESS_SKIP_COUNT));
            update.put(TableColumns.FILTERCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.FILTER_COUNT));
            update.put(TableColumns.WRITESKIPCOUNT, stepExecutionImpl.getStepMetrics().get(Metric.MetricType.WRITE_SKIP_COUNT));
            update.put(TableColumns.READERCHECKPOINTINFO, BatchUtil.objectToBytes(stepExecutionImpl.getReaderCheckpointInfo()));
            update.put(TableColumns.WRITERCHECKPOINTINFO, BatchUtil.objectToBytes(stepExecutionImpl.getWriterCheckpointInfo()));

            db.getCollection(TableColumns.STEP_EXECUTION).update(
                    new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecution.getStepExecutionId()),
                    new BasicDBObject("$set", update));

        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "updateStepExecution");
        }
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
                final DBObject query = new BasicDBObject(TableColumns.STEPEXECUTIONID, partitionExecution.getStepExecutionId());
                query.put(TableColumns.PARTITIONEXECUTIONID, partitionExecution.getPartitionId());

                final DBObject update = new BasicDBObject(TableColumns.BATCHSTATUS, partitionExecution.getBatchStatus().name());
                update.put(TableColumns.EXITSTATUS, partitionExecution.getExitStatus());
                update.put(TableColumns.EXECUTIONEXCEPTION, TableColumns.formatException(partitionExecution.getException()));
                update.put(TableColumns.PERSISTENTUSERDATA, BatchUtil.objectToBytes(partitionExecution.getPersistentUserData()));
                update.put(TableColumns.READERCHECKPOINTINFO, BatchUtil.objectToBytes(partitionExecution.getReaderCheckpointInfo()));
                update.put(TableColumns.WRITERCHECKPOINTINFO, BatchUtil.objectToBytes(partitionExecution.getWriterCheckpointInfo()));

                db.getCollection(TableColumns.PARTITION_EXECUTION).update(query, new BasicDBObject("$set", update));
            } catch (final Exception e) {
                throw BatchMessages.MESSAGES.failToRunQuery(e, "savePersistentData");
            }
        }
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
        final DBCollection collection = db.getCollection(TableColumns.STEP_EXECUTION);
        DBCursor cursor = jobExecutionId == null ? collection.find() :
                collection.find(new BasicDBObject(TableColumns.JOBEXECUTIONID, jobExecutionId));
        cursor = cursor.sort(new BasicDBObject(TableColumns.STEPEXECUTIONID, 1));
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
        db.getCollection(TableColumns.PARTITION_EXECUTION).insert(dbObject);
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
                                                                 final JobExecutionImpl jobExecutionToRestart,
                                                                 final ClassLoader classLoader) {
        final StepExecutionImpl result = super.findOriginalStepExecutionForRestart(stepName, jobExecutionToRestart, classLoader);
        if (result != null) {
            return result;
        }

        final DBObject keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
        keys.put(TableColumns._id, 0);
        final DBCursor cursor = db.getCollection(TableColumns.JOB_EXECUTION).find(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobExecutionToRestart.getJobInstance().getInstanceId()),
                keys);
        final BasicDBList basicDBList = new BasicDBList();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            basicDBList.add(next.get(TableColumns.JOBEXECUTIONID));
        }
        final DBObject inClause = new BasicDBObject("$in", basicDBList);
        final DBObject query = new BasicDBObject(TableColumns.JOBEXECUTIONID, inClause);
        query.put(TableColumns.STEPNAME, stepName);
        final DBCursor cursor1 = db.getCollection(TableColumns.STEP_EXECUTION).find(query).sort(
                new BasicDBObject(TableColumns.STEPEXECUTIONID, -1));

        return createStepExecutionFromDBObject(cursor1.one(), classLoader);
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
        final DBCursor cursor = db.getCollection(TableColumns.PARTITION_EXECUTION).find(
                new BasicDBObject(TableColumns.STEPEXECUTIONID, stepExecutionId)).sort(
                new BasicDBObject(TableColumns.PARTITIONEXECUTIONID, 1));

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
                            BatchUtil.bytesToSerializableObject((byte[]) next.get(TableColumns.PERSISTENTUSERDATA), classLoader),
                            BatchUtil.bytesToSerializableObject((byte[]) next.get(TableColumns.READERCHECKPOINTINFO), classLoader),
                            BatchUtil.bytesToSerializableObject((byte[]) next.get(TableColumns.WRITERCHECKPOINTINFO), classLoader)
                    ));
                }
            }
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "getPartitionExecutions");
        }
        return result;
    }

    private void createStepExecutionsFromDBCursor(final DBCursor cursor, final List<StepExecution> result, final ClassLoader classLoader) {
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
                    BatchUtil.bytesToSerializableObject((byte[]) dbObject.get(TableColumns.PERSISTENTUSERDATA), classLoader),
                    numberObjectToLong(dbObject.get(TableColumns.READCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.WRITECOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.COMMITCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.ROLLBACKCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.READSKIPCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.PROCESSSKIPCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.FILTERCOUNT)),
                    numberObjectToLong(dbObject.get(TableColumns.WRITESKIPCOUNT)),
                    BatchUtil.bytesToSerializableObject((byte[]) dbObject.get(TableColumns.READERCHECKPOINTINFO), classLoader),
                    BatchUtil.bytesToSerializableObject((byte[]) dbObject.get(TableColumns.WRITERCHECKPOINTINFO), classLoader)
            );
        } catch (final Exception e) {
            throw BatchMessages.MESSAGES.failToRunQuery(e, "createStepExecutionFromDBObject");
        }
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        final DBObject keys = new BasicDBObject(TableColumns.JOBEXECUTIONID, 1);
        keys.put(TableColumns._id, 0);
        final DBCursor cursor = db.getCollection(TableColumns.JOB_EXECUTION).find(
                new BasicDBObject(TableColumns.JOBINSTANCEID, jobInstanceId),
                keys);
        final BasicDBList basicDBList = new BasicDBList();
        while (cursor.hasNext()) {
            final DBObject next = cursor.next();
            basicDBList.add(next.get(TableColumns.JOBEXECUTIONID));
        }
        final DBObject inClause = new BasicDBObject("$in", basicDBList);
        final DBObject query = new BasicDBObject(TableColumns.JOBEXECUTIONID, inClause);
        query.put(TableColumns.STEPNAME, stepName);
        return db.getCollection(TableColumns.STEP_EXECUTION).find(query).count();
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
        final List<AbstractMap.SimpleEntry<DBObject, DBCollection>> dbObjectsAndCollections =
                new ArrayList<AbstractMap.SimpleEntry<DBObject, DBCollection>>();
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
            queryConditionJson = q.substring(leftParenthesisPos + 1, q.length() - 1);
            DBObject parsedDBObject = (DBObject) JSON.parse(queryConditionJson);
            final DBCollection coll;

            if (collectionName.equalsIgnoreCase(TableColumns.JOB_EXECUTION)) {
                coll = db.getCollection(TableColumns.JOB_EXECUTION);
            } else if (collectionName.equalsIgnoreCase(TableColumns.STEP_EXECUTION)) {
                coll = db.getCollection(TableColumns.STEP_EXECUTION);
            } else if (collectionName.equalsIgnoreCase(TableColumns.JOB_INSTANCE)) {
                coll = db.getCollection(TableColumns.JOB_INSTANCE);
            } else if (collectionName.equalsIgnoreCase(TableColumns.PARTITION_EXECUTION)) {
                coll = db.getCollection(TableColumns.PARTITION_EXECUTION);
            } else {
                throw BatchMessages.MESSAGES.failToRunQuery(null, q);
            }
            if (parsedDBObject == null) {
                parsedDBObject = new BasicDBObject();
            }
            dbObjectsAndCollections.add(new AbstractMap.SimpleEntry<DBObject, DBCollection>(parsedDBObject, coll));
            BatchLogger.LOGGER.tracef("About to remove from collection: %s, with query: %s%n", coll, parsedDBObject);
        }

        for (final AbstractMap.SimpleEntry<DBObject, DBCollection> e : dbObjectsAndCollections) {
            e.getValue().remove(e.getKey());
        }

    }

    private Long incrementAndGetSequence(final String whichId) {
        final DBObject query = new BasicDBObject(TableColumns._id, whichId);
        final DBObject update = new BasicDBObject("$inc", new BasicDBObject(TableColumns.SEQ, 1));
        final DBObject result = seqCollection.findAndModify(query, update);
        return (Long) result.get(TableColumns.SEQ);
    }

    private static long numberObjectToLong(final Object obj) {
        return obj == null ? 0 : ((Number) obj).longValue();
    }
}
