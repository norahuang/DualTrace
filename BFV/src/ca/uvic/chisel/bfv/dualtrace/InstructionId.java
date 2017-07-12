package ca.uvic.chisel.bfv.dualtrace;

public class InstructionId implements Comparable<InstructionId> {
	private final String globallyUniqueInstructionId;
	
	public InstructionId(String globallyUniqueId){
		this.globallyUniqueInstructionId = globallyUniqueId;
	}
	
	public String getString(){
		return this.globallyUniqueInstructionId;
	}
	
	@Override
	public boolean equals(Object b){
		return b instanceof InstructionId &&
				this.globallyUniqueInstructionId.equals(((InstructionId)b).globallyUniqueInstructionId);
	}
	
	@Override
	public int compareTo(InstructionId b){
		return this.globallyUniqueInstructionId.compareTo(b.globallyUniqueInstructionId);
	}
	
	@Override
	public String toString(){
		return this.globallyUniqueInstructionId;
	}

}
