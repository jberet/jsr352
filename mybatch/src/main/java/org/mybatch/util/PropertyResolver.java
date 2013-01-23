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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    private Properties systemProperties;
    private Properties jobParameters;
    private Properties savedProperties;
    private Properties partitionPlanProperties;
    private List<Properties> jobPropertiesList = new ArrayList<Properties>();

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

    public void addJobProperties(Properties jobProps) {
        this.jobPropertiesList.add(jobProps);
    }

    public String resolve(String rawVale) {
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

    private int replaceAndGetEndPosition(StringBuilder sb, int startExpression, int endExpression, String replacingVal) {
        sb.replace(startExpression, endExpression + 1, replacingVal);
        return startExpression - 1 + replacingVal.length();
    }

    private String getPropertyValue(String variableName, String propCategory, StringBuilder sb) {
        String val = null;
        if (propCategory.equals(jobParametersToken)) {
            val = jobParameters.getProperty(variableName);
        } else if (propCategory.equals(jobPropertiesToken)) {
            for (Properties p : jobPropertiesList) {
                val = p.getProperty(variableName);
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
