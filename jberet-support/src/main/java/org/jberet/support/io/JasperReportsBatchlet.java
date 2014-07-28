/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXlsxDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.data.XlsDataSource;
import org.jberet.support._private.SupportMessages;

@Named
@Dependent
public class JasperReportsBatchlet implements Batchlet {
    @Inject
    @BatchProperty
    protected String resource;

    @Inject
    @BatchProperty
    protected String charset;

    @Inject
    @BatchProperty
    protected String template;

    @Inject
    @BatchProperty
    protected String outputType;

    @Inject
    @BatchProperty
    protected String outputFile;

    @Inject
    @BatchProperty
    protected Map reportParameters;

    @Inject
    protected Instance<OutputStream> outputStreamInstance;

    @Inject
    protected Instance<JRDataSource> jrDataSourceInstance;

    @Inject
    protected Instance<Map<String, Object>> reportParametersInstance;

    private InputStream resourceInputStream;

    private String templateFilePath;

    @Override
    public String process() throws Exception {
        InputStream templateInputStream = null;
        OutputStream outputStream = null;

        try {
            templateInputStream = getTemplateInputStream();
            if (template == null || !template.toLowerCase().endsWith(".jasper")) {
                //if the template file in *.jrxml, or *.xml format, need to compile the xml design file into
                // serialized "*.jasper" report file
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, template, "template (*.jasper)");
            }

            outputStream = getOutputStream();
            final String ftype = outputType.toLowerCase();
            if (ftype.equals("pdf")) {
                JasperRunManager.runReportToPdfStream(getTemplateInputStream(), outputStream, getReportParameters(), getJrDataSource());
                outputStream.flush();
            } else if (ftype.equals("html")) {
                JasperRunManager.runReportToHtmlFile(getTemplateFilePath(templateInputStream), outputFile, getReportParameters(), getJrDataSource());
            } else if (ftype.equals("jrprint")) {
                JasperFillManager.fillReportToFile(getTemplateFilePath(templateInputStream), outputFile, getReportParameters(), getJrDataSource());
            } else {
                final JasperPrint jasperPrint = JasperFillManager.fillReport(templateInputStream, getReportParameters(), getJrDataSource());
                // export to common file types: txt, rtf, odt, xml, csv, xls

            }

            return null;
        } finally {
            if (templateInputStream != null) {
                try {
                    templateInputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
            if (resourceInputStream != null) {
                try {
                    resourceInputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }

    }

    @Override
    public void stop() throws Exception {

    }

    protected InputStream getTemplateInputStream() {
        if (template == null) {
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, null, "template");
        }

        return ItemReaderWriterBase.getInputStream(template, false);
    }

    protected OutputStream getOutputStream() throws FileNotFoundException {
        if (outputFile != null) {
            return new FileOutputStream(outputFile);
        }

        // if output needs to be directed to an injected OutputStream
        if (outputStreamInstance != null && !outputStreamInstance.isUnsatisfied()) {
            return outputStreamInstance.get();
        }

        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, outputFile, "outputFile");
    }

    protected Map<String, Object> getReportParameters() {
        if (reportParameters != null) {
            return reportParameters;
        }

        if (reportParametersInstance != null && !reportParametersInstance.isUnsatisfied()) {
            return reportParametersInstance.get();
        }

        return new HashMap<String, Object>();
    }

    protected JRDataSource getJrDataSource() throws IOException, JRException {
        if (resource != null) {
            final String res = resource.toLowerCase();
            resourceInputStream = ItemReaderWriterBase.getInputStream(resource, false);
            if (res.endsWith(".csv")) {
                final JRCsvDataSource csvDataSource =  charset == null ? new JRCsvDataSource(resourceInputStream) :
                        new JRCsvDataSource(resourceInputStream, charset);
                csvDataSource.setUseFirstRowAsHeader(true);
                csvDataSource.setRecordDelimiter("\n");
                return csvDataSource;
            }
            if (res.endsWith(".xls")) {
                return new XlsDataSource(resourceInputStream);
            }
            if (res.endsWith(".xlsx")) {
                return new JRXlsxDataSource(resourceInputStream);
            }
            if (res.endsWith(".xml")) {
                return new JRXmlDataSource(resourceInputStream);
            }
            if (res.endsWith(".json")) {
                return new JsonDataSource(resourceInputStream);
            }
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, resource, "resource");
        } else {
            if (jrDataSourceInstance != null && !jrDataSourceInstance.isUnsatisfied()) {
                return jrDataSourceInstance.get();
            }
        }
        return new JREmptyDataSource();
    }

    private String getTemplateFilePath(final InputStream templateInputStream) throws IOException {
        if (templateFilePath != null) {
            return templateFilePath;
        }
        File templateAsFile = new File(template);
        if (templateAsFile.exists()) {
            return templateFilePath = template;
        }

        //the template file path is unknown, need to save it to a file first
        final byte[] buffer = new byte[102400];
        templateAsFile = File.createTempFile("jberet-support-JasperReportsBatchlet", String.valueOf(System.currentTimeMillis()));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(templateAsFile);
            int len;
            while ((len = templateInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }
        return templateFilePath = templateAsFile.getPath();
    }
}
