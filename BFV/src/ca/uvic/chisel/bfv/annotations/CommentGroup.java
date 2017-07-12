package ca.uvic.chisel.bfv.annotations;

import ca.uvic.chisel.bfv.*;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * Represents a group or category of comments. Comments that do not have a group should be placed in the "No Group" comment group.
 * @see Comment
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"name", "showStickyTooltip", "colour", "comments"})
public class CommentGroup {
	/** Name of the CommentGroup that contains comments that have no group specified */
	public static final String NO_GROUP = "(No group)"; 
	
	public static final String DEFAULT_COLOUR = ColourConstants.TOOLTIP_GREEN;

	private String name;
	private List<Comment> comments;
	private boolean showStickyTooltip;
	private String colour; // ID of a colour from the plugin's colour registry
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private CommentGroup() {}
	
	/**
	 * Creates a new comment group with the specified name.
	 * @param name
	 */
	public CommentGroup(String name) {
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		this.name = name;	
		comments = new ArrayList<Comment>();
		showStickyTooltip = true;
		colour = DEFAULT_COLOUR;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	/**
	 * Returns the name of this comment group.
	 * @return comment group name
	 */
	@XmlElement
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of this comment group.
	 * @param name name to use for this group
	 */
	protected void setName(String name) {
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		this.name = name;
	}
	
	/**
	 * Gets a list of all comments in this group. Comments will be sorted by location.
	 * @return list of comments 
	 */
	@XmlElementWrapper
	@XmlElement(name="comment")
	public List<Comment> getComments() {
		return comments;
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setComments(List<Comment> comments) {
		this.comments = comments;
	}
	
	/**
	 * Get the comment at the specified location.
	 * @param line line in which the comment is located
	 * @param character character within the line at which the comment is located
	 * @return the comment at the location, or null if there is no comment at that location
	 */
	public Comment getCommentAt(int line, int character) {
		for (Comment comment : comments) {
			if (comment.getLine() == line && comment.getCharacter() == character) {
				return comment;
			}
		}
		return null;
	}
	
	/**
	 * Add a comment with the specified text at the given location to this group.
	 * @param line in which the comment to add is located
	 * @param character char within the line at which the comment to add is located
	 * @param text text of comment to add
	 * @throws InvalidCommentLocationException if this group already has a comment at that location
	 */
	protected void addComment(int line, int character, String text) throws InvalidCommentLocationException {
		Comment comment = new Comment(this, line, character, text);
		addComment(comment);
	}
	
	/**
	 * Add the specified comment to this group, keeping the comments in this group sorted by location.
	 * @param comment comment to add
	 * @throws InvalidCommentLocationException if this group already has a comment at that location
	 */
	protected void addComment(Comment comment) throws InvalidCommentLocationException {
		if (this.getCommentAt(comment.getLine(), comment.getCharacter()) != null) {
			throw new InvalidCommentLocationException("Comment group " + name + " already has a comment at line " + comment.getLine(true) + 
					" char " + comment.getCharacter(true));
		}
		
		// Add the comment, making sure that the comments list remains sorted by comment location
		if (comments.isEmpty()) {
			comments.add(comment); 
		} else {
			int size = comments.size();
			for (int i = 0; i < size; i++) {
				int compareResult = comment.compareTo(comments.get(i));
				if (compareResult == 0) {
					throw new RuntimeException("Invalid comment check failed to detect invalid comment " + comment);
				} else if (compareResult < 0) {
					comments.add(i, comment);
					return;
				} else if (i == size - 1) {
					comments.add(comment);
					return;
				}
			}
		}
	}
	
	/**
	 * Deletes the specified comment.
	 * @param comment comment to be deleted
	 * @return true if the comment existed and was deleted, false otherwise
	 */
	protected boolean deleteComment(Comment comment) {
		return this.comments.remove(comment);
	}
	
	/**
	 * Returns whether the File Viewer should show sticky tooltips for the comments in this group.
	 * @return true if sticky tooltips for this group's comments should be shown, false otherwise
	 */
	@XmlElement
	public boolean getShowStickyTooltip() {
		return showStickyTooltip;
	}
	
	/**
	 * Sets whether the File Viewer should show sticky tooltips for the comments in this group. If applyToComments is set to true, 
	 * all of the comments in this group will receive the same value for whether to show sticky tooltips.
	 * @param showStickyTooltip whether or not sticky tooltips for this group's comments should be shown
	 * @param applyToComments whether or not to apply the same value for showStickyTooltip to all of this group's comments as well
	 */
	protected void setShowStickyTooltip(boolean showStickyTooltip, boolean applyToComments) {
		this.setShowStickyTooltip(showStickyTooltip);
		if (applyToComments) {
			for (Comment comment : comments) {
				comment.setShowStickyTooltip(showStickyTooltip);
			}
		} 
	}
	
	/**
	 * Sets whether the File Viewer should show sticky tooltips for the comments in this group.
	 * Does not call setShowStickyTooltip() on any of its comments.
	 * (JAXB uses this setter when reading comment group data from XML)
	 * @param showStickyTooltip whether or not sticky tooltips for this group's comments should be shown
	 */
	private void setShowStickyTooltip(boolean showStickyTooltip) {
		this.showStickyTooltip = showStickyTooltip;
	}
	
	/**
	 * Get the ID of the colour to be used in the sticky tooltips for this comment group's comments
	 * @return the colour for this comment group's sticky tooltips
	 */
	public String getColour() {
		return colour;
	}
	
	/**
	 * Set the colour to be used in the sticky tooltips for this comment group's comments. 
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
