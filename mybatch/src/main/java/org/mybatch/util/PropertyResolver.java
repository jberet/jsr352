/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.mybatch.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Decision;
import org.mybatch.job.Flow;
import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Property;
import org.mybatch.job.Step;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class PropertyResolver {
    protected static final String jobParametersToken = "jobParameters";
    protected static final String jobPropertiesToken = "jobProperties";
    protected static final String systemPropertiesToken = "systemProperties";
    protected static final String partitionPlanToken = "partitionPlan";
    protected static final String savedToken = "saved";

    private static final String prefix = "#{";
    private static final String defaultValuePrefix = "?:";

    private static final int shortestTemplateLen = "#{saved['x']}".length();
    private static final int prefixLen = prefix.length();

    private Properties systemProperties = System.getProperties();
    private Properties jobParameters;
    private Properties savedProperties;
    private Properties partitionPlanProperties;
    private Deque<org.mybatch.job.Properties> jobPropertiesStack = new ArrayDeque<org.mybatch.job.Properties>();

    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = jobParameters;
    }

    public void setSavedProperties(Properties savedProperties) {
        this.savedProperties = savedProperties;
    }

    public void setPartitionPlanProperties(Properties partitionPlanProperties) {
        this.partitionPlanProperties = partitionPlanProperties;
    }

    public void pushJobProperties(org.mybatch.job.Properties jobProps) {
        this.jobPropertiesStack.push(jobProps);
    }

    /**
     * Resolves property expressions for job-level elements contained in the job.
     * The job's direct job properties, job parameters and system properties should already have been set up properly,
     * e.g., when this instance was instantiated.
     *
     * @param job the job element whose properties need to be resolved
     */
    public void resolve(Job job) {
        //do not push or pop the top-level properties.  They need to be sticky and may be referenced by lower-level props
        resolve(job.getProperties(), false);
        resolve(job.getListeners());
        resolveDecision(job.getDecisionOrFlowOrSplit());
    }

    /**
     * Resolves property expressions for step-level elements contained in the step.
     * The step's direct job properties, job parameters and system properties should already have been set up properly,
     * e.g., when this instance was instantiated.
     *
     * @param step the step element whose properties need to be resolved
     */
    public void resolve(Step step) {
        //do not push or pop the top-level properties.  They need to be sticky and may be referenced by lower-level props
        resolve(step.getProperties(), false);
        resolve(step.getListeners());
        Batchlet batchlet = step.getBatchlet();

        if (batchlet != null) {
            resolve(batchlet.getProperties(), true);
            return;
        }

        Chunk chunk = step.getChunk();
        if (chunk != null) {
            //chunk has no direct properties.  Its reader, writer, processor and check-point-algorithm each has its own props
            //TODO add after schema update
        }
    }

    public void resolve(Flow flow) {
        //do not push or pop the top-level properties.  They need to be sticky and may be referenced by lower-level props
        //flow has no listeners
        resolve(flow.getProperties(), false);
        resolveDecision(flow.getDecisionOrStepOrSplit());
    }

    public String resolve(String rawVale) {
        if (rawVale.indexOf(prefix) < 0) {
            return rawVale;
        }
        StringBuilder sb = new StringBuilder(rawVale);
        resolve(sb, 0, true);
        return sb.toString();
    }

    public void resolve(StringBuilder sb, int start, boolean defaultAllowed) {
        //distance-to-end doesn't have space for any template, so no variable referenced
        if (sb.length() - start < shortestTemplateLen) {
            return;
        }
        int startExpression = sb.indexOf(prefix, start);
        if (startExpression < 0) {    //doesn't reference any variable
            return;
        }
        int startPropCategory = startExpression + prefixLen;
        int openBracket = sb.indexOf("[", startPropCategory);
        String propCategory = sb.substring(startPropCategory, openBracket);
        int startVariableName = openBracket + 2;  //jump to the next char after ', the start of variable name
        int endBracket = sb.indexOf("]", startVariableName + 1);
        String variableName = sb.substring(startVariableName, endBracket - 1);  // ['abc']
        String val = getPropertyValue(variableName, propCategory, sb);

        int endExpression = endBracket + 1;
        int endCurrentPass = endExpression;
        if (!defaultAllowed) {  //a default expression should not have default again
            if (val != null) {
                endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endExpression, val);
            } else {  //not resolved, keep unchanged
                //do nothing for now
            }
        } else {
            int startDefaultMarker = endExpression + 1;
            int endDefaultMarker = startDefaultMarker + 1;  //?:
            String next2Chars = null;
            if (endDefaultMarker >= sb.length()) {
                //no default value expression
            } else {
                next2Chars = sb.substring(startDefaultMarker, endDefaultMarker + 1);
            }
            boolean hasDefault = defaultValuePrefix.equals(next2Chars);

            int endDefaultExpressionMarker = sb.indexOf(";", endDefaultMarker + 1);
            if (endDefaultExpressionMarker < 0) {
                endDefaultExpressionMarker = sb.length();
            }

            if (val != null) {
                if (!hasDefault) {  //resolved, no default: replace the expression with value
                    endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endExpression, val);
                } else {  //resolved, has default: replace the expression and the default expression with value
                    endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endDefaultExpressionMarker, val);
                }
            } else {
                if (!hasDefault) {  //not resolved, no default: leave unchanged
                    //do nothing for now
                } else {  //not resolved, has default: resolve and apply the default
                    StringBuilder sb4DefaultExpression = new StringBuilder(sb.substring(endDefaultMarker + 1, endDefaultExpressionMarker));
                    resolve(sb4DefaultExpression, 0, false);
                    endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endDefaultExpressionMarker, sb4DefaultExpression.toString());
                }
            }
        }

        resolve(sb, endCurrentPass + 1, true);
    }

    private void resolve(org.mybatch.job.Properties props, boolean pushAndPopProps) {
        if (props == null) {
            return;
        }
        if (pushAndPopProps) {
            jobPropertiesStack.push(props);
        }
        try {
            for (Property p : props.getProperty()) {
                String rawVal = p.getValue();
                String newVal = resolve(rawVal);
                if (rawVal != newVal && !rawVal.equals(newVal)) {
                    p.setValue(newVal);
                }
            }
        } finally {
            if (pushAndPopProps) {
                jobPropertiesStack.pop();
            }
        }
    }

    private void resolve(Listeners listeners) {
        if (listeners == null) {
            return;
        }
        for (Listener l : listeners.getListener()) {
            resolve(l.getProperties(), true);
        }
    }

    private void resolveDecision(List<?> jobElements) {
        for (Object e : jobElements) {
            if (e instanceof Decision) {
                resolve(((Decision) e).getProperties(), true);
            }
        }
    }

    private int replaceAndGetEndPosition(StringBuilder sb, int startExpression, int endExpression, String replacingVal) {
        sb.replace(startExpression, endExpression + 1, replacingVal);
        return startExpression - 1 + replacingVal.length();
    }

    private String getPropertyValue(String variableName, String propCategory, StringBuilder sb) {
        String val = null;
        if (propCategory.equals(jobParametersToken) && jobParameters != null) {
            val = jobParameters.getProperty(variableName);
        } else if (propCategory.equals(jobPropertiesToken)) {
            for (org.mybatch.job.Properties p : jobPropertiesStack) {
                val = BatchUtil.getBatchProperty(p, variableName);
                if (val != null) {
                    break;
                }
            }
        } else if (propCategory.equals(systemPropertiesToken)) {
            val = systemProperties.getProperty(variableName);
        } else if (propCategory.equals(savedToken)) {
            val = savedProperties.getProperty(variableName);
        } else if (propCategory.equals(partitionPlanToken)) {
            val = partitionPlanProperties.getProperty(variableName);
        } else {
            LOGGER.unrecognizedPropertyReference(propCategory, variableName, sb.toString());
        }
        return val;
    }
}
