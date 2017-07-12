package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;

import ca.uvic.chisel.atlantis.database.InstructionId;

public class LightweightThreadFunctionBlock implements Comparable<LightweightThreadFunctionBlock> {
	private long xOffset;
	private long width;
	private long startLineNumber;
	private String blockModule;
	private String functionModule;
	private int blockModuleId;
	private int functionModuleId;
	private long functionStartAddress;
	private boolean endsWithCall;
	private InstructionId functionStartInstruction;
	private int threadId;

	private static ArrayList<String> modules = new ArrayList<String>();

	public LightweightThreadFunctionBlock(long xOffset, long width,
			long startLineNumber, String blockModule,
			String functionModule, int blockModuleId,
			int functionModuleId, long functionStartAddress,
			boolean endsWithCall, InstructionId functionStartInstruction, int threadId) {
		this.xOffset = xOffset;
		this.width = width;
		this.startLineNumber = startLineNumber;
		this.blockModule = blockModule;
		this.functionModule = functionModule;
		this.blockModuleId = blockModuleId;
		this.functionModuleId = functionModuleId;
		this.functionStartAddress = functionStartAddress;
		this.endsWithCall = endsWithCall;
		this.functionStartInstruction = functionStartInstruction;
		this.threadId = threadId;

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
	
	public long getStartLineNumber() {
		return startLineNumber;
	}
	
	public void setStartLineNumber(long startLineNumber) {
		this.startLineNumber = startLineNumber;
	}

	public long getEndLineNumber() {
		return this.startLineNumber + this.width - 1;
	}
	
	public int getThreadId(){	
		return this.threadId;
	}
	
	public long getFunctionStartAddress() {
		return functionStartAddress;
	}
	
	public void setFunctionStartAddress(long functionStartAddress) {
		this.functionStartAddress = functionStartAddress;
	}
	
	public boolean endsWithCall() {
		return endsWithCall;
	}
	
	public void setEndsWithCall(boolean endsWithCall) {
		this.endsWithCall = endsWithCall;
	}
	
	public String getBlockModule() {
		return blockModule;
	}
	
	public String getFunctionModule() {
		return functionModule;
	}
	
	public InstructionId getFunctionStartInstruction() {
		return functionStartInstruction;
	}

	public void setFunctionStartInstruction(InstructionId functionStartInstruction) {
		this.functionStartInstruction = functionStartInstruction;
	}

	@Override
	public int compareTo(LightweightThreadFunctionBlock o) {
		return Long.valueOf(this.startLineNumber).compareTo(o.startLineNumber);
	}
}
