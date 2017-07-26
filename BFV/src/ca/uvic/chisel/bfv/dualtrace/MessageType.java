package ca.uvic.chisel.bfv.dualtrace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import ca.uvic.chisel.bfv.ColourConstants;
import ca.uvic.chisel.bfv.annotations.Comment;

/**
 * Represents a group or category of comments. Comments that do not have a group should be placed in the "No Group" comment group.
 * @see Comment
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"name", "send", "receive","sendChannelCreate","receiveChannelCreate"})
public class MessageType {	
	public static final String DEFAULT_COLOUR = ColourConstants.TOOLTIP_GREEN;

	private String name;
	private MessageFunction send;
	private MessageFunction receive;
	private MessageFunction sendChannelCreate;
	private MessageFunction receiveChannelCreate;

	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private MessageType() {}
	
	/**
	 * Creates a new comment group with the specified name.
	 * @param name
	 */
	/**
	 * @param name
	 */
	public MessageType(String name) {
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		this.name = name;	
	}
	
	@XmlElement(name="send")
	public MessageFunction getSend() {
		return send;
	}

	public void setSend(MessageFunction send) {
		this.send = send;
		this.send.setType(this);
	}
	
	@XmlElement(name="receive")
	public MessageFunction getReceive() {
		return receive;
	}

	public void setReceive(MessageFunction receive) {
		this.receive = receive;
		this.receive.setType(this);
	}
	
	@XmlElement(name="sendChannelCreate")
	public MessageFunction getSendChannelCreate() {
		return sendChannelCreate;
	}

	public void setSendChannelCreate(MessageFunction sendChannelCreate) {
		this.sendChannelCreate = sendChannelCreate;
		this.sendChannelCreate.setType(this);
	}
	
	@XmlElement(name="receiveChannelCreate")
	public MessageFunction getReceiveChannelCreate() {
		return receiveChannelCreate;
	}

	public void setReceiveChannelCreate(MessageFunction receiveChannelCreate) {
		this.receiveChannelCreate = receiveChannelCreate;
		this.receiveChannelCreate.setType(this);
	}

	@Override
	public String toString() {
		return this.getName();
	}
	
	/**
	 * Returns the name of this comment group.
	 * @return comment group name
	 */
	@XmlElement(name="name")
	public String getName() {
		return name;
	}
	
	
	/**
	 * Sets the name of this comment group.
	 * @param name name to use for this group
	 */
	public void setName(String name) {
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		this.name = name;
	}

	
	
}
