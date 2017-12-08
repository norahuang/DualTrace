package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;

public class ChannelTypeInstruction{
	private String channelTypeName;
	private List<ChannelFunctionInstruction> channelOpenStageList;
	private List<ChannelFunctionInstruction> dataTransStageList;
	private List<ChannelFunctionInstruction> channelCloseStageList;
	
	public ChannelTypeInstruction(String channelTypeName) {
		super();
		this.channelTypeName = channelTypeName;
		this.channelOpenStageList = new ArrayList<ChannelFunctionInstruction>();
		this.dataTransStageList = new ArrayList<ChannelFunctionInstruction>();
		this.channelCloseStageList =  new ArrayList<ChannelFunctionInstruction>();
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
	public List<ChannelFunctionInstruction> getChannelOpenStageList() {
		return channelOpenStageList;
	}
	public void setChannelOpenStageList(List<ChannelFunctionInstruction> channelOpenStageList) {
		this.channelOpenStageList = channelOpenStageList;
	}
	public List<ChannelFunctionInstruction> getDataTransStageList() {
		return dataTransStageList;
	}
	public void setDataTransStageList(List<ChannelFunctionInstruction> dataTransStageList) {
		this.dataTransStageList = dataTransStageList;
	}
	public List<ChannelFunctionInstruction> getChannelCloseStageList() {
		return channelCloseStageList;
	}
	public void setChannelCloseStageList(List<ChannelFunctionInstruction> channelCloseStageList) {
		this.channelCloseStageList = channelCloseStageList;
	}
	
	public void addAllToChannelOpenStageList(List<ChannelFunctionInstruction> list) {
		this.channelOpenStageList = ListUtils.union(this.channelOpenStageList,list);
	}
	public void addAllToDataTransStageList(List<ChannelFunctionInstruction> list) {
		this.dataTransStageList = ListUtils.union(this.dataTransStageList,list);
	}
	public void addAllToChannelCloseStageList(List<ChannelFunctionInstruction> list) {
		this.channelCloseStageList = ListUtils.union(this.channelCloseStageList,list);
	}
	
	
	
}
