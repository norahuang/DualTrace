package ca.uvic.chisel.atlantis.bytecodeparsing.execution;


import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ThreadItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsItable;

public class ExecVtable extends BinaryFileParser {
	
	protected ExecOffsets execOffsets;
	
	public ContextMapItable contextMap;
	
	private InsItable insItable;
	
	private ThreadItable threadItable;
	
	public ExecVtable(AtlantisBinaryFormat binaryFormat, InsItable insItable, ThreadItable threadItable, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getExecVtableFile(), traceXml);
		
		this.insItable = insItable;
		this.threadItable = threadItable;
		// parse for fixed header values
		version = getNextInt();
		skipPadBytes(4);
		recordCount = getNextLong();
		
		execOffsets = new ExecOffsets(binaryFormat, this, traceXml);
		
		contextMap = new ContextMapItable(binaryFormat, traceXml);
	}
	
	public ExecRec getExecRec(long lineNumber){
		return new ExecRec(this, this.insItable, this.threadItable, lineNumber);
	}
	
	/** ver Oct 2013
	 * Version number (only version 0 is currently supported)
	 * 
	 * 32-bit int
	 */
	public final int version;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 4-byte pad
	 */
	// byte[] pad;
	
	/** ver Oct 2013
	 * Valid IDs for all three files are in [ 0,  RecordCount]
	 * 
	 * 64-bit long
	 */
	public final long recordCount;

}


