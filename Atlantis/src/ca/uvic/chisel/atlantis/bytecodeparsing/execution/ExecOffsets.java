package ca.uvic.chisel.atlantis.bytecodeparsing.execution;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;

public class ExecOffsets extends BinaryFileParser {
	
	static final private int BIT_WIDTH_OF_LONG = 64;  // 8 bytes, 64 bit long
	
	private ExecVtable execVtable;
	
	ExecOffsets(AtlantisBinaryFormat binaryFormat, ExecVtable execVtable, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getExecOffsetsFile(), traceXml);
		this.execVtable = execVtable;
	}

	/** ver Oct 2013
	 * Absolute file offsets into exec.vtable for each record, and an extra file
	 * offset pointing after the end of the last record.
	 * Any record i is always fully contained in exec.vtable within [ Offset[i], Offset[i+1] ].
	 * 
	 * @param lineNumber
	 * @return
	 */
	protected Long getExecOffsetFromLineNumber(long lineNumber) {
		// Line numbers are zero indexed, so should be below record count
		if(lineNumber < 0 || this.execVtable.recordCount <= lineNumber){
			return null;
		}
		
		long entryStartPosition = lineNumber * BIT_WIDTH_OF_LONG; // long is several bytes
		this.bitBuffer.setBitPos(entryStartPosition); // bits not bytes
		
		// For access to the end offset, uncomment these.
		// Long startOffset = this.getNextLong();
		// Long endOffset = this.getNextLong();
		// return startOffset;
		
		return this.getNextLong();
	}
}
