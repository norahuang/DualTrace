package ca.uvic.chisel.bfv.datacache;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.annotations.InvalidRegionException;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageType;

public interface IFileModelDataLayer extends IFileAnnotationStorage {

	public abstract void clearListeners();

	public abstract void registerAnnotationChangedListener(
			AnnotationsChangedListener listener);

	public abstract void deregisterAnnotationChangedListener(
			AnnotationsChangedListener listener);

	public abstract void registerRegionChangedListener(
			RegionChangeListener listener);

	public abstract void deregisterRegionChangedListener(
			RegionChangeListener listener);

	public abstract IPath getOpenFileRelativePath();
	
	public String getFileDelimiter();

	String getFinalizingText();
	
	public String getFileBuildText();
	
	/**
	 * Checks to see if the file has been read, and if it hasn't, it reads it.
	 */
	public abstract void readFileIfNeeded() throws Exception;
	
	public abstract void removeIndexForFile(File file);
	
	public abstract boolean fileReadSuccessfully();

	/**
	 * Retrieves and returns all lines in the database that are between start and end line concatanated together
	 * into one String
	 */
	public abstract String getFileLines(int startLine, int endLine);

	/**
	 * Get the number of lines (or the 1-indexed last line number).
	 * @return
	 */
	public abstract long getNumberOfLines();

	/**
	 * Although deprecated, could be useful later. No longer needed for paging,
	 * since we do a different computation to facilitate caching.
	 */
	@Deprecated
	public int getCharLengthBetween(long startLine, long endLine);
	
	/**
	 * Per line within the line span specified, returns both the character count of the
	 * line and the character count from the start of the span until the end of the line.
	 * 
	 * The map is indexed by line number, and the pair is: 
	 * <length from start of line, length from start of startLine. 
	 * 
	 * @param startLine
	 * @param endLine
	 * @return
	 */
	public Map<Long, Pair<Long, Long>> getAllLineCharLengthsBetween(long startLine, long endLine);
	
	public abstract FileSearchQuery createSearchQuery(String searchText, boolean regExSearch, boolean caseSensitiveSearch, FileTextSearchScope scope);
	
	/**
	 * Cancel any currently running search.
	 */
	public abstract void cancelCurrentlyRunningSearchStatement();

	@Override
	public abstract Collection<CommentGroup> getCommentGroups();

	@Override
	public abstract void addComment(String groupName, int line, int character,
			String text) throws JAXBException, CoreException,
			InvalidCommentLocationException;

	@Override
	public abstract void renameCommentGroup(CommentGroup group, String newName)
			throws JAXBException, CoreException;

	@Override
	public abstract void editComment(Comment comment, String newGroupName,
			String newText) throws JAXBException, CoreException,
			InvalidCommentLocationException;

	@Override
	public abstract void editTag(Tag tag, String newName)
			throws JAXBException, CoreException;
	
	@Override
	public abstract void editTagOccurrence(TagOccurrence occurrence, String text)
			throws JAXBException, CoreException;

	@Override
	public abstract void moveComment(Comment comment, int newLine, int newChar)
			throws JAXBException, CoreException,
			InvalidCommentLocationException;

	@Override
	public abstract void deleteCommentGroup(CommentGroup group)
			throws JAXBException, CoreException;

	@Override
	public abstract void deleteComment(Comment comment) throws JAXBException,
			CoreException;

	@Override
	public abstract void setShowStickyTooltip(Comment comment,
			boolean showStickyTooltip) throws JAXBException, CoreException;

	@Override
	public abstract void setShowStickyTooltip(CommentGroup group,
			boolean showStickyTooltip, boolean applyToAllComments)
			throws JAXBException, CoreException;

	@Override
	public abstract void setColour(CommentGroup group, String colourID)
			throws JAXBException, CoreException;

	@Override
	public abstract boolean isUniqueCommentGroupName(String name);

	@Override
	public abstract void addRegion(RegionModel region)
			throws InvalidRegionException, JAXBException, CoreException;

	@Override
	public abstract void renameRegion(String newName, RegionModel region)
			throws JAXBException, CoreException;

	@Override
	public abstract void removeRegion(RegionModel region) throws InvalidRegionException, JAXBException, CoreException;

	@Override
	public abstract void validateRegionBounds(RegionModel region)
			throws InvalidRegionException;

	public abstract void collapseRegion(RegionModel region);

	public abstract void expandRegion(RegionModel region);

	@Override
	public abstract Collection<Tag> getTags();

	@Override
	public abstract TagOccurrence getTagOccurrence(String tagName,
			int startLine, int startChar, int endLine, int endChar);

	@Override
	public abstract void addTag(String tagName, int startLine, int startChar,
int endLine, int endChar) throws DuplicateTagOccurrenceException,
			JAXBException, CoreException;

	@Override
	public abstract void deleteTag(Tag tag) throws JAXBException, CoreException;

	@Override
	public abstract void deleteTagOccurrence(TagOccurrence occurrence)
			throws JAXBException, CoreException;

	@Override
	public abstract void setShowStickyTooltip(TagOccurrence occurrence,
			boolean showStickyTooltip) throws JAXBException, CoreException;
	

	@Override
	public abstract void setShowStickyTooltip(Tag tag,
			boolean showStickyTooltip, boolean applyToAllOccurrences)
			throws JAXBException, CoreException;

	@Override
	public abstract void setColour(Tag tag, String colourID)
			throws JAXBException, CoreException;

	@Override
	public abstract Comment getComment(String groupName, int line, int character);

	@Override
	public abstract Collection<RegionModel> getRegions();

	/**
	 * TODO, this method looks like it is redundant now, we should look to remove it
	 */
	public abstract void saveRegionData();

	/**
	 * Releases all memory heavy objects held by this file model, for memory reasons only,
	 * not guaranteed to be called
	 */
	public abstract void dispose();

	void updateDecorator();

	public abstract void deleteMessageOccurrence(MessageOccurrence occurrence) throws JAXBException, CoreException;

	public void setMessageTypes(SortedMap<String, MessageType> messageTypes);
	
}