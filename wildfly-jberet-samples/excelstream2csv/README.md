First need to download the big test Excel file to `src/main/resources/` directory:

    wget -O IBM_unadjusted.xls https://github.com/jberet/jsr352/blob/master/jberet-support/src/test/resources/IBM_unadjusted.xls\?raw=true

This sample reads rows of data from `IBM_unadjusted.xls`, deserialize each row of data into POJO of type `StockTrade`,
and writes them out to `$TMPDIR/excelstream2csv.csv`.  This sample demonstrates the reading of large binary excel files
with low memory footprint.

This is a webapp packaged as `excelstream2csv.war`, and deployed to WildFly.  To run samples,

    cd wildfly-jberet-samples/excelstream2csv
    mvn install -Pwildfly

    # to verify the output from CsvItemWriter:
    view $TMPDIR/excelstream2csv.csv