package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;

public class ChannelType{
	private String channelTypeName;
	private List<ChannelFunction> channelOpenStageList;
	private List<ChannelFunction> dataTransStageList;
	private List<ChannelFunction> channelCloseStageList;
	
	public ChannelType(String channelTypeName) {
		super();
		this.channelTypeName = channelTypeName;
		this.channelOpenStageList = new ArrayList<ChannelFunction>();
		this.dataTransStageList = new ArrayList<ChannelFunction>();
		this.channelCloseStageList =  new ArrayList<ChannelFunction>();
	}

	public void addFuncInChannelOpenStage(ChannelFunction function){
		this.channelOpenStageList.add(function);
	}
	
	public void addFuncInDataTransStage(ChannelFunction function){
		this.dataTransStageList.add(function);
	}
	
	public void addFuncInChannelCloseStage(ChannelFunction function){
		this.channelCloseStageList.add(function);
	}
	
	public void removeFuncInChannelOpenStage(ChannelFunction function){
		this.channelOpenStageList.remove(function);
	}
	
	public void removeFuncInDataTransStage(ChannelFunction function){
		this.dataTransStageList.remove(function);
	}
	
	public void removeFuncInChannelCloseStage(ChannelFunction function){
		this.channelCloseStageList.remove(function);
	}
	
	public String getChannelTypeName() {
		return channelTypeName;
	}
	public void setChannelTypeName(String channelTypeName) {
		this.channelTypeName = channelTypeName;
	}
	public List<ChannelFunction> getChannelOpenStageList() {
		return channelOpenStageList;
	}
	public void setChannelOpenStageList(List<ChannelFunction> channelOpenStageList) {
		this.channelOpenStageList = channelOpenStageList;
	}
	public List<ChannelFunction> getDataTransStageList() {
		return dataTransStageList;
	}
	public void setDataTransStageList(List<ChannelFunction> dataTransStageList) {
		this.dataTransStageList = dataTransStageList;
	}
	public List<ChannelFunction> getChannelCloseStageList() {
		return channelCloseStageList;
	}
	public void setChannelCloseStageList(List<ChannelFunction> channelCloseStageList) {
		this.channelCloseStageList = channelCloseStageList;
	}	
	
}
