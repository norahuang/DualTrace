package ca.uvic.chisel.bfv.annotations;

import java.util.Collections;
import javax.xml.bind.annotation.*;

import org.eclipse.jface.text.source.Annotation;

import ca.uvic.chisel.bfv.datacache.FileModelDataLayer;

/**
 * Represents a comment in the file. 
 * The difference between tags and comments: tags mark and categorize sections of the file; beyond that, they do not offer any commentary.
 * A tag can have many occurrences throughout the file.
 * Comments allow users to offer commentary on particular parts of the file. A comment can only have one occurrence of itself in the file.
 * 
 * @see CommentGroup
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"line", "character", "showStickyTooltip"})
public class Comment extends Annotation implements Comparable<Comment> {
	private static final String COMMENT_ANNOTATION_TYPE = "ca.uvic.chisel.bfv.editor.comment";
	private static final int TOSTRING_LENGTH_LIMIT = 20;
	
	private CommentGroup group;
	// Note: line and character are stored in 0-indexed form, but the line number ruler and status bar use 1-indexing.
	// When displaying this information to the user, it will need to be converted into a 1-indexed form.
	// TODO: will these need to be longs in the future?
	private int line;
	private int character;
	
	private boolean showStickyTooltip;
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private Comment() {}
	
	/**
	 * Creates a new comment with the given location and text, associating it with the specified group. Note that while this constructor 
	 * associates the comment with the group, it does not add the comment to the group. Clients should use {@link FileModelDataLayer.addComment} instead.
	 * @param group group that this comment is to be a part of 
	 * @param line line at which to add the comment
	 * @param character character within the line at which to add the comment
	 * @param text the new comment's text
	 */
	protected Comment(CommentGroup group, int line, int character, String text) {
		super(COMMENT_ANNOTATION_TYPE, false, text);
		if (group == null) {
			throw new IllegalArgumentException("Comment group cannot be null");
		} 
		if (line < 0 || character < 0) {
			throw new IllegalArgumentException("Line and character cannot be negative");
		}
		
		this.group = group;
		this.line = line;
		this.character = character;
		showStickyTooltip = group.getShowStickyTooltip();
	}
	
	/**
	 * Returns the first line of the comment's text or its first 20 characters, whichever is shorter.
	 */
	@Override
	public String toString() {
		int newline = this.getText().indexOf("\n");
		if (newline > 0 && newline <= TOSTRING_LENGTH_LIMIT) {
			return this.getText().substring(0, newline - 1) + "...";
		} else {
			if (this.getText().length() < TOSTRING_LENGTH_LIMIT) {
				return this.getText();
			} else {
				return this.getText().substring(0, TOSTRING_LENGTH_LIMIT) + "...";
			}
		}
	}
	
	/**
	 * Gets the group that this comment is a part of.
	 * @return the comment's group
	 */
	public CommentGroup getCommentGroup() {
		return group;
	}
	
	/**
	 * Sets the group that this comment is a part of.
	 * @param group new comment group
	 */
	protected void setCommentGroup(CommentGroup group) {
		this.group = group;
	}
	
	/**
	 * Move this comment from its current group to the specified comment group. 
	 * This method first checks if the specified group is already the comment's group--if this is so, nothing else will be done.
	 * @param group comment group to which to move the comment
	 * @throws InvalidCommentLocationException if the new group already has a comment at that location
	 */
	protected void moveToGroup(CommentGroup group) throws InvalidCommentLocationException {
		if (group != this.group) { // No point in moving if they're the same group!
			// Attempt adding it to the new group before deleting it from the old one--that way, if adding it to the new 
			// one fails, it'll still be in the old group
			group.addComment(this); 
			this.group.deleteComment(this);
			this.setCommentGroup(group);
		}
	}

	/**
	 * Gets the line where this comment is located.
	 * @return line where the comment is located 
	 */
	@XmlElement
	public int getLine() {
		return line;
	}
	
	/**
	 * Utility method for providing the option of getting the line in 1-indexed form (needed when displaying line information to the user
	 * in error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return line in 1-indexed form if useOneIndexing == true, line in 0-indexed form otherwise
	 */
	public int getLine(boolean useOneIndexing) {
		if (useOneIndexing) {
			return line + 1;
		} else {
			return this.getLine();
		}
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setLine(int line) {
		this.line = line;
	}
	
	/**
	 * Gets the character within the line at which this comment is located.
	 * @return character where the comment is located
	 */
	@XmlElement
	public int getCharacter() {
		return character;
	}
	
	/**
	 * Utility method for providing the option of getting the character index in 1-indexed form (needed when displaying character information
	 * to the user in error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return character index in 1-indexed form if useOneIndexing == true, character index in 0-indexed form otherwise
	 */
	public int getCharacter(boolean useOneIndexing) {
		if (useOneIndexing) {
			return character + 1;
		} else {
			return this.getCharacter();
		}
	}
	
	/**
	 * Move this comment to the specified location. Does nothing if the new location is the same as the old one.
	 * @param newLine line of location to which to move the comment
	 * @param newChar character within line of location to which to move the comment
	 * @throws InvalidCommentLocationException if this comment's group already has another comment at that location
	 */
	public void move(int newLine, int newChar) throws InvalidCommentLocationException {
		if (newLine != line || newChar != character) { // No point in moving if the location hasn't changed!
			if (newLine < 0 || newChar < 0) {
				throw new IllegalArgumentException("Line and character cannot be negative");
			}
			if (group.getCommentAt(newLine, newChar) != null) {
				throw new InvalidCommentLocationException("Comment group " + group.getName() + " already has a comment at line " + (newLine + 1) + 
						" char " + (newChar + 1));
			}
			
			line = newLine;
			character = newChar;
			Collections.sort(this.getCommentGroup().getComments());
		}
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setCharacter(int character) {
		this.character = character;
	}
	
	/**
	 * Returns whether the File Viewer should show a sticky tooltip for this comment.
	 * @return true if the sticky tooltip for this comment should be shown, false otherwise
	 */
	@XmlElement 
	public boolean getShowStickyTooltip() {
		return showStickyTooltip;
	}
	
	/**
	 * Sets whether the File Viewer should show a sticky tooltip for this comment. If this method receives true as a parameter 
	 * but the comment's group's showStickyTooltip value is false, the group's showStickyTooltip will also be set to true so that this
	 * comment's tooltip will be shown (other comments in the group will be left alone).
	 * @param showStickyTooltip whether or not a sticky tooltip should be shown
	 */
	protected void setShowStickyTooltip(boolean showStickyTooltip) {
		this.showStickyTooltip = showStickyTooltip;
		if (group != null) { // should only be null when JAXB is unmarshalling from XML; this just prevents a NullPointerException in that case
			if (this.showStickyTooltip && !group.getShowStickyTooltip()) {
				group.setShowStickyTooltip(true, false);
			}
		}
	}

	/**
	 * Compare this comment's location to another comment's. This method does not check the comments' comment groups.
	 * @param other comment whose location will be compared to this comment's location
	 * @return -1 if this comment's location is before the other comment's, 1 if it is after the other comment's, or 0 if they have the same location
	 */
	@Override
	public int compareTo(Comment other) {
		if (other == null) {
			return -1;
		}
		
		if (other.getLine() > this.getLine()) {
			return -1;
		} else if (other.getLine() < this.getLine()) {
			return 1;
		} else { // both comments are on the same line, so check the character
			if (other.getCharacter() > this.getCharacter()) {
				return -1;
			} else if (other.getCharacter() < this.getCharacter()) {
				return 1;
			} else { // both comments are at the same location
				return 0;
			}
		}
	}
}
