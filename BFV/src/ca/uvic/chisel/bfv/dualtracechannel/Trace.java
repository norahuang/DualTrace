package ca.uvic.chisel.bfv.dualtracechannel;

import java.util.ArrayList;
import java.util.List;

public class Trace {
	private String traceName;
	private List<Channel> channels;
    private ChannelGroup channelGroup;
    
	public Trace(String traceName, ChannelGroup channelGroup) {
		super();
		this.traceName = traceName;
		this.channels = new ArrayList<Channel>();
		this.channelGroup = channelGroup;
	}
	
	public Trace(String traceName) {
		super();
		this.traceName = traceName;
		this.channels = new ArrayList<Channel>();
	}
	
	public String toString(){
		return traceName;
	}
    
	public String getTraceName() {
		return traceName;
	}
	public void setTraceName(String traceName) {
		this.traceName = traceName;
	}
	public List<Channel> getChannels() {
		return channels;
	}

	public void setChannels(List<Channel> channels) {
		this.channels = channels;
		for(Channel c: channels){
			c.setTrace(this);
		}
	}
	public ChannelGroup getChannelGroup() {
		return channelGroup;
	}
	public void setChannelGroup(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}

	public void addChannel(Channel channel){
		this.channels.add(channel);
	}
	
	public void removeChannel(Channel channel){
		this.channels.remove(channel);
	}
}
