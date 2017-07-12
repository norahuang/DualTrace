package ca.uvic.chisel.atlantis.bytecodeparsing.execution;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** ver Oct 2013
 * full register context (all register values) and optionnaly some relevant values from memory. 
 *
 */
public class RmContext {
	
	final public ExecLineType type;
	
	final ContextMapItable contextMap;
	
	RmContext(ExecVtable parentParser, ExecLineType execLineType, long temporaryNextRmContextFp){

		this.contextMap = parentParser.contextMap;
		this.type = execLineType;
		
		long structureFpByteAddress = parentParser.getCurrentBitAddress()/8;
		
		this.memCount = parentParser.getNextInt();
		
		// 4-byte pad to ignore
		parentParser.skipPadBytes(4);
		
		this.regsFp = parentParser.getNextLong();
		long regValueDataAbsFp = structureFpByteAddress + this.regsFp;
		this.memRefs = new ArrayList<ContextMem>(this.memCount);
		for(int i = 0; i < this.memCount; i++){
			memRefs.add(new ContextMem(parentParser, structureFpByteAddress));
		}
		
		// This buffer will be used only once, and we will clear it when it is done being used.
		// This is intended to help with reducing memory usage, but may have little efficacy.
		// Nonetheless, it won't be around for inspection during debugging or redundant reads.
		this.registerValuesBuffer = parentParser.randomAccessRead(regValueDataAbsFp, this.contextMap.dumpSize);
		
		// We used to parse the ContextMem values here, before memory was changed to use just-in-time (JIT) access.
		// Now, we do it via calls to ContextMem class, and at a different time.
		
		return;
	}
	

	/** ver Oct 2013
	 *  Number of elements in memRefs
	 *  
	 *  32-bit int
	 */
	public final int memCount;
	
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 4-byte pad
	 */
	// List<Byte> pad;
	
	/** ver Oct 2013
	 * Points to a buffer that holds all registers value.
	 * Size and data layout of this buffer is given by the Context
	 * map (see section 11). FP is relative to the start of this structure
	 * (its position in the file) 
	 * 
	 * 64-bit long
	 */
	public final long regsFp; // buf[?] file pointer to Context map, at end of this RmContext data
	
	/** ver Oct 2013
	 * Array of optional memory references added to the context.
	 * 
	 * List of 'memCount' elements
	 */
	public final List<ContextMem> memRefs;

	
	/**
	 * Actual register data, as pointed to by regsFp. Can be parsed to useful data by using
	 * structure provided in the RegInfo objects owned by the ContextMapItable.
	 * 
	 * The equivalent memory structure (non-register) is in the individual ContextMap objects
	 * of the RmContext.
	 * 
	 * Size derived from context map's 'dumpSize'.
	 * 
	 * NB Should only be used once, because we clear it after the first usage of it.
	 */
	public final ByteBuffer registerValuesBuffer;
	
	// Looking for memoryBuffers? See constructor and memRef list.
	
}
