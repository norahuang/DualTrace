package ca.uvic.chisel.atlantis.functionparsing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Function implements Comparable<Function> {

	private Instruction first;
	private String name;
	private List<Function> callees;
	private Function fromParent;
	
	// flags
	private boolean startsThread; 	// this function is called at the beginning of a thread
	
	private boolean unknownStart; 	// the start address of this function is the instruction
									// entered at from a return which had no address on the known stack
									// if this is set, so should oddReturn
	
	private boolean oddReturn;		// this function contains an instruction that was returned
									// to without a return address on the known stack
	
	public Function(Instruction first, String name) {
		this.first = first;
		this.name = name;
		this.callees = null;
		this.fromParent = null;
		this.startsThread = this.unknownStart = this.oddReturn = false;
	}
	
	public Instruction getFirst() {
		return first;
	}
	
	public void setFirst(Instruction first) {
		this.first = first;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setFromParent(Function parent) {
		this.fromParent = parent;
	}
	
	public Function getFromParent() {
		return fromParent;
	}
	
	public void attachCallees(List<Function> callees) {
		this.callees = callees;
	}
	
	public List<Function> getCallees() {
		return callees;
	}
	
	public boolean areCalleesAttached() {
		return callees != null;
	}
	
	public void setStartsThread(boolean startsThread) {
		this.startsThread = startsThread;
	}
	
	public boolean getStartsThread() {
		return startsThread;
	}
 	
	public void setUnknownStart(boolean unknownStart) {
		this.unknownStart = unknownStart;
	}
	
	public boolean getUnknownStart() {
		return unknownStart;
	}
	
	public void setOddReturn(boolean oddReturn) {
		this.oddReturn = oddReturn;
	}
	
	public boolean getOddReturn() {
		return oddReturn;
	}
	
	@Override
	public int compareTo(Function other) {
		// functions are the same if they start on the same instruction or they end
		// on the same instruction, so null's are treated as all equal
		if(other == null) {
			return 1;
		}
		
		return first != null ? first.compareTo(other.first) : 0;
	}
	
	@Override
	public boolean equals(Object other) {
		// functions are the same if they start on the same instruction or they end
		// on the same instruction, so null's are treated as all equal
		
		if(other == null) {
			return false;
		}
		
		Function otherFunction = (Function)other;
		
		if (this.first == null && otherFunction.first == null) {
			return true;
		}
		
		if (otherFunction.first == null) {
			return false;
		}
		
		if (this.first == null) {
			return false;
		}
		
		return first.equals(otherFunction.first);
	}
	
	@Override
	public String toString() {
		String functionRegistryEntry = FunctionNameRegistry.getFunction(first.getModule(), first.getModuleOffset());
		
		if(functionRegistryEntry != null) {
			return functionRegistryEntry;
		}
		
		return "Function: " + first.toString();
	}
}
