package ca.uvic.chisel.bfv.dualtracechannel;

import javax.xml.bind.annotation.XmlElement;

public class ChannelGroup{
	private String groupName;
	private Trace trace1;
	private Trace trace2;
	
/*	public enum ChennelType {
	    TCPChannels,
	    UDPChannels,
	    NamedPipeChannels;			
	}*/
	
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private ChannelGroup() {}
	
	/**
	 * Creates a new comment group with the specified name.
	 * @param name
	 */
	public ChannelGroup(String name, Trace trace1, Trace trace2) {
		this.groupName = name;	
		this.trace1 = trace1;
		this.trace2 = trace2;
        
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

	public Trace getTrace1() {
		return trace1;
	}

	public void setTrace1(Trace trace1) {
		this.trace1 = trace1;
	}

	public Trace getTrace2() {
		return trace2;
	}

	public void setTrace2(Trace trace2) {
		this.trace2 = trace2;
	}
	

}
