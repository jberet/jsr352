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

package org.jberet.util;

import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jberet.job.Property;

import static org.jberet.util.PropertyResolver.jobParametersToken;
import static org.jberet.util.PropertyResolver.jobPropertiesToken;
import static org.jberet.util.PropertyResolver.partitionPlanToken;
import static org.jberet.util.PropertyResolver.systemPropertiesToken;

public class PropertyResolverTest {
    private static final String jobParam1 = "infile.path";
    private static final String jobParam1Val = "/home";
    private static final String jobParam2 = "infile.name";
    private static final String jobParam2Val = "in.txt";

    private static final String partitionPlan1 = "partitionPlan1";
    private static final String partitionPlan1Val = "partitionPlan1 val";
    private static final String partitionPlan2 = "partitionPlan2";
    private static final String partitionPlan2Val = "partitionPlan2 val";

    private static final String jobProp1 = "jobProp1";
    private static final String jobProp1Val = "jobProp1 val";
    private static final String jobProp2 = "jobProp2";
    private static final String jobProp2Val = "jobProp2 val";
    private static final String jobProp3 = "jobProp3";
    private static final String jobProp3Val = "jobProp3 val";

    private static final String sysProp1 = "sysProp1";
    private static final String sysProp1Val = "sysProp1 val";
    private static final String sysProp2 = "sysProp2";
    private static final String sysProp2Val = "sysProp2 val";

    private PropertyResolver resolver = new PropertyResolver();
    private Properties jobParams = new Properties();
    private Properties partitionPlan = new Properties();
    private Properties jobProps1 = new Properties();
    private Properties jobProps2 = new Properties();
    private Properties jobProps3 = new Properties();
    private Properties sysProps = System.getProperties();

    String javaVersion = sysProps.getProperty("java.version");
    String osName = sysProps.getProperty("os.name");

    @Before
    public void setUp() throws Exception {
        jobParams.setProperty(jobParam1, jobParam1Val);
        jobParams.setProperty(jobParam2, jobParam2Val);
        partitionPlan.setProperty(partitionPlan1, partitionPlan1Val);
        partitionPlan.setProperty(partitionPlan2, partitionPlan2Val);
        jobProps1.setProperty(jobProp1, jobProp1Val);
        jobProps2.setProperty(jobProp2, jobProp2Val);
        jobProps3.setProperty(jobProp3, jobProp3Val);
        sysProps.setProperty(sysProp1, sysProp1Val);
        sysProps.setProperty(sysProp2, sysProp2Val);

        resolver.setSystemProperties(sysProps);
        resolver.setJobParameters(jobParams);
        resolver.setPartitionPlanProperties(partitionPlan);
        resolver.pushJobProperties(fromJavaUtilProperties(jobProps1));
        resolver.pushJobProperties(fromJavaUtilProperties(jobProps2));
        resolver.pushJobProperties(fromJavaUtilProperties(jobProps3));
    }

    private org.jberet.job.Properties fromJavaUtilProperties(Properties props) {
        org.jberet.job.Properties result = new org.jberet.job.Properties();
        List<Property> propertyList = result.getProperty();
        for (String s : props.stringPropertyNames()) {
            Property toAdd = new Property();
            toAdd.setName(s);
            toAdd.setValue(props.getProperty(s));
            propertyList.add(toAdd);
        }
        return result;
    }

    private void run(String[] raws, String[] expected) {
        for (int i = 0, j = raws.length; i < j; i++) {
            String actual = resolver.resolve(raws[i]);
            Assert.assertEquals(expected[i], actual);
//            System.out.println("Expected: " + expected[i] + ", actual: " + actual);
        }
    }

    @Test public void cycle() {
        Properties props = new Properties();
        props.setProperty("one", "#{jobProperties['one']}");  //self reference

        props.setProperty("two", "#{jobProperties['three']}");  //mutual reference
        props.setProperty("three", "#{jobProperties['two']}");

        props.setProperty("four", "#{jobProperties['five']}");  //cycle
        props.setProperty("five", "#{jobProperties['six']}");
        props.setProperty("six", "#{jobProperties['four']}");

        props.setProperty("seven", "#{jobProperties['eight']}");  //valid one
        props.setProperty("eight", "#{jobProperties['nine']}");
        props.setProperty("nine", "#{jobProperties['ten']}");
        props.setProperty("ten", "TEN");  //cycle

        props.setProperty("java.version", "#{systemProperties['java.version']}");  //valid one, not cycle

        resolver.pushJobProperties(fromJavaUtilProperties(props));
        Assert.assertNull(resolver.resolve("#{jobProperties['one']}"));
        Assert.assertNull(resolver.resolve("#{jobProperties['two']}"));
        Assert.assertNull(resolver.resolve("#{jobProperties['four']}"));
        Assert.assertEquals("TEN", resolver.resolve("#{jobProperties['seven']}"));

        org.junit.Assert.assertEquals(javaVersion, resolver.resolve("#{jobProperties['java.version']}"));
    }

    @Test public void literal() {
        String[] raw = {"", "1", " 2 ", "#3", "4name@company.com", "5 people", "$6",
        "~!@#$%^&*()_+\":?></.raw,?:\\][[]qwert" };
        run(raw, raw);
    }

    @Test public void singleVariableAndUnusedDefault() {
        String[] raw = {
                String.format("#{%s['%s']}", jobParametersToken, jobParam1),
                String.format("#{%s['%s']}", partitionPlanToken, partitionPlan1),

                String.format("#{%s['%s']}", jobPropertiesToken, jobProp1),
                String.format("#{%s['%s']}", jobPropertiesToken, jobProp2),
                String.format("#{%s['%s']}", jobPropertiesToken, jobProp3),

                String.format("#{%s['%s']}", systemPropertiesToken, sysProp1),
                String.format("#{%s['%s']}", systemPropertiesToken, sysProp2)
        };

        //defaults (literal value) are not used, since the variable can be resolved
        String[] raw2 = {
                String.format("#{%s['%s']}?:in.txt;", jobParametersToken, jobParam1),
                String.format("#{%s['%s']}?:in.txt;", partitionPlanToken, partitionPlan1),

                String.format("#{%s['%s']}?:in.txt;", jobPropertiesToken, jobProp1),
                String.format("#{%s['%s']}?:in.txt", jobPropertiesToken, jobProp2),
                String.format("#{%s['%s']}?:in.txt", jobPropertiesToken, jobProp3),

                String.format("#{%s['%s']}?:in.txt", systemPropertiesToken, sysProp1),
                String.format("#{%s['%s']}?:in.txt", systemPropertiesToken, sysProp2)
        };
        
        //defaults (expression value) not used
        String[] raw3 = {
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", jobParametersToken, jobParam1),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", partitionPlanToken, partitionPlan1),

                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", jobPropertiesToken, jobProp1),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", jobPropertiesToken, jobProp2),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", jobPropertiesToken, jobProp3),

                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", systemPropertiesToken, sysProp1),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", systemPropertiesToken, sysProp2)
        };

        String[] expected = {
            jobParam1Val,
                partitionPlan1Val,
                jobProp1Val,
                jobProp2Val,
                jobProp3Val,
                sysProp1Val,
                sysProp2Val
        };
        run(raw,  expected);
        run(raw2, expected);
        run(raw3, expected);
    }

    @Test public void literalVariableMix() {
        String preLiteral = "#";
        String postLiteral = ";";
        String[] raw = {
                String.format("%s#{%s['%s']}%s", preLiteral, jobParametersToken, jobParam1, postLiteral),
                String.format("%s#{%s['%s']}%s", preLiteral, partitionPlanToken, partitionPlan1, postLiteral),

                String.format("%s#{%s['%s']}%s", preLiteral, jobPropertiesToken, jobProp1, postLiteral),
                String.format("%s#{%s['%s']}%s", preLiteral, jobPropertiesToken, jobProp2, postLiteral),
                String.format("%s#{%s['%s']}%s", preLiteral, jobPropertiesToken, jobProp3, postLiteral),

                String.format("%s#{%s['%s']}%s", preLiteral, systemPropertiesToken, sysProp1, postLiteral),
                String.format("%s#{%s['%s']}%s", preLiteral, systemPropertiesToken, sysProp2, postLiteral)
        };
        String[] expected = {
                preLiteral + jobParam1Val + postLiteral,
                preLiteral + partitionPlan1Val + postLiteral,
                preLiteral + jobProp1Val + postLiteral,
                preLiteral + jobProp2Val + postLiteral,
                preLiteral + jobProp3Val + postLiteral,
                preLiteral + sysProp1Val + postLiteral,
                preLiteral + sysProp2Val + postLiteral
        };
        run(raw, expected);
    }

    @Test public void defaultLiteral() {
        //defaults (literal value) are used, since the variable cannot be resolved
        //the last param below reprents non-existent, unresolvable variable
        //there can be more text after the default value (see ?:3;x)
        String[] raw = {
                String.format("#{%s['%s']}?:1.txt", jobParametersToken, "x"),
                String.format("#{%s['%s']}?:3;x", partitionPlanToken, "."),

                String.format("#{%s['%s']}?:4.txt;", jobPropertiesToken, ","),
                String.format("#{%s['%s']}?: 5 ", jobPropertiesToken, "?"),
                String.format("#{%s['%s']}?:#6", jobPropertiesToken, "#"),

                String.format("#{%s['%s']}?:$7;", systemPropertiesToken, "@"),
                String.format("#{%s['%s']}?::", systemPropertiesToken, "*")
        };

        String[] expected = {
                "1.txt", "3x", "4.txt", " 5 ", "#6", "$7", ":"
        };

        run(raw, expected);
    }

    @Test public void defaultExpressioin() {
        //defaults (expression value) are used, since the variable cannot be resolved
        //the last param below reprents non-existent, unresolvable variable
        //there can be more text after the default value
        String[] raw = {
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};!", jobParametersToken, ""),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", partitionPlanToken, ""),

                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", jobPropertiesToken, ""),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};", jobPropertiesToken, ""),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", jobPropertiesToken, ""),

                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", systemPropertiesToken, ""),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']}", systemPropertiesToken, "")
        };
        String s = sysProps.getProperty("os.name");
        String[] expected = {
                s + "!", s, s, s, s, s, s
        };

        run(raw, expected);
    }

    @Test public void noDefaultHasDefault() {
        String[] raw = {
                String.format("#{%s['%s']}#{%s['%s']}?:#{systemProperties['os.name']}", systemPropertiesToken, "java.version", jobParametersToken, ""),
                String.format("#{%s['%s']}?:#{systemProperties['os.name']};#{%s['%s']}", jobParametersToken, "", systemPropertiesToken, "java.version"),
        };

        String[] expected = {
                javaVersion + osName,
                osName + javaVersion
        };
        run(raw, expected);
    }

    @Test public void mixedCategory() {
        String[] raw = {
                String.format("#{%s['%s']}#{%s['%s']}#{%s['%s']}",
                        jobParametersToken, jobParam1, systemPropertiesToken, "file.separator", jobParametersToken, jobParam2)};
        String[] expected = {
                jobParam1Val + sysProps.getProperty("file.separator") + jobParam2Val
        };
        run(raw, expected);
    }
}
