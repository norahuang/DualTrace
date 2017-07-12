package ca.uvic.chisel.atlantis.bytecodeparsing.execution;


/** ver Oct 2013
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
 */

public enum ExecFlags {
	INS_64_BIT_MODE(0x0),
	
	INSTRUMENTATION_OFF(0x2),
	NON_EXECUTED_CONDITIONAL_INS(0x4),
	
	FIRST_ITERATION_INS(0x8),
	
	EXCEPTION_INPUT_OPERANDS(0x10),
	EXCEPTION_DURING_EXECUTION(0x20),
	EXCEPTION_OUPUT_OPERANDS(0x40),
	
	EXCEPTION_INTERNAL_TRACER_ERROR(0x80);
		
	public int number;
	
	ExecFlags(int hex){
		this.number = hex;
	}

	public boolean matchesExecFlag(int number){
		return this.number == (this.number & number);
	}

}

