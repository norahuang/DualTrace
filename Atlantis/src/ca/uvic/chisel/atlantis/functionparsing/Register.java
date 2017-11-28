package ca.uvic.chisel.atlantis.functionparsing;

public class Register {
	private String name;
	private boolean isValue; //is register storing a value or a memory address?
	
	public Register(String name, boolean isValue) {
		super();
		this.name = name;
		this.isValue = isValue;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isValue() {
		return isValue;
	}
	public void setValue(boolean isValue) {
		this.isValue = isValue;
	}
	

}
