package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.CommonITableHeader;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;

public class ThreadItable  extends CommonITableHeader {
	
	public ThreadItable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getThreadItableFile(), traceXml);
		
		// Parse for ThreadItable specific headers
		// But there are none! All done then.
	}
	
	/**
	 * Parse on demand for the InsRec objects, of which we have 'rowCount' of.
	 * The threadId is the internal id, provided by an {@link ExecRec}.
	 * 
	 * @param threadId
	 * @return
	 */
	public ThreadRec getThreadRec(long threadId){
		// There are this.recordCount of these, and are all direct addressable via the offset file.	
		return new ThreadRec(this, threadId);
	}
	
	/** ver Oct 2013
	 * Table-specific additional header information.
	 * Note: this is at offset 32+, until any Table data.
	 */
	// No specialized data other than thread rows.
	
	/** ver Oct 2013
	 * Array of RowCount contiguous records.
	 * 
	 * 'rowCount' entries.
	 * 
	 * See getThreadRec for read-on-demand rather than storing the collection.
	 */
	 // ThreadRec[] threadRecords; 
	
}
