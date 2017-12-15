package ca.uvic.chisel.bfv.dualtracechannelmatch;

import ca.uvic.chisel.bfv.dualtracechannel.FullFunctionMatch;

public class FullFunctionMatchOfTrace extends FullFunctionMatch{
	private String traceName;
	private String functionName;
	

	public FullFunctionMatchOfTrace(FullFunctionMatch match, String name, String functionName) {
		super(match.getEventEnd(), match.getEventStart(), match.getInputVal(), match.getRetVal(), match.getType());
		this.traceName = name;
		this.functionName = functionName;
	}

	public String toString(){
		return this.getType()+": "+this.functionName+"() in "+ this.traceName+ " at line " + this.getEventStart().getLineElement().getLine();
	}

	public String getTraceName() {
		return traceName;
	}


	public void setTraceName(String traceName) {
		this.traceName = traceName;
	}
	
	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	
}
