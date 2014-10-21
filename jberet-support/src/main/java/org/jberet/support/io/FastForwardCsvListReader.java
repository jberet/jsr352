package org.jberet.support.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.AbstractCsvReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Copied and modified from supercsv CsvListReader, which  is a simple reader that reads a row from a CSV file
 * into a <tt>List</tt> of Strings.
 *
 * @author Kasper B. Graversen
 * @author James Bassett
 *
 * @see     org.jberet.support.io.FastForwardCsvBeanReader
 * @see     org.jberet.support.io.FastForwardCsvMapReader
 * @since   1.0.0
 */
final class FastForwardCsvListReader extends AbstractCsvReader implements ICsvListReader {
    private final int startRowNumber;

    /**
	 * Constructs a new <tt>CsvListReader</tt> with the supplied Reader and CSV preferences. Note that the
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
	public FastForwardCsvListReader(final Reader reader, final CsvPreference preferences, final int startRowNumber) {
		super(reader, preferences);
        this.startRowNumber = startRowNumber;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> read() throws IOException {
		fastForwardToStartRow();
		if( readRow() ) {
			return new ArrayList<String>(getColumns());
		}
		
		return null; // EOF
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Object> read(final CellProcessor... processors) throws IOException {
		fastForwardToStartRow();
		if( processors == null ) {
			throw new NullPointerException("processors should not be null");
		}
		
		if( readRow() ) {
			return executeProcessors(processors);
		}
		
		return null; // EOF
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Object> executeProcessors(final CellProcessor... processors) {
		return super.executeProcessors(new ArrayList<Object>(getColumns().size()), processors);
	}

    private void fastForwardToStartRow() throws IOException {
        while (getRowNumber() < this.startRowNumber) {
            readRow();
        }
    }
}
