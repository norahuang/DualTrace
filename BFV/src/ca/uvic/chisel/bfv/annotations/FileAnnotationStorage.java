package ca.uvic.chisel.bfv.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.datacache.IFileAnnotationStorage;
import ca.uvic.chisel.bfv.dualtrace.DuplicateMessageOccurrenceException;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

/**
 * Model of all annotations that are associated with a given file
 * 
 * @author Laura Chan
 */
public class FileAnnotationStorage implements IFileAnnotationStorage {

	private IFolder parentFolder;
	private String associatedFileName;

	// Metadata files for this file
	private IFile commentsFile;
	private IFile regionsFile;
	private IFile tagsFile;
	private IFile messageTypesFile;

	private File associatedFile;
	private File associatedMessageFile;

	private SortedMap<String, CommentGroup> commentGroups;
	private SortedMap<Integer, RegionModel> rootLevelRegions;
	private SortedMap<String, Tag> tags;
	private SortedMap<String, MessageType> messageTypes;

	private String lineEnding = "";

	/**
	 * Creates a model of the input file.
	 * 
	 * @param input
	 *            the file to which annotations will be added
	 * @throws JAXBException
	 *             if a problem occurs while attempting to read the file's XML
	 *             metadata files
	 * @throws CoreException
	 *             if the file is out of sync with the file system and something
	 *             goes wrong while trying to refresh it
	 * @throws IOException
	 */
	public FileAnnotationStorage(File inputFile) throws JAXBException, CoreException, IOException {
		// This should be the original file, not the blank
		IFileUtils fileUtils = RegistryUtils.getFileUtils();
		// Would use all File, but IFile good here for the synchronization
		// below.
		IFile input = BfvFileUtils.convertFileIFile(inputFile);
		File o = fileUtils.convertBlankFileToActualFile(inputFile);
		IFile originalFile = BfvFileUtils.convertFileIFile(o);
		String dotFilePath = originalFile.getLocation().toOSString();
		String path = dotFilePath;
		associatedFile = new File(path);

		if (!input.isSynchronized(IResource.DEPTH_ZERO)) {
			input.refreshLocal(IResource.DEPTH_ZERO, null); // Prevents errors
															// resulting from
															// the file being
															// out of sync
		}

		IPath folderPath = Path.fromOSString(input.getParent().getLocation().toString());
		parentFolder = (IFolder) input.getParent().getFolder(folderPath);
		associatedFileName = FilenameUtils.getBaseName(input.getName());

		commentsFile = fileUtils.getCommentFile(path);
		regionsFile = fileUtils.getRegionFile(path);
		tagsFile = fileUtils.getTagFile(path);
		messageTypesFile = fileUtils.getMessageTypeFile(folderPath.toString());

		// Read in the metadata from XML
		commentGroups = XMLUtil.readCommentData(this);
		rootLevelRegions = XMLUtil.readRegionData(this);
		tags = XMLUtil.readTagData(this);
		messageTypes = XMLUtil.readMessageTypeData(this);

		// Make sure that the "No group" value for comment groups is present
		if (commentGroups.get(CommentGroup.NO_GROUP) == null) {
			commentGroups.put(CommentGroup.NO_GROUP, new CommentGroup(CommentGroup.NO_GROUP));
		}
	}

	/**
	 * Returns the name of this file's parent folder
	 * 
	 * @return parent folder name
	 */
	public String getParentFolderName() {
		return parentFolder.getName();
	}

	/**
	 * Returns the name of the associated file
	 * 
	 * @return associated file name
	 */
	public String getFileName() {
		return associatedFileName;
	}

	public File getFile() {
		return associatedFile;
	}

	public File getMessageFile() {
		return associatedMessageFile;
	}

	public String getFileLineEnding() {
		return this.lineEnding;
	}

	/**
	 * Returns the comments metadata file for this file
	 * 
	 * @return the file's comments file
	 */
	public IFile getCommentsFile() {
		return commentsFile;
	}

	/**
	 * Returns the regions metadata file for this file
	 * 
	 * @return the file's regions file
	 */
	public IFile getRegionsFile() {
		return regionsFile;
	}

	/**
	 * Returns the tags metadata file for this file
	 * 
	 * @return the file's tags file
	 */
	public IFile getTagsFile() {
		return tagsFile;
	}

	@Override
	public Collection<CommentGroup> getCommentGroups() {
		return commentGroups.values();
	}

	@Override
	public Comment getComment(String groupName, int line, int character) {
		CommentGroup group = commentGroups.get(groupName);
		if (group != null) {
			return group.getCommentAt(line, character);
		} else {
			return null;
		}
	}

	@Override
	public void addComment(String groupName, int line, int character, String text)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		CommentGroup group;
		if (groupName == null || "".equals(groupName.trim())) {
			group = commentGroups.get(CommentGroup.NO_GROUP);
		} else {
			group = commentGroups.get(groupName);
			if (group == null) {
				group = new CommentGroup(groupName);
				commentGroups.put(group.getName(), group);
			}
		}
		group.addComment(line, character, text);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void renameCommentGroup(CommentGroup group, String newName) throws JAXBException, CoreException {
		if (CommentGroup.NO_GROUP.equals(group.getName())) {
			throw new IllegalArgumentException("The '(No Group)' comment group is not allowed to be renamed");
		}
		if (isUniqueCommentGroupName(newName)) {
			commentGroups.remove(group.getName());
			group.setName(newName);
			commentGroups.put(group.getName(), group);
			XMLUtil.writeCommentData(this);
		} else {
			throw new IllegalArgumentException("Cannot rename comment group " + group.getName()
					+ ": there is already another comment group with name " + newName);
		}
	}

	@Override
	public void editComment(Comment comment, String newGroupName, String newText)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		// Update the comment's group
		CommentGroup newGroup;
		if (newGroupName == null || "".equals(newGroupName.trim())) {
			newGroup = commentGroups.get(CommentGroup.NO_GROUP);
		} else {
			newGroup = commentGroups.get(newGroupName);
			if (newGroup == null) {
				newGroup = new CommentGroup(newGroupName);
				commentGroups.put(newGroup.getName(), newGroup);
			}
		}

		comment.moveToGroup(newGroup);

		// Update the comment's text. Doing this after attempting to change the
		// group will ensure that absolutely nothing happens
		// if moving the comment failed due to an
		// InvalidCommentLocationException being thrown
		comment.setText(newText);

		XMLUtil.writeCommentData(this);
	}

	@Override
	public void editTag(Tag tag, String newName) throws JAXBException, CoreException {
		tag.changeName(newName);
		XMLUtil.writeTagData(this);
	}

	@Override
	public void editTagOccurrence(TagOccurrence occurrence, String newTagName) throws JAXBException, CoreException {
		try {
			renameTagOccurrence(newTagName, occurrence);
		} catch (DuplicateTagOccurrenceException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void moveComment(Comment comment, int newLine, int newChar)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		comment.move(newLine, newChar);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void deleteCommentGroup(CommentGroup group) throws JAXBException, CoreException {
		if (CommentGroup.NO_GROUP.equals(group.getName())) {
			throw new IllegalArgumentException("The '(No Group)' comment group is not allowed to be deleted");
		}
		commentGroups.remove(group.getName());
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void deleteComment(Comment comment) throws JAXBException, CoreException {
		comment.getCommentGroup().deleteComment(comment);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void setShowStickyTooltip(Comment comment, boolean showStickyTooltip) throws JAXBException, CoreException {
		comment.setShowStickyTooltip(showStickyTooltip);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void setShowStickyTooltip(CommentGroup group, boolean showStickyTooltip, boolean applyToAllComments)
			throws JAXBException, CoreException {
		group.setShowStickyTooltip(showStickyTooltip, applyToAllComments);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public void setColour(CommentGroup group, String colourID) throws JAXBException, CoreException {
		group.setColour(colourID);
		XMLUtil.writeCommentData(this);
	}

	@Override
	public boolean isUniqueCommentGroupName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Comment group name to search for cannot be null");
		}
		return commentGroups.get(name) == null;
	}

	/**
	 * Get all of the regions associated with this file as a forest
	 * 
	 * @return a Collection of all of this file's regions (which will contain
	 *         child regions)
	 */
	@Override
	public Collection<RegionModel> getRegions() {
		return rootLevelRegions.values();
	}

	/**
	 * This method assumes that the currently existing regions are in a valid
	 * state.
	 */
	@Override
	public void addRegion(RegionModel newRegion) throws InvalidRegionException, JAXBException, CoreException {

		if (newRegion == null || newRegion.getStartLine() < 0 || newRegion.getEndLine() < 0) {
			throw new InvalidRegionException("The new region has invalid parameters");
		}

		newRegion.setAnnotationStorage(this);

		RegionModel currentParent = null;

		List<RegionModel> newChildRegions = new ArrayList<>();

		Interval newRegionInterval = new Interval(newRegion.getStartLine(), newRegion.getEndLine());

		for (RegionModel currentRegion : getRegions()) {
			Interval currentRegionInterval = new Interval(currentRegion.getStartLine(), currentRegion.getEndLine());

			// if the new region overlaps an existing one, throw an error.
			// We will grab parent and children later.
			if (currentRegionInterval.intersects(newRegionInterval)
					&& !currentRegionInterval.strictEncloses(newRegionInterval)
					&& !newRegionInterval.strictEncloses(currentRegionInterval)) {
				throw new InvalidRegionException("The new region can not overlap existing regions");
			}

			// Get the parent region of this new region, which may be the child
			// of the current region
			if (currentParent == null) {
				currentParent = getDeepestParentRegion(newRegion, currentRegion);
			}

			// if the root level region is wrapped by the new one, it needs to
			// be removed from the rootLevelRegions
			if (newRegionInterval.strictEncloses(currentRegionInterval)) {
				newChildRegions.add(currentRegion);
			}
		}

		// get the regions that this region encloses
		for (RegionModel childRegion : newChildRegions) {
			getRegions().remove(childRegion);
			newRegion.addChild(childRegion);
		}

		if (currentParent == null) {
			// then this is a new root level region
			rootLevelRegions.put(newRegion.getStartLine(), newRegion);
		} else {
			// otherwise assign to parent; could pre-exist with another parent
			// though.
			if (null != newRegion.getParent()) {
				newRegion.getParent().removeChild(newRegion);
			}
			currentParent.addChild(newRegion);
		}

		XMLUtil.writeRegionData(this);
	}

	/**
	 * Gets the deepest region in the region tree with root currentRegion which
	 * encloses the newRegion. If not even the root encloses the newRegion, then
	 * it will just return null.
	 */
	private RegionModel getDeepestParentRegion(RegionModel newRegion, RegionModel currentRegion) {
		if (!currentRegion.asInterval().strictEncloses(newRegion.asInterval())) {
			return null;
		}

		RegionModel currentParent = currentRegion;
		boolean changed = true;

		// TODO the logic of this loop is wrong
		while (changed) {

			changed = false;

			for (RegionModel childRegion : currentParent.getChildren()) {
				if (childRegion.asInterval().strictEncloses(newRegion.asInterval())) {
					currentParent = childRegion;
					changed = true;
					break;
				}
			}
		}

		return currentParent;
	}

	@Override
	public void renameRegion(String newName, RegionModel region) throws JAXBException, CoreException {
		if (region == null || newName == null || newName.trim().equals("")) {
			throw new IllegalArgumentException("Region must not be null and name must be non-null and non-empty");
		}

		region.setName(newName);
		XMLUtil.writeRegionData(this);
		if (!newName.equals(region.getName())) {
		}
	}

	@Override
	public void removeRegion(RegionModel region) throws JAXBException, CoreException {
		if (rootLevelRegions.get(region.getStartLine()) != null) {
			// Case 1: we have a top level region, so we need to remove it from
			// the regions map directly and add all of its children to
			// the regions map
			rootLevelRegions.remove(region.getStartLine());
			for (RegionModel child : region.getChildren()) {
				rootLevelRegions.put(child.getStartLine(), child);
			}
		} else {
			// Case 2: we have to remove the region from its parent's list of
			// children, and add its children to the parent's list of children
			RegionModel parent = region.getParent();
			parent.removeChild(region);
			for (RegionModel child : region.getChildren()) {
				// This should never be possible! Children of children should
				// be valid for parent of parent, by virtue of logic.
				// Otherwise I would use the bounds checking version and
				// attempt recovery...
				try {
					parent.addChild(child);
				} catch (InvalidRegionException e) {
					// Don't recover; logically impossible.
					BigFileApplication.showErrorDialog("Error removing region",
							"Child invalid for identified new parent", e);
				}
			}
		}
		XMLUtil.writeRegionData(this);
	}

	@Override
	public void validateRegionBounds(RegionModel region) throws InvalidRegionException {
		// If this region will become a child of another region, it cannot have
		// the same start line as that other region, or it would not be
		// possible to collapse/expand one of them using the buttons on the File
		// Viewer's code folding ruler
		RegionModel startLineRegion = RegionModel.getEnclosingRegion(getRegions(), region.getStartLine());
		if (startLineRegion != null && region.getStartLine() == startLineRegion.getStartLine()) {
			throw new InvalidRegionException("Line " + +region.getStartLine(true)
					+ ": The new region cannot have the same start line as its parent/child region");
		}

		if (hasOffendingRegion(getRegions(), region) != null) {
			throw new InvalidRegionException("Lines " + region.getStartLine(true) + "-" + region.getEndLine(true)
					+ " cross two or more different regions or are only partially within an existing region");
		}
	}

	public static RegionModel hasOffendingRegion(Collection<RegionModel> allRegions, RegionModel region) {
		for (int i = region.getStartLine() + 1; i <= region.getEndLine(); i++) {
			RegionModel tmp = RegionModel.getEnclosingRegion(allRegions, i);
			if (tmp != null) {
				if ((region.getStartLine() < tmp.getStartLine() && region.getEndLine() < tmp.getEndLine())
						|| (region.getStartLine() > tmp.getStartLine() && region.getEndLine() > tmp.getEndLine())) {
					return tmp;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<Tag> getTags() {
		return tags.values();
	}

	@Override
	public TagOccurrence getTagOccurrence(String tagName, int startLine, int startChar, int endLine, int endChar) {
		Tag tag = tags.get(tagName);
		if (tag != null) {
			return tag.getOccurrenceAt(startLine, startChar, endLine, endChar);
		} else {
			return null;
		}
	}

	@Override
	public void addTag(String tagName, int startLine, int startChar, int endLine, int endChar)
			throws DuplicateTagOccurrenceException, JAXBException, CoreException {
		Tag tag = tags.get(tagName);
		if (tag == null) {
			tag = new Tag(tagName);
			tags.put(tag.getName(), tag);
		}
		tag.addOccurrence(startLine, startChar, endLine, endChar);
		XMLUtil.writeTagData(this);
	}

	@Override
	public void renameTagOccurrence(String tagName, TagOccurrence occurrence)
			throws DuplicateTagOccurrenceException, JAXBException, CoreException {
		Tag tag = tags.get(tagName);
		if (tag == null) {
			tag = new Tag(tagName);
			tags.put(tag.getName(), tag);
		}
		// remove original occurrence
		deleteTagOccurrence(occurrence);
		tag.addOccurrence(occurrence);
		XMLUtil.writeTagData(this);
	}

	@Override
	public void deleteTag(Tag tag) throws JAXBException, CoreException {
		tags.remove(tag.getName());
		XMLUtil.writeTagData(this);
	}

	@Override
	public void deleteTagOccurrence(TagOccurrence occurrence) throws JAXBException, CoreException {
		occurrence.getTag().deleteOccurrence(occurrence);
		if (occurrence.getTag().getOccurrences().size() == 0) {
			deleteTag(occurrence.getTag());
		}
		XMLUtil.writeTagData(this);
	}

	@Override
	public void setShowStickyTooltip(TagOccurrence occurrence, boolean showStickyTooltip)
			throws JAXBException, CoreException {
		occurrence.setShowStickyTooltip(showStickyTooltip);
		XMLUtil.writeTagData(this);
	}

	@Override
	public void setShowStickyTooltip(Tag tag, boolean showStickyTooltip, boolean applyToAllOccurrences)
			throws JAXBException, CoreException {
		tag.setShowStickyTooltip(showStickyTooltip, applyToAllOccurrences);
		XMLUtil.writeTagData(this);
	}

	@Override
	public void setColour(Tag tag, String colourID) throws JAXBException, CoreException {
		tag.setColour(colourID);
		XMLUtil.writeTagData(this);
	}

	public IFile getMessageTypesFile() {
		return messageTypesFile;
	}

	@Override
	public MessageType getMessageType(String messageTypeName) {
		if (messageTypes != null) {
			return messageTypes.get(messageTypeName);
		} else {
			return null;
		}
	}

	@Override
	public void addMessageType(MessageType messagetype) throws JAXBException, CoreException {
		// searchAllMatchOccurrences(messagetype);
		messageTypes.put(messagetype.getName(), messagetype);
		XMLUtil.writeMessageTypeData(this);

	}

	@Override
	public Collection<MessageType> getMessageTypes(boolean forRead) {
		if (forRead) {
			try {
				messageTypes = XMLUtil.readMessageTypeData(this);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (messageTypes != null) {
			return messageTypes.values();
		} else {
			return null;
		}
	}

	@Override
	public void setMessageTypes(SortedMap<String, MessageType> messageTypes) {
		this.messageTypes = messageTypes;

	}

	@Override
	public boolean isUniqueMessageTypeName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Message Type name to search for cannot be null");
		}
		return messageTypes.get(name) == null;
	}

	@Override
	public void renameMessageType(MessageType type, String newName)
			throws JAXBException, CoreException, DuplicateMessageOccurrenceException {
		if (isUniqueMessageTypeName(newName)) {
			messageTypes.remove(type.getName());
			type.setName(newName);
			messageTypes.put(type.getName(), type);

			XMLUtil.writeMessageTypeData(this);
		} else {
			throw new IllegalArgumentException("Cannot rename Message Type " + type.getName()
					+ ": there is already another Message Type with name " + newName);
		}
	}

	@Override
	public void deleteMessageType(MessageType type) throws JAXBException, CoreException {
		// TODO Auto-generated method stub
		messageTypes.remove(type.getName());
		XMLUtil.writeMessageTypeData(this);
	}


}
