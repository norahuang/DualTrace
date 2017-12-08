package ca.uvic.chisel.atlantis.functionparsing;

public class ChannelFunctionGeneral {
	private Register RetrunValReg = new Register("RAX",true);
	
	public Register getRetrunValReg() {
		return RetrunValReg;
	}

	public void setRetrunValReg(Register retrunValReg) {
		RetrunValReg = retrunValReg;
	}
}
