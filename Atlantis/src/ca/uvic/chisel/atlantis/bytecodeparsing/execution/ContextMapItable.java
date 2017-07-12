package ca.uvic.chisel.atlantis.bytecodeparsing.execution;

import java.util.ArrayList;
import java.util.List;


import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.BinaryFileParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;

// TODO Maybe in the array filling sections, we could save some time by skipping
// decoded regmem data rather than reading into RegInfo objects that we never use?
public class ContextMapItable extends BinaryFileParser {
	/*
	 * Despite documentation implying that the context_map.ibable follows the itable format,
	 * I received confirmation that this is not true for the format we received in November 2013.
	 * The context_map.itable does not have the common header, but does match the rest of that
	 * documentation. Therefore, we do not extend CommonITableHeader (for now).
	 */
	
	public ContextMapItable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getContextMapItable(), traceXml);
		
		this.dumpSize = this.getNextMiniInt(16);
		
		this.elementCount = this.getNextMiniInt(16);
		
		// 12 byte pad
		this.skipPadBytes(12);
		
		// Don't try refactoring these two loops together, mkay?
		this.commonName = new int[this.elementCount];
		for(int i = 0; i < 512; i++){
			// We need to parse over even invalid entries until we have read 512 of them
			if(i < this.elementCount){
				this.commonName[i] = this.getNextMiniInt(16);
			} else {
				this.skipPadBytes(16/8);
			}
		}
		
		this.strName = new String[this.elementCount];
		for(int i = 0; i < 512; i++){
			// We need to parse over even invalid entries until we have read 512 of them
			if(i < this.elementCount){
				this.strName[i] = this.getNextStringOfByteLength(16);
			} else {
				this.skipPadBytes(16/8);
			}
		}
		
		this.regMap32 = new ArrayList<RegInfo>(this.elementCount);
		for(int i = 0; i < 512; i++){
			// We need to parse over even invalid entries until we have read 512 of them
			if(i < this.elementCount){
				RegInfo regInfo = new RegInfo(this);
				this.regMap32.add(i, regInfo);
			} else {
				this.skipPadBytes(RegInfo.SIZE_IN_BYTES);
			}
		}
		
		this.regMap64 = new ArrayList<RegInfo>(this.elementCount);
		// We need to parse over even invalid entries until we have read 512 of them
		for(int i = 0; i < 512; i++){
			if(i < this.elementCount){
				RegInfo regInfo = new RegInfo(this);
				this.regMap64.add(i, regInfo);
			} else {
				this.skipPadBytes(RegInfo.SIZE_IN_BYTES);
			}
		}
	}
	
	
	/** ver Oct 2013
	 * Size of a context dump buffer. All offsets in RegMap32 and RegMap64 are smaller than DumpSize.
	 * 
	 * 16-bit int
	 */
	public final int dumpSize;
	
	/** ver Oct 2013
	 * Count of valid elements in all four arrays below. All Arrays have 512 elements,
	 * but only the first ElemCount are meaningful.
	 * 
	 * 16-bit int
	 */
	public final int elementCount;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 12-byte pad
	 */
	// byte[] pad; // 12 byte pad
	
	/** ver Feb 2015
	 * 
	 * "Common register name (see 11.1 above)"
	 * The 'Common register name' section reads:
	 * 
	 * Incidentally, this map also provides generic static information about registers and relations among
	 * themselves. Namely, it gives the largest enclosing register for each register as well as a common register
	 * name for all overlapping registers.
	 * The largest enclosing register for a given register is taken from available registers on the CPU and mode
	 * (32/64-bit) the trace was made. For instance, the largest register enclosing al is eax on 32-bit traces,
	 * but rax on 64-bit traces. Enclosures are:
	 * - For each general purpose registers, as well as flags register and instruction pointer:
	 * 64-bit reg > 32-bit reg > 16-bit reg > 8-bit reg
	 * Possible 8-bit reg includes ah, bh, ch and dh
	 * - x87 registers (80-bit) > MMX registers (64-bit)
	 * In reverse order, by Intel hardware design: st0 > mm7, st1 > mm6, … st7 > mm0
	 * - ZMM registers (512-bit) > YMM registers (256-bit) > XMM registers (128-bit)
	 * Whichever the largest supported on the traced CPU
	 * - For all others, including pseudo-registers, the largest enclosing register is that register itself
	 * Common register names are independent of the traced CPU architecture and 32/64-bit mode. The
	 * current mapping is:
	 * - Legacy 16-bit general purpose registers, including flags and instruction pointer:
	 * All their relatives map to the 16-bit name, e.g. al, ah, ax, eax, rax -> ax
	 * This includes registers added in the x64 extension, i.e. sil -> si and bpl -> bp
	 * - GPRs added in the x64 extensions use their 64-bit version name, e.g. r8l, r8w, r8d, r8 -> r8
	 * - Each MMX registers map to enclosing x87 registers
	 * In reverse order, as with above enclosing registers, i.e. mm7 -> st0, mm6 -> st1, …
	 * - XMM, YMM and ZMM registers map to corresponding XMM
	 * - For all others, including pseudo-registers, the common register name is that register itself
	 * Previous versions of UMTracer (using insRec version 0) used a different mapping: all registers would
	 * map to their largest known-at-that-time register, which could change over time, as newer tools may
	 * know more registers. Analysis tools relying on the mapping provided here, instead of hardcoded
	 * constants, won’t require any changes. But the new mapping may ease implementation since it is stable:
	 * it may only be augmented as new registers are introduced, but never otherwise modified, as long
	 * 
	 * ver Oct 2013
	 * Absolute largest enclosing register for each register (see 11.1 above)
	 * 
	 * size is 512 elements, with 'elementCount' valid ones.
	 * 16-bit int
	 */
	public final int[] commonName;
	
	/** ver Oct 2013
	 * 
	 * NB Feb 2015 spec revision merely changed from "name" to "strName"
	 * 
	 * Array of 512 “strings”, the textual name of each register.
	 * Each string is actually a fixed 16-bytes array and names are null-terminated.
	 * 
	 * Note these strings are ac data type, an i8, not cstr, but it is null terminated.
	 * They also happen to be limited to 16 characters
	 * So that is 8 byte characters in a collection of size 16.
	 * 
	 * size is 512 elements, with 'elementCount' valid ones.
	 * 
	 */
	public final String[] strName;
	
	/** ver Oct 2013
	 * Per register contextual information for instructions in 32-bit mode code segment.
	 * 
	 * size is 512 elements, with 'elementCount' valid ones.
	 */
	public final List<RegInfo> regMap32;
	
	/** ver Oct 2013
	 * Per register contextual information for instructions in 64-bit mode (a.k.a. long mode) code segment.
	 * 
	 * size is 512 elements, with 'elementCount' valid ones.
	 */
	public final List<RegInfo> regMap64;
	
	
	public class RegInfo {
		
		final static int SIZE_IN_BYTES = 16/8 + 8/8 + 1 + 16/8 + 2;
		
		public RegInfo(ContextMapItable parentParser) {
			
			this.offset = parentParser.getNextMiniInt(16);
			
			this.registerDoesNotApply = (this.offset == 0xffff);
			
			
			this.size = parentParser.getNextMiniInt(8);
			
			// 1 byte pad
			parentParser.skipPadBytes(1);
			
			this.effLargest = parentParser.getNextMiniInt(16);
			
			// 2 byte pad
			parentParser.skipPadBytes(2);
		}

		/** ver Oct 2013
		 * Offset of the register in a context dump buffer, relative to the start of the
		 * buffer, or 0xffff if the current register is not part of this 32/64-bit context.
		 * 
		 * 16-bit int
		 */
		public final int offset;
		
		/**
		 * Convenience field to show whether this register applies to the program context.
		 * Reflects whether the offset is set to 0xffff.
		 */
		public final boolean registerDoesNotApply;
		
		
		/** ver Oct 2013
		 * Size of the register in a context dump buffer, or 0 if the current register is
		 * not part of this 32/64-bit context.
		 * 
		 * 8-bit int
		 */
		public final int size;
		
		/** ver Oct 2013
		 * Pad
		 * 
		 * 1-byte
		 */
		// byte[] pad1; // 1 byte pad
		
		/** ver Oct 2013
		 * Effective largest enclosing register for current register (see 11.1 above)
		 * 
		 * 16-bit int
		 */
		public final int effLargest;
		
		/** ver Oct 2013
		 * Pad
		 * 
		 * 2-byte
		 */
		// byte[] pad2; // 2 byte pad
	}
}
