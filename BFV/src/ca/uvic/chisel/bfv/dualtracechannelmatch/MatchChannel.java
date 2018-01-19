package ca.uvic.chisel.bfv.dualtracechannelmatch;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.source.Annotation;


public class MatchChannel extends Annotation implements Comparable<MatchChannel> {
	public static final String TYPE_ANNOTATION_TYPE = "ca.uvic.chisel.bfv.dualtrace.matchchannel";	
	private List<ChannelOpenCloseEvent> openEventsInTrace1;
	private List<ChannelOpenCloseEvent> openEventsInTrace2;
	private List<ChannelOpenCloseEvent> closeEventsInTrace1;
	private List<ChannelOpenCloseEvent> closeEventsInTrace2;
	private List<ChannelDataTransEvent> channelDatatransEvents;
	private String channelID;
	private MatchChannelGroup channelGroup;


	public MatchChannel(String channelID, MatchChannelGroup channelGroup){
		this.channelID = channelID;
		this.openEventsInTrace1 = new ArrayList<ChannelOpenCloseEvent>();
		this.openEventsInTrace2 = new ArrayList<ChannelOpenCloseEvent>();
		this.closeEventsInTrace1 = new ArrayList<ChannelOpenCloseEvent>();
		this.closeEventsInTrace2 = new ArrayList<ChannelOpenCloseEvent>();
		this.channelDatatransEvents = new ArrayList<ChannelDataTransEvent>();
		this.channelGroup = channelGroup;
	}


	@Override
	public int compareTo(MatchChannel other) {
		if (other == null) {
			return -1;
		} 
		
		int minStartLineNumber = 0;
		for(ChannelOpenCloseEvent openevent: openEventsInTrace1){
			int startLineNumber = openevent.getFullFunctionMatch().getEventStart().getLineElement().getLine();
			if(minStartLineNumber == 0 || startLineNumber < minStartLineNumber){
				minStartLineNumber = startLineNumber;
			}
		}
		
		int minStartLineNumberOther = 0;
		for(ChannelOpenCloseEvent openevent: other.openEventsInTrace1){
			int startLineNumber = openevent.getFullFunctionMatch().getEventStart().getLineElement().getLine();
			if(minStartLineNumberOther == 0 || startLineNumber < minStartLineNumberOther){
				minStartLineNumberOther = startLineNumber;
			}
		}
		
		return Integer.compare(minStartLineNumber,minStartLineNumberOther);	
	}
	
	@Override
	public String toString(){
		return "Communication:" + channelID;
	}
	
	

	public List<ChannelOpenCloseEvent> getOpenEventsInTrace1() {
		return openEventsInTrace1;
	}


	public void setOpenEventsInTrace1(List<ChannelOpenCloseEvent> openEventsInTrace1) {
		this.openEventsInTrace1 = openEventsInTrace1;
	}


	public List<ChannelOpenCloseEvent> getOpenEventsInTrace2() {
		return openEventsInTrace2;
	}


	public void setOpenEventsInTrace2(List<ChannelOpenCloseEvent> openEventsInTrace2) {
		this.openEventsInTrace2 = openEventsInTrace2;
	}


	public List<ChannelOpenCloseEvent> getCloseEventsInTrace1() {
		return closeEventsInTrace1;
	}


	public void setCloseEventsInTrace1(List<ChannelOpenCloseEvent> closeEventsInTrace1) {
		closeEventsInTrace1 = closeEventsInTrace1;
	}


	public List<ChannelOpenCloseEvent> getCloseEventsInTrace2() {
		return closeEventsInTrace2;
	}


	public void setCloseEventsInTrace2(List<ChannelOpenCloseEvent> closeEventsInTrace2) {
		closeEventsInTrace2 = closeEventsInTrace2;
	}


	public List<ChannelDataTransEvent> getChannelDatatransEvents() {
		return channelDatatransEvents;
	}


	public void setChannelDatatransEvents(List<ChannelDataTransEvent> channelDatatransEvents) {
		this.channelDatatransEvents = channelDatatransEvents;
	}



	public String getChannelID() {
		return channelID;
	}


	public void setChannelID(String channelID) {
		this.channelID = channelID;
	}
	
	public void addEventToOpenList1(ChannelOpenCloseEvent event){
		this.openEventsInTrace1.add(event);
	}
	
	public void addEventToOpenList2(ChannelOpenCloseEvent event){
		this.openEventsInTrace2.add(event);
	}
	
	public void addEventToCloseList1(ChannelOpenCloseEvent event){
		this.closeEventsInTrace1.add(event);
	}
	
	public void addEventToCloseList2(ChannelOpenCloseEvent event){
		this.closeEventsInTrace2.add(event);
	}
	
	public void addEventToDataTransList(ChannelDataTransEvent event){
		this.channelDatatransEvents.add(event);
	}
	

	public MatchChannelGroup getChannelGroup() {
		return channelGroup;
	}


	public void setChannelGroup(MatchChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
}
