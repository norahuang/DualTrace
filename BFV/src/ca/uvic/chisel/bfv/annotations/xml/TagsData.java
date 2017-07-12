package ca.uvic.chisel.bfv.annotations.xml;

import ca.uvic.chisel.bfv.annotations.*;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * Represents tags data to be written to or read from XML.
 * @author Laura Chan
 */
@XmlRootElement
public class TagsData extends XMLMetadata {

	private Collection<Tag> tags;
	
	/**
	 * Creates a new, empty TagsData instance.
	 */
	public TagsData() {
		super();
		tags = null;
	}

	/**
	 * Gets the tags that were read from XML.
	 * @return tags tags that were read
	 */
	@XmlElementWrapper
	@XmlElement(name="tag")
	public Collection<Tag> getTags() {
		return tags;
	}

	/**
	 * Sets the tags to be written to XML.
	 * @param tags tags to be written
	 */
	public void setTags(Collection<Tag> tags) {
		this.tags = tags;
	}
}
