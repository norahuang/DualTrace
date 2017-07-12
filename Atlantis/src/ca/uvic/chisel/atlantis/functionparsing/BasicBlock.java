package ca.uvic.chisel.atlantis.functionparsing;

import java.util.Set;
import java.util.TreeSet;

public class BasicBlock implements Comparable<BasicBlock> {
	
	private Instruction start;
	private Instruction end;
	private int length;
	private Set<BasicBlock> successors;
	private Set<BasicBlock> predecessors;
	private boolean loadedAsBranchTaken;
	private boolean isFunctionStart;
	
	public BasicBlock(Instruction start, Instruction end, int length) {
		this.start = start;
		this.end = end;
		this.setLength(length);
		this.successors = new TreeSet<BasicBlock>();
		this.predecessors = new TreeSet<BasicBlock>();
	}

	public Instruction getStart() {
		return start;
	}
	
	public void setStart(Instruction start) {
		this.start = start;
	}

	public Instruction getEnd() {
		return end;
	}

	public void setEnd(Instruction end) {
		this.end = end;
	}
	
	/**
	 * This is used, but the accessors to related data are not used.
	 * 
	 * @param successor
	 * @return
	 */
	@Deprecated
	public boolean addSuccessor(BasicBlock successor) {
		successor.predecessors.add(this);
		return this.successors.add(successor);
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isLoadedAsBranchTaken() {
		return loadedAsBranchTaken;
	}

	public void setLoadedAsBranchTaken(boolean loadedAsBranchTaken) {
		this.loadedAsBranchTaken = loadedAsBranchTaken;
	}

	public boolean isFunctionStart() {
		return isFunctionStart;
	}

	public void setFunctionStart(boolean isFunctionStart) {
		this.isFunctionStart = isFunctionStart;
	}

	@Override
	public int compareTo(BasicBlock other) {
		// block are the same if they start on the same instruction or they end
		// on the same instruction, so null's are treated as all equal
		int startCompare = other != null ? start.compareTo(other.start) : 0;
		
		if(startCompare == 0) {
			return end != null ? end.compareTo(other.end) : 0;
		}
		
		return startCompare;
	}
	
	@Override
	public boolean equals(Object other) {
		// block are the same if they start on the same instruction or they end
		// on the same instruction, so null's are treated as all equal
		
		BasicBlock otherBlock = (BasicBlock)other;
		
		if(start == null && end == null) {
			return otherBlock.start == null && otherBlock.end == null;
		} else if(start == null) {
			return end.equals(otherBlock.end);
		} else if(end == null) {
			return start.equals(otherBlock.start);
		}
		
		return start.equals(otherBlock.start) && end.equals(otherBlock.end);
	}
	
	@Override
	public String toString() {
		String result = "";
		
		if(start != null) {
			result += start.getModule() + "+" + Long.toString(start.getModuleOffset(), 16);
		} else {
			result += "unknown";
		}
		
		result += " -> ";
		
		if(end != null) {
			result += end.getModule() + "+" + Long.toString(end.getModuleOffset(), 16);
		} else {
			result += "unknown";
		}
		
		return result;
	}
}
