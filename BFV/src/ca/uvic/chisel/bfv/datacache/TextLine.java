package ca.uvic.chisel.bfv.datacache;

public class TextLine implements AbstractLine {

	private final String str;

	TextLine(String str){
		this.str = str;
	}

	@Override
	public String getStringRepresentation() {
		return this.str;
	}
	
}
