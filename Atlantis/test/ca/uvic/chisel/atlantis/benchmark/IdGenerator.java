package ca.uvic.chisel.atlantis.benchmark;

public class IdGenerator {

	private int branchOutFactor;
	private int treeHeight;

	public IdGenerator(int branchOutFactor, int treeHeight) {
		this.treeHeight = treeHeight;
		this.branchOutFactor = branchOutFactor;
		
	}
	
	// level is 0 indexed, pos is 0 indexed
	public int getId(int level, int pos) {
		int start = 0;
		for(int i=1; i <= level; i++) {
			start += Math.pow(branchOutFactor, treeHeight - i);
		}
		
		int id = start + pos;
		
		return id;
	}
}
