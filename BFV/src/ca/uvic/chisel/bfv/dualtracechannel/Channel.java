package ca.uvic.chisel.bfv.dualtracechannel;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.source.Annotation;


public class Channel extends Annotation implements Comparable<Channel> {
	public static final String TYPE_ANNOTATION_TYPE = "ca.uvic.chisel.bfv.dualtrace.channel";		
	private Trace trace;
	private int channelStartLineNum;
	private int channelEndLineNum;
	private List<ChannelEvent> events;
	private String channelHandle;
	private String channelID;



	public Channel(Trace trace, int channelStartLineNum, String channelHandle, String channelID){
		this.trace = trace;
		this.channelStartLineNum = channelStartLineNum;
		this.channelHandle = channelHandle;
		this.channelID = channelID;
		this.events = new ArrayList<ChannelEvent>();
	}


	@Override
	public int compareTo(Channel other) {
		if (other == null) {
			return -1;
		} 
		
		return Integer.compare(channelStartLineNum,other.getChannelStartLineNum());	
	}
	
	@Override
	public String toString(){
		return "Channel:" + channelID + ", Channel Handle:" + channelHandle;
	}
	
	public List<ChannelEvent> getEvents() {
		return events;
	}

	public void setEvents(List<ChannelEvent> events) {
		this.events = events;
	}

	public Trace getTrace() {
		return trace;
	}

	public void setTrace(Trace trace) {
		this.trace = trace;
	}

	public int getChannelStartLineNum() {
		return channelStartLineNum;
	}

	public void setChannelStartLineNum(int channelStartLineNum) {
		this.channelStartLineNum = channelStartLineNum;
	}

	
	public void addEvent(ChannelEvent event){
		this.events.add(event);
	}
	
	public String getChannelHandle() {
		return channelHandle;
	}

	public void setChannelHandle(String channelHandle) {
		this.channelHandle = channelHandle;
	}
	
	public int getChannelEndLineNum() {
		return channelEndLineNum;
	}


	public void setChannelEndLineNum(int channelEndLineNum) {
		this.channelEndLineNum = channelEndLineNum;
	}
	

	public String getChannelID() {
		return channelID;
	}


	public void setChannelID(String channelID) {
		this.channelID = channelID;
	}
}
