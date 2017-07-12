package ca.uvic.chisel.atlantis.recomposition;

import java.util.LinkedList;
import java.util.List;

public class BasicBlockElement implements Comparable<BasicBlockElement> {

	private String startId;
	private String endId;
	
	private long startModuleOffset;
	private long endModuleOffset;
	private String module;
	
	private boolean isRoot;
	private String startInstruction;
	private String endInstruction;
	private List<JumpElement> sourceConnections;
	private List<JumpElement> targetConnections;
	
	public BasicBlockElement(String start, String end) {
		this.startId = start;
		this.endId = end;
		this.sourceConnections = new LinkedList<JumpElement>();
		this.targetConnections = new LinkedList<JumpElement>();
		this.isRoot = false;
		
		this.module = "";
		this.startModuleOffset = this.endModuleOffset = 0;
	}
	
	public String getStartId() {
		return startId;
	}
	
	public void setStartId(String startId) {
		this.startId = startId;
	}
	
	public String getEndId() {
		return endId;
	}
	
	public void setEndId(String endId) {
		this.endId = endId;
	}
	
	public List<JumpElement> getSourceConnections() {
		return sourceConnections;
	}
	
	public void addSourceConnection(JumpElement connection) {
		this.sourceConnections.add(connection);
	}
	
	public List<JumpElement> getTargetConnections() {
		return targetConnections;
	}
	
	public void addTargetConnection(JumpElement connection) {
		this.targetConnections.add(connection);
	}

	public String getStartInstruction() {
		return startInstruction;
	}

	public void setStartInstruction(String startInstruction) {
		this.startInstruction = startInstruction;
	}

	public String getEndInstruction() {
		return endInstruction;
	}

	public void setEndInstruction(String endInstruction) {
		this.endInstruction = endInstruction;
	}
	
	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}
	
	@Override
	public boolean equals(Object other) {
		BasicBlockElement otherElement = (BasicBlockElement)other;
		return startId == otherElement.startId && endId == otherElement.endId;
	}

	@Override
	public int compareTo(BasicBlockElement other) {
		int compare = startId.compareTo(other.startId);
		
		if(compare == 0) {
			compare = endId.compareTo(other.endId);
		}
		
		return (int)compare;
	}

	public long getStartModuleOffset() {
		return startModuleOffset;
	}

	public void setStartModuleOffset(long startModuleOffset) {
		this.startModuleOffset = startModuleOffset;
	}

	public long getEndModuleOffset() {
		return endModuleOffset;
	}

	public void setEndModuleOffset(long endModuleOffset) {
		this.endModuleOffset = endModuleOffset;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}
}
