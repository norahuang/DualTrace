package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.bfv.dualtrace.InstructionXml;

public class Instruction implements Comparable<Instruction> {
	
	/**
	 * Only used during processing, to deal with the common situation of dropping
	 * into the trace in the middle of a running function, with no knowledge of its
	 * start instruction identity.
	 * Currently for debugging only, stacks used for logic.
	 */
	public boolean bareEntryToFunction = false;
	
	
	private static ArrayList<String> instructionNames = new ArrayList<String>();
	
	/** 
	 * May be null. Not currently stored in the database, because it is not a unique
	 * identifier. It may also differ for multiple instances of the same instruction.
	 */
	private final Long binaryFormatInternalId;
	private final int instructionNameIndex;


	private final String moduleName;
	private final int moduleId;
	private final String fullText;
	private final long moduleOffset;
	private final long firstLine;
	private final InstructionId uniqueIdentifier;
	private final InstructionId parentFunction;
	
	/**
	 * @param uniqueGlobalIdentifier
	 * @param firstLine
	 * @param name
	 * @param fullText
	 * @param module
	 * @param moduleOffset
	 * @param binaryFormatInternalId	May be null. Not currently stored in the database, because it is not a unique identifier. It may also differ for multiple instances of the same instruction.
	 */
	public Instruction(InstructionId uniqueGlobalIdentifier, long firstLine, String name, String fullText, String module, int moduleId, long moduleOffset, Long binaryFormatInternalId, InstructionId parentFunctionId) {

		// Set/find the instruction name
		int iId = instructionNames.indexOf(name);
		if(iId == -1) {
			instructionNameIndex = instructionNames.size();
			instructionNames.add(name);
		} else {
			instructionNameIndex = iId;
		}
		
		this.moduleId = moduleId;
		
		this.moduleName = module;
		
		this.binaryFormatInternalId = binaryFormatInternalId;
		
		this.fullText = fullText;
		this.moduleOffset = moduleOffset;
		this.firstLine = firstLine;
		this.uniqueIdentifier = uniqueGlobalIdentifier;
		this.parentFunction = parentFunctionId;
	}
	
	
	public Instruction(InstructionXml ins)
	{
		this.instructionNameIndex = ins.getInstructionNameIndex();
		this.moduleId = ins.getModuleId();
		
		this.moduleName = ins.getModuleName();
		
		this.binaryFormatInternalId = null;
		
		this.fullText = ins.getFullText();
		this.moduleOffset = ins.getModuleOffset();
		this.firstLine = ins.getFirstLine();
		this.uniqueIdentifier = new InstructionId(ins.getUniqueIdentifier());
		this.parentFunction = new InstructionId(ins.getParentFunction());
	}
	
	/**
	 * Each instruction can have multiple binary format ids, so these are not unique identifiers.
	 * @return
	 */
	@Deprecated
	public long getBinaryFormatInstructionId() {
		return binaryFormatInternalId;
	}
	
	public String getIdStringGlobalUnique() {
		return this.uniqueIdentifier.toString();
	}
	
	public InstructionId getIdGlobalUnique() {
		return this.uniqueIdentifier;
	}
	

	public InstructionId getParentFunction(){
		return this.parentFunction;
	}
	
	public String getInstruction() {
		return instructionNames.get(instructionNameIndex);
	}
	
	public String getFullText() {
		return fullText;
	}
	
	public String getModule() {
		return moduleName;
	}
	
	public int getModuleId() {
		return moduleId;
	}
	
	public long getModuleOffset() {
		return moduleOffset;
	}
	
	public long getFirstLine() {
		return firstLine;
	}
	
	public boolean inSameModuleAs(Instruction other) {
		return (moduleId == other.moduleId);
	}
	
	public boolean isRet(){
		String inst = getInstruction();
		if(inst.equalsIgnoreCase("ret")) {
			// it's a function return
			return true;
		}
		return false;
	}
	
	public boolean isGuaranteedJump() {
		String inst = getInstruction();
		
		if(inst.equalsIgnoreCase("jmp")) {
			// it's an unconditional jump
			return true;
		}
		
		if(inst.equalsIgnoreCase("call")) {
			// it's a function call
			return true;
		}
		
		if(inst.equalsIgnoreCase("ret")) {
			// it's a function return
			return true;
		}
		
		return false;
	}
	
	public boolean isBranch() {
		String inst = getInstruction();
		
		if(inst.equalsIgnoreCase("jmp")) {
			// it's an unconditional jump, not a branch
			return false;
		}
		
		if(inst.charAt(0) == 'j' || inst.charAt(0) == 'J') {
			// it's a conditional jump
			return true;
		}
		
		return false;
	}
	
	public boolean wouldEndBasicBlock() {
		if(getInstruction().equalsIgnoreCase("call")) {
			// it's an call, don't end the block
			// I think that call should end a block. From wikipedia:
			//    "function calls can be at the end of a basic block if they cannot return, such as
			//    functions which throw exceptions or special calls like C's longjmp and exit"
			// Further, it said:
			//    "this method does not always generate maximal basic blocks, by the formal definition,
			//    but they are usually sufficient (maximal basic blocks are basic blocks which cannot
			//    be extended by including adjacent blocks without violating the definition of a basic block"
			return true;
		}
		
		return this.isGuaranteedJump() || this.isBranch();
	}
	
	public boolean wouldBeBranchedToBy(Instruction branch, String flagsValue) 
	{
		int flags = Integer.parseInt(flagsValue, 16);
		
		// TODO Make this an enum for use in multiple places. Search for these hexes and find other uses.
		int carryFlag = 0x00000001;         
		int parityFlag = 0x00000004;         
		int adjustFlag = 0x00000010;         
		int zeroFlag = 0x00000040;         
		int signFlag = 0x00000080;         
		int trapFlag = 0x00000100;         
		int interruptEnableFlag = 0x00000200;  
		int directionFlag = 0x00000400;  
		int overflowFlag = 0x00000800;    
		
		String instruction = branch.getInstruction();
		
		// overflow
		if(instruction.equalsIgnoreCase("jo")) {
			return (flags & overflowFlag) > 0;
		}
		
		// not overflow
		if(instruction.equalsIgnoreCase("jno")) {
			return (flags & overflowFlag) == 0;
		}
		
		// sign
		if(instruction.equalsIgnoreCase("js")) {
			return (flags & signFlag) > 0;
		}
		
		// not sign
		if(instruction.equalsIgnoreCase("jns")) {
			return (flags & signFlag) == 0;
		}
		
		// equal/zero
		if(instruction.equalsIgnoreCase("je") || instruction.equalsIgnoreCase("jz")) {
			return (flags & zeroFlag) > 0;
		}
		
		// not equal/not zero
		if(instruction.equalsIgnoreCase("jne") || instruction.equalsIgnoreCase("jnz")) {
			return (flags & zeroFlag) == 0;
		}
		
		// below/not above or equal/carry 
		if(instruction.equalsIgnoreCase("jb") || instruction.equalsIgnoreCase("jnae") || instruction.equalsIgnoreCase("jc")) {
			return (flags & carryFlag) > 0;
		}
		
		// not below/above or equal/not carry 
		if(instruction.equalsIgnoreCase("jb") || instruction.equalsIgnoreCase("jnae") || instruction.equalsIgnoreCase("jc")) {
			return (flags & carryFlag) == 0;
		}
		
		// below or equal/not above
		if(instruction.equalsIgnoreCase("jbe") || instruction.equalsIgnoreCase("jna")) {
			return (flags & carryFlag) > 0 || (flags & zeroFlag) > 0;
		}
		
		// above/not below or equal
		if(instruction.equalsIgnoreCase("ja") || instruction.equalsIgnoreCase("jnbe")) {
			return (flags & carryFlag) == 0 && (flags & zeroFlag) == 0;
		}
		
		// less/not greater or equal
		if(instruction.equalsIgnoreCase("jl") || instruction.equalsIgnoreCase("jnge")) {
			return ((flags & signFlag) == 0) != ((flags & overflowFlag) == 0);
		}
		
		// greater or equal/not less
		if(instruction.equalsIgnoreCase("jge") || instruction.equalsIgnoreCase("jnl")) {
			return ((flags & signFlag) == 0) == ((flags & overflowFlag) == 0);
		}
		
		// less or equal/not greater
		if(instruction.equalsIgnoreCase("jl") || instruction.equalsIgnoreCase("jnge")) {
			return (((flags & signFlag) == 0) != ((flags & overflowFlag) == 0)) || ((flags & zeroFlag) > 0);
		}
		
		// greater/not less or equal
		if(instruction.equalsIgnoreCase("jl") || instruction.equalsIgnoreCase("jnge")) {
			return (((flags & signFlag) == 0) == ((flags & overflowFlag) == 0)) && ((flags & zeroFlag) == 0);
		}
		
		// parity/parity even
		if(instruction.equalsIgnoreCase("jp") || instruction.equalsIgnoreCase("jpe")) {
			return (flags & parityFlag) > 0;
		}
		
		// not parity/parity odd
		if(instruction.equalsIgnoreCase("jnp") || instruction.equalsIgnoreCase("jpo")) {
			return (flags & parityFlag) == 0;
		}
		
		return false;
	}

	@Override
	public int compareTo(Instruction other) {
		if(other == null) {
			return 1;
		}
		
		int moduleCompare = Integer.compare(moduleId, other.moduleId);
		
		if(moduleCompare == 0) {
			return Long.compare(moduleOffset, other.getModuleOffset());
		}
		
		return moduleCompare;
	}
	
	@Override
	public boolean equals(Object other) {
		Instruction otherInstruction = (Instruction)other;
		
		if(otherInstruction == null) {
			return false;
		}
		
		return moduleId == otherInstruction.moduleId && moduleOffset == otherInstruction.moduleOffset;
	}
	
	@Override
	public String toString() {
		return getModule() + "+" + Long.toString(getModuleOffset(), 16) + " " + getFullText();
	}
	
	public int getInstructionNameIndex() {
		return instructionNameIndex;
	}
}
