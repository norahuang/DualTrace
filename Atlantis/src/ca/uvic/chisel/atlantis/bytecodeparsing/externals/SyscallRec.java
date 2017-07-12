package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.bytecodeparsing.externals.SyscallOffsets.OffsetSizePair;

public class SyscallRec {
	
	public SyscallRec(SyscallVtable parentParser, long syscallNumber) {
		// There are this.recordCount of these, and are all direct addressable via the offset file.
		OffsetSizePair offsetAndSizeForLineNumber = parentParser.syscallOffsets.getSyscallOffsetSizePairFromSyscallNumber(syscallNumber);
		// Note that the record size is only relevant if we are reading the
		// array of argsFp and extrasFp. Everything before those elements is of a fixed size or size determined otherwise.
		
		// The offset file happens to have an extra entry at the very end for precisely this porpoise. Fintastic!
		parentParser.seekToByteAddress(offsetAndSizeForLineNumber.offset);
		
		// Parse it all!
		this.rawId = parentParser.getNextMiniInt(16);
		
		this.uniformId = parentParser.getNextMiniInt(16);

		this.convention = parentParser.getNextMiniInt(8);

		this.completed = parentParser.getNextBooleanByte();

		this.argCount = parentParser.getNextMiniInt(8);

		this.extraCount = parentParser.getNextMiniInt(8);

		this.beginEId = parentParser.getNextLong();

		this.endEId = parentParser.getNextLong();

		this.beforeSp = parentParser.getNextLong();

		this.result = parentParser.getNextLong();
		
		this.argsFp = parentParser.getNextLong();
		
		long originalSeekPosition = parentParser.getCurrentBitAddress();
		parentParser.seekToByteAddress(this.argsFp);
		this.args = new ArrayList<Long>(this.argCount);
		for(int i = 0; i < this.argCount; i++){
			this.args.add(parentParser.getNextLong());
		}
		parentParser.seekToBitAddress(originalSeekPosition);
	}

	/** ver Oct 2013
	 * OS version-specific function ordinal. Relevant only when combined with the OS version the trace was created on.
	 * 
	 * 16-bit int
	 */
	public final int rawId;
	
	/** ver Oct 2013
	 * OS version-independent kernel function ID. See section 8.1 above.
	 * 
	 * 16-bit int
	 */
	public final int uniformId;

	/** ver Oct 2013
	 * System call convention used to make that call:
	 * 	•	0: Unknown 
	 * 	•	1: 32-bit fast system call (syscall/sysenter)
	 * 	•	2: 64-bit fast system call (syscall/sysenter)
	 * 	•	3: alternate system call (int 0x2e on Windows)
	 * 	•	4: WOW64 system call
	 * 	•	5: software interrupt (int instruction, except alternate system call)
	 * 
	 * 8-bit int
	 */
	public final int convention;

	/** ver Oct 2013
	 * 1 if the system call returned like a normal function (EndEID is valid),
	 * 0 otherwise. In the latter case, either the call never completed before
	 * the end of the thread or trace (EndEID = -1) or its exit was shortcut by
	 * any kind of context jump (EndEID is valid). See system call related
	 * execution in section 6.3. 
	 * 
	 * 1-bit boolean
	 */
	public final boolean completed;

	/** ver Oct 2013
	 * Number of arguments in array pointed to by Args. Always 4 for 64-bit calls
	 * with an unknown argument count between 0 and 4.
	 * 
	 * 8-bit int
	 */
	public final int argCount;

	/** ver Oct 2013
	 * Number of “extra” items in array pointed to by ExtrasFP. Undocumented, see section 8.2 above.
	 * 
	 * 8-bit int
	 */
	public final int extraCount;

	/** ver Oct 2013
	 * System call entry execution record ID.
	 * 
	 * 64-bit long
	 */
	public final long beginEId;

	/** ver Oct 2013
	 * System call exit execution record ID. It is -1 if the thread was
	 * still in the context of the call when it stopped (or where the tracer
	 * stopped recording). Otherwise, it is either a normal (CF-wise) system
	 * call execution record, or a skipped system call exit (Completed = 0, see
	 * system call related execution in section 6.3).
	 * 
	 * 64-bit long
	 */
	public final long endEId;

	/** ver Oct 2013
	 * Value of stack pointer (esp/rsp) before the call. Zero-extended for 32-bit traces.
	 * 
	 * 64-bit long
	 */
	public final long beforeSp;

	/** ver Oct 2013
	 * Return value in eax/rax at exit point, zero-extended for 32-bit traces. Invalid (garbage) if Completed = 0.
	 * 
	 * 64-bit long
	 */
	public final long result;
	
	// Some unneeded elements...

	/** ver Oct 2013
	 * FP on the array of ArgCount argument values. FP is relative to the start of
	 * this syscallRec record. Arguments of 32-bit calls are zero-extended to 64 bits.
	 * Arguments are in left-to-right order of exported kernel function prototypes.
	 * 
	 * Number of elements in the argument buffer is 'argCount'.
	 * 64-bit longs
	 */
	public final long argsFp;
	
	 /**
	  * Parsed arguments based on the argsFp pointer, of length argsCount.
	  */
	public final List<Long> args;
	 
	// This one is particularly unneeded:
	/** ver Oct 2013
	 * FP on the array of ExtraCount “extras” items. FP is relative to the start of this syscallRec
	 * record. Undocumented, see section 8.2 above.
	 * 
	 * The extrasFp is present, but can only be useful or interpreted at trace time.
	 * The data is there, we skip it, so don't even bother parsing it.
	 * 
	 * An OS trace-time-only bound object.
	 * 64-bit long
	 */
	// long extrasFp;
	
}
