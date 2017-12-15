package ca.uvic.chisel.atlantis.functionparsing;

import ca.uvic.chisel.bfv.dualtracechannel.FunctionType;

public class ChannelFunction {
	private Register retrunValReg = new Register("RAX", true);
	private Register valueInputReg;
	private Register memoryInputReg;
	private Register memoryOutputReg;
	private Register memoryInputLenReg;
	private Register memoryOutputBufLenReg;
	private String functionName;
	private boolean createHandle;
	private FunctionType type;
	private String outputDataAddressIndex;

	public ChannelFunction(String functionName, Register retrunValReg, Register valueInputReg, Register memoryInputReg,
			Register memoryOutputReg, Register memoryInputLenReg, Register memoryOutputBufLenReg, boolean createHandle,
			FunctionType isSend, String outputDataAddressIndex) {
		super();
		this.retrunValReg = retrunValReg;
		this.valueInputReg = valueInputReg;
		this.memoryInputReg = memoryInputReg;
		this.memoryOutputReg = memoryOutputReg;
		this.memoryInputLenReg = memoryInputLenReg;
		this.memoryOutputBufLenReg = memoryOutputBufLenReg;
		this.functionName = functionName;
		this.createHandle = createHandle;
		this.type = isSend;
		this.outputDataAddressIndex = outputDataAddressIndex;
	}

	public Register getValueInputReg() {
		return valueInputReg;
	}

	public void setValueInputReg(Register valueInputReg) {
		this.valueInputReg = valueInputReg;
	}

	public Register getMemoryInputReg() {
		return this.memoryInputReg;
	}

	public void setMemoryInputReg(Register memoryInputReg) {
		this.memoryInputReg = memoryInputReg;
	}

	public Register getMemoryOutputReg() {
		return this.memoryOutputReg;
	}

	public void setMemoryOutputReg(Register memoryOutputReg) {
		this.memoryOutputReg = memoryOutputReg;
	}

	public Register getRetrunValReg() {
		return this.retrunValReg;
	}

	public void setRetrunValReg(Register retrunValReg) {
		this.retrunValReg = retrunValReg;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public boolean isCreateHandle() {
		return createHandle;
	}

	public void setCreateHandle(boolean createHandle) {
		this.createHandle = createHandle;

	}

	public FunctionType getType() {
		return type;
	}

	public void setType(FunctionType type) {
		this.type = type;
	}

	public Register getMemoryInputLenReg() {
		return memoryInputLenReg;
	}

	public void setMemoryInputLenReg(Register memoryInputLenReg) {
		this.memoryInputLenReg = memoryInputLenReg;
	}

	public Register getMemoryOutputBufLenReg() {
		return memoryOutputBufLenReg;
	}

	public void setMemoryOutputBufLenReg(Register memoryOutputBufLenReg) {
		this.memoryOutputBufLenReg = memoryOutputBufLenReg;
	}

	public String getOutputDataAddressIndex() {
		return outputDataAddressIndex;
	}

	public void setOutputDataAddressIndex(String outputDataAddressIndex) {
		this.outputDataAddressIndex = outputDataAddressIndex;
	}
}
