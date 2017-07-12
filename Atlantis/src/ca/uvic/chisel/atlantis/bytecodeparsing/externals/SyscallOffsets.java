package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;

public class SyscallOffsets  extends BinaryFileParser {
	
	static final private int BYTE_WIDTH_OF_TWO_LONG = (2*64) / 8;  // 8 bytes each for a pair of 64 bit long
	
	private SyscallVtable syscallVtable;
	
	SyscallOffsets(AtlantisBinaryFormat binaryFormat, SyscallVtable syscallVtable, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getSyscallOffsetsFile(), traceXml);
		this.syscallVtable = syscallVtable;
	}

	/** ver Oct 2013
	 * Absolute file offsets into exec.vtable for each record, and an extra file
	 * offset pointing after the end of the last record.
	 * Any record i is always fully contained in exec.vtable within [ Offset[i], Offset[i+1] ].
	 * 
	 * @param lineNumber
	 * @return
	 */
	protected OffsetSizePair getSyscallOffsetSizePairFromSyscallNumber(long lineNumber) {
		// Line numbers are zero indexed, so should be below record count
		if(lineNumber < 0 || this.syscallVtable.recordCount <= lineNumber){
			return null;
		}
		
		long entryStartPosition = lineNumber * BYTE_WIDTH_OF_TWO_LONG; // long is several bytes
		this.bitBuffer.setBitPos(entryStartPosition * 8); // bits not bytes, so * 8 again
		return new OffsetSizePair(this.getNextLong(), this.getNextLong());
	}

	/** ver Oct 2013
	 * Each pair holds the absolute file offset and size of the corresponding record in syscall.vtable.
	 * 
	 * Access on demand via {@link getSyscallOffsetFromLineNumber(long)}
	 * 
	 * Number of elements is syscallVtable.recordCount + 1.
	 * Elements are a pair of 64-bit longs.
	 */
	// List<OffsetSizePair> offsetSizePairs;
	
	public class OffsetSizePair {
		long offset;
		long size;
		
		OffsetSizePair(Long offset, Long size){
			this.offset = offset;
			this.size = size;
		}
	}

	
}
