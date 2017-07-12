package ca.uvic.chisel.bfv.dualtrace;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
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
@XmlType(propOrder={"name", "send", "receive", "occurrences"})
public class MessageType {	
	public static final String DEFAULT_COLOUR = ColourConstants.TOOLTIP_GREEN;

	private String name;
	private MessageFunction send;
	private MessageFunction receive;
	private List<MessageOccurrence> occurrences;

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
		this.occurrences = new ArrayList<MessageOccurrence>();
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
	
	@XmlElementWrapper
	@XmlElement(name="occurrence")
	public List<MessageOccurrence> getOccurrences() {
		return occurrences;
	}
	
	
	public void setOccurrences(List<MessageOccurrence> occurrences) {
		this.occurrences = occurrences;
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

	public MessageOccurrence getOccurrenceAt(MessageOccurrence occurrence) {
		for (MessageOccurrence occ : occurrences) {
             if (occ.compareTo(occurrence)==0)
             {return occ;}
		}
		return null;
	}
	
	public void addOccurrence(MessageOccurrence occurrence) throws DuplicateMessageOccurrenceException{
		MessageOccurrence existing = getOccurrenceAt(occurrence);
		if (existing != null) {
			throw new DuplicateMessageOccurrenceException("Message " + name + " already has an occurrence  ");
		} 
		
		// Insert the occurrence, making sure that the list stays sorted
		if (occurrences.isEmpty()) {
			occurrences.add(occurrence);
		} else {
			int size = occurrences.size();
			for (int i = 0; i < size; i++) {
				int compareResult = occurrence.compareTo(occurrences.get(i));
				
				if (compareResult == 0) {
					throw new RuntimeException("Duplicate tag occurrence check failed to detect duplicate occurrence of " + occurrence);
				} else if (compareResult < 0) {
					occurrences.add(i, occurrence);
					break;
				} else if (i == size - 1) {
					// Occurrence comes after all of the existing ones, so add it at the end
					occurrences.add(occurrence);
					break;
				}
			}
		}
		// We can do this after because the compareTo doesn't look at tag name.
		occurrence.setMessageType(this);
	}
	
	public boolean deleteOccurrence(MessageOccurrence occurrence) {
		return this.occurrences.remove(occurrence);
	}
	
	
}
