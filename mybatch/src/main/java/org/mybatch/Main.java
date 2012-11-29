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
 
package org.mybatch;

import java.util.Arrays;
import javax.batch.operations.JobOperator;

import org.mybatch.operations.JobOperatorImpl;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0) {
            usage(args);
        }
        String jobXml = args[0];
        if(jobXml == null || jobXml.isEmpty()) {
            usage(args);
        }
        getJobOperator().start(jobXml, System.getProperties());
    }

    private static JobOperator getJobOperator() {
        JobOperator jo = null;
        //need to use javax.batch.spi.JobOperatorFactory (not available yet)
        jo = new JobOperatorImpl();
        return jo;
    }

    private static void usage(String[] args) {
        System.out.printf("Usage: java -classpath ... -Dkey1=val1 ... org.mybatch.Main jobXML%n");
        System.out.printf("The following application args are invalid:%n%s", Arrays.asList(args));
    }
}
