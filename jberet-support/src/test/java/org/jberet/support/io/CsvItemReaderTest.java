/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CsvItemReaderTest {
    static final String jobName = "org.jberet.support.io.CsvReaderTest";
    static final String personResource = "person.csv";
    static final String personPipeResource = "person-pipe.txt";
    static final String personTabResource = "person-tab.txt";
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final int waitTimeoutMinutes = 0;

    static final String nameMapping =
    "number, gender, title, givenName, middleInitial, surname, streetAddress, city, state, zipCode, " +
    "country, countryFull, emailAddress, username, password, telephoneNumber, mothersMaiden, birthday, CCType, CCNumber, " +
    "CVV2, CCExpires, nationalID, UPS, color, occupation, company, vehicle, domain, bloodType, " +
    "pounds, kilograms, feetInches, centimeters, GUID, latitude, longitude";

    static final String cellProcessors =
                    "NotNull, LMinMax(1, 99999); " +    //Number  convert to long and enforce range
                    "Token('male', 'M'), Token('female', 'F');" +   //Gender
                    "null; " +  //Title
                    "StrNotNullOrEmpty; " +  //GivenName
                    "ParseChar; " +     //MiddleInitial
                    "null; " +      //Surname
                    "null; " +      //StreetAddress
                    "null; " +      //City
                    "null; " +      //State
                    "Strlen(5);" +       //ZipCode
                    "Equals('US'); " +      //Country
                    "IsElementOf('United States', 'United States of America'); " +      //CountryFull
                    "RequireSubStr('@'), ForbidSubStr('@gmail.com', '@yahoo.com'); " +      //EmailAddress rules
                    "Unique; " +      //UserName  must be unique
                    "StrMinMax(8, 20), StrRegEx('^[a-zA-Z0-9]*$');" +      //Password, enforce length and regex rule
                    "null; " +      //TelephoneNumber
                    "null; " +      //MothersMaiden
                    "Optional, ParseDate('MM/dd/yyyy'); " +     //Birthday
                    "IsIncludedIn('Visa', 'MasterCard', 'Discover', 'AmericanExpress'; " +      //CCType
                    "null;" +       //CCNumber
                    "StrMinMax(3, 4); " +      //CVV2
                    "null; " +      //CCExpires
                    "null; " +      //NationalID
                    "null; " +      //UPS
                    "null; " +      //Color
                    "null; " +      //Occupation
                    "null; " +      //Company
                    "null; " +      //Vehicle
                    "RequireSubStr('.'); " +      //Domain
                    "NotNull, StrReplace('\\+', '');" +       //BloodType, + is java regex meta char, so need to escape it
                    "ParseBigDecimal('en_us'); " +      //Pounds
                    "ParseBigDecimal; " +       //Kilograms
                    "null; " +      //FeetInches
                    "ParseInt; " +      //Centimeters
                    "Truncate(10, '...'); " +       //GUID
                    "DMinMax(-99999, 99999); " +       //Latitude  convert to double and enforce range
                    "ParseDouble";          //Longitude

    @Test
    public void testBeanType() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testBeanType0(personResource, null, null, "|");
    }

    @Test
    public void testBeanTypeTab() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testBeanType0(personTabResource, CsvProperties.TAB_PREFERENCE, null, "|");
    }

    @Test
    public void testBeanTypePipe() throws Exception {
        //override the default quote char ", which is used in feetInches cell. | is already used as the delimiterChar
        //so cannot be used as quoteChar again.
        testBeanType0(personPipeResource, null, "|", "^");
    }

    private void testBeanType0(final String resource, final String preference, final String delimiterChar, final String quoteChar) throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, "org.jberet.support.io.Person");
        params.setProperty(CsvProperties.RESOURCE_KEY, resource);

        if (preference != null) {
            params.setProperty(CsvProperties.PREFERENCE_KEY, preference);
        }
        if (delimiterChar != null) {
            params.setProperty(CsvProperties.DELIMITER_CHAR_KEY, delimiterChar);
        }
        if (quoteChar != null) {
            params.setProperty(CsvProperties.QUOTE_CHAR_KEY, quoteChar);
        }
        params.setProperty("cellProcessors", cellProcessors);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    @Test
    public void testListAndMapType() throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, "java.util.List");
        params.setProperty(CsvProperties.RESOURCE_KEY, personResource);
        params.setProperty(CsvProperties.QUOTE_CHAR_KEY, "|");

        long jobExecutionId = jobOperator.start(jobName, params);
        JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        params.setProperty(CsvProperties.BEAN_TYPE_KEY, "java.util.Map");
        jobExecutionId = jobOperator.start(jobName, params);
        jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

    @Test @Ignore("restore it if needed")
    public void testStringsToInts() throws Exception {
        final String[] ss = {"1", "2", "3", "4"};
        int[] ints = CsvItemReader.convertToIntParams(ss, 0, ss.length);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(4, ints.length);
        Assert.assertEquals(1, ints[0]);
        Assert.assertEquals(2, ints[1]);
        Assert.assertEquals(3, ints[2]);
        Assert.assertEquals(4, ints[3]);

        ints = CsvItemReader.convertToIntParams(ss, 1, ss.length - 1);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(3, ints.length);
        Assert.assertEquals(2, ints[0]);
        Assert.assertEquals(3, ints[1]);
        Assert.assertEquals(4, ints[2]);

        ints = CsvItemReader.convertToIntParams(ss, 2, ss.length - 2);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(2, ints.length);
        Assert.assertEquals(3, ints[0]);
        Assert.assertEquals(4, ints[1]);

        ints = CsvItemReader.convertToIntParams(ss, 3, ss.length - 3);
        System.out.printf("ints: %s%n", Arrays.toString(ints));
        Assert.assertEquals(1, ints.length);
        Assert.assertEquals(4, ints[0]);
    }

    static Properties createParams(final String key, final String val) {
        final Properties params = new Properties();
        if (key != null) {
            params.setProperty(key, val);
        }
        return params;
    }

}
