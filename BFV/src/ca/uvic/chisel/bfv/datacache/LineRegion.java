package ca.uvic.chisel.bfv.datacache;

import org.eclipse.jface.text.IRegion;

public class LineRegion implements IRegion {
	private long offset;
	private long length;
	private long lineNum;
	private long delimeterLength;

	public LineRegion(long offset, long length, long lineNum, long delimeterLength) {
		this.offset = offset;
		this.length = length;
		this.lineNum = lineNum;
		this.delimeterLength = delimeterLength;
	}
	
	public long getLineNum() {
		return lineNum;
	}

	@Override
	public int getLength() {
		return (int)(length - delimeterLength);
	}

	@Override
	public int getOffset() {
		return (int)offset;
	}
}