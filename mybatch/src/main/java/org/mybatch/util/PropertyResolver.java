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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.mybatch.job.Analyzer;
import org.mybatch.job.Batchlet;
import org.mybatch.job.Chunk;
import org.mybatch.job.Collector;
import org.mybatch.job.Decision;
import org.mybatch.job.End;
import org.mybatch.job.ExceptionClassFilter;
import org.mybatch.job.Fail;
import org.mybatch.job.Flow;
import org.mybatch.job.ItemProcessor;
import org.mybatch.job.ItemReader;
import org.mybatch.job.ItemWriter;
import org.mybatch.job.Job;
import org.mybatch.job.Listener;
import org.mybatch.job.Listeners;
import org.mybatch.job.Next;
import org.mybatch.job.Partition;
import org.mybatch.job.PartitionMapper;
import org.mybatch.job.PartitionPlan;
import org.mybatch.job.PartitionReducer;
import org.mybatch.job.Property;
import org.mybatch.job.Split;
import org.mybatch.job.Step;
import org.mybatch.job.Stop;

import static org.mybatch.util.BatchLogger.LOGGER;

public final class PropertyResolver {
    protected static final String jobParametersToken = "jobParameters";
    protected static final String jobPropertiesToken = "jobProperties";
    protected static final String systemPropertiesToken = "systemProperties";
    protected static final String partitionPlanToken = "partitionPlan";

    private static final String prefix = "#{";
    private static final String defaultValuePrefix = "?:";

    private static final int shortestTemplateLen = "#{jobProperties['x']}".length();
    private static final int prefixLen = prefix.length();

    private Properties systemProperties = System.getProperties();
    private Properties jobParameters;
    private Properties partitionPlanProperties;
    private Deque<org.mybatch.job.Properties> jobPropertiesStack = new ArrayDeque<org.mybatch.job.Properties>();

    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = jobParameters;
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
        String oldVal, newVal;
        oldVal = job.getRestartable();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                job.setRestartable(newVal);
            }
        }

        org.mybatch.job.Properties props = job.getProperties();
        if (props != null) {
            jobPropertiesStack.push(props);
        }
        //do not push or pop the top-level properties.  They need to be sticky and may be referenced by lower-level props
        resolve(job.getProperties(), false);
        resolve(job.getListeners());
        resolveJobElements(job.getDecisionOrFlowOrSplit());

        if (props != null && jobPropertiesStack.peek() == props) {
            jobPropertiesStack.pop();
        }
    }

    private void resolveJobElements(List<?> jobElements) {
        if (jobElements == null) {
            return;
        }
        for (Object e : jobElements) {
            if (e instanceof Step) {
                resolve((Step) e);
            } else if (e instanceof Flow) {
                resolve((Flow) e);
            } else if (e instanceof Decision) {
                resolve((Decision) e);
            } else if (e instanceof Split) {
                resolve((Split) e);
            }
        }
    }

    /**
     * Resolves property expressions for step-level elements contained in the step.
     * The step's direct job properties, job parameters and system properties should already have been set up properly,
     * e.g., when this instance was instantiated.
     *
     * @param step the step element whose properties need to be resolved
     */
    public void resolve(Step step) {
        String oldVal, newVal;
        oldVal = step.getNext();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                step.setNext(newVal);
            }
        }
        oldVal = step.getAllowStartIfComplete();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                step.setAllowStartIfComplete(newVal);
            }
        }
        oldVal = step.getStartLimit();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                step.setStartLimit(newVal);
            }
        }
        org.mybatch.job.Properties props = step.getProperties();
        if (props != null) {
            jobPropertiesStack.push(props);
        }
        resolve(step.getProperties(), false);
        resolve(step.getListeners());
        Batchlet batchlet = step.getBatchlet();

        if (batchlet != null) {
            oldVal = batchlet.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                batchlet.setRef(newVal);
            }
            resolve(batchlet.getProperties(), true);
        }
        resolve(step.getChunk());
        resolve(step.getPartition());
        resolveTransitionElements(step.getTransitionElements());

        if(props != null && jobPropertiesStack.peek() == props) {
            jobPropertiesStack.pop();
        }
    }

    private void resolve(Partition partition) {
        if (partition == null) {
            return;
        }
        String oldVal, newVal;
        Analyzer analyzer = partition.getAnalyzer();
        if (analyzer != null) {
            oldVal = analyzer.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                analyzer.setRef(newVal);
            }
            resolve(analyzer.getProperties(), true);
        }
        Collector collector = partition.getCollector();
        if (collector != null) {
            oldVal = collector.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                collector.setRef(newVal);
            }
            resolve(collector.getProperties(), true);

        }
        PartitionReducer reducer = partition.getReducer();
        if (reducer != null) {
            oldVal = reducer.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                reducer.setRef(newVal);
            }
            resolve(reducer.getProperties(), true);
        }
        PartitionPlan plan = partition.getPlan();
        if (plan != null) {
            oldVal = plan.getPartitions();
            if (oldVal != null) {
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    plan.setPartitions(newVal);
                }
            }
            oldVal = plan.getThreads();
            if (oldVal != null) {
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    plan.setThreads(newVal);
                }
            }
            for (org.mybatch.job.Properties p : plan.getProperties()) {
                resolve(p, true);
            }
        }
        PartitionMapper mapper = partition.getMapper();
        if (mapper != null) {
            oldVal = mapper.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                mapper.setRef(newVal);
            }
            resolve(mapper.getProperties(), true);
        }
    }

    private void resolve(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        resolve(chunk.getSkippableExceptionClasses());
        resolve(chunk.getRetryableExceptionClasses());
        resolve(chunk.getNoRollbackExceptionClasses());

        String oldVal, newVal;
        oldVal = chunk.getCheckpointPolicy();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                chunk.setCheckpointPolicy(newVal);
            }
        }
        oldVal = chunk.getItemCount();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                chunk.setItemCount(newVal);
            }
        }
        oldVal = chunk.getTimeLimit();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                chunk.setTimeLimit(newVal);
            }
        }
        oldVal = chunk.getSkipLimit();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                chunk.setSkipLimit(newVal);
            }
        }
        oldVal = chunk.getRetryLimit();
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                chunk.setRetryLimit(newVal);
            }
        }

        ItemReader reader = chunk.getReader();
        oldVal = reader.getRef();
        newVal = resolve(oldVal);
        if (!oldVal.equals(newVal)) {
            reader.setRef(newVal);
        }
        resolve(reader.getProperties(), true);

        ItemWriter writer = chunk.getWriter();
        oldVal = writer.getRef();
        newVal = resolve(oldVal);
        if (!oldVal.equals(newVal)) {
            writer.setRef(newVal);
        }
        resolve(writer.getProperties(), true);

        ItemProcessor processor = chunk.getProcessor();
        if (processor != null) {
            oldVal = processor.getRef();
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                processor.setRef(newVal);
            }
            resolve(processor.getProperties(), true);
        }
    }

    private void resolve(ExceptionClassFilter filter) {
        if (filter == null) {
            return;
        }
        String oldVal, newVal;
        List<ExceptionClassFilter.Include> in = filter.getInclude();
        if (in != null) {
            for (ExceptionClassFilter.Include i : in) {
                oldVal = i.getClazz();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    i.setClazz(newVal);
                }
            }
        }
        List<ExceptionClassFilter.Exclude> ex = filter.getExclude();
        if (ex != null) {
            for (ExceptionClassFilter.Exclude e : ex) {
                oldVal = e.getClazz();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    e.setClazz(newVal);
                }
            }
        }
    }

    private void resolve(Split split) {
        String oldVal = split.getNext();
        if (oldVal != null) {
            String newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                split.setNext(newVal);
            }
        }
        for (Flow e : split.getFlow()) {
            resolve(e);
        }
    }

    public void resolve(Flow flow) {
        String oldVal = flow.getNext();
        if (oldVal != null) {
            String newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                flow.setNext(newVal);
            }
        }
        resolveTransitionElements(flow.getTransitionElements());
        resolveJobElements(flow.getDecisionOrFlowOrSplit());
    }

    private void resolve(org.mybatch.job.Properties props, boolean pushAndPopProps) {
        if (props == null) {
            return;
        }
        if (pushAndPopProps) {
            jobPropertiesStack.push(props);
        }

        String oldVal = props.getPartition();
        String newVal;
        if (oldVal != null) {
            newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                props.setPartition(newVal);
            }
        }

        try {
            for (Property p : props.getProperty()) {
                oldVal = p.getName();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    p.setName(newVal);
                }

                oldVal = p.getValue();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
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
            String oldVal = l.getRef();
            String newVal = resolve(oldVal);
            if (!oldVal.equals(newVal)) {
                l.setRef(newVal);
            }
            resolve(l.getProperties(), true);
        }
    }

    private void resolve(Decision decision) {
        String oldVal = decision.getRef();
        String newVal = resolve(oldVal);
        if (!oldVal.equals(newVal)) {
            decision.setRef(newVal);
        }
        resolve(decision.getProperties(), true);
        resolveTransitionElements(decision.getTransitionElements());
    }

    private void resolveTransitionElements(List<?> transitions) {
        String oldVal, newVal;
        for (Object e : transitions) {
            if (e instanceof Next) {
                Next next = (Next) e;
                oldVal = next.getTo();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    next.setTo(newVal);
                }
                oldVal = next.getOn();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    next.setOn(newVal);
                }
            } else if (e instanceof Fail) {
                Fail fail = (Fail) e;
                oldVal = fail.getOn();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    fail.setOn(newVal);
                }
                oldVal = fail.getExitStatus();
                if (oldVal != null) {
                    newVal = resolve(oldVal);
                    if (!oldVal.equals(newVal)) {
                        fail.setExitStatus(newVal);
                    }
                }
            } else if (e instanceof End) {
                End end = (End) e;
                oldVal = end.getOn();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    end.setOn(newVal);
                }
                oldVal = end.getExitStatus();
                if (oldVal != null) {
                    newVal = resolve(oldVal);
                    if (!oldVal.equals(newVal)) {
                        end.setExitStatus(newVal);
                    }
                }
            } else if (e instanceof Stop) {
                Stop stop = (Stop) e;
                oldVal = stop.getOn();
                newVal = resolve(oldVal);
                if (!oldVal.equals(newVal)) {
                    stop.setOn(newVal);
                }
                oldVal = stop.getExitStatus();
                if (oldVal != null) {
                    newVal = resolve(oldVal);
                    if (!oldVal.equals(newVal)) {
                        stop.setExitStatus(newVal);
                    }
                }
                oldVal = stop.getRestart();
                if (oldVal != null) {
                    newVal = resolve(oldVal);
                    if (!oldVal.equals(newVal)) {
                        stop.setRestart(newVal);
                    }
                }
            }
        }
    }




    public String resolve(String rawVale) {
        if (rawVale.length() < shortestTemplateLen || !rawVale.contains(prefix)) {
            return rawVale;
        }
        StringBuilder sb = new StringBuilder(rawVale);
        resolve(sb, 0, true, null);
        return sb.toString();
    }

    private void resolve(StringBuilder sb, int start, boolean defaultAllowed, LinkedList<String> referringExpressions) {
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
        int endExpression = endBracket + 1;

        if (endExpression >= sb.length()) {
            //this can happen when missing an ending } (e.g.,   #{jobProperties['step-prop']   )
            LOGGER.possibleSyntaxErrorInProperty(sb.toString());
            endExpression = sb.length() - 1;
        }
        String expression = sb.substring(startExpression, endExpression + 1);

        if (referringExpressions != null && referringExpressions.contains(expression)) {
            throw LOGGER.cycleInPropertyReference(referringExpressions);  //exception thrown
        }

        String variableName = sb.substring(startVariableName, endBracket - 1);  // ['abc']
        String val = getPropertyValue(variableName, propCategory, sb);
        if (val != null) {
            val = reresolve(expression, val, defaultAllowed, referringExpressions);
        }

        int endCurrentPass = endExpression;
        if (!defaultAllowed) {  //a default expression should not have default again
            if (val != null) {
                endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endExpression, val);
            } else {  //not resolved, keep unchanged
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
                } else {  //resolved, has default: replace    the expression and the default expression    with value
                    endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endDefaultExpressionMarker, val);
                }
            } else {
                if (!hasDefault) {  //not resolved, no default: leave unchanged
                    //do nothing for now
                } else {  //not resolved, has default: resolve and apply the default
                    StringBuilder sb4DefaultExpression = new StringBuilder(sb.substring(endDefaultMarker + 1, endDefaultExpressionMarker));
                    resolve(sb4DefaultExpression, 0, false, null);
                    endCurrentPass = replaceAndGetEndPosition(sb, startExpression, endDefaultExpressionMarker, sb4DefaultExpression.toString());
                }
            }
        }

        resolve(sb, endCurrentPass + 1, true, null);
    }

    private String reresolve(String expression, String currentlyResolvedToVal, boolean defaultAllowed, LinkedList<String> referringExpressions) {
        if (currentlyResolvedToVal.length() < shortestTemplateLen || !currentlyResolvedToVal.contains(prefix)) {
            return currentlyResolvedToVal;
        }
        if (referringExpressions == null) {
            referringExpressions = new LinkedList<String>();
        }
        referringExpressions.add(expression);
        StringBuilder sb = new StringBuilder(currentlyResolvedToVal);
        resolve(sb, 0, defaultAllowed, referringExpressions);
        return sb.toString();
    }

    private int replaceAndGetEndPosition(StringBuilder sb, int startExpression, int endExpression, String replacingVal) {
        sb.replace(startExpression, endExpression + 1, replacingVal);
        return startExpression - 1 + replacingVal.length();
    }

    private String getPropertyValue(String variableName, String propCategory, StringBuilder sb) {
        String val = null;
        if (propCategory.equals(jobParametersToken)) {
            if (jobParameters != null) {
                val = jobParameters.getProperty(variableName);
            }
        } else if (propCategory.equals(jobPropertiesToken)) {
            for (org.mybatch.job.Properties p : jobPropertiesStack) {
                val = BatchUtil.getBatchProperty(p, variableName);
                if (val != null) {
                    break;
                }
            }
        } else if (propCategory.equals(systemPropertiesToken)) {
            val = systemProperties.getProperty(variableName);
        } else if (propCategory.equals(partitionPlanToken)) {
            if (partitionPlanProperties != null) {
                val = partitionPlanProperties.getProperty(variableName);
            }
        } else {
            LOGGER.unrecognizedPropertyReference(propCategory, variableName, sb.toString());
        }
        return val;
    }
}
