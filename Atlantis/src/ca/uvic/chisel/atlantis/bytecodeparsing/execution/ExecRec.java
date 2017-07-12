package ca.uvic.chisel.atlantis.bytecodeparsing.execution;

import java.util.ArrayList;

import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ThreadItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ThreadRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.LcSlot;

public class ExecRec {
	
	// For debugging.
	static long lastOffset = 0;
	// For debugging.
	static long lastLocalSize = 0;
	
	public final long lineNumber;
	
	// If there are too many functions of this type, just dipping in to grab a datum, it might be more efficient
	// to get the whole structure and cache it.
	/**
	 * Get the execution mode for the line requested, without retrieving additional {@link ExecRec} data.
	 * @return
	 */
	static public boolean isExecutionMode64Bit(ExecVtable parentParser, long lineNumber){
		// There are this.recordCount of these, and are all direct addressable via the offset file.
		Long offsetForLineNumber = parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber);
		// Skip ahead to the third field, flags field, by jumping over the threadID 32b int, and the type 8b int.
		int skipFieldsWidth = 32 + 8;
		// The offset file happens to have an extra entry at the very end for precisely this porpoise. Fintastic!
		parentParser.seekToByteAddress(offsetForLineNumber + skipFieldsWidth);

		int flags = parentParser.getNextMiniInt(8);
		return (flags & 0x1) == FlagValues.flag64BitInstruction.hexIntValue;
	}

	ExecRec(ExecVtable parentParser, InsItable insItable, ThreadItable threadItable, long lineNumber){
		this.lineNumber = lineNumber;
		// There are this.recordCount of these, and are all direct addressable via the offset file.
		Long offsetForLineNumber = parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber);
		// The offset file happens to have an extra entry at the very end for precisely this porpoise. Fintastic!
		parentParser.seekToByteAddress(offsetForLineNumber);
		
		this.parentParser = parentParser;
		
		this.threadId_InThreadITable = parentParser.getNextInt();
		this.typeRawNumber = parentParser.getNextMiniInt(8);
		this.type = ExecLineType.getEnum(this.typeRawNumber);


		// Note that the flag is a logical value, not a numeric one.
		// Multiple flags can be set, and we need to deal with those individually.
		this.flags = parentParser.getNextMiniInt(8);
		
		long bitAddress = parentParser.getCurrentBitAddress();
		if(bitAddress % 8 != 0){
			// Shouldn't happen, right?
			System.out.println("ExecRec dataFp not byte aligned");
		}
		
		this.dataFpByteAddress = bitAddress / 8;

		if(this.type == ExecLineType.INSTRUCTION){
			this.insId = parentParser.getNextLong();
			this.dataFpByteAddressLocalContext = parentParser.getCurrentBitAddress()/8;
			this.insRec = insItable.getInsRec(this);
			
			// For debugging.
			lastLocalSize = (parentParser.getCurrentBitAddress()/8 - offsetForLineNumber) + this.insRec.decodedIns.lcSize;

		} else if(this.type == ExecLineType.THREAD_BEGIN){
			this.threadBeginContext = new RmContext(parentParser, this.type, parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber+1));
			this.threadRec = threadItable.getThreadRec(this.threadId_InThreadITable);
			// The threadRec.tibAddr happens to point at the location that the memory reference does.
			// This indicates that there is not a memory context here, but only the TIB (thread information block).
			// We aren't using it yet, so don't do anything with it.
			// See http://en.wikipedia.org/wiki/Win32_Thread_Information_Block
			// Note that this also means that we don't want to print it to the textual trace.

		} else if(this.type == ExecLineType.THREAD_END){
			this.threadExitCode = parentParser.getNextInt();
			this.threadRec = threadItable.getThreadRec(this.threadId_InThreadITable);

		} else if(this.type == ExecLineType.APP_END){
			this.appExitCode = parentParser.getNextInt();

		} else if(this.type == ExecLineType.SYSCALL_ENTRY){
			this.syscallEnterId = parentParser.getNextLong();
			this.syscallEntryContext = new RmContext(parentParser, this.type, parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber+1));

		} else if(this.type == ExecLineType.SYSCALL_EXIT){
			this.syscallExitId = parentParser.getNextLong();
			this.sysCallExitContext = new RmContext(parentParser, this.type, parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber+1));

		} else if(this.type == ExecLineType.SKIPPED_SYSCALL_EXIT){
			this.syscallSkippedId = parentParser.getNextLong();

		} else if(this.type.number >= 7 && this.type.number <= 11){
			this.contextChange = new RmContext(parentParser, this.type, parentParser.execOffsets.getExecOffsetFromLineNumber(lineNumber+1));
			this.threadRec = threadItable.getThreadRec(this.threadId_InThreadITable);
		}
		
		// For debugging.
		lastOffset = parentParser.getCurrentBitAddress()/8;

	}
	
	public ThreadRec threadRec;
	
	/** ver Oct 2013
	 * Thread ID (in Thread table) this record belongs to.
	 * 
	 * Do not use as an informative OS related thread ID, that comes from {@link ThreadRec#winTid}. I extended the name of this
	 * to make it less of a bug attractor.
	 * 
	 * 32-bit int
	 */
	public final int threadId_InThreadITable;
	
	/** ver Oct 2013
	 * Trace line type (e.g. Begin, Ins, End)
	 *
	 * Fields in execRec.Data depend on execRec.Type. The following table list all possible types, their meaning and the fields found in execRec.Data. Some fields are structures and those structure types are defined after the table. Multiple fields, if there are, are always packed without any padding. 
	 * 
	 * {
	 *  Type	Name
	 * 	[Description]
	 * 	Field type	Field name 
	 *  Field description
	 * }
	 *
	 * 0	Instruction 
	 * 	i64	InsID	Corresponding instruction record ID in Instruction table
	 * 	buf[?]	OperVals	Values of all instruction operands, both implicit and explicit. Provides the instruction “local context” (see 6.1.1 What’s in an execution record). The size and data layout of the buffer is given in the instruction record. 
	 *
	 * 1	Thread begin
	 * Exactly once for each thread, before any other record for that thread.
	 * 	rmContext	Context	Registers context at the thread start point, before the first user-mode instruction, as well as the thread environment block (TEB).
	 *
	 * 2	Thread end 
	 * Exactly once for each thread that terminated before the traced application exited. This is the last record for a thread. 
	 * 	i32	ExitCode	Windows thread exit code
	 *
	 * 3	Application end 
	 * The very last record of the trace. Present only if the traced application exited gracefully. 
	 * 	i32	ExitCode	Windows process exit code
	 *
	 * 4	System call entry
	 * Thread is about to enter kernel mode through one of the various kind of system calls. Typically follows a syscall/sysenter/int 2e instruction record. 
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
	 * 	rmContext	Context	Value of all registers just before the call. Additional information, such as call parameters on the stack, is in the System calls table.
	 *
	 * 5	System call exit 
	 * Thread just exit from kernel mode system call. Does not always immediately follow a system call entry record, and may not even match an entry at all (matching is done according to the stack pointer). Actually, system calls can be nested, involve user-mode callbacks, and can end on an exception or a context change. Also, some system calls, such as NtContinue, can shortcut a few system call exits at once.
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table, or -1 if there is no corresponding system call entry record (thus nothing in System calls table either). The latter case is semantically equivalent to a context change.
	 * 	rmContext	Context	Value of all registers right after the call. Additional information may be found in the System calls table.
	 *
	 * 6	Skipped system call exit 
	 * A previous system call entry occurred, but starting from this record, the stack was unwound further than it would be on a normal system call exit (a “return” to the user-mode caller). In other words, the thread is not anymore in the context of that system call, and that call will never have a “System call exit” record.
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
	 * 
	 *  8	Context change – Asynchronous Procedure Call 
	 *  9	Context change – Exception Handling 
	 *  10	Context change – Callback 
	 *  11	Context change – Unknown reason 
	 * For one of the above reasons, the thread execution context just changed. 
	 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
	 * 
	 * 8-bit int enumeration
	 */
	public final ExecLineType type;
	public final int typeRawNumber;
	
	
	/** ver Feb 2015
	 * 
	 * Changes to the flags are only in 0x10 to 0x80, skip there to see 'version 0' and 'version 1' specifications.
	 * 
	 * execRec.Flags can be a combination of the following values:
	 * - 0x01: 64-bits. Instruction or special event occurred within the context of a 64-bit mode (aka
	 * long-mode) code segment. 32-bit otherwise.
	 * - 0x02: Instruction instrumentation was OFF before execution of this record (see 1 Introduction).
	 * When set, context values (register values and perhaps memory references) from previous
	 * records cannot be assumed to be correct for the current record. However, context values in
	 * current record data are valid for that record, and can be propagated to the next record if it does
	 * not have this flag set.
	 * - 0x04: non-executed conditional instruction. Applies to the following conditional instructions:
	 * cmovcc, fcmovcc, jcc (conditional jumps) and repcc-prefixed instructions. This flag is set when
	 * the condition cc was false and the remaining of the instruction was not carried on. All input and
	 * (unchanged) output operand values nevertheless appear in the current record data.
	 * - 0x08: set on the first iteration of a repcc-prefixed instruction.
	 * 0x10 to 0x80: one or more of those flags are set when the instruction could not be completely
	 * traced, either because it caused a low-level exception/fault or the application abruptly
	 * terminated (processed killed). Although input and output operands values appear in the current
	 * record, they may not be reliable. Meaning of those flags depends on Execution record
	 * version:
	 * 
	 * Version 0
	 * - 0x10: exception occurred while recording input operands. Both input and
	 * output operands values contain garbage.
	 * - 0x20: exception occurred during instruction execution. Input values are
	 * properly recorded, but output values are not.
	 * - 0x40: exception occurred when tracing output memory operands. For some
	 * reason, although the instruction executed fine, output operands could not be
	 * read back to be recorded.
	 * - 0x80: internal error, probably a bug in the tracer tool.
	 * 
	 * Version 1
	 * - 0x10: inputs are not reliable. Most often, register values are fine, but memory
	 * operand values are probably not. This includes pre-execution value of outputonly
	 * operands.
	 * - 0x20: outputs are not reliable. Can’t tell whether output operand values were
	 * fully, partially or not at all written to the trace.
	 * - 0x40: instruction triggered an exception/fault. Access violation is probably the
	 * most common cause.
	 * - 0x80: aborted process. Traced application was abruptly terminated while a
	 * thread was around this instruction (about to execute it, executing it, or just done
	 * with it).
	 * 
	 * Documentation from older version
	 * ver Oct 2013
	 * Combination of bit flags, see below
	 * 
	 * execRec.Flags can be a combination of the following values:
	 * •	0x01: 64-bits. Instruction or special event occurred within the context of a 64-bit mode (aka long-mode)
	 * 			code segment. 32-bit otherwise.
	 * •	0x02: Instruction instrumentation was OFF before execution of this record (see 1 Introduction). When set,
	 * 			context values (register values and perhaps memory references) from previous records cannot be assumed to be correct for the current record. However, context values in current record data are valid for that record, and can be propagated to the next record if it does not have this flag set.
	 * •	0x04: non-executed conditional instruction. Applies to the following conditional
	 * 			instructions: cmovcc, fcmovcc, jcc (conditional jumps) and repcc-prefixed instructions. This flag is set when the condition cc was false and the remaining of the instruction was not carried on. All input and (unchanged) output operand values nevertheless appear in the current record data. 
	 * •	0x08: set on the first iteration of a repcc-prefixed instruction.
	 * •	0x10 to 0x80: set when a low-level exception occurred during instruction execution, such as a memory access violation.
	 * 			Flags do not reveal the kind of exception but tell when it happened.
	 * 			Also, since the instruction and/or associated trace recording code did not complete, input and/or output
	 * 			values were not recorded either (although space was reserved for them in the record data, which then contains garbage). 
	 * 				o	0x10: occurred while recording input operands. Both input and output operands values contain garbage.
	 * 				o	0x20: occurred during instruction execution. Input values are properly recorded, but output values are not.
	 * 				o	0x40: occurred when tracing output memory operands. For some reason, although the instruction executed fine, output operands could not be read back to be recorded.
	 * 				o	0x80: internal error, probably a bug in the tracer tool. 
	 * 
	 * 8-bit int
	 */
	public final int flags;
	
	/**
	 * Type-dependent data, see type descriptions below.
	 * 
	 * The FP is needed for LcSlots, to retrieve values. See {@link LcSlot#getMemoryValue(InsItable, ca.uvic.chisel.atlantis.bytecodeparsing.instruction.ExpOper, boolean)}
	 * for implementation.
	 * 
	 * Note that {@link ExecRec#dataFpByteAddressLocalContext} is to be used isntead. The dataFp is only used by instructions,
	 * and the dataFp always contains an insId, that once it is read, must be accounted for and its bytes skipped
	 * for all further parsing of local context data.
	 */
	private final long dataFpByteAddress;
	
	/**
	 * This is a convenience/documentation member. The {@link ExecRec#dataFpByteAddress} has the InsId (line number) prior to the
	 * local context data. This can be easy to forget when calling on this data block for parsing.
	 * The spec describes the canonical member as the one pointing to just before the *possible* InsId.
	 */
	public Long dataFpByteAddressLocalContext = null;
	
	/**
	 * Only needed to be used by {@link LcSlot#getMemoryValue(InsItable, ca.uvic.chisel.atlantis.bytecodeparsing.instruction.ExpOper, boolean)}
	 * in conjunction with the {@link ExecRec#dataFpByteAddressLocalContext}.
	 */
	public final ExecVtable parentParser;
	
	static public enum FlagValues {
		flag64BitInstruction(0x01, "64bit"),
		flagInstrumentationOff(0x02, "InstrOff"),
		flagNonExecutedConditional(0x04, "NonExecutedCond"),
		flagREPccPrefixedInstruction(0x08, "REPcc-prefix"),
		flagInvalidBecauseErrorRecordingInputOperands(0x10, "Invalid:FailInput"),
		flagInvalidBecauseErrorDuringExecution(0x20, "Invalid:FailExec"),
		flagInvalidBecauseErrorTracingOutputOperands(0x40, "Invalid:FailOutput"),
		flagInvalidBecauseInternalTraceToolBug(0x80, "Invalid:TraceBug");
		
		static ArrayList<FlagValues> allFlags = new ArrayList<FlagValues>();
		static {
			if(FlagValues.allFlags.size() == 0){
				FlagValues.allFlags.add(FlagValues.flag64BitInstruction);
				FlagValues.allFlags.add(FlagValues.flagInstrumentationOff);
				FlagValues.allFlags.add(FlagValues.flagNonExecutedConditional);
				FlagValues.allFlags.add(FlagValues.flagREPccPrefixedInstruction);
				FlagValues.allFlags.add(FlagValues.flagInvalidBecauseErrorRecordingInputOperands);
				FlagValues.allFlags.add(FlagValues.flagInvalidBecauseErrorDuringExecution);
				FlagValues.allFlags.add(FlagValues.flagInvalidBecauseErrorTracingOutputOperands);
				FlagValues.allFlags.add(FlagValues.flagInvalidBecauseInternalTraceToolBug);
			}
		}
		
		public final int hexIntValue;
		public final String humanReadable;
		
		private FlagValues(int hexIntValue, String humanReadable){
			this.hexIntValue = hexIntValue;
			this.humanReadable = humanReadable;
		}
		
		static public String emitHumanReadable(int hexValue){
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Flags Present (0x"+Integer.toHexString(hexValue)+"): ");
			boolean needComma = false;
			for(FlagValues val: allFlags){
				if(val.hexIntValue == (hexValue & val.hexIntValue)){
					if(needComma){
						stringBuilder.append(", ");
					}
					stringBuilder.append(val.humanReadable);
					needComma = true;
				}
			}
			
			// Cheeky...need a comma only if we added a flag value.
			if(needComma){
				return stringBuilder.toString();
			} else {
				return "";
			}
		}
	}

	
	/** ver Oct 2013
	 * Type-dependent data, see type descriptions below
	 * 
	 * Exec rec data is actually polymorphic on Type and Flags.
	 */
	
	/** ver Oct 2013
	 * Corresponding instruction record ID in Instruction table
	 * 
	 * Type 0: Instruction
	 * 
	 * If("execLineType==0")
	 * 64-bit long
	 */
	public long insId;
	
	/** ver Oct 2013
	 * 
	 * [This strange method that returns {@link InsRec} is because of odd ordering of dependencies.
	 * The method is meant to highlight this, and direct toward where the operVals are actually located.]
	 * 
	 * Values of all instruction operands, both implicit and explicit. Provides the
	 * instruction “local context” (see 6.1.1 What’s in an execution record). The size
	 * and data layout of the buffer is given in the instruction record. 
	 * 
	 * If("execLineType==0") 
	 *
	 * 
	 * If operVals are needed, they were parsed by the child instruction record (insRec),
	 * as that is where the structural information is all contained.
	 * 
	 * 
	 */
	InsRec operVals(){
		// Used to have (incorrect) access to: public List<LcSlotParsedData> operVals;
		return this.insRec;
	}
	public InsRec insRec;
	
	
	/*

	 */
	/** ver Oct 2013
	 * Registers context at the thread start point, before the first user-mode instruction, as well as the thread environment block (TEB).
	 * 
	 * Type 1: Thread begin
	 * Exactly once for each thread, before any other record for that thread.
	 * 
	 * If("execLineType==1")
	 */
	public RmContext threadBeginContext;

	
	/**
	 * Type 2: Thread end
	 * Exactly once for each thread that terminated before the traced application exited. This is the last record for a thread.
	 * 
	 *  If("execLineType==2")
	 *  32-bit int
	 */
	public int threadExitCode;

	/** ver Oct 2013
	 * Windows process exit code
	 *
	 * Application end
	 * The very last record of the trace. Present only if the traced application exited gracefully. 
	 *
	 * If("execLineType==3")
	 * 32-bit int
	 */
	public int appExitCode;
	
	/*

	 */
	/** ver Oct 2013
	 * Corresponding system call record ID in System calls table.
	 * 
	 * System call entry
	 * Thread is about to enter kernel mode through one of the various kind of system calls.
	 * Typically follows a syscall/sysenter/int 2e instruction record. 
	 * 
	 * If("execLineType==4")
	 * 64-bit long
	 */
	public long syscallEnterId;
	
	/** ver Oct 2013
	 * Value of all registers just before the call. Additional information,
	 * such as call parameters on the stack, is in the System calls table.
	 *
	 * If("execLineType==4")
	 */
	public RmContext syscallEntryContext;

	/** ver Oct 2013
	 * Corresponding system call record ID in System calls table, or -1 if there
	 * is no corresponding system call entry record (thus nothing in System calls
	 * table either). The latter case is semantically equivalent to a context change.
	 * 
	 *  System call exit
	 *  Thread just exit from kernel mode system call. Does not always
	 *  immediately follow a system call entry record, and may not even
	 *  match an entry at all (matching is done according to the stack pointer).
	 *  Actually, system calls can be nested, involve user-mode callbacks, and
	 *  can end on an exception or a context change. Also, some system calls,
	 *  such as NtContinue, can shortcut a few system call exits at once.
	 * 
	 * If("execLineType==5")
	 * 64-bit long
	 */
	public long syscallExitId;
	
	/** ver Oct 2013
	 * Value of all registers right after the call. Additional information may
	 * be found in the System calls table.
	 * If("execLineType==5")
	 */
	public RmContext sysCallExitContext;

	/** ver Oct 2013
	 * Corresponding system call record ID in System calls table.
	 * 
	 *  Skipped system call exit
	 *  A previous system call entry occurred, but starting from this record,
	 *  the stack was unwound further than it would be on a normal system call
	 *  exit (a “return” to the user-mode caller). In other words, the thread is
	 *  not anymore in the context of that system call, and that call will
	 *  never have a “System call exit” record.
	 * 
	 * If("execLineType==6")
	 * 64-bit long
	 */
	public long syscallSkippedId;
	
	// execLineType==7 doesn't exist in documentation
											
	/** ver Oct 2013
	 * Value of all registers right after the context change, before carrying execution of the next instruction.
	 * 
	 * Context change {
	 * 8: Asynchronous procedure call,
	 * 9: Exception handling,
	 * 10: Callback,
	 * 11: Unknown reason
	 * }
	 * For one of the above reasons, the thread execution context just changed. 
	 * 
	 * If("execLineType>7 && execLineType<=11")
	 */
	public RmContext contextChange;

	private String dissassemblyStringForErrors = null;
	public void setDissassemblyForErrors(String dissasembly) {
		this.dissassemblyStringForErrors = dissasembly;
	}
	
	public String getDissassemblyForErrors() {
		return this.dissassemblyStringForErrors;
	}

	
	
	
}
