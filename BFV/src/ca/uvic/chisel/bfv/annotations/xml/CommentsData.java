package ca.uvic.chisel.bfv.annotations.xml;

import ca.uvic.chisel.bfv.annotations.*;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * Represents comments metadata to be written to or read from XML.
 * @author Laura Chan
 */
@XmlRootElement
public class CommentsData extends XMLMetadata {
	
	private Collection<CommentGroup> commentGroups;
	
	/**
	 * Creates a new, empty CommentsData instance.
	 */
	public CommentsData() {
		super();
		commentGroups = null;
	}

	/**
	 * Get the comment groups (and the comments that they contain) that were read from XML.
	 * @return comment groups that were read
	 */
	@XmlElementWrapper
	@XmlElement(name="commentGroup")
	public Collection<CommentGroup> getCommentGroups() {
		return commentGroups;
	}

	/**
	 * Set the comment groups (and the comments that they contain) to be written to XML.
	 * @param commentGroups comment groups to be written
	 */
	public void setCommentGroups(Collection<CommentGroup> commentGroups) {
		this.commentGroups = commentGroups;
	}
}
