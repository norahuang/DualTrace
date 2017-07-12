package ca.uvic.chisel.atlantis.recomposition;

public class JumpElement implements Comparable<JumpElement> {
	private BasicBlockElement from;
	private BasicBlockElement to;
	private boolean isBranch;
	private boolean branchTaken;
	
	public JumpElement(BasicBlockElement to, BasicBlockElement from, boolean isBranch, boolean branchTaken) {
		this.to = to;
		this.from = from;
		this.isBranch = isBranch;
		this.branchTaken = branchTaken;
	}
	
	public BasicBlockElement getFrom() {
		return from;
	}
	
	public void setFrom(BasicBlockElement from) {
		this.from = from;
	}
	
	public BasicBlockElement getTo() {
		return to;
	}
	
	public void setTo(BasicBlockElement to) {
		this.to = to;
	}

	public boolean isBranch() {
		return isBranch;
	}

	public void setBranch(boolean isBranch) {
		this.isBranch = isBranch;
	}

	public boolean isBranchTaken() {
		return branchTaken;
	}

	public void setBranchTaken(boolean branchTaken) {
		this.branchTaken = branchTaken;
	}
	
	@Override
	public boolean equals(Object other) {
		JumpElement otherJump = (JumpElement)other;
		
		return from.getEndId() == otherJump.from.getEndId() &&
				to.getStartId() == otherJump.to.getStartId();
	}
	
	@Override
	public String toString() {
		return from.getEndId() + " --> " + to.getStartId();
	}

	@Override
	public int compareTo(JumpElement other) {
		int compare = from.getEndId().compareTo(other.from.getEndId());
		
		if(compare == 0) {
			compare = to.getStartId().compareTo(other.to.getStartId());
		}
		
		return compare;
	}
}
