package ca.uvic.chisel.atlantis.functionparsing;

public class ChannelFunction {
	private Register retrunValReg = new Register("RAX",true);
	private Register valueInputReg;
	private Register memoryInputReg;
	private Register memoryOutputReg;
	private String functionName;
	private boolean createHandle;
	


	public ChannelFunction(String functionName, Register retrunValReg, Register valueInputReg, Register memoryInputReg,
			Register memoryOutputReg, boolean createHandle) {
		super();
		this.retrunValReg = retrunValReg;
		this.valueInputReg = valueInputReg;
		this.memoryInputReg = memoryInputReg;
		this.memoryOutputReg = memoryOutputReg;
		this.functionName = functionName;
		this.createHandle = createHandle;
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
}
