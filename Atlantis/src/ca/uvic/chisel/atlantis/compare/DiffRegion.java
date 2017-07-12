package ca.uvic.chisel.atlantis.compare;

import org.eclipse.jface.text.Position;

public class DiffRegion {
	
	private int leftStartLine;
	private int leftEndLine;
	private int rightStartLine;
	private int rightEndLine;
	
	private long leftOffset;
	private int leftLength;
	private long rightOffset;
	private int rightLength;
	
	private boolean isMatch;
	
	public DiffRegion(){
		
	}
	
	public DiffRegion(int rightStartLine, int rightEndLine, int leftStartLine, int leftEndLine){
		this.rightStartLine = rightStartLine;
		this.rightEndLine = rightEndLine;
		this.leftStartLine = leftStartLine;
		this.leftEndLine = leftEndLine;
	}
	
	public boolean checkStartValidity(){
		if (this.rightStartLine != -1 &&
				this.leftStartLine != -1){
			return true;
		}
		return false;
	}
	
	public int getLeftStartLine(){
		return leftStartLine;
	}
	
	public int getLeftEndLine(){
		return leftEndLine;
	}
	
	public void setLeftStartLine(int newStartLine){
		leftStartLine = newStartLine;
	}

	public void setLeftEndLine(int newEndLine){
		leftEndLine = newEndLine;
	}
	
	public int getRightStartLine(){
		return rightStartLine;
	}
	
	public int getRightEndLine(){
		return rightEndLine;
	}
	
	public void setRightStartLine(int newStartLine){
		rightStartLine = newStartLine;
	}
	
	public void setRightEndLine(int newEndLine){
		rightEndLine = newEndLine;
	}
	
	public void setLeftOffset(long newLeftOffset){
		leftOffset = newLeftOffset;
	}
	
	public void setLeftLength(int newLeftLength){
		leftLength = newLeftLength;
	}
	
	public void setRightOffset(long newRightOffset){
		rightOffset = newRightOffset;
	}
	
	public void setRightLength(int newRightLength){
		rightLength = newRightLength;
	}
	
	public long getLeftOffset(){
		return leftOffset;
	}
	
	public int getLeftLength(){
		return leftLength;
	}
	
	public long getRightOffset(){
		return rightOffset;
	}
	
	public int getRightLength(){
		return rightLength;
	}
	
	public void setOffsets(long newRightOffset, int newRightLength, long newLeftOffset, int newLeftLength ){
		rightOffset = newRightOffset;
		rightLength = newRightLength;
		leftOffset = newLeftOffset;
		leftLength = newLeftLength;
	}
	
	public Position getLeftPosition(){
		return new Position((int)leftOffset, leftLength);
	}
	
	public Position getRightPosition(){
		return new Position((int)rightOffset, rightLength);
	}
	
	public boolean intersectsWithLeft(Position p){
		if(p != null){
			return p.overlapsWith((int)leftOffset, leftLength);	
		}
		return false;
	}
	
	public boolean intersectsWithRight(Position p){
		if(p != null){
			return p.overlapsWith((int)rightOffset, rightLength);	
		}
		return false;
	}
	
	
	boolean isInRangeLeft(int pos) {
		return (pos >= leftOffset) && (pos < (leftOffset +leftLength));
	}
	
	boolean isInRangeRight(int pos) {
		return (pos >= rightOffset) && (pos < (rightOffset +rightLength));
	}
	
	public void setMatchState(boolean matchState){
		isMatch = matchState;
	}
	
	public boolean getMatchState(){
		return isMatch;
	}
	
	
	
	@Override
	public boolean equals(Object competitor){
		DiffRegion otherRegion = (DiffRegion)competitor;
		if(this.getLeftEndLine() == otherRegion.getLeftEndLine() &&
				this.getLeftStartLine() == otherRegion.getLeftStartLine() &&
					this.getRightEndLine() == otherRegion.getRightEndLine() &&
						this.getRightStartLine() == otherRegion.getRightStartLine()){
			return true;
		}
		
		return false;
	}
	
	public int getLeftRegionHeight(){
		return leftEndLine - leftStartLine + 1;
	}
	
	public int getRightRegionHeight(){
		return rightEndLine - rightStartLine + 1;
	}
	
	public int getMaxRegionHeight(){
		return Math.max((leftEndLine - leftStartLine +1),(rightEndLine - rightStartLine +1));
	}
	
	public String printOffset(){
		return ("Left Offset:Length\t" + leftOffset +":" + leftLength + "\t\tRight Offset:Length\t" + rightOffset + ":" + rightLength );
	}
	
	@Override
	public String toString(){
		return ("Left: " + leftStartLine + "..." + leftEndLine + "	Right: "  + rightStartLine + "..." + rightEndLine);
	}
}
