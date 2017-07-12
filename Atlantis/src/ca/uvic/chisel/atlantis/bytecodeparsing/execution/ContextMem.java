package ca.uvic.chisel.atlantis.bytecodeparsing.execution;

import java.nio.ByteBuffer;

import javax.management.RuntimeErrorException;

import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;


/** ver Oct 2013
 * one memory reference, part of a rmContext.  
 *
 */
public class ContextMem {
	
	/**
	 * Parent RmContext position in file, provides basis for the {@link ContextMem#dataFp} dataFp member.
	 * Received via constructor.
	 */
	public final long parentFp;
	
	/**
	 * File pointer {@link ContextMem#dataFp} plus {@link ContextMem#parentFp}
	 */
	public final long absDataFp;

	private ExecVtable parentParser;
	
	ContextMem(ExecVtable parentParser, long parentFp){
		this.address = parentParser.getNextLong();
		
		this.size = parentParser.getNextLong();
		
		// buf[size] file pointer to memory buffers, located after final ContextMem of the RmContext parent
		this.dataFp = parentParser.getNextLong();
		
		this.parentParser = parentParser;
		
		// This file pointer is the base for the dataFp parsed above. The combination is the absolute location of the memory buffers.
		this.parentFp = parentFp;
		
		this.absDataFp = this.dataFp + parentFp;
	}

	/** ver Oct 2013
	 * Memory address (high 32 bits are always 0 on 32-bit traces)
	 * 
	 * 64-bit long
	 */
	public final long address;
	
	/** ver Oct 2013
	 * Size of the memory buffer 
	 * 
	 * 64-bit long
	 */
	public final long size;
	
	/** ver Oct 2013
	 * Points to memory content. FP is relative to the start of
	 * the parent rmContext structure (its position in the file) 
	 * 
	 * 64-bit long
	 */
	public final long dataFp;
	
	/** ver Oct 2013
	 *
	 * Data pointed to from those structures is stored right after rmContext instance.
	 * That way, each record remains a monolithic stream of bytes. Any record i is always
	 * fully contained in exec.vtable within [ Offset[i], Offset[i+1] ], where Offset is
	 * found in exec.offsets.
	 *
	 * Implementation note:
	 * This cannot be parsed when this object is parsed, and it is therefore held in the parent.
	 * We can therefore point the field at the array stored in the parent, but only
	 * once the parent is done parsing them.
	 * 
	 * Preon would prefer if the data be read in via a bound parameter, as follows.
	 * We don't need the file pointer; it is a relative pointer, and the data follows
	 * immediately after the parent RmContext. This means that we have the following:
	 * 
	 * RmContext.memCount
	 * RmContext.pad[4]
	 * RmContext.regsFp
	 * RmContext.memRefs[memCount]:
	 * 		contextMem.address
	 * 		contextMem.size
	 * 		contextMem.dataFp
	 * RmContext.regs {where the regsFp points to, note taht this byte buffer is of unknown size}
	 * RmContext.data {where the multiple contextMem.dataFp point to. We know the sizes of these byte buffers.}
	 * 
	 * How can we easily get these in Preon?
	 * Well...we can read the whole set of regs and data as one huge buffer, then address into that using the FPs
	 * we have available. This means that we won't parse the data associated with the MemContext objects as a part
	 * of these objects, but only will parse them as part of the parent. The data should therefore be accessed from
	 * the parent RmContext object.
	 * 
	 */
	// Used internally only, no need to actually make a member variable.
	// public ByteBuffer memoryDataBuffer;
	public String memoryHexValue = null;

	/**
	 * Before accessing the {@link ContextMem#memoryHexValue}, call this method. During parsing,
	 * we defer reading from buffers for memory performance reasons. When in Atlantis, post-DB,
	 * the value should be available already, if this class is even used in that context.
	 * @param valueByteWidth 
	 * @param byteAddress 
	 */
	public void performRead(Long readStartAddress, Long readByteWidth) {
		// Although this reference has its starting address and its width, users of it
		// actually desire subsets (due to the JIT reading prior to DB storage of memory values)
		// This means that the caller can provide parameters that shrink the region returned.
		
		if(null == readStartAddress){
			readStartAddress = address;
		}
		if(null == readByteWidth){
			readByteWidth = this.size;
		}
		
		long readStartOffset = readStartAddress - address;
		
		if((readStartAddress + readByteWidth) > (address + this.size)){
			throw new RuntimeErrorException(new Error("ContextMemory read requested with invalid width or end address, requested width"+readByteWidth+" but reference width is "+this.size));
		}
		
		if(readStartOffset < 0){
			throw new RuntimeErrorException(new Error("ContextMemory read requested with invalid start address, requested "+ (this.absDataFp + readStartOffset) +" but reference starts at "+this.absDataFp));
		}
		
		
		ByteBuffer memoryDataBuffer = parentParser.randomAccessRead(this.absDataFp + readStartOffset, (int)(long)readByteWidth);
		
		StringBuilder valueBuilding = new StringBuilder();
		while(memoryDataBuffer.hasRemaining()){
			valueBuilding.append(BinaryFormatParser.toHex(memoryDataBuffer.get()));
		}
		// try clearing to improve memory usage
		memoryDataBuffer.clear();
		this.memoryHexValue = valueBuilding.toString();
	}
	
	public void clearReadValue(){
		this.memoryHexValue = null;
	}
	
	
}
