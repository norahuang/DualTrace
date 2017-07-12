package ca.uvic.chisel.atlantis.bytecodeparsing.instruction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames.SpecRegs;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;


/** ver Oct 2013
 * 
 * (
 *  https://software.intel.com/sites/landingpage/pintool/docs/62732/Xed/html/main.html
 *  https://software.intel.com/sites/landingpage/pintool/docs/62141/Xed/html/group__CMDLINE.html
 * )
 * 
 * main (and somewhat monstruous) variable-size part of an instruction record 
 * Size: variable (at least 88)
 */
public class DecodedIns {
	
	/**
	* A constant holding the maximum value an <code>unsigned long</code> can
	* have, 2<sup>64</sup>-1.
	* Stole from JOOU, which didn't meet my needs for BigInteger arithmetic.
	*/
    public static final BigInteger MAX_UNSIGNED_LONG_VALUE = new BigInteger("18446744073709551615");
    
    public static final BigInteger MAX_UNSIGNED_INT_VALUE = new BigInteger("4294967295");

	private static BigInteger maxNumber = null;
    
    /**
     * This will not cope with unsigned longs that are more than twice as big as the max long.
     * 
     * @param a
     * @param b
     * @return
     */
    public BigInteger addUnsignedWrap(BigInteger a, BigInteger b){
    	BigInteger c = a.add(b);
    	if(1 == c.compareTo(maxNumber)){
    		// If max is 10, and we have 10, keep 10. If max is 10 and we have 11, we would want...0.
    		// Wrap it. The result should be larger than zero by the diff between the max and the
    		// value attained.
    		c = c.subtract(maxNumber.add(BigInteger.ONE));
    	}
    	return c;
    }
	
	// We may not need this decoded instructions when we first get the new format working
	// with Atlantis, but instead perhaps we only need to use the input and output operands
	// in the local context slots.
	
	public DecodedIns(InsItable parentParser, long decodedFp, long extraFp, ExecRec execRec) {
		if(null == maxNumber){
			if(parentParser.bitness.equals("32")){
				maxNumber = MAX_UNSIGNED_INT_VALUE;
			} else {
				maxNumber = MAX_UNSIGNED_LONG_VALUE;
			}
		}
		
		// Use the decodedFp and extraFp to compute where this Decoded Instruction exists.
		// Record parent position so we can reset back to it when we are done
		long parentBitAddressFp = parentParser.getCurrentBitAddress();
		// System.out.println("extraFp + decodedFp = "+extraFp+" + "+ decodedFp+" = "+(extraFp + decodedFp)+" bytes");
		parentParser.seekToByteAddress(extraFp + decodedFp);
		
		this.category = parentParser.getNextMiniInt(16);
		
		// TODO lookup category from the provided header files?
		
		this.mnemonic = parentParser.getNextMiniInt(16);
		
		this.form = parentParser.getNextMiniInt(16);
		
		// TODO lookup form from provided header files?
		
		this.flags = parentParser.getNextMiniInt(8);
		
		this.expCount = parentParser.getNextMiniInt(8);
		
		// Array of four explicit operands. Only the first ExpCount elements are valid. See expOper below.
		this.expOpers = new ExpOper[this.expCount];
		for(int i = 0; i < 4; i++){
			// In place array, not a fp. Only the first expCount ones are valid, so don't
			// put invalid ones in the container.
			ExpOper expOper = new ExpOper(parentParser);
			if(i < this.expCount){
				this.expOpers[i] = expOper;
			}
		}
		
		this.effOperSize = parentParser.getNextMiniInt(8);
		
		this.effAddrSize = parentParser.getNextMiniInt(8);
		
		// Lots of Local Context stuff
		
		this.lcSize = parentParser.getNextMiniInt(16);
		
		this.firstOutSlot = parentParser.getNextMiniInt(8);
		
		this.firstAfterSlot = parentParser.getNextMiniInt(8);
		
		this.lcCount = parentParser.getNextMiniInt(8);
		
		this.mem0SegSlot = parentParser.getNextMiniInt(8);
		
		this.mem0BaseSlot = parentParser.getNextMiniInt(8);
		
		this.mem0Scale = parentParser.getNextMiniInt(8);
		
		this.mem0IndexSlot = parentParser.getNextMiniInt(8);
		
		this.mem1BaseSlot = parentParser.getNextMiniInt(8);
		
		// 4 byte pad to ignore.
		parentParser.skipPadBytes(4);
		
		// For constSlot, any IP-relative offsets are converted to absolute addresses
		this.constSlot = new long[2]; // checked docs, is indeed i64 numeric
		// If Mem0 operand present, constSlot[0] is the offset for the memory phrase,
		// and the number of significant bytes (max 8) is given by lcSlot.addrWidth for that Mem0
		// operand.
		// Otherwise, both of these constants do not have corresponding LcSlots.
		this.constSlot[0] = parentParser.getNextLong();
		this.constSlot[1] = parentParser.getNextLong();
		
		// Have to parse all the LcSlots this way, even though the value parsing doesn't make sense for ExpOper slots
		// without the ExpOper information.
		// For other slots (mem0BaseSlot, etc.), we can't reach them via ExpOper instances, so we need to parse
		// based on expected lcCount.
		this.lcSlots = new ArrayList<LcSlot>(this.lcCount);
		for(int i = 0; i < this.lcCount; i++){
			LcSlot slot = new LcSlot(parentParser, this, i < this.firstOutSlot, this.firstAfterSlot <= i, execRec);
			this.lcSlots.add(slot);
			// Need to initialize for expOper loop to know if I get doubles
		}
		
		// Get memory values that require the before and after offsets from ExpOpers
		for(ExpOper expOper: this.expOpers){
			
			BEFORE_SLOTS: {
				int beforeSlotIndex = expOper.beforeSlot;
				// NA is impossible for before slots, IMM0 and IMM1 have values stored in const[] instead.
				if(beforeSlotIndex != SpecialSlotIndex.SLOT_NA.index && beforeSlotIndex != SpecialSlotIndex.SLOT_IMM0.index && beforeSlotIndex != SpecialSlotIndex.SLOT_IMM1.index){
					LcSlot slot = this.lcSlots.get(expOper.beforeSlot);
					slot.getMemoryValue(parentParser, expOper, true);
				}
			}
			
			AFTER_SLOTS: {
				int afterSlotIndex = expOper.afterSlot;
				// IMM0 and IMM1 have values stored in const[] instead.
				if(afterSlotIndex != SpecialSlotIndex.SLOT_NA.index && afterSlotIndex != SpecialSlotIndex.SLOT_IMM0.index && afterSlotIndex != SpecialSlotIndex.SLOT_IMM1.index){
					if(this.lcSlots.get(afterSlotIndex) != null){
						if(this.lcSlots.get(afterSlotIndex).temporaryDeleteOwningExpOper != null && this.lcSlots.get(afterSlotIndex).temporaryDeleteOwningExpOper != expOper){
							// Since it's valid to have two references to same register, I can't discern if there's a problem after all.
							// System.out.println("Double slot reference with diff expOper: "+afterSlotIndex+" from assembly: "+execRec.getDissassemblyForErrors());
							// When this happens, we have the same after slot being pointed to by two different expOpers. Is that ok?
							// Well, in this case the assembly is "xchng al,ah", which is copying the lower part of the register to the higher.
							// I think we can allow this to happen (it happens naturally for xchg at least), but I want to
							// flag the reference, for debugging later. I will have to trust the merge code to do the right thing,
							// because setting up the logic to filter these out from the delta merging is wrought with hazard.
							this.lcSlots.get(expOper.afterSlot).numberExpOpersRefering++;
						} else if(expOper.afterOffset != expOper.beforeOffset){
							System.out.println("Re-using lcSlot for before and after on same expOper but with diff offsets");
						} else if(expOper.afterOffset == expOper.beforeOffset){
							// System.out.println("Safely re-using lcSlot for expOper");
							// TODO If this is the common occurrence, I need to shuffle things around. This is awkward as is...
						}
					}
					LcSlot slot = this.lcSlots.get(expOper.afterSlot);
					slot.getMemoryValue(parentParser, expOper, false);
				}
			}
			
		}
		
		// Read away from parent, set it back for linear reading
		parentParser.seekToBitAddress(parentBitAddressFp);
		
		// --------------------
		
		for(ExpOper operand: this.expOpers){
			if(operand.regName == RegisterNames.SpecRegs.MEM0.regNameId
					|| operand.regName == RegisterNames.SpecRegs.AGEN.regNameId
					){
				// AGEN is only used with lea instructions, and does not read memory, though it uses memory addresses.
				
				// TODO I am not sure we need to use this in Atlantis. The documentation suggests that some tools may only
				// need to use the input and output operands in the local context slots.
				// seg:[base + scale*index + offset]
				// this.mem0Phrase = mem0Seg+":["+mem0Base+" + "+this.mem0Scale+"*"+mem0Index+" + "+this.constSlot[0]+"]";
				this.computeMemoryPhrase(operand, execRec);
				
			}
			// STACKPUSH (from XED): output operand is on the stack at address rsp/esp/sp - n, where n depends on the operand size and pre-execution stack pointer value is used. Local context value in execution record provides the computed address like it does for any memory operands.
			// STACKPOP (from XED): input operand is on the stack at address rsp/esp/sp. Local context value in execution record provides this address like it does for any memory operand.
			// FSBASE (from XED) and GSBASE (from XED): in a user-mode trace, it would be pretty useless to provide fs/gs selector values when those are used as segment overrides in memory phrases. Instead, their base linear address is provided.
			// INVALID (from XED): no associated register (to name a constant or non-existent operand for instance).
			// These don't require additional processing.
		}
		
		
	}
		
	public void computeMemoryPhrase(ExpOper operand, ExecRec execRec){ // don't commit
		// Memory phrase have the form:
		// parse: MEMlength:[segment:]base,index,scale[,displacement]
	    // parse: AGEN:base,index,scale[,displacement]
		StringBuilder phraseBuilder = new StringBuilder();
		BigInteger effectiveAddress = BigInteger.ZERO;
		
		if(this.mem0SegSlot != SpecialSlotIndex.SLOT_NA.index){
			LcSlot mem0Seg = this.lcSlots.get(this.mem0SegSlot);
			phraseBuilder.append(mem0Seg.getLocationName()+":");
			
			BigInteger segmentValue = BigInteger.ZERO;
			// No unsigned longs...well, use BigInt?
			segmentValue = new BigInteger(mem0Seg.specialAddressForMemoryPhraseSlotsString, 16);
			effectiveAddress = addUnsignedWrap(effectiveAddress, segmentValue);
		}
		phraseBuilder.append("[");
		if(this.mem0BaseSlot != SpecialSlotIndex.SLOT_NA.index){
			LcSlot mem0Base = this.lcSlots.get(this.mem0BaseSlot);
			phraseBuilder.append(mem0Base.getLocationName()+ " + ");
			// For a whiel, thought that the base could be mem0Base.memoryLocation,
			// but I am certain it is only ever the "special" address as parsed in the LcSLot constructor.
			
			BigInteger baseForPhrase = BigInteger.ZERO;
			// No unsigned longs...well, use BigInt?
			try{
				baseForPhrase = new BigInteger(mem0Base.specialAddressForMemoryPhraseSlotsString, 16);
				// Getting an RDX with value FFFFFFFFFFFFFF88 here. That's waaay too big...it's right next to the 64-bit limit.
			} catch(NumberFormatException e){
				// Overflowed my Java long, have you?
			}
			effectiveAddress = addUnsignedWrap(effectiveAddress, baseForPhrase);
		}
		if(this.mem0IndexSlot != SpecialSlotIndex.SLOT_NA.index){
			// XED docs indicate that the MEM0 "can only be an index and displacement associated with MEM0"
			// which I take to be an awkward way of saying that the other two (scale and base [and segment]) are optional,
			// implying those two (index and displacement) are necessary.
			LcSlot mem0IndexSlot = this.lcSlots.get(this.mem0IndexSlot);
			Long mem0Index = mem0IndexSlot.specialAddressForMemoryPhraseSlots.longValue(); //.longValue(); // this usage here will never be truly a BigInt
			if(this.mem0Scale != 0){
				phraseBuilder.append(this.mem0Scale+"*");
			}
			phraseBuilder.append(mem0IndexSlot.getLocationName()+" + ");
			
			if(null != mem0Index && mem0Index != 0){
				long ind = mem0Index;
				if(this.mem0Scale != 0){
					ind *= this.mem0Scale;
				}
				// System.out.println("effectiveAddress += ind : "+BinaryFormatParser.toHex(effectiveAddress, 8)+"+="+BinaryFormatParser.toHex(ind));
				effectiveAddress = addUnsignedWrap(effectiveAddress, BigInteger.valueOf(ind));
			}
		}
		
		// Finally, the displacement (const[0]).
		phraseBuilder.append(BinaryFormatParser.toHex(this.constSlot[0], 0));
		phraseBuilder.append("]");
		
		effectiveAddress = addUnsignedWrap(effectiveAddress, BigInteger.valueOf(this.constSlot[0]));
		// System.out.println("effectiveAddress += this.constSlot[0] : "+BinaryFormatParser.toHex(effectiveAddress)+"+="+BinaryFormatParser.toHex(this.constSlot[0]));
		
		this.mem0Phrase = phraseBuilder.toString();
		this.mem0EffectiveAddress = effectiveAddress;
		
		if(operand.beforeSlot != SpecialSlotIndex.SLOT_NA.index){
			LcSlot beforeSlot = this.lcSlots.get(operand.beforeSlot);
			beforeSlot.mem0EffectiveAddress = this.mem0EffectiveAddress;
			beforeSlot.mem0MemoryPhrase = this.mem0Phrase;
			
			// Verifying identity of computed address from LcSlot parsing to the memory Phrase one
			if(operand.regName == SpecRegs.MEM0.regNameId
					|| operand.regName == SpecRegs.AGEN.regNameId
					){
				if(null == beforeSlot.memoryLocation || null == beforeSlot.mem0EffectiveAddress){
					// NOOP
				}
				else if(!beforeSlot.mem0EffectiveAddress.equals(BigInteger.valueOf(beforeSlot.memoryLocation))
						&& !beforeSlot.mem0EffectiveAddress.mod(maxNumber.add(BigInteger.ONE)).equals(BigInteger.valueOf(beforeSlot.memoryLocation))
						){
					// Canary
					if(this.lcSlots.get(this.mem0IndexSlot).specialAddressForMemoryPhraseSlots.longValue() == 4294967295L){
						System.out.println("Bad match with maxint, so maybe it doesn't matter...");
					} else {
						System.out.println("No hex beforeSlot match: "+BinaryFormatParser.toHex(beforeSlot.memoryLocation, 8)+" != "+BinaryFormatParser.toHex(beforeSlot.mem0EffectiveAddress, 8)+" from phrase "+beforeSlot.mem0MemoryPhrase);
						throw new RuntimeException("Line: "+execRec.lineNumber+" ("+execRec.insRec.dissasembly+"): "+"No hex beforeSlot match: "+BinaryFormatParser.toHex(beforeSlot.memoryLocation, 8)+" != "+BinaryFormatParser.toHex(beforeSlot.mem0EffectiveAddress, 8)+" from phrase "+beforeSlot.mem0MemoryPhrase);
					}
				}
			}
		}
		
		if(operand.afterSlot != SpecialSlotIndex.SLOT_NA.index){
			LcSlot afterSlot = this.lcSlots.get(operand.afterSlot);
			afterSlot.mem0EffectiveAddress = this.mem0EffectiveAddress;
			afterSlot.mem0MemoryPhrase = this.mem0Phrase;
			
			// Verifying identity of computed address from LcSlot parsing to the memory Phrase one
			if(operand.regName == SpecRegs.MEM0.regNameId
					|| operand.regName == SpecRegs.AGEN.regNameId){
				if(!afterSlot.mem0EffectiveAddress.equals(BigInteger.valueOf(afterSlot.memoryLocation))
						&& !afterSlot.mem0EffectiveAddress.mod(maxNumber.add(BigInteger.ONE)).equals(BigInteger.valueOf(afterSlot.memoryLocation))
						){
					// Canary
					System.out.println("No hex afterSlot match: "+BinaryFormatParser.toHex(afterSlot.memoryLocation, 8)+" != "+BinaryFormatParser.toHex(afterSlot.mem0EffectiveAddress, 8)+"["+afterSlot.mem0MemoryPhrase+"]");
					throw new RuntimeException("Line: "+execRec.lineNumber+" ("+execRec.insRec.dissasembly+"): "+"No hex afterSlot match: "+BinaryFormatParser.toHex(afterSlot.memoryLocation, 8)+" != "+BinaryFormatParser.toHex(afterSlot.mem0EffectiveAddress, 8)+"["+afterSlot.mem0MemoryPhrase+"]");
				}
			}
		}
		
	}
	
	public enum SpecialSlotIndex {
		SLOT_IMM0(0xfd),
		SLOT_IMM1(0xfe),
		SLOT_NA(0xff);
		
		public int index;

		SpecialSlotIndex(int index){
			this.index = index;
		}
	}

	/** ver Oct 2013
	 * Instruction category, from xed_category enum table. See section 14.1.
	 * 
	 * 16-bit int
	 */
	public final int category;
	
	/** ver Oct 2013
	 * Instruction mnemonic, from xed_iclass enum table. See section 14.1.
	 * 
	 * 16-bit int
	 */
	public final int mnemonic;
	
	/** ver Oct 2013
	 * Specific instruction form for the given mnemonic, from xed_iform enum table. See section 14.1.
	 * 
	 * 16-bit int
	 */
	public final int form;
	
	/** ver Oct 2013
	 * Bitwise combination of zero or more of the following:
	 * 	•	0x01: has rep or repe/repz prefix
	 * 	•	0x02: has repne/repnz prefix
	 * 	•	0x04: has lock prefix
	 * 	•	0x08: instruction executes in 64-bit long mode, but operates on 32-bit register operands, in which case all output operands have their highest 32 bits zeroed 
	 * 	•	0x10: instruction potentially reads from memory
	 * 	•	0x20: instruction potentially writes to memory 
	 * 
	 * 8-bit int
	 */
	public final int flags;
	
	
	/** ver Oct 2013
	 * Explicit operand count (0 to 4 incl.)
	 * 
	 * 8-bit int
	 */
	public final int expCount;
	
	/** ver Oct 2013
	 * Array of explicit operands. Only the first ExpCount elements are valid. See expOper below.
	 * 
	 * List of 4 elements
	 */
	public final ExpOper[] expOpers;
	
	/** ver Oct 2013
	 * Effective operand width in bytes (2, 4 or 8). Not the actual operands width,
	 * but rather related to instruction encoding and some zero/sign-extend rules.
	 * See Intel manual about “operand-size and address-size attributes”.
	 * 
	 * 8-bit int
	 */
	public final int effOperSize;
	
	/** ver Oct 2013
	 * Effective address size in bytes (2, 4 or 8). Not the actual size of code
	 * addresses, but rather related to instruction encoding and some zero/sign-extend rules.
	 * See Intel manual about “operand-size and address-size attributes”.
	 * 
	 * 8-bit int
	 */
	public final int effAddrSize;
	
	/** ver Oct 2013
	 * Size of local context data in execution record.
	 * 
	 * 16-bit int
	 */
	public final int lcSize;
	
	/** ver Oct 2013
	 * Index of the first LC slot for “before execution” value of write-only
	 * operand (see Figure 3), thus also the number of “before execution” values
	 * slots for read-only and read-write operands.
	 * 
	 * 8-bit int
	 */
	public final int firstOutSlot;
	
	/** ver Oct 2013
	 * Index of the first LC slot for after-execution operands (see Figure 3),
	 * i.e. after before-execution write-only operand slots.
	 * 
	 * 8-bit int
	 */
	public final int firstAfterSlot;
	
	/** ver Oct 2013
	 * Total count of LC slots (see Figure 3). Length of LCSlots array.
	 * 
	 * 8-bit int
	 */
	public final int lcCount;
	
	/** ver Oct 2013
	 * LC slot index of the memory phrase segment override for any operand named MEM0 or AGEN. 
	 * 
	 * 8-bit int
	 */
	public final int mem0SegSlot;
	
	/** ver Oct 2013
	 * LC slot index of the memory phrase base register for any operand named MEM0 or AGEN.
	 * 
	 * 8-bit int
	 */
	public final int mem0BaseSlot;
	
	/** ver Oct 2013
	 * Scale factor (1, 2, 4 or 8) of the memory phrase base register for any operand
	 * named MEM0 or AGEN, or 0 if there is not an index register as well.
	 * 
	 * 8-bit int
	 */
	public final int mem0Scale;
	
	/** ver Oct 2013
	 * LC slot index of the memory phrase index register for any operand named MEM0 or AGEN.
	 * 
	 * 8-bit int
	 */
	public final int mem0IndexSlot;
	
	/** ver Oct 2013
	 * LC slot index of the memory phrase base register for any operand named MEM1.
	 * 
	 * 8-bit int
	 */
	public final int mem1BaseSlot;
	
	/** ver Oct 2013
	 * Pad
	 * 
	 * 4-byte pad
	 */
	// byte[] pad;
	
	/** ver Oct 2013
	 * Two constant slot values. Those are not lcSlot structures but only the actual value
	 * of an operand or memory phrase offset. If a MEM0 or AGEN operand is present in
	 * this instruction, ConstSlot[0] contains the memory phrase offset, and the number
	 * of significant bytes (up to 8) is given by lcSlot.AddrWidth of the corresponding
	 * operand slot. For explicit operands that directly refer to a constant slot, the
	 * number of valid bytes is given by the operand size. 
	 * 
	 * NB It appears that I cannot find my way to a valid lcSlot when these occur.
	 * Am I making a mistake, or is it then not possible to get addrWidth for a constSlot?
	 * 
	 * List of two 64-bit long
	 */
	public final long[] constSlot;
	
	/** ver Oct 2013
	 * Array of all local context slots, see lcSlot below.
	 * 
	 * Note the dependence on ExpOper offset values.
	 * 
	 * Includes 'lcCount' elements
	 */
	public final List<LcSlot> lcSlots;
	
	/**
	 * Rehydrated memory phrase for the Mem0 slot, as needed.
	 * String of format like:
	 * seg:[base + scale*index + offset]
	 * where all portions are defined in the mem0 data, except for offset, which is defined
	 * in the (first) LCSlot.
	 */
	public String mem0Phrase;
	
	public BigInteger mem0EffectiveAddress;
	
}