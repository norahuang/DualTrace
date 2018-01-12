package ca.uvic.chisel.atlantis.functionparsing;

import java.util.HashSet;
import java.util.Set;

public class ChannelTypeInstruction{
	private String channelTypeName;
	private Set<ChannelFunctionInstruction> channelOpenStageList;
	private Set<ChannelFunctionInstruction> dataTransStageList;
	private Set<ChannelFunctionInstruction> channelCloseStageList;
	
	public ChannelTypeInstruction(String channelTypeName) {
		super();
		this.channelTypeName = channelTypeName;
		this.channelOpenStageList = new HashSet<ChannelFunctionInstruction>();
		this.dataTransStageList = new HashSet<ChannelFunctionInstruction>();
		this.channelCloseStageList =  new HashSet<ChannelFunctionInstruction>();
	}

	public void addFuncInChannelOpenStage(ChannelFunctionInstruction function){
		this.channelOpenStageList.add(function);
	}
	
	public void addFuncInDataTransStage(ChannelFunctionInstruction function){
		this.dataTransStageList.add(function);
	}
	
	public void addFuncInChannelCloseStage(ChannelFunctionInstruction function){
		this.channelCloseStageList.add(function);
	}
	
	public void removeFuncInChannelOpenStage(ChannelFunctionInstruction function){
		this.channelOpenStageList.remove(function);
	}
	
	public void removeFuncInDataTransStage(ChannelFunctionInstruction function){
		this.dataTransStageList.remove(function);
	}
	
	public void removeFuncInChannelCloseStage(ChannelFunctionInstruction function){
		this.channelCloseStageList.remove(function);
	}
	
	public String getChannelTypeName() {
		return channelTypeName;
	}
	public void setChannelTypeName(String channelTypeName) {
		this.channelTypeName = channelTypeName;
	}
	public Set<ChannelFunctionInstruction> getChannelOpenStageList() {
		return channelOpenStageList;
	}
	public void setChannelOpenStageList(Set<ChannelFunctionInstruction> channelOpenStageList) {
		this.channelOpenStageList = channelOpenStageList;
	}
	public Set<ChannelFunctionInstruction> getDataTransStageList() {
		return dataTransStageList;
	}
	public void setDataTransStageList(Set<ChannelFunctionInstruction> dataTransStageList) {
		this.dataTransStageList = dataTransStageList;
	}
	public Set<ChannelFunctionInstruction> getChannelCloseStageList() {
		return channelCloseStageList;
	}
	public void setChannelCloseStageList(Set<ChannelFunctionInstruction> channelCloseStageList) {
		this.channelCloseStageList = channelCloseStageList;
	}
	
	
	
	
}
