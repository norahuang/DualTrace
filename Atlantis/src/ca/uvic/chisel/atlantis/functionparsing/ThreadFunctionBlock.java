package ca.uvic.chisel.atlantis.functionparsing;

import javax.print.attribute.standard.MediaSize.Other;

public class ThreadFunctionBlock implements Comparable<ThreadFunctionBlock> {
	
	private int threadId;
	private Instruction startInstruction;
	private Instruction endInstruction;
	private Instruction functionStartInstruction;
	private long startLineNumber;
	private long xOffset;
	/**
	 * The line width. Pixels map to lines 1:1, so it is also the pixel width.
	 */
	private long width;
	
	public int getThreadId() {
		return threadId;
	}
	
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	
	
	public long getStartLineNumber() {
		return startLineNumber;
	}
	
	public long getEndLineNumber() {
		return startLineNumber + width - 1;
	}
	
	public void setStartLineNumber(long startLineNumber) {
		this.startLineNumber = startLineNumber;
	}
	
	public long getXOffset() {
		return xOffset;
	}
	
	public void setXOffset(long xOffset) {
		this.xOffset = xOffset;
	}
	
	public long getWidth() {
		return width;
	}

	public void setWidth(long width) {
		this.width = width;
	}
	
	public Instruction getStartInstruction() {
		return startInstruction;
	}

	public void setStartInstruction(Instruction startInstruction) {
		this.startInstruction = startInstruction;
	}
	
	public Instruction getFunctionStartInstruction() {
		return functionStartInstruction;
	}

	public void setFunctionStartInstruction(Instruction functionStartInstruction) {
		this.functionStartInstruction = functionStartInstruction;
	}

	public Instruction getEndInstruction() {
		return endInstruction;
	}

	public void setEndInstruction(Instruction endInstruction) {
		this.endInstruction = endInstruction;
	}
	
	@Override
	public boolean equals(Object other) {
		if(other == null) {
			return false;
		}
		
		ThreadFunctionBlock otherBlock = (ThreadFunctionBlock)other;
		
		return this.startLineNumber == otherBlock.startLineNumber;
	}

	@Override
	public int compareTo(ThreadFunctionBlock o) {
		if(o == null) {
			return 1;
		}
		
		if(this.startInstruction == null) {
			return -1;
		}
		
		return this.startInstruction.compareTo(o.startInstruction);
	}
}
