package org.jberet.support.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.AbstractCsvReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.Util;

/**
 * Copied and modified from supercsv CsvMapReader to support fast forward.
 * CsvMapReader reads each CSV row into a Map with the column name as the map key, and the column value as the map
 * value.
 *
 * @author Kasper B. Graversen
 * @author James Bassett
 *
 * @see     org.jberet.support.io.FastForwardCsvBeanReader
 * @see     org.jberet.support.io.FastForwardCsvListReader
 * @since   1.0.0
 */
final class FastForwardCsvMapReader extends AbstractCsvReader implements ICsvMapReader {
    private final int startRowNumber;

    /**
	 * Constructs a new <tt>CsvMapReader</tt> with the supplied Reader and CSV preferences. Note that the
	 * <tt>reader</tt> will be wrapped in a <tt>BufferedReader</tt> before accessed.
	 * 
	 * @param reader
	 *            the reader
	 * @param preferences
	 *            the CSV preferences
     * @param startRowNumber the row number to start reading
     * @throws NullPointerException
	 *             if reader or preferences are null
	 */
	public FastForwardCsvMapReader(final Reader reader, final CsvPreference preferences, final int startRowNumber) {
		super(reader, preferences);
        this.startRowNumber = startRowNumber;
    }
	
	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> read(final String... nameMapping) throws IOException {
        fastForwardToStartRow();
        if (nameMapping == null) {
            throw new NullPointerException("nameMapping should not be null");
        }

        if (readRow()) {
            final Map<String, String> destination = new HashMap<String, String>();
            Util.filterListToMap(destination, nameMapping, getColumns());
            return destination;
        }

        return null; // EOF
    }

    /**
	 * {@inheritDoc}
	 */
	public Map<String, Object> read(final String[] nameMapping, final CellProcessor[] processors) throws IOException {
        fastForwardToStartRow();
        if (nameMapping == null) {
            throw new NullPointerException("nameMapping should not be null");
        } else if (processors == null) {
            throw new NullPointerException("processors should not be null");
        }

        if (readRow()) {
            // process the columns
            final List<Object> processedColumns = executeProcessors(new ArrayList<Object>(getColumns().size()),
                    processors);

            // convert the List to a Map
            final Map<String, Object> destination = new HashMap<String, Object>(processedColumns.size());
            Util.filterListToMap((Map<String, Object>) destination, nameMapping, (List<Object>) processedColumns);
            return destination;
        }

        return null; // EOF
    }

    private void fastForwardToStartRow() throws IOException {
        while (getRowNumber() < this.startRowNumber) {
            readRow();
        }
    }
}
