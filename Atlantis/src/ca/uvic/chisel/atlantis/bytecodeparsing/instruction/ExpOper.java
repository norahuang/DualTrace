package ca.uvic.chisel.atlantis.bytecodeparsing.instruction;

import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames;


/** ver Oct 2013
 * description of an instruction explicit operand 
 */
public class ExpOper {
	
	public LcSlot beforeSlotObject;
	
	public LcSlot afterSlotObject;
	
	/**
	 * Value of register or memory operand before the operation
	 */
	public String regMemoryBeforeValueHexString;
	
	/**
	 * Value of register or memory operand after the operation,
	 * if it was not a read-only operand
	 */
	public String regMemoryAfterValueHexString;

	public ExpOper(InsItable parentParser) {
		long byteLocation = parentParser.getCurrentBitAddress()/8;
		this.regName = parentParser.getNextMiniInt(16);
		
		this.size = parentParser.getNextMiniInt(16);
		
		this.flags = parentParser.getNextMiniInt(8);
		
		this.beforeSlot = parentParser.getNextMiniInt(8);
		
		this.afterSlot = parentParser.getNextMiniInt(8);
		
		this.beforeOffset = parentParser.getNextMiniInt(8);
		
		this.afterOffset = parentParser.getNextMiniInt(8);
		
		// 3 byte pad to ignore.
		parentParser.skipPadBytes(3);
		
		//-----------------------------
		// Fetch register name
		this.regNameString = RegisterNames.lookupRegistryNumberReference(this.regName);

	}
	
	/** ver Oct 2013
	 * Register name (from Intel XED, see section 14.1) or one of the special
	 * values (see top of this section on page 22). May be different than
	 * RegName of the corresponding before/after slots since the latter may
	 * refer to a larger enclosing register. For instance, in mov al,[ebx+eax],
	 * the first explicit operand may refer to an input slot that contains
	 * eax (no need to have al as a separate input slot).
	 * 
	 * 16-bit int
	 */
	public final int regName;
	
	public final String regNameString;
	
	/** ver Oct 2013
	 * Operand value size in bytes. Lesser or equal to ValueSize of the
	 * corresponding before/after slots. Even for memory operands, this is
	 * the size of the value in memory, not counting the address itself stored
	 * in the execution record. When RegName is AGEN, this is the size of
	 * the computed address.
	 * Note: while Size is most often a register size (up to 32 bytes for
	 * YMM registers), some instructions may have memory operands of about
	 * 600 bytes (fxrstor and relatives for instance).
	 * 
	 * 16-bit int
	 */
	public final int size;
	
	/** ver Oct 2013
	 * Combination of at least one of
	 * 	•	0x01: operand is read
	 * 	•	0x02: operand is written
	 * 
	 * 8-bit int
	 */
	public final int flags;
	
	/** ver Oct 2013
	 * LC slot index for this operand “before execution” value. Never SLOT_NA.
	 * 
	 * 8-bit int
	 */
	public final int beforeSlot;
	
	/** ver Oct 2013
	 * LC slot index for this operand “after execution” value. SLOT_NA for read-only operands.
	 * 
	 * 8-bit int
	 */
	public final int afterSlot;
	
	/** ver Oct 2013
	 * Byte offset to add to LCSlots[BeforeSlot].Offset to get the actual offset, in
	 * the execution record local context, where the operand “before execution” value
	 * is stored. For register operands, this is always 0, except for ah, bh, ch dh
	 * registers and only if the corresponding slot refers to a larger enclosing
	 * register (for instance, operand is ah but slot refers to ax, hence an additional
	 * offset of 1). For memory operands, this is always 8 since a 64-bit address is
	 * stored before the actual memory value, even on 32-bits traces.
	 * 
	 * 8-bit int
	 */
	public final int beforeOffset;
	
	/** ver Oct 2013
	 * Same as BeforeOffset, for “after execution” operand value.
	 * 
	 * 8-bit int
	 */
	public final int afterOffset;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 3-byte pad
	 */
	 // byte[] pad; // 3 byte pad to ignore.
}