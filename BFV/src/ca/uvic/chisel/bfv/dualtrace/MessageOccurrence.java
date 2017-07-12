package ca.uvic.chisel.bfv.dualtrace;

import javax.xml.bind.annotation.*;

import org.eclipse.jface.text.source.Annotation;

/**
 * Represents a specific occurrence of a tag.
 * @see Tag
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"send", "recv"})
public class MessageOccurrence extends Annotation implements Comparable<MessageOccurrence> {
	public static final String TYPE_ANNOTATION_TYPE = "ca.uvic.chisel.bfv.dualtrace.messagetype";
	
	private MessageType messageType;
	
	// Note: lines and characters are stored in 0-indexed form, but the line number ruler and status bar use 1-indexing.
	// When displaying this information to the user, it will need to be converted into a 1-indexed form.
	// TODO: will the start and end chars need to be longs in the future?
	private MessageOccurrenceSR send;
	private MessageOccurrenceSR recv;

	
	//private boolean showStickyTooltip;
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private MessageOccurrence() {}
	
	/**
	 * Creates a new occurrence of the specified tag at the given location. Note that while this constructor associates this occurrence 
	 * with the tag, it does not actually add the occurrence to the tag. Clients should use FileAnnotationStorage.addTag() instead.
	 * @param tag tag for which we are creating a new occurrence
	 * @param startLine start line of new occurrence
	 * @param startChar start char of new occurrence
	 * @param endLine end line of new occurrence
	 * @param endChar end char of new occurrence
	 */
	public MessageOccurrence(MessageType type, MessageOccurrenceSR send, MessageOccurrenceSR recv) {
		super();
		
		if (type == null) {
			throw new IllegalArgumentException("Associated type cannot be null");
		}
		if (send.getStartLine() < 0 || send.getStartChar() < 0 || send.getEndLine() < 0 || send.getEndChar() < 0) {
			throw new IllegalArgumentException("Start and end lines/chars of sender cannot be negative");
		}
		
		if (recv.getStartLine() < 0 || recv.getStartChar() < 0 || recv.getEndLine() < 0 || recv.getEndChar() < 0) {
			throw new IllegalArgumentException("Start and end lines/chars of receiver cannot be negative");
		} 
		
		if (send.getStartLine() > send.getEndLine()) {
			throw new IllegalArgumentException("Start line cannot be greater than end line in sender");
		}
		
		if (recv.getStartLine() > recv.getEndLine()) {
			throw new IllegalArgumentException("Start line cannot be greater than end line in receiver");
		}
		
		if (send.getStartLine() == send.getEndLine() && send.getStartChar() > send.getEndChar()) {
			throw new IllegalArgumentException("Start char cannot be greater than end char if they are on the same line");
		}
		
		if (send.getStartLine() == send.getEndLine() && send.getStartChar() > send.getEndChar()) {
			throw new IllegalArgumentException("Start char cannot be greater than end char if they are on the same line");
		}
	
		this.messageType = type;
		this.send = send;
		this.recv = recv;
		
		setType(TYPE_ANNOTATION_TYPE);
		setText(this.toString());
	}

	
	/**
	 * Get the tag that this occurrence is associated with.
	 * @return associated tag
	 */

	public MessageType getMessageType() {
		return this.messageType;
	}
	/**
	 * Sets the tag that this occurrence is associated with.
	 * @param tag tag to associate with this occurrence
	 */
	protected void setMessageType(MessageType type) {
		this.messageType = type;
	}


	/**
	 * Compares the start and end location of this tag occurrence to another occurrence. Does not check the occurrences' associated tags.
	 * @param other tag occurrence to compare this occurrence to
	 * @return -1 if this occurrence's location is before the other's, 1 if it is after the other's, or 0 if they have the same location
	 */
	@Override
	public int compareTo(MessageOccurrence other) {
		if (other == null) {
			return -1;
		} 
		
		int sendresult = this.send.compareTo(other.send);
		if (sendresult == 0){
			sendresult = this.recv.compareTo(other.recv);
        }
		return sendresult;		
	}
	
	@XmlElement
	public MessageOccurrenceSR getSend() {
		return send;
	}

	public void setSend(MessageOccurrenceSR send) {
		this.send = send;
	}

	@XmlElement
	public MessageOccurrenceSR getRecv() {
		return recv;
	}

	public void setRecv(MessageOccurrenceSR recv) {
		this.recv = recv;
	}
}
