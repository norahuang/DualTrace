package ca.uvic.chisel.bfv.dualtracechannelmatch;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class MatchChannelGroup{
	private String groupName;
	private List<MatchChannel> channels;
	private String trace1Name;
	private String trace2Name;

	

	public String getTrace1Name() {
		return trace1Name;
	}

	public void setTrace1Name(String trace1Name) {
		this.trace1Name = trace1Name;
	}

	public String getTrace2Name() {
		return trace2Name;
	}

	public void setTrace2Name(String trace2Name) {
		this.trace2Name = trace2Name;
	}

	public List<MatchChannel> getChannels() {
		return channels;
	}

	public void setChannels(List<MatchChannel> channels) {
		this.channels = channels;
	}

	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private MatchChannelGroup() {}
	
	public MatchChannelGroup(String groupName, String trace1Name, String trace2Name) {
		super();
		this.channels = channels;
		this.groupName = groupName;
		this.trace1Name = trace1Name;
		this.trace2Name = trace2Name;
		this.channels = new ArrayList<MatchChannel>();
	}

	/**
	 * Creates a new comment group with the specified name.
	 * @param name
	 */
	public MatchChannelGroup(String name) {
		this.groupName = name;	
	}
	
	@Override
	public String toString() {
		return this.getName().toString();
	}
	
	/**
	 * Returns the name of this comment group.
	 * @return comment group name
	 */
	@XmlElement
	public String getName() {
		return groupName;
	}
	
	/**
	 * Sets the name of this comment group.
	 * @param name name to use for this group
	 */
	protected void setName(String name) {
		this.groupName = name;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public void addChannelToGroup(MatchChannel channel){
	    this.channels.add(channel);	
	}
	
	public void removeChannelFromGroup(MatchChannel channel){
		this.channels.remove(channel);	
	}

}
