package ca.uvic.chisel.atlantis.bytecodeparsing.execution;


import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;

public class ExecPrevNextColumn  extends BinaryFileParser {
	
	
	public ExecPrevNextColumn(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getExecPrevNextColumn(), traceXml);
	}
	
	public PrevNextPair getPrevNextForExecLine(long lineNumber){
		return new PrevNextPair(this, lineNumber);
	}

	/** ver Oct 2013
	 * Each pair PrevNext[i] holds respectively the IDs of the previous and next records,
	 * thread CF-wise, for record i in [  0,  RecordCount ].
	 * ID -1 is used when there is no previous or next record.
	 * 
	 * See {@link #getPrevNextForExecLine()}
	 * 
	 * Element number is execVtable.recordCount, and consist of two longs. 
	 */
	 //PrevNextPair[] prevNext;

	public class PrevNextPair {
		static final long BYTE_SIZE_PREV_PAIR = 2 * (64/8); // two 64-bit longs
		
		public PrevNextPair(ExecPrevNextColumn parentParser, long lineNumber) {
			parentParser.seekToByteAddress(lineNumber * BYTE_SIZE_PREV_PAIR);
			this.prev = parentParser.getNextLong();
			this.next = parentParser.getNextLong();
		}
		/**
		 * 64-bit long
		 */
		public final long prev;
		
		/**
		 * 64-bit long
		 */
		public final long next;
	}
}
