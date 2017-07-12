package ca.uvic.chisel.atlantis.bytecodeparsing.instruction;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.CommonITableHeader;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;

public class InsItable extends CommonITableHeader {
	
	protected final String bitness;

	public InsItable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getInsItableFile(), traceXml);
		
		this.bitness = traceXml.getAppBitness();
				
		// Parse for InsItable specific headers
		
		this.extraFp = getNextLong(); // buf[ExtraSize] * // 256 is the bit position here, so 32 bits in...correct...
		
		this.extraSize = getNextLong();
		
		// Everything after this is subordinate data (non-header)
	}
	
	/**
	 * Parse on demand for the InsRec objects, of which we have 'rowCount' of.
	 * 
	 * @param execRec.insId
	 * @return
	 */
	public InsRec getInsRec(ExecRec execRec){
		// There are this.recordCount of these, and are all direct addressable via the offset file.	
		return new InsRec(this, execRec);
	}
	
	/** ver Oct 2013
	 * Table-specific additional header information.
	 * Note: this is at offset 32+, until any Table data.
	 */
	
	/** ver Oct 2013
	 * Absolute file pointer to the start of the two collections of
	 * all variable-size sub-records (decoded instruction and disassembly).
	 * 
	 * This long points like so: buf[ExtraSize] *
	 * 
	 * NB: Data at ExtraFP is not readily usable. It is rather accessed via instruction records, found at Table[i].
	 * 
	 * 64-bit long
	 */
	public final long extraFp; // buf[ExtraSize] *
	
	/** ver Oct 2013
	 * Total size of those two collections [above]
	 * 
	 * 64-bit long
	 */
	public final long extraSize;

	/** ver Oct 2013
	 * Array of RowCount contiguous records. 
	 * 
	 * size is 'rowCount'
	 * 
	 * See accessor method instead
	 */
	 // InsRec[] insRecords; 
	 

	
}
