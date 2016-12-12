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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CsvItemReaderWriterTest {
    static final String jobName = "org.jberet.support.io.CsvReaderTest";
    static final String personResource = "fake-person.csv";
    static final String personPipeResource = "fake-person-pipe.txt";
    static final String personTabResource = "fake-person-tab.txt";
    final JobOperator jobOperator = BatchRuntime.getJobOperator();
    static final int waitTimeoutMinutes = 5;
    static final String writeComments = "# Comments written by csv writer.";
    static final File tmpdir = new File(System.getProperty("jberet.tmp.dir"));

    static final String nameMapping =
    "number, gender, title, givenName, middleInitial, surname, streetAddress, city, state, zipCode, " +
    "country, countryFull, emailAddress, username, password, telephoneNumber, mothersMaiden, birthday, CCType, CCNumber, " +
    "CVV2, CCExpires, nationalID, UPS, color, occupation, company, vehicle, domain, bloodType, " +
    "pounds, kilograms, feetInches, centimeters, GUID, latitude, longitude";

    static final String cellProcessors =
                    "NotNull, UniqueHashCode, LMinMax(1, 99999); " +    //Number  convert to long and enforce range
                    "Token('male', 'M'), Token('female', 'F');" +   //Gender
                    "null; " +  //Title
                    "StrNotNullOrEmpty; " +  //GivenName
                    "ParseChar; " +     //MiddleInitial
                    "org.jberet.support.io.ToggleCase(a);" +      //Surname to lower case with a custom cell processor
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

    //some content from row 7-9, which is configured in job xml
    static final String personResourceExpect =
            "Cynthia,I,Crowley,3792 Green Hill Road,Fayetteville,AR,72701,US,United States,CynthiaICrowley@dayrep.com," +
            "Barry,M,Sparks,553 Timbercrest Road,Sparrevohn A.F.S.,AK,99506,US,United States,BarryMSparks@cuvox.de," +
            "Joe,K,Davis,1342 Java Lane,Bishopville,SC,29010,US,United States,JoeKDavis@dayrep.com";

    static final String personResourceExpect1_5 =
            "Martin,T,Mejia, Debbie,P,Judson, Margie,J,Chaney, Jerry,K,Smith, Gloria,K,Lewis";

    //content from row 6 & 10
    static final String personResourceForbid = "MarthaEValentine@dayrep.com, CindyNKeyes@jourrapide.com";

    @Test
    public void testBeanType() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testReadWrite0(personResource, "testBeanType.out", org.jberet.support.io.Person.class.getName(),
                null, null, "|", personResourceExpect, personResourceForbid);
    }

    @Test
    public void testBeanTypeTab() throws Exception {
        //override the default quote char ", which is used in feetInches cell
        testReadWrite0(personTabResource, "testBeanTypeTab.out", org.jberet.support.io.Person.class.getName(),
                CsvProperties.TAB_PREFERENCE, null, "|", null, null);
    }

    @Test
    public void testBeanTypePipe() throws Exception {
        //override the default quote char ", which is used in feetInches cell. | is already used as the delimiterChar
        //so cannot be used as quoteChar again.
        testReadWrite0(personPipeResource, "testBeanTypePipe.out", org.jberet.support.io.Person.class.getName(),
                null, "|", "^", null, null);
    }

    //test will print out the path of output file from CsvItemWriter, which can then be verified.
    //e.g., CSV resource to read:
    //fake-person.csv,
    //to write:
    //        /var/folders/s3/2m3bc7_n0550tp44h4bcgwtm0000gn/T/testMapType.out
    private void testReadWrite0(final String resource, final String writeResource,
                                final String beanType, final String preference,
                                final String delimiterChar, final String quoteChar,
                                final String expect, final String forbid) throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, beanType);
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
        params.setProperty(CsvProperties.CELL_PROCESSORS_KEY, cellProcessors);

        final File writeResourceFile = new File(tmpdir, writeResource);
        params.setProperty("writeResource", writeResourceFile.getPath());
        params.setProperty(CsvProperties.WRITE_COMMENTS_KEY, writeComments);
        params.setProperty(CsvProperties.HEADER_KEY, nameMapping);
        CsvItemReaderWriterTest.setRandomWriteMode(params);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        validate(writeResourceFile, expect, forbid);
    }

    @Test
    public void testListType() throws Exception {
        testReadWrite0(personResource, "testListType.out", java.util.List.class.getName(), null, null, "|", personResourceExpect, personResourceForbid);
    }

    @Test
    public void testMapType() throws Exception {
        testReadWrite0(personResource, "testMapType.out", java.util.Map.class.getName(), null, null, "|", personResourceExpect, personResourceForbid);
    }

    @Test
    public void testInvalidWriteResource() throws Exception {
        final Properties params = createParams(CsvProperties.BEAN_TYPE_KEY, List.class.getName());
        params.setProperty(CsvProperties.RESOURCE_KEY, personResource);
        params.setProperty(CsvProperties.QUOTE_CHAR_KEY, "|");
        final String writeResourceFullPath = tmpdir.getPath();
        params.setProperty("writeResource", writeResourceFullPath);
        params.setProperty(CsvProperties.WRITE_COMMENTS_KEY, writeComments);
        params.setProperty(CsvProperties.HEADER_KEY, nameMapping);

        final long jobExecutionId = jobOperator.start(jobName, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        jobExecution.awaitTermination(waitTimeoutMinutes, TimeUnit.MINUTES);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
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

    static String getStreamContent(final InputStream inputStream) throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = new Scanner(inputStream);
            return scanner.useDelimiter("\\Z").next();
        } finally {
            try {
                if (scanner != null) {
                    scanner.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final Exception e) {
                //ignore
            }
        }
    }

    static void validate(final File file, final String expect, final String forbid) throws Exception {
        final String content = getStreamContent(new FileInputStream(file));
        if (expect != null && !expect.isEmpty()) {
            for (String s : expect.split(",")) {
                s = s.trim();
                if (!content.contains(s)) {
                    throw new IllegalStateException("Expected string " + s + " not found");
                } else {
                    System.out.printf("Found expected string %s%n", s);
                }
            }
        }
        if (forbid != null && !forbid.isEmpty()) {
            for (String s : forbid.split(",")) {
                s = s.trim();
                if (!content.contains(s)) {
                    System.out.printf("Forbidden string %s not found%n", s);
                } else {
                    throw new IllegalStateException("Found forbidden string " + s);
                }
            }
        }
        final int length = content.length();
        System.out.printf("%nResult file content%s:%n%s%n%n", length < 1000 ? "" : " first 1000 characters",
                length < 1000 ? content : content.substring(0, 1000));
    }

    /**
     * Randomizes writeMode to return null, CsvProperties.APPEND or CsvProperties.OVERWRITE, and add it to params.
     * @param params job params to which the generated writeMode will be added
     */
    static void setRandomWriteMode(final Properties params) {
        final int i = (new Random()).nextInt() + 3;
        final int m = i % 3;
        final String writeMode;
        switch (m) {
            case 1:
                writeMode = CsvProperties.OVERWRITE;
                params.setProperty(CsvProperties.WRITE_MODE_KEY, writeMode);
                break;
            case 2:
                writeMode = CsvProperties.APPEND;
                params.setProperty(CsvProperties.WRITE_MODE_KEY, writeMode);
                break;
            default:
                writeMode = null;
                //java.util.Properties does not take null value
        }
        System.out.printf("Use randomly picked writeMode %s%n", writeMode);
    }
}
