package ca.uvic.chisel.bfv.annotations;

import ca.uvic.chisel.bfv.*;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * Represents a tag in the file. 
 * The difference between tags and comments: tags mark and categorize sections of the file; beyond that, they do not offer any commentary.
 * A tag can have many occurrences throughout the file.
 * Comments allow users to offer commentary on particular parts of the file. A comment can only have one occurrence of itself in the file.
 * 
 * @see TagOccurrence
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"name", "showStickyTooltip", "colour", "occurrences"})
public class Tag {
	public static final String DEFAULT_COLOUR = ColourConstants.TOOLTIP_BLUE;

	private String name;
	private List<TagOccurrence> occurrences;
	private boolean showStickyTooltip;
	private String colour; // ID of a colour from the plugin's colour registry
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private Tag() {}
	
	/**
	 * Creates a new tag with the specified name.
	 * @param name tag name
	 */
	public Tag(String name) {
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		
		this.name = name;
		occurrences = new ArrayList<TagOccurrence>();
		showStickyTooltip = true;
		colour = DEFAULT_COLOUR;
	}
	
	@Override
	public String toString() {
		return name + " (" + occurrences.size() + ")";
	}
	
	/**
	 * Returns this tag's name
	 * @return tag name
	 */
	@XmlElement
	public String getName() {
		return name;
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setName(String name) {
		this.name = name;
	}
	
	public void changeName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns a list of all occurrences of this tag. Occurrences will be sorted by location.
	 * @return list of occurrences
	 */
	@XmlElementWrapper
	@XmlElement(name="occurrence")
	public List<TagOccurrence> getOccurrences() {
		return occurrences;
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setOccurrences(List<TagOccurrence> occurrences) {
		this.occurrences = occurrences;
	}
	
	/**
	 * Add an occurrence of this tag at the specified location, making sure the tag's occurrences stay sorted by location
	 * @param startLine start line of occurrence to add
	 * @param startChar start char of occurrence to add
	 * @param endLine end line of occurrence to add
	 * @param endChar end char of occurrence to add
	 * @throws DuplicateTagOccurrenceException if this tag already has an occurrence at that location
	 */
	protected void addOccurrence(int startLine, int startChar, int endLine, int endChar) throws DuplicateTagOccurrenceException {
		TagOccurrence occurrence = new TagOccurrence(this, startLine, startChar, endLine, endChar);
		addOccurrence(occurrence);
	}
	
	protected void addOccurrence(TagOccurrence occurrence) throws DuplicateTagOccurrenceException{
		TagOccurrence existing = getOccurrenceAt(occurrence.getStartLine(), occurrence.getStartChar(), occurrence.getEndLine(), occurrence.getEndChar());
		if (existing != null) {
			throw new DuplicateTagOccurrenceException("Tag " + name + " already has an occurrence from " + existing.getStartLine(true) + 
					" : " + existing.getStartChar(true) + " to  " + existing.getEndLine(true) + " : "+ existing.getEndChar(true));
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
		occurrence.setTag(this);
	}
	
	/**
	 * Retrieve the occurrence at the specified location
	 * @param startLine start line of the desired occurrence
	 * @param startChar start char of the desired occurrence
	 * @param endLine end line of the desired occurrence
	 * @param endChar end char of the desired occurrence
	 * @return the occurrence at that location, or null if no occurrence exists at that location
	 */
	public TagOccurrence getOccurrenceAt(int startLine, int startChar, int endLine, int endChar) {
		for (TagOccurrence occurrence : occurrences) {
			if (occurrence.getStartLine() == startLine && occurrence.getStartChar() == startChar 
					&& occurrence.getEndLine() == endLine && occurrence.getEndChar() == endChar) {
				return occurrence;
			}
		}
		return null;
	}
	
	/**
	 * Delete the specified occurrence of this tag
	 * @param occurrence occurrence to delete
	 * @return true if that tag occurrence existed and was removed, false otherwise
	 */
	protected boolean deleteOccurrence(TagOccurrence occurrence) {
		return this.occurrences.remove(occurrence);
	}
	
	/**
	 * Returns whether the File Viewer should show sticky tooltips for occurrences of this tag.
	 * @return true if sticky tooltips for this tag's occurrences should be shown, false otherwise
	 */
	@XmlElement
	public boolean getShowStickyTooltip() {
		return showStickyTooltip;
	}
	
	/**
	 * Sets whether the File Viewer should show sticky tooltips for occurrences of this tag. If applyToOccurrences is set to true, 
	 * all occurrences of this tag will receive the same value for whether to show sticky tooltips.
	 * @param showStickyTooltip whether or not sticky tooltips for this tag's occurrences should be shown
	 * @param applyToOccurrences whether or not to apply the same value for showStickyTooltip to all occurrences of this tag
	 */
	protected void setShowStickyTooltip(boolean showStickyTooltip, boolean applyToOccurrences) {
		this.setShowStickyTooltip(showStickyTooltip);
		if (applyToOccurrences) {
			for (TagOccurrence occurrence : occurrences) {
				occurrence.setShowStickyTooltip(showStickyTooltip);
			}
		} 
	}
	
	/**
	 * Sets whether the File Viewer should show sticky tooltips for occurrences of this tag.
	 * Does not call setShowStickyTooltip() on any of its occurrences.
	 * (JAXB uses this setter when reading tag data from XML)
	 * @param showStickyTooltip whether or not sticky tooltips for occurrences of this tag should be shown
	 */
	private void setShowStickyTooltip(boolean showStickyTooltip) {
		this.showStickyTooltip = showStickyTooltip;
	}
	
	 /** 
	  * Get the ID of the colour to be used in the sticky tooltips for occurrences of this tag.
	  * @return the colour for this tag's sticky tooltips
	  */
	public String getColour() {
		return colour;
	}
	
	/**
	 * Set the colour to be used in the sticky tooltips for occurrences of this tag. 
	 * @param colourID ID of the new colour to use. If null or the ID doesn't exist in the colour registry, the default colour will be used.
	 */
	public void setColour(String colourID) {
		if (colourID == null || !BigFileActivator.getDefault().getColorRegistry().hasValueFor(colourID)) {
			this.colour = DEFAULT_COLOUR;
		} else {
			this.colour = colourID;
		}
	}
}
