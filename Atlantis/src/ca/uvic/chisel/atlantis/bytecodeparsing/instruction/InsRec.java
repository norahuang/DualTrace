package ca.uvic.chisel.atlantis.bytecodeparsing.instruction;

import java.nio.ByteBuffer;

import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;


public class InsRec {
	
	static private final int BYTE_SIZE_INS_REC = 64; // defined as 56 bytes plus a pad of 8 bytes

	public final DecodedIns decodedIns;
	
	public final String dissasembly;
	
	InsRec(InsItable parentParser, ExecRec execRec){
		long insNumber = execRec.insId;
		long originalBitOffset = parentParser.getCurrentBitAddress();
		long offsetForLineNumber = parentParser.tableFp + insNumber * BYTE_SIZE_INS_REC;
		
		// The offset file happens to have an extra entry at the very end for precisely this porpoise. Fintastic!
		parentParser.seekToByteAddress(offsetForLineNumber);
		
		this.id = parentParser.getNextLong();
		
		this.type = parentParser.getNextMiniInt(8);
		
		this.csIs64Bits = parentParser.getNextBooleanByte();
		
		this.codeSize = parentParser.getNextMiniInt(8);
		
		// 1 byte pad to ignore.
		parentParser.skipPadBytes(1);
		
		this.moduleId = parentParser.getNextInt();	// actually i32 (based off of offsets within spec table)
		
		this.address = parentParser.getNextLong(); // actually i64 (based off of offsets within spec table)
		
		// This is unused, thus wasting memory. If we use it eventually, try clearing the buffer after use,
		// assuming it is not needed repeatedly.
		// this.codeBytes = parentParser.getNextChunkOfBytes(15);
		this.codeBytes = null;
		parentParser.skipPadBytes(15); // skipping rather than reading
		
		// 1 byte pad to ignore.
		parentParser.skipPadBytes(1);
		
		this.disasmFp = parentParser.getNextLong();
		
		this.decodedFp = parentParser.getNextLong();
		
		// 8 byte pad to ignore.
		parentParser.skipPadBytes(8);
		
		//---------------------
		// Grab referenced data
		
		long dissasemblyOffset = this.disasmFp + parentParser.extraFp;
		this.dissasembly = parentParser.getCStrAtByteAddress(dissasemblyOffset);
		
		execRec.setDissassemblyForErrors(this.dissasembly);
		
		// File pointer is relative to extraFp in this InsRec header
		this.decodedIns = new DecodedIns(parentParser, this.decodedFp, parentParser.extraFp, execRec);
		
		// Not critical due to how this class is used, but we'll play nice anyhow.
		parentParser.seekToBitAddress(originalBitOffset);
	}
	
	
	/** ver Oct 2013
	 * Self-ID in the current table.
	 * 
	 * 64-bit long
	 */
	public final long id;
	
	/** ver Oct 2013
	 * Record type, provides a hint to the analyzer:
	 * 	•	0: normal instruction
	 * 	•	1: software interrupt (except alternate system call)
	 * 	•	2: 32-bit fast system call (syscall/sysenter)
	 * 	•	3: 64-bit fast system call (syscall/sysenter)
	 * 	•	4: alternate system call (int 0x2e on Windows)
	 * 	•	5: WOW64 system call
	 * 	•	6: other unknown system call
	 * 	•	8: NOP
	 * 		o	There are several ways with various mnemonics to encode a “no operation”. All of
	 * 			them have this flag set.
	 * 	•	9: Prefetch
	 * 		o	Prefetched values are not recorded by the tracer and corresponding execution record
	 * 			operand values are likely to contain garbage.
	 * 
	 * 8-bit int
	 */
	public final int type;
	
	/** ver Oct 2013
	 * 1 if the instruction was executed from a “long mode” 64-bit code segment, 0 otherwise. 
	 * 
	 * 1-byte boolean (zero extended)
	 */
	public final boolean csIs64Bits;
	
	/** ver Oct 2013
	 * Number of valid bytes in CodeBytes array. 
	 * 
	 *  8-bit int
	 */
	public final int codeSize;
	
	/** ver Oct 2013
	 * 
	 * 1-byte pad
	 */
	//	byte pad1; // 1 byte pad to ignore.
	
	/** ver Oct 2013
	 * Module ID (in Module table) this instruction is from, or -1 if this instruction
	 * was from floating code in memory.
	 * 
	 * 32-bit int (spec incorrectly stated as 4-bit int, updated in Feb 2015 pec)
	 */
	public final int moduleId;
	
	/** ver Oct 2013
	 * Address where this instruction was found (zero-extended on 32-bit applications). Note that
	 * addresses are not unique:
	 * 	•	Instructions can be instrumented more than once.
	 * 	•	Different code can be loaded/unloaded at a given address.
	 *
	 * 64-bit long (spec incorrectly stated as 8-bit int, updated in Feb 2015 pec)
	 */
	public final long address;
	
	/** ver Oct 2013
	 * Actual bytes of the instruction. Only the first CodeSize bytes are valid.
	 * 
	 * NB Currently not allocated as a buffer, bytes are skipped, so as to avoid
	 * memory wastage when we are not actually using the values therein.
	 * Change to read if needed, and try clearing the buffer after usage if it is
	 * not needed repeatedly.
	 * 
	 *  15-byte buffer
	 */
	public final ByteBuffer codeBytes;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 1-byte pad
	 */
	// byte pad; // 1 byte pad to ignore.
	
	/** ver Oct 2013
	 * Disassembly string of instruction, Intel style. FP is relative to ExtraFP in the file header.
	 * 
	 * This is a pointer to a CStr
	 * 
	 * 64-bit long
	 */
	public final long disasmFp;
	
	/** ver Oct 2013
	 * Fully decoded instruction. FP is relative to ExtraFP in the file header. See decodedIns below.
	 * 
	 * This is a pointer to decodedIns, which we can parse into a {@link DecodedIns} instance.
	 * 
	 * 64-bit long
	 */
	public final long decodedFp;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 8-byte pad
	 */
	// byte[] pad3; // 8 byte pad to ignore.
	
}
