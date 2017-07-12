package ca.uvic.chisel.atlantis.bytecodeparsing.instruction;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames.SpecRegs;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;


/** ver Oct 2013
 * local context slot, defines a portion of the local context data layout in an execution record
 * Size: 12
 */
public class LcSlot {
	
	public final ExecRec execRec;
	
	public final DecodedIns decodedInstruction;	
	
	/**
	 * Tells us whether the operand was before executing the instruction, and whether it was R/RW
	 */
	public final boolean isABeforeReadOrRWOperand;
	
	/**
	 * Tells us whether the operand was after executing the instruction (and therefore Write)
	 */
	public final boolean isAnAfterWriteOperand;
	
	/**
	 * Tells us whether the operand was before executing the instruction, and was Write only.
	 */
	public final boolean isABeforeWriteOnlyOperand;
	
	/**
	 * Register name (Intel). Only set if this is a register operand.
	 */
	public final String regNameString;
	
	/**
	 * Renamed from 'LargestEnclosing' to 'CommonRegName' in Feb 2015 spec revision.
	 */
	public final String regNameCommonRegNameString;
	
	/**
	 * This is set to null for MEM0, AGEN, and MEM1 special register names. 
	 * 64-bit address 16 character hex string. 16 hex characters (4 bits * 16 = 64 bits) Only set if this is a memory operand.
	 */
	public final String memoryLocationString;
	
	/**
	 * Comes from an 8 bit integer...but I also expected it to have the stored computed addresses??
	 */
	public final Long memoryLocation; // sums of phrase add up eventually, right?
	
	// Changed when this was parsed to include ExpOper before or after offset
	/**
	 * When an ExpOper is associated with the LcSlot, the beforeValue should have the same value
	 * as this. We need it for the LcSlots that have no ExpOper present.
	 */
	public final BigInteger specialAddressForMemoryPhraseSlots;
	public final String specialAddressForMemoryPhraseSlotsString;
	
	/**
	 * This is only used for MEM0, AGEN, and MEM1 special register names. 
	 * When the special register MEM0 occurs, we can compute the effective
	 * address of its memory phrase. This happens out of step with the parsing
	 * of the LCSlot, since it needs ExpOper information. It is therefore set late,
	 * and cannot be final. The parsing of ExpOpers happens *just* after* we parse
	 * LcSlots, and itself requires LcSlot data. Ther eis asome chance that
	 * the two could be combined into the LcSlot parsing.
	 */
	public BigInteger mem0EffectiveAddress = null;
	public String mem0MemoryPhrase = null;

	public ExpOper temporaryDeleteOwningExpOper = null;
	public ExpOper temporarySecondDeleteOwningExpOper = null;
	
	/**
	 * Default to 1, increment when it happens again. operations like xchg will lead to this.
	 * Mostly for debugging purposes.
	 */
	public int numberExpOpersRefering = 1;

	/**
	 * Trying to make LcSlots more primary, to ease traversal of arguments.
	 * We are forced to process some LcSlots without the ExpOpers, but these
	 * didn't help with that. Shall we deprecate these?
	 */
	private String expOperRegNameString;

	private String regMemoryBeforeValueHexString;

	private String regMemoryAfterValueHexString;
	
	/**
	 * Although LcSlot definitions are documented beneath the context map, the data appears in the
	 * exec vtable, in a variable sized buffer. This buffer is of interest to this class for parsing
	 * purposes, so we need both the InsRec and ExecVtable parent parsers.
	 * We only need the InsTable parent when first creating the LcSlot, but we need the ExecVtable when
	 * using the structure data to parse the exec vtable data buffer.
	 * 
	 * @param parentParser
	 * @param c 
	 * @param b 
	 */
	public LcSlot(InsItable insItableParentParser, DecodedIns decodedInstruction, boolean isABeforeReadOrRWOperand, boolean isAnAfterWriteOperand, ExecRec execRec) {
		
		this.execRec = execRec;
		
		this.decodedInstruction = decodedInstruction;
		
		this.isABeforeReadOrRWOperand = isABeforeReadOrRWOperand;
		this.isAnAfterWriteOperand = isAnAfterWriteOperand;
		this.isABeforeWriteOnlyOperand = !isABeforeReadOrRWOperand && !isAnAfterWriteOperand;
		
		this.regName = insItableParentParser.getNextMiniInt(16);
		
		this.commonRegName = insItableParentParser.getNextMiniInt(16);
		
		this.valueSize = insItableParentParser.getNextMiniInt(16);
		
		this.offset = insItableParentParser.getNextMiniInt(16);
		
		this.addrWidth = insItableParentParser.getNextMiniInt(8);
		
		// 3 byte pad to ignore.
		insItableParentParser.skipPadBytes(3);
		
		this.hasComputedAddress = (this.addrWidth > 0);
		
		// For non-registers, we get an invalid mock register name. As long as we aren't using this null property as
		// identifier of register status, that's fine.
//		System.out.println("Prior to Feb 2016 spec revision, the regname here appears to always be the same as the largest enclosing regname, even when the instruction and the expOper have smaller one (e.g. EDI sub to RDI). Maybe this only happens with instructions that extend (e.g. movx edi, al -> move, but extend...to 64-bit?)");
		this.regNameString = RegisterNames.lookupRegistryNumberReference(this.regName);
		this.regNameCommonRegNameString = RegisterNames.lookupRegistryNumberReference(this.commonRegName);

		
		// ------------------------
		// Get referenced values...
		// Note that we are using the ExecRec parser object, not the LcSlot one.
		// I could have put this code in the ExecRec class, but it's half a dozen of one and six of the other
		// with regards to breaking encapsulation; the data resides in the purview of another entity, so it
		// is an inevitable effect.
		
		long originalBitAddress = execRec.parentParser.getCurrentBitAddress(); //31652704 bit is 3956580 byte
		
		// There is an InsId (line number) prior to actual data at the dataFpByteAddress
		execRec.parentParser.seekToByteAddress(this.offset + execRec.dataFpByteAddressLocalContext);
		long byteAddress = execRec.parentParser.getCurrentBitAddress()/8;
		
		// If the addrWidth is 0, then the entire size of the data at the offset is
		// equal to the valueSize. If it is not 0, then the addrWidth is the *actual stored width in bytes* of the
		// address in question. This means that it is not a Long, despite being an address.
		
		/*
		 * Email from Fred via contractor:
		 * Operands MEM0, MEM1, STACKPUSH or STACKPOP (names in lcSlot.RegName) are memory-based operands.
		 * Their actual linear address is, as you guessed, at offset "lcSlot.Offset" in the execution record.
		 * For the sake of simplicity, addresses are always stored as 64 bits values, although obviously only
		 * the first 32 bits are meaningful in a 32-bit trace (the number of meaningful bytes is given by
		 * lcSlot.AddrWidth). The operand value is stored immediately after the address in the execution
		 * record. Value size is given by lcSlot.ValueSize. The AGEN operand is a different beast. It is
		 * not memory-based (lcSlot.AddrWidth = 0) and it only has a "value" stored at offset "lcSlot.Offset"
		 * in the execution record, just like a register. This value is the computed result of the memory
		 * phrase and its width is given by lcSlot.ValueSize (same as corresponding expOper.Size in this case).
		 * If needed, individual memory phrase components are also stored like they are for MEM0 operands: each
		 * component is linked to a slot, and slots are listed in the various decodedIns.Mem0*** members. Also,
		 * as with MEM0 operands again, the offset portion of the memory phrase is in decodedIns.ConstSlot[0].
		 */
		
		// Commenting out because I think I am not allowed to access memory address or values without
		// the ExpOper offsets
		
		// Accessing the memory values without offsets works really well after all, once I account for the
		// InsId logn that preceeds the entire set of LcSlot entries.
		
		long memoryValueByteAddress = execRec.parentParser.getCurrentBitAddress()/8;
		
		// For registry values and AGEN, there will not be a leading long in front of the value like there is for memory operands.
		if(this.hasComputedAddress){
			// Get both the memory address and the value from the data pointed to by the offset.
			// Want it do be 16 hex digits in string form.
			// The address is of course a 64-bit value, immediately preceding the value.
			// Feedback to some questions about memoryLocation led to: "For the sake of simplicity, addresses are always stored as 64 bits values"
			// Why are significant bits used with a long that could just stay long?
			// int leftOverWidth = 64 - this.addrWidth * 8;
			// Despite the addrWidth part, we get the same value parsing just part vs a whole 64 bites.
			this.memoryLocation = execRec.parentParser.getNextLong();
			this.memoryLocationString = BinaryFormatParser.toHex(this.memoryLocation, 8);
		} else {
			// MM0 memory phrases do not use this memory location, it appears.
			this.memoryLocation = null;
			this.memoryLocationString = null;
		}

		// For MEM0, MEM1, STACKPUSH, STACKPOP, and AGEN, this memory value is here (being either right at this.offset
		// or one Long read after that, as per conditional above). But...not-quite-contradictory facts come from
		// the documentation regarding before and after slot status as determined by owning ExpOper objects.
		// For AGEN (according to a clarifying email):
		// "This value is the computed result of the memory phrase and its width is given by lcSlot.ValueSize
		// (same as corresponding expOper.Size in this case)."
		// Need to grab this for the cases where there is no corresponding ExpOper to provide before and after offset values.
		// Likely invalid for when the ExpOper is available.
		
		ByteBuffer specialValue = execRec.parentParser.getNextChunkOfBytes(this.valueSize);
		// Less fuss to do string then number. Slower likely.
		// Looked into whether this value can have a width less than valueSize. Documentation didn't offer me a hint.
		// I don't know if we need a subvalue like happens with subregisters.
		// RFLAGS reserve here is only for debug, never commit true
		if(this.regNameString.toLowerCase().contains("flags")){
			this.specialAddressForMemoryPhraseSlotsString = BinaryFormatParser.toHex(specialValue, false);
		} else {
			this.specialAddressForMemoryPhraseSlotsString = BinaryFormatParser.toHex(specialValue);
		}
		this.specialAddressForMemoryPhraseSlots = new BigInteger(specialAddressForMemoryPhraseSlotsString, 16);
		
		// try clearing to improve memory usage
		specialValue.clear();
		
		execRec.parentParser.seekToBitAddress(originalBitAddress);
	}
	
	/**
	 * Update some properties on the owning {@link ExpOper}. Since {@link LcSlot} objects are meant to be used for
	 * multiple {@link ExpOper}, this method should never change any properties on the {@link LcSlot} in question. 
	 * 
	 * @param parentParser
	 * @param owningExpOper
	 * @param beforeSlot
	 */
	public void getMemoryValue(InsItable insItableParentParser, ExpOper owningExpOper, boolean beforeSlot){
		long originalBitAddress = execRec.parentParser.getCurrentBitAddress();
		
		if(null == this.temporaryDeleteOwningExpOper){
			this.temporaryDeleteOwningExpOper = owningExpOper;
		} else {
			this.temporarySecondDeleteOwningExpOper = owningExpOper;
		}
		
		this.expOperRegNameString = owningExpOper.regNameString;
		if(beforeSlot){
			owningExpOper.beforeSlotObject = this;
		} else {
			owningExpOper.afterSlotObject = this;
		}
		
		// regMemoryBefore/AfterValueHexString can be register or memory values.
		// From spec: "For instance, in mov byte ptr [eax],al, a single slot will be allocated for eax
		// with a value size of 4, while the second explicit operand will point to that slot but will
		// specify a size of 1."
		
		// IMPORTANT Values for before and after operands are used within the same LcSlot potentially, and
		// that to get the values you need to look up with the offset from the before and after ExpOper
		// object. So, there are two values.
		// I would attach these values to the LcSlot rather than making before and after ones on the expOper,
		// but the expOper already has beforeSlot and afterSlot, and the LcSlot *could* be re-used, and thus
		// would itself require the before and after value to be put on it. These two objects are very tightly
		// involved with each other.
		
		long expOperOffset = beforeSlot ? owningExpOper.beforeOffset : owningExpOper.afterOffset;
		// These offsets are i8 values, so it's safe to add them.
		// the base needs to be the position of execRec.data *per execRec*.
		// There is an InsId (line number) prior to actual data at the dataFpByteAddress
		long byteAddress = expOperOffset + this.offset + execRec.dataFpByteAddressLocalContext;
		// System.out.println("long "+byteAddress+" = "+expOperOffset + "+" +this.offset+"+" + execRec.dataFpByteAddressLocalContext);
		
		// Note that we are only getting values here. The before/afterOffset already accounts for the address
		// which we do not need nor want to re-parse. We have already parsed the address in the LcSlot
		// constructor, and the address will not differ between before and after.
		execRec.parentParser.seekToByteAddress(byteAddress);
		
		ByteBuffer subBuffer = execRec.parentParser.getNextChunkOfBytes(this.valueSize);
		String valueHexString = BinaryFormatParser.toHex(subBuffer);
		// try clearing to improve memory usage
		subBuffer.clear();
		// We already accounted for any subregister offset above (see expOperOffset, in computing the byteAddress),
		// but we still do need to ensure that the value we have is not larger than expected. 32-bit registers are
		// getting 64-bit width values (e.g. EDX getting a value of FFFFFFFFFFFFFF88 on one sample line).
		if(this.valueSize > owningExpOper.size){
			// multiply size * 2 to compensate for hex strings having nibble characters; size is bytes, hex chars are half a byte.
			valueHexString = valueHexString.substring(valueHexString.length() - 2 * owningExpOper.size, valueHexString.length());
		}

		
		if(beforeSlot){
			this.regMemoryBeforeValueHexString = valueHexString;
			owningExpOper.regMemoryBeforeValueHexString = valueHexString;
		} else {
			this.regMemoryAfterValueHexString = valueHexString;
			owningExpOper.regMemoryAfterValueHexString = valueHexString;
		}
		
		if(this.addrWidth != 0
			&& !RegisterNames.lookupRegistryNumberReference(this.regName).equals("?REG?")
			&& !RegisterNames.lookupRegistryNumberReference(this.regName).equals("MEM0")
			&& !RegisterNames.lookupRegistryNumberReference(this.regName).equals("MEM1")
			){
			// I expect never to see AGEN here, because that has its address as the value of valueSize width, see above
			// I expect to never get in here, or I expect to see MEM0 to have an address here. This is because the docs
			// talk about a computed address being stored. For AGEN, this is clearly (?) the one of valueSize width (see above).
			// But for MEM0, which also have a memory phrase that can be used to compute the address, there is supposed to be
			// an address pre-computed somewhere.
			System.err.println(""+'\t'+'\t'+"RegName: "+'\t'+RegisterNames.lookupRegistryNumberReference(this.regName)+'\t'+"Memory Addr Size: "+'\t'+this.addrWidth);
		}
		
		if(this.regName == SpecRegs.MEM0.regNameId){
			// All memory phrase components exist for MEM0 and for AGEN, both.
			// If we need them, that is.
			// But...I have the processing of the memory phrase occurring elsewhere, in DecodedIns constructor.
			// this.mem0EffectiveAddress = this.decodedInstruction.constSlot[0];
		}
		if(this.regName == SpecRegs.AGEN.regNameId){
			/* From email with contractor via Fred:
			 * The AGEN operand is a different beast. It is not memory-based (lcSlot.AddrWidth = 0) and it only
			 * has a "value" stored at offset "lcSlot.Offset" in the execution record, just like a register.
			 * This value is the computed result of the memory phrase and its width is given by lcSlot.ValueSize
			 * (same as corresponding expOper.Size in this case). If needed, individual memory phrase components
			 * are also stored like they are for MEM0 operands: each component is linked to a slot, and slots are
			 * listed in the various decodedIns.Mem0*** members. Also, as with MEM0 operands again, the offset
			 * portion of the memory phrase is in decodedIns.ConstSlot[0].
			 */
		}
		
		if(this.regName == SpecRegs.STACKPUSH().regNameId() || this.regName == SpecRegs.STACKPOP().regNameId()){
			//	STACKPUSH (from XED): output operand is on the stack at address rsp/esp/sp -
			//	n, where n depends on the operand size and pre-execution stack pointer value is used. Local
			//	context value in execution record provides the computed address like it does for any memory
			//	operands.
			//	STACKPOP (from XED): input operand is on the stack at address rsp/esp/sp. Local
			//	context value in execution record provides this address like it does for any memory operand.
			
			// I think that constants[0] contains the precomputed stack pointer address
			// But...I have the processing of the memory phrase occurring elsewhere, in DecodedIns constructor.
			// this.mem0EffectiveAddress = this.decodedInstruction.constSlot[0];
		}
		
		execRec.parentParser.seekToBitAddress(originalBitAddress);
	}
	
	public boolean hasMemoryLocationAvailable(){
		return this.hasComputedAddress;
	}
	
	/**
	 * Returns a memory address if possible, otherwise a register name.
	 * @return
	 */
	public String getLocationName(){
		if(this.hasComputedAddress){
			return this.memoryLocationString;
		} else {
			return this.regNameString;
		}
	}
	
	/**
	 * Convenience field, defined as whether addrWidth is equal to 0.
	 */
	private final boolean hasComputedAddress;

	/** ver Oct 2013
	 * Register name (from Intel XED, see section 14.1) or one of the special values
	 * (see top of this section on page 22).
	 * 
	 * 16-bit int
	 */
	public final int regName;
	
	/** ver Feb 2016
	 * Renamed from 'LargestEnclosing' to 'CommonRegName' in Feb 2015 spec revision. 
	 * 
	 * ver Oct 2013
	 * Largest register enclosing RegName, according to the absolute enclosing
	 * register definition in section 11. This comes handy for a trace analysis
	 * tool looking for a specific register (al for instance) in the LC. It can
	 * search by largest enclosing register (rax) instead of looking for all
	 * possible enclosing registers (al, ax, eax, rax)
	 * 
	 * 16-bit int
	 */
	public final int commonRegName;
	
	/** ver Oct 2013
	 * Operand value size. For memory operands, this is the size of the value in
	 * memory, not counting the address itself stored in the execution record.
	 * When RegName is AGEN, this is the size of the computed address.
	 * Note: while Size is most often a register size (up to 32 bytes for
	 * YMM registers), some instructions may have memory operands of about
	 * 600 bytes (fxrstor and relatives for instance).
	 * 
	 * 16-bit int
	 */
	public final int valueSize;
	
	/** ver Oct 2013
	 * Offset, relative to the start of the execution record LC data, where
	 * the register/memory value (ValueSize bytes wide) is found. For memory
	 * operands, a 64-bit address (zero-extended from original if needed)
	 * precedes the value. 
	 * 
	 * 16-bit int
	 */
	public final int offset;
	
	
	
	
	/** ver Oct 2013
	 * 0 for non-memory operands. Otherwise, the significant size of the
	 * address value in the execution record LC. Addresses in the execution
	 * record are always 64-bit (zero-extended if needed), but only the first
	 * AddrWidth bytes are significant.
	 * 
	 * 8-bit int
	 */
	public final int addrWidth;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 3-byte pad
	 */
	 // byte[] pad; // 3 byte pad to ignore.
}
