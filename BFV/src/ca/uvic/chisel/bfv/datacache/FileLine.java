package ca.uvic.chisel.bfv.datacache;

public class FileLine {

	private int lineNumber;
	private String lineContent;
	private long lineOffset;
	private int lineLength;
	
	public FileLine(int line, String content){
		lineNumber = line;
		lineContent = content;
	}
	
	public FileLine(){
		
	}
	
	public int getLineNumber(){
		return lineNumber;
	}
	
	public String getLineContent(){
		return lineContent;
	}
	
	public void setLineNumber(int lineNumber){
		this.lineNumber = lineNumber;
	}
	
	public void setLineContent(String lineContent){
		this.lineContent = lineContent;
	}
	
	public void setLineOffset(long lineOffset){
		this.lineOffset = lineOffset;
		this.lineLength = this.lineContent.length();
	}
	
	public long getLineOffset(){
		return lineOffset;
	}
	
	public int getLineLength(){
		return lineLength;
	}
	
	@Override
	public String toString(){
		return (lineNumber + "..." + lineContent);
	}
	
	@Override
	public boolean equals(Object object){
		FileLine competitor = (FileLine)object;
		if(lineNumber == competitor.getLineNumber()){
			return true;
		}
		return false;
	}
}
