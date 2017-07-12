package ca.uvic.chisel.bfv.editor;

import ca.uvic.chisel.bfv.intervaltree.Interval;

public abstract class DocumentProjectionInterval extends Interval {

	/**
	 *  The idea for this field is that line number in our projection interval + offsetFromFile = line number in the file
	 */
	protected int lineOffsetFromFile;
	
	/**
	 * @param start the start line of the interval (in the projection document, not the file document)
	 * @param end the end line of the interval (in the projection document, not the file document)
	 */
	public DocumentProjectionInterval(long start, long end, int lineOffsetFromFile) {
		super(start, end);
		this.lineOffsetFromFile = lineOffsetFromFile;
	}

	public int getLineOffsetFromFile() {
		return lineOffsetFromFile;
	}

	public void setLineOffsetFromFile(int lineOffsetFromFile) {
		this.lineOffsetFromFile = lineOffsetFromFile;
	}
	
	public abstract boolean isPagedIn();		
	
	@Override
	public String toString() {
		return (isPagedIn()? "i" : "o") + ":["+getStartValue() + ", " + getEndValue() + "] offset("+lineOffsetFromFile+")";
	}
	
	public void shift(long shift) {
		start += shift;
		end += shift;
		lineOffsetFromFile -= shift;
	}
	
	public long getIntervalCharLength() {
		return getIntervalCharLength(start, end);
	}
	
	public long getIntervalCharLength(int line) {
		return getIntervalCharLength(line, line);
	}
	
	public abstract long getIntervalCharLength(long startValue, long endValue);
}
