/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 
package org.mybatch.state;

import java.util.Properties;
import javax.batch.state.JobInstance;

import org.mybatch.creation.ArtifactFactory;
import org.mybatch.creation.SimpleArtifactFactory;
import org.mybatch.job.Job;
import org.mybatch.metadata.ApplicationMetaData;

public class JobInstanceImpl implements JobInstance {
    private long id;
    private Job job;
    private ApplicationMetaData appData;
    private ArtifactFactory artifactFactory;

    public JobInstanceImpl(Job job, ApplicationMetaData appData, ArtifactFactory artifactFactory) {
        this.job = job;
        this.appData = appData;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public String getJobName() {
        return job.getId();
    }

    @Override
    public long getInstanceId() {
        return 0;
    }

    @Override
    public String getJobXML() {
        return null;
    }

    @Override
    public Properties getOriginalJobParams() {
        return null;
    }

    public ApplicationMetaData getApplicationMetaData() {
        return this.appData;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

}
