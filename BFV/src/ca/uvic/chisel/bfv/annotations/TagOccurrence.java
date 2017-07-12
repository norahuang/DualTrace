package ca.uvic.chisel.bfv.annotations;

import javax.xml.bind.annotation.*;

import org.eclipse.jface.text.source.Annotation;

/**
 * Represents a specific occurrence of a tag.
 * @see Tag
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"startLine", "startChar", "endLine", "endChar", "showStickyTooltip"})
public class TagOccurrence extends Annotation implements Comparable<TagOccurrence> {
	public static final String TAG_ANNOTATION_TYPE = "ca.uvic.chisel.bfv.editor.tag";
	
	private Tag tag;
	
	// Note: lines and characters are stored in 0-indexed form, but the line number ruler and status bar use 1-indexing.
	// When displaying this information to the user, it will need to be converted into a 1-indexed form.
	// TODO: will the start and end chars need to be longs in the future?
	private int startLine;
	private int startChar;
	private int endLine;
	private int endChar;
	
	private boolean showStickyTooltip;
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private TagOccurrence() {}
	
	/**
	 * Creates a new occurrence of the specified tag at the given location. Note that while this constructor associates this occurrence 
	 * with the tag, it does not actually add the occurrence to the tag. Clients should use FileAnnotationStorage.addTag() instead.
	 * @param tag tag for which we are creating a new occurrence
	 * @param startLine start line of new occurrence
	 * @param startChar start char of new occurrence
	 * @param endLine end line of new occurrence
	 * @param endChar end char of new occurrence
	 */
	protected TagOccurrence(Tag tag, int startLine, int startChar, int endLine, int endChar) {
		super();
		
		if (tag == null) {
			throw new IllegalArgumentException("Associated tag cannot be null");
		}
		if (startLine < 0 || startChar < 0 || endLine < 0 || endChar < 0) {
			throw new IllegalArgumentException("Start and end lines/chars cannot be negative");
		}
		if (startLine > endLine) {
			throw new IllegalArgumentException("Start line cannot be greater than end line");
		}
		if (startLine == endLine && startChar > endChar) {
			throw new IllegalArgumentException("Start char cannot be greater than end char if they are on the same line");
		}
	
		this.tag = tag;
		this.startLine = startLine;
		this.startChar = startChar;
		this.endLine = endLine;
		this.endChar = endChar;
		this.showStickyTooltip = tag.getShowStickyTooltip();
		
		setType(TAG_ANNOTATION_TYPE);
		setText(this.toString());
	}
	
	@Override
	public String toString() {
		return "Tag '" + tag.getName() + "' at " + this.getStartLine(true) + " : " + this.getStartChar(true) + 
				" to " + this.getEndLine(true) + " : "+ this.getEndChar(true);
	}
	
	/**
	 * Get the tag that this occurrence is associated with.
	 * @return associated tag
	 */
	public Tag getTag() {
		return tag;
	}
	
	/**
	 * Sets the tag that this occurrence is associated with.
	 * @param tag tag to associate with this occurrence
	 */
	protected void setTag(Tag tag) {
		this.tag = tag;
	}
	
	/**
	 * Gets the start line of this tag occurrence.
	 * @return start line
	 */
	@XmlElement
	public int getStartLine() {
		return startLine;
	}
	
	/**
	 * Utility method for providing the option of getting the start line in 1-indexed form (needed when displaying start line information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return start line in 1-indexed form if useOneIndexing == true, start line in 0-indexed form otherwise
	 */
	public int getStartLine(boolean useOneIndexing) {
		if (useOneIndexing) {
			return startLine + 1;
		} else {
			return this.getStartLine();
		}
	}

	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	/**
	 * Gets the start char of this tag occurrence.
	 * @return start char
	 */
	@XmlElement
	public int getStartChar() {
		return startChar;
	}
	
	/**
	 * Utility method for providing the option of getting the start char in 1-indexed form (needed when displaying start char information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return start char in 1-indexed form if useOneIndexing == true, start char in 0-indexed form otherwise
	 */
	public int getStartChar(boolean useOneIndexing) {
		if (useOneIndexing) {
			return startChar + 1;
		} else {
			return this.getStartChar();
		}
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setStartChar(int startChar) {
		this.startChar = startChar;
	}
	
	/**
	 * Gets the end line of this tag occurrence.
	 * @return end line
	 */
	@XmlElement
	public int getEndLine() {
		return endLine;
	}
	
	/**
	 * Utility method for providing the option of getting the end line in 1-indexed form (needed when displaying end line information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return end line in 1-indexed form if useOneIndexing == true, end line in 0-indexed form otherwise
	 */
	public int getEndLine(boolean useOneIndexing) {
		if (useOneIndexing) {
			return endLine + 1;
		} else {
			return this.getEndLine();
		}
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setEndLine(int endLine) {
		this.endLine = endLine;
	}

	/**
	 * Gets the end char of this tag occurrence.
	 * @return end char
	 */
	@XmlElement
	public int getEndChar() {
		return endChar;
	}
	
	/**
	 * Utility method for providing the option of getting the end char in 1-indexed form (needed when displaying end char information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return end charin 1-indexed form if useOneIndexing == true, end char in 0-indexed form otherwise
	 */
	public int getEndChar(boolean useOneIndexing) {
		if (useOneIndexing) {
			return endChar + 1;
		} else {
			return this.getEndChar();
		}
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setEndChar(int endChar) {
		this.endChar = endChar;
	}
	
	/**
	 * Returns whether the File Viewer should show a sticky tooltip for this occurrence.
	 * @return true if the sticky tooltip for this occurrence should be shown, false otherwise
	 */
	@XmlElement 
	public boolean getShowStickyTooltip() {
		return showStickyTooltip;
	}
	
	/**
	 * Sets whether the File Viewer should show a sticky tooltip for this tag occurrence. If this method receives true as a parameter 
	 * but the tag's showStickyTooltip value is false, the tag's showStickyTooltip will also be set to true so that this
	 * occurrence's tooltip will be shown (other occurrences of the tag will be left alone).
	 * @param showStickyTooltip whether or not a sticky tooltip should be shown
	 */
	protected void setShowStickyTooltip(boolean showStickyTooltip) {
		this.showStickyTooltip = showStickyTooltip;
		if (tag != null) { // should only be null when JAXB is unmarshalling from XML; this just prevents a NullPointerException in that case
			if (this.showStickyTooltip && !tag.getShowStickyTooltip()) {
				tag.setShowStickyTooltip(true, false);
			}
		}
	}

	/**
	 * Compares the start and end location of this tag occurrence to another occurrence. Does not check the occurrences' associated tags.
	 * @param other tag occurrence to compare this occurrence to
	 * @return -1 if this occurrence's location is before the other's, 1 if it is after the other's, or 0 if they have the same location
	 */
	@Override
	public int compareTo(TagOccurrence other) {
		if (other == null) {
			return -1;
		} 
		
		if (this.getStartLine() < other.getStartLine()) {
			return -1;
		} else if (this.getStartLine() > other.getStartLine()) {
			return 1;
		} // else, both start on the same line, so check the start char next
		
		if (this.getStartChar() < other.getStartChar()) {
			return -1;
		} else if (this.getStartChar() > other.getStartChar()) {
			return 1;
		} // else, both start at the same place, so check the end line next
		
		if (this.getEndLine() < other.getEndLine()) {
			return -1;
		} else if (this.getEndLine() > other.getEndLine()) {
			return 1;
		} // else, both end on the same line, so check the end char 
		
		if (this.getEndChar() < other.getEndChar()) {
			return -1;
		} else if (this.getEndChar() > other.getEndChar()) {
			return 1;
		} else { // both start and end in the same place
			return 0;
		}
	}
}
