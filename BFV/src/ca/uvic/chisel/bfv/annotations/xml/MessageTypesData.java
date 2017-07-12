package ca.uvic.chisel.bfv.annotations.xml;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import ca.uvic.chisel.bfv.dualtrace.MessageType;

/**
 * Represents comments metadata to be written to or read from XML.
 * @author Laura Chan
 */
@XmlRootElement
public class MessageTypesData extends XMLDualTraceMetadata {
	
	private Collection<MessageType> MessageTypes;
	
	/**
	 * Creates a new, empty CommentsData instance.
	 */
	public MessageTypesData() {
		super();
		MessageTypes = null;
	}

	/**
	 * Get the comment groups (and the comments that they contain) that were read from XML.
	 * @return comment groups that were read
	 */
	@XmlElementWrapper
	@XmlElement(name="messageType")
	public Collection<MessageType> getMessageTypes() {
		return MessageTypes;
	}

	/**
	 * Set the comment groups (and the comments that they contain) to be written to XML.
	 * @param commentGroups comment groups to be written
	 */
	public void setMessageTypes(Collection<MessageType> messageTypes) {
		this.MessageTypes = messageTypes;
	}
}
