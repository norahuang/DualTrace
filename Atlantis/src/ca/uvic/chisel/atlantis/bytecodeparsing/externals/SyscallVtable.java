package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;

public class SyscallVtable  extends BinaryFileParser {
	
	SyscallOffsets syscallOffsets;
	
	
	public SyscallVtable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getSyscallVtableFile(), traceXml);
		
		this.version = this.getNextInt();
		
		this.unifiedIDsVer = this.getNextInt();
		
		this.recordCount = this.getNextLong();
		
		this.syscallOffsets = new SyscallOffsets(binaryFormat, this, traceXml);
	}
	
	public SyscallRec getSyscallRec(long syscallId){
		return new SyscallRec(this, syscallId);
	}
	
	// These are in the header of the syscall.vtable
	
	/** ver Oct 2013
	 * Format version number (only version 0 is currently supported)
	 * 
	 * 32-bit int
	 */
	public final int version;
	
	/** ver Oct 2013
	 * Uniform ID enum version (only version 0 is currently supported). See section 8.1 above.
	 * 
	 * 32-bit int
	 */
	public final int unifiedIDsVer;
	
	/** ver Oct 2013
	 * Valid IDs for both files are in [ 0,  RecordCount ]
	 * 
	 * 64-bit long
	 */
	public final long recordCount;
	
	// see ExecVtable for similar.
	/** ver Oct 2013
	 * Variable-length record (see syscallRec structure below) for each i in [ 0,  RecordCount ].
	 * Offset and size are given in syscall.offsets. Records in the file are not sorted by their ID.
	 * 
	 * Number of elements is 'recordCount', offset='syscallOffsets.offsetSizePairs[index].offset'
	 * 
	 * See {@link #getSyscallRec(long)} for on-demand access rather than storing
	 * container of them.
	 */
	// SyscallRec[] syscallRecs;

}
