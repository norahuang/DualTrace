package ca.uvic.chisel.bfv.datacache;

import java.util.Collection;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.CoreException;

import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.annotations.InvalidRegionException;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.dualtrace.DuplicateMessageOccurrenceException;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageType;

public interface IFileAnnotationStorage {

	/**
	 * Get all of this file's comment groups along with the comments in them
	 * @return a collection of this file's comment groups
	 */
	public abstract Collection<CommentGroup> getCommentGroups();

	/**
	 * Get the comment from the specified group at the given location 
	 * @param groupName group that the comment belongs to
	 * @param line line in which the comment is located
	 * @param character character within the line at which the comment is located
	 * @return the comment, or null if it doesn't exist
	 */
	public abstract Comment getComment(String groupName, int line, int character);

	/**
	 * Add a comment at the given location to the specified group, creating the group if it does not exist.
	 * @param groupName name of group to which the comment will be added
	 * @param line at which to add the comment
	 * @param character character within the line at which to add the comment
	 * @param text text of the comment to add
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while creating the comments file or refreshing it after updating
	 * @throws InvalidCommentLocationException if the group already has a comment at that location
	 */
	public abstract void addComment(String groupName, int line, int character,
			String text) throws JAXBException, CoreException,
			InvalidCommentLocationException;

	/**
	 * Rename the specified comment group.
	 * @param group comment group to rename
	 * @param newName new name to give the comment group (must be unique)
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void renameCommentGroup(CommentGroup group, String newName)
			throws JAXBException, CoreException;

	/**
	 * Edit the specified comment.
	 * @param comment comment to edit
	 * @param newGroupName name of comment's new group
	 * @param newText comment's new text
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 * @throws InvalidCommentLocationException if the comment's group has changed and the new group already has a comment at that location
	 */
	public abstract void editComment(Comment comment, String newGroupName,
			String newText) throws JAXBException, CoreException,
			InvalidCommentLocationException;

	public abstract void editTag(Tag tag, String newName)
			throws JAXBException, CoreException;
	
	public void editTagOccurrence(TagOccurrence occurrence, String text)
			throws JAXBException, CoreException;

	/**
	 * Move the specified comment to a new location. Does not change the comment's group. 
	 * @param comment comment to be moved
	 * @param newLine line number of new location
	 * @param newChar character within the line of the new location
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 * @throws InvalidCommentLocationException if the comment's group already has another comment at that location
	 */
	public abstract void moveComment(Comment comment, int newLine, int newChar)
			throws JAXBException, CoreException,
			InvalidCommentLocationException;

	/**
	 * Delete the specified comment group and all comments within it
	 * @param group comment group to delete
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void deleteCommentGroup(CommentGroup group)
			throws JAXBException, CoreException;

	/**
	 * Delete the specified comment.
	 * @param comment comment to delete
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void deleteComment(Comment comment) throws JAXBException,
			CoreException;

	/**
	 * Sets whether the File Viewer should show a sticky tooltip for the specified comment.
	 * @param comment comment whose sticky tooltip should be shown or hidden
	 * @param showStickyTooltip whether or not a sticky tooltip should be shown
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void setShowStickyTooltip(Comment comment,
			boolean showStickyTooltip) throws JAXBException, CoreException;

	/**
	 * Sets whether the File Viewer should show sticky tooltips for the comments in the specified group. If applyToAllComments is set to true, 
	 * all of the comments in this group will receive the same value for whether to show sticky tooltips.
	 * @param group comment group whose comments' sticky tooltips should be shown or hidden
	 * @param showStickyTooltip whether or not sticky tooltips for the specified group's comments should be shown
	 * @param applyToAllComments whether or not to apply the same value for showStickyTooltip to all of this group's comments as well
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void setShowStickyTooltip(CommentGroup group,
			boolean showStickyTooltip, boolean applyToAllComments)
			throws JAXBException, CoreException;

	/**
	 * Sets the colour to be used in the sticky tooltips for the comments in the specified comment group
	 * @param group group whose comments' sticky tooltips will use the colour
	 * @param colourID ID of colour to use for the sticky tooltips
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public abstract void setColour(CommentGroup group, String colourID)
			throws JAXBException, CoreException;

	/**
	 * Tests whether the given comment group name is unique
	 * @param name name to test
	 * @return true if the name is unique, false if there is already a group with that name 
	 */
	public abstract boolean isUniqueCommentGroupName(String name);

	/**
	 * Adds the region to the storage
	 * @param region to add
	 * @throws InvalidRegionException if the region is not valid
	 * @throws JAXBException if something goes wrong while updating the regions file
	 * @throws CoreException if something goes wrong while creating the regions file or refreshing it after updating
	 */
	public abstract void addRegion(RegionModel region)
			throws InvalidRegionException, JAXBException, CoreException;

	/**
	 * Changes the specified region's name.
	 * @param newName new name for the region
	 * @param region region to rename
	 * @throws JAXBException if something goes wrong while updating the regions file
	 * @throws CoreException if something goes wrong while refreshing the regions file after updating
	 */
	public abstract void renameRegion(String newName, RegionModel region)
			throws JAXBException, CoreException;

	/**
	 * Remove the specified region. This will only remove the region from the regions file and make that section of the file unfoldable.
	 * No sections of the actual file are removed, and sub-regions are preserved.
	 * @param region region to remove
	 * @throws InvalidRegionException if the children cannot be assigned to a parent for some reason
	 * @throws JAXBException if something goes wrong while updating the regions file
	 * @throws CoreException if something goes wrong while refreshing the regions file after updating
	 * @throws  
	 */
	public abstract void removeRegion(RegionModel region) throws InvalidRegionException, JAXBException,
			CoreException;

	/**
	 * Tests whether the given region's bounds are valid by ensuring the following:
	 * <ul>
	 * <li>The region does not straddle two or more different existing regions or define an area that is only partially in some existing region.</li>
	 * <li>The region doesn't start on the same line as its enclosing parent region (if the region would be a child of an existing region)</li>
	 * </ul>
	 * @param region region to test
	 * @throws InvalidRegionException if the region's bounds are not valid for one of the above reasons
	 */
	public abstract void validateRegionBounds(RegionModel region)
			throws InvalidRegionException;

	/**
	 * Get all of the tags in this file
	 * @return a Collection of this file's tags
	 */
	public abstract Collection<Tag> getTags();

	/**
	 * Returns the occurrence of the specified tag that appears at the specified location.
	 * @param tagName name of the tag
	 * @param startLine start line of the desired occurrence
	 * @param startChar start char of the desired occurrence
	 * @param endLine end line of the desired occurrence
	 * @param endChar end char of the desired occurrence
	 * @return the tag occurrence, or null if no occurrence of that tag exists at that location
	 */
	public abstract TagOccurrence getTagOccurrence(String tagName,
			int startLine, int startChar, int endLine, int endChar);

	/**
	 * Add an occurrence of the specified tag at the given location
	 * @param tagName name of the tag to be added
	 * @param startLine start line of occurrence to add
	 * @param startChar start char of occurrence to add
	 * @param endLine end line of occurrence to add
	 * @param endChar end char of occurrence to add
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while creating or refreshing the tags file
	 * @throws DuplicateTagOccurrenceException 
	 */
	public abstract void addTag(String tagName, int startLine, int startChar,
			int endLine, int endChar) throws DuplicateTagOccurrenceException,
			JAXBException, CoreException;
	
	/**
	 * Move an existing occurrence from one tag to another.
	 * @param tagName
	 * @param occurrence
	 * @throws CoreException 
	 * @throws JAXBException 
	 * @throws DuplicateTagOccurrenceException 
	 */
	public abstract void renameTagOccurrence(String tagName, TagOccurrence occurrence) throws DuplicateTagOccurrenceException, JAXBException, CoreException;

	/**
	 * Delete all occurrences of the specified tag
	 * @param tag tag to delete
	 * @throws JAXBException if something goes wrong while updating the file's tags file
	 * @throws CoreException if something goes wrong while refreshing the file's tags file
	 */
	public abstract void deleteTag(Tag tag) throws JAXBException, CoreException;

	/**
	 * Delete the specified tag occurrence.
	 * @param occurrence tag occurrence to delete
	 * @throws JAXBException if something goes wrong while updating the file's tags file
	 * @throws CoreException if something goes wrong while refreshing the file's tags file
	 */
	public abstract void deleteTagOccurrence(TagOccurrence occurrence)
			throws JAXBException, CoreException;

	/**
	 * Sets whether the File Viewer should show a sticky tooltip for the specified tag occurrence.
	 * @param occurrence tag occurrence whose sticky tooltip should be shown or hidden
	 * @param showStickyTooltip whether or not a sticky tooltip should be shown
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file
	 */
	public abstract void setShowStickyTooltip(TagOccurrence occurrence,
			boolean showStickyTooltip) throws JAXBException, CoreException;

	/**
	 * Sets whether the File Viewer should show sticky tooltips for occurrences of the specified tag. If applyToAllOccurrences is set to true, 
	 * all of occurrences will receive the same value for whether to show sticky tooltips.
	 * @param tag tag whose occurrences' sticky tooltips should be shown or hidden
	 * @param showStickyTooltip whether or not sticky tooltips for this tag's occurrences should be shown
	 * @param applyToAllOccurrences whether or not to apply the same value for showStickyTooltip to all of this tag's occurrences as well
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file
	 */
	public abstract void setShowStickyTooltip(Tag tag,
			boolean showStickyTooltip, boolean applyToAllOccurrences)
			throws JAXBException, CoreException;

	/**
	 * Sets the colour to be used in the sticky tooltips for occurrences of the specified tag.
	 * @param tag tag whose occurrences' sticky tooltips will use the colour
	 * @param colourID ID of colour to use for the sticky tooltips
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file
	 */
	public abstract void setColour(Tag tag, String colourID)
			throws JAXBException, CoreException;

	public abstract Collection<RegionModel> getRegions();


	public abstract Collection<MessageType> getMessageTypes(boolean forRead);	
	
	public abstract void deleteMessageType(MessageType type) throws JAXBException, CoreException;

	public abstract MessageType getMessageType(String messageTypeName);
	
	public abstract void addMessageType(MessageType messagetype) throws JAXBException, CoreException;

	public boolean isUniqueMessageTypeName(String trim);

	public void renameMessageType(MessageType type, String newName) throws JAXBException, CoreException, DuplicateMessageOccurrenceException;

	public void setMessageTypes(SortedMap<String, MessageType> messageTypes);
	
}