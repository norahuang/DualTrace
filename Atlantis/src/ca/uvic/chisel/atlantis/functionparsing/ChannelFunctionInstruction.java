package ca.uvic.chisel.atlantis.functionparsing;

public class ChannelFunctionInstruction{

	
    private Instruction instruction;
    private ChannelFunction function;

	public ChannelFunctionInstruction(ChannelFunction function,Instruction instruction) {
		this.instruction = instruction;
		this.function = function;

	}

	public Instruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
	}
	
	public ChannelFunction getFunction() {
		return function;
	}

	public void setFunction(ChannelFunction function) {
		this.function = function;
	}

	
}
