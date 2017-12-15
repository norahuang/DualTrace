package ca.uvic.chisel.bfv.dualtracechannel;

public class FullFunctionMatch {
	private BfvFileWithAddrMatch eventStart;
	private BfvFileWithAddrMatch eventEnd;
	private String inputVal;
	private String retVal;
	private FunctionType type;



	public FullFunctionMatch(BfvFileWithAddrMatch eventStart, BfvFileWithAddrMatch eventEnd, String inputVal, String retVal, FunctionType type) {
		super();
		this.eventStart = eventStart;
		this.eventEnd = eventEnd;
		this.inputVal = inputVal;
		this.retVal = retVal;
		this.type = type;
	}
	
	public BfvFileWithAddrMatch getEventStart() {
		return eventStart;
	}
	public FunctionType getType() {
		return type;
	}

	public void setType(FunctionType type) {
		this.type = type;
	}

	public void setEventStart(BfvFileWithAddrMatch eventStart) {
		this.eventStart = eventStart;
	}
	public BfvFileWithAddrMatch getEventEnd() {
		return eventEnd;
	}
	public void setEventEnd(BfvFileWithAddrMatch eventEnd) {
		this.eventEnd = eventEnd;
    }
	public String getRetVal() {
		return retVal;
	}

	public void setRetVal(String retVal) {
		this.retVal = retVal;
	}
	
	public String getInputVal() {
		return inputVal;
	}

	public void setInputVal(String inputVal) {
		this.inputVal = inputVal;
	}
	
}
