package ca.uvic.chisel.bfv.datacache;

import static ca.uvic.chisel.bfv.datacache.AnnotationsChangedListener.EventType.CommentsChanged;
import static ca.uvic.chisel.bfv.datacache.AnnotationsChangedListener.EventType.MessagesChanged;
import static ca.uvic.chisel.bfv.datacache.AnnotationsChangedListener.EventType.TagsChanged;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.annotations.FileAnnotationStorage;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.annotations.InvalidRegionException;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.annotations.XMLUtil;
import ca.uvic.chisel.bfv.datacache.BigFileReader.FileModelProvider;
import ca.uvic.chisel.bfv.datacache.BigFileReader.FileWrapper;
import ca.uvic.chisel.bfv.datacache.RegionChangeListener.RegionEventType;
import ca.uvic.chisel.bfv.dualtrace.DuplicateMessageOccurrenceException;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.filebackend.FileLineFileBackend;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.bfv.views.BigFileIndexedFileDecorator;

public class FileModelDataLayer implements IFileModelDataLayer, FileModelProvider {

	protected List<AnnotationsChangedListener> annotationChangedListeners = new ArrayList<>();
	protected List<RegionChangeListener> regionChangedListeners = new ArrayList<>();

	@Override
	public void clearListeners() {
		annotationChangedListeners.clear();
	}

	protected FileLineFileBackend fileBackend;

	public static boolean isFileIndexed(File file) {
		return FileLineFileBackend.checkIfFileHasIndex(file);
	}

	/**
	 * This method allows you define the different line consumers that will be
	 * used when reading the file. This method is intended to be extended by
	 * anyone extending the framework.
	 */
	protected ArrayList<IFileContentConsumer> createFileLineConsumers() throws Exception {
		this.fileBackend = new FileLineFileBackend(originalFile);
		IFileContentConsumer fileLineConsumer = new FileLineFileBackendConsumer((FileLineFileBackend) fileBackend);

		ArrayList<IFileContentConsumer> consumers = new ArrayList<IFileContentConsumer>();
		consumers.add(fileLineConsumer);
		return consumers;
	}

	protected FileModelDataLayer(File blankInputFile) throws Exception {
		fileRead = false;
		running = false;

		blankFile = blankInputFile;
		originalFile = RegistryUtils.getFileUtils().convertBlankFileToActualFile(blankInputFile);

		constructorInit();

		fileContentConsumers = createFileLineConsumers();
		fileAnnotationModel = new FileAnnotationStorage(blankInputFile);
	}

	protected void constructorInit() {
	}

	@Override
	public FileWrapper getWrappedFile() throws IOException {
		return new PlainFileWrapper(originalFile);
	}

	@Override
	public void setFileReadComplete(boolean value) {
		this.fileRead = value;
	}

	@Override
	public boolean getFileReadComplete() {
		return this.fileRead;
	}

	@Override
	public void setFileReadRunning(boolean value) {
		this.running = value;
	}

	@Override
	public boolean getFileReadRunning() {
		return this.running;
	}

	public class PlainFileWrapper implements FileWrapper {
		File original;
		File blank;
		LinePreservingBufferedReader reader;
		String prevLineRead = null;

		PlainFileWrapper(File file) throws IOException {
			IFileUtils fileUtils = RegistryUtils.getFileUtils();
			original = file;
			blank = fileUtils.convertFileToBlankFile(file); // new
															// File(fileUtils.convertBlankFileToActualFile(file).getLocation().toString());
			reader = LinePreservingBufferedReader.newBufferedReader(blank.toPath(), Charset.forName("ISO-8859-1"));
		}

		public boolean validate() {
			return true;
		}

		public int getFileSize() {
			return (int) original.length();
		}

		public int getLinesWorked() {
			return prevLineRead.length();
		}

		public long getLinesCompleted() {
			return getLinesWorked();
		}

		public TextLine getLine() throws IOException {
			prevLineRead = reader.readLine();
			TextLine line = new TextLine(prevLineRead);
			return line;
		}

		public void dispose() throws IOException {
			reader.close();
		}

		@Override
		public long getFileSizeOnDisk() {
			return original.length();
		}

		@Override
		public long getFileOutputSizeOnDisk() {
			// Used for printing size results after processing, I don't really
			// care about generic file model size.
			return 0;
		}

		@Override
		public String getFilePath() {
			return original.getAbsolutePath();
		}
	}

	@Override
	public int getCharLengthBetween(long fileStartLine, long fileEndLine) {
		return (int) (fileBackend.getOffsetFromIndexForLine(fileEndLine + 1)
				- fileBackend.getOffsetFromIndexForLine(fileStartLine));
	}

	@Override
	public Map<Long, Pair<Long, Long>> getAllLineCharLengthsBetween(long fileStartLine, long fileEndLine) {
		Map<Long, Pair<Long, Long>> results = new HashMap<>();
		Long prevOffset = fileBackend.getOffsetFromIndexForLine(fileStartLine);
		long sumFromStart = 0;
		for (long lineNumber = fileStartLine; lineNumber <= fileEndLine; lineNumber++) {
			Long offset = fileBackend.getOffsetFromIndexForLine(lineNumber + 1);
			Long size = offset - prevOffset;
			sumFromStart += size;
			results.put(lineNumber, Pair.of(size, sumFromStart));
			prevOffset = offset;
		}
		return results;
	}

	@Override
	public void registerAnnotationChangedListener(AnnotationsChangedListener listener) {
		annotationChangedListeners.add(listener);
	}

	@Override
	public void deregisterAnnotationChangedListener(AnnotationsChangedListener listener) {
		annotationChangedListeners.remove(listener);
	}

	@Override
	public void registerRegionChangedListener(RegionChangeListener listener) {
		regionChangedListeners.add(listener);
	}

	@Override
	public void deregisterRegionChangedListener(RegionChangeListener listener) {
		regionChangedListeners.remove(listener);
	}

	protected File originalFile = null;
	protected File blankFile = null;

	@Override
	public IPath getOpenFileRelativePath() {
		return BfvFileUtils.convertFileIFile(originalFile).getProjectRelativePath();
	}

	protected List<IFileContentConsumer> fileContentConsumers;
	protected boolean fileRead = false;
	protected boolean running = false;

	public File getOriginalFile() {
		return originalFile;
	}

	public File getBlankFile() {
		return blankFile;
	}

	protected FileAnnotationStorage fileAnnotationModel;

	private void doFileRead() throws Exception {
		BigFileReader.doFileRead(false, fileContentConsumers, this);
	}

	@Override
	public void updateDecorator() {
		IDecoratorManager idm = PlatformUI.getWorkbench().getDecoratorManager();
		idm.update(BigFileIndexedFileDecorator.ID);
	}

	@Override
	public boolean fileReadSuccessfully() {
		return fileRead;
	}

	@Override
	public String getFinalizingText() {
		return "Finalizing file index...";
	}

	@Override
	public String getFileBuildText() {
		return "Building line indexes for file...";
	}

	@Override
	public void removeIndexForFile(File file) {
		if (null != fileBackend) {
			fileBackend.abortAndDeleteIndex();
		}
	}

	@Override
	public void readFileIfNeeded() throws Exception {
		if (!fileRead && !running) {
			doFileRead();
		}
	}

	// -----------------------------------------------------------------------\\
	// ------------------------- Line Stuff -------------------------\\
	// -----------------------------------------------------------------------\\

	@Override
	public String getFileLines(int startLine, int endLine) {
		// TODO Changes I made make this method pointless except as a raw
		// wrapped.
		// Or rather, a question: can we pass a String back instead of a list?
		List<String> lines = fileBackend.getLineRange(startLine, endLine);

		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			buffer.append(line);
		}

		return buffer.toString();
	}

	@Override
	public long getNumberOfLines() {
		return fileBackend.getNumberOfLines();
	}

	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean regExSearch, boolean caseSensitiveSearch,
			FileTextSearchScope scope) {
		return fileBackend.createSearchQuery(searchText, regExSearch, caseSensitiveSearch, scope);
	}

	@Override
	public void cancelCurrentlyRunningSearchStatement() {
		fileBackend.cancelCurrentlyRunningSearchStatement();
	}

	@Override
	public Collection<CommentGroup> getCommentGroups() {
		return fileAnnotationModel.getCommentGroups();
	}

	@Override
	public void addComment(String groupName, int line, int character, String text)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		fileAnnotationModel.addComment(groupName, line, character, text);
		fireCommentsChangedEvent();
	}

	@Override
	public void renameCommentGroup(CommentGroup group, String newName) throws JAXBException, CoreException {
		fileAnnotationModel.renameCommentGroup(group, newName);
		// TODO fire changed event
	}

	@Override
	public void editComment(Comment comment, String newGroupName, String newText)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		fileAnnotationModel.editComment(comment, newGroupName, newText);
		// TODO fire changed event
	}

	@Override
	public void editTag(Tag tag, String newName) throws JAXBException, CoreException {
		fileAnnotationModel.editTag(tag, newName);
		// TODO fire changed event
	}

	@Override
	public void editTagOccurrence(TagOccurrence occurence, String newName) throws JAXBException, CoreException {
		fileAnnotationModel.editTagOccurrence(occurence, newName);
	}

	@Override
	public void moveComment(Comment comment, int newLine, int newChar)
			throws JAXBException, CoreException, InvalidCommentLocationException {
		fileAnnotationModel.moveComment(comment, newLine, newChar);
		fireCommentsChangedEvent();
	}

	@Override
	public void deleteCommentGroup(CommentGroup group) throws JAXBException, CoreException {
		fileAnnotationModel.deleteCommentGroup(group);
		fireCommentsChangedEvent();
	}

	@Override
	public void deleteComment(Comment comment) throws JAXBException, CoreException {
		fileAnnotationModel.deleteComment(comment);
		fireCommentsChangedEvent();
	}

	@Override
	public void setShowStickyTooltip(Comment comment, boolean showStickyTooltip) throws JAXBException, CoreException {
		fileAnnotationModel.setShowStickyTooltip(comment, showStickyTooltip);
		// TODO fire changed event
	}

	@Override
	public void setShowStickyTooltip(CommentGroup group, boolean showStickyTooltip, boolean applyToAllComments)
			throws JAXBException, CoreException {
		fileAnnotationModel.setShowStickyTooltip(group, showStickyTooltip, applyToAllComments);
		// TODO fire changed event
	}

	@Override
	public void setColour(CommentGroup group, String colourID) throws JAXBException, CoreException {
		fileAnnotationModel.setColour(group, colourID);
		this.fireCommentsChangedEvent();
	}

	@Override
	public boolean isUniqueCommentGroupName(String name) {
		return fileAnnotationModel.isUniqueCommentGroupName(name);
	}

	private void fireEventToRegionListeners(RegionEventType type, RegionModel region) {
		for (RegionChangeListener regionListener : regionChangedListeners) {
			regionListener.handleRegionChanged(type, region);
		}
	}

	@Override
	public void renameRegion(String newName, RegionModel region) throws JAXBException, CoreException {
		fileAnnotationModel.renameRegion(newName, region);
		fireEventToRegionListeners(RegionEventType.REGION_RENAMED, region);
	}

	@Override
	public void removeRegion(RegionModel region) throws InvalidRegionException, JAXBException, CoreException {
		fileAnnotationModel.removeRegion(region);
		fireEventToRegionListeners(RegionEventType.REGION_REMOVED, region);
	}

	@Override
	public void addRegion(RegionModel region) throws InvalidRegionException, JAXBException, CoreException {
		fileAnnotationModel.addRegion(region);

		fireEventToRegionListeners(RegionEventType.REGION_ADDED, region);
	}

	@Override
	public void validateRegionBounds(RegionModel region) throws InvalidRegionException {
		fileAnnotationModel.validateRegionBounds(region);
	}

	@Override
	public void collapseRegion(RegionModel region) {

		if (region.isCollapsed()) {
			return;
		}

		region.markCollapsed();
		fireEventToRegionListeners(RegionEventType.REGION_COLLAPSED, region);
	}

	@Override
	public void expandRegion(RegionModel region) {
		if (!region.isCollapsed()) {
			return;
		}

		region.markExpanded();
		fireEventToRegionListeners(RegionEventType.REGION_EXPANDED, region);
	}

	@Override
	public Collection<Tag> getTags() {
		return fileAnnotationModel.getTags();
	}

	@Override
	public TagOccurrence getTagOccurrence(String tagName, int startLine, int startChar, int endLine, int endChar) {
		return fileAnnotationModel.getTagOccurrence(tagName, startLine, startChar, endLine, endChar);
	}

	@Override
	public void addTag(String tagName, int startLine, int startChar, int endLine, int endChar)
			throws DuplicateTagOccurrenceException, JAXBException, CoreException {
		fileAnnotationModel.addTag(tagName, startLine, startChar, endLine, endChar);
		fireTagsChangedEvent();
	}

	@Override
	public void renameTagOccurrence(String tagName, TagOccurrence tagOccurrence)
			throws DuplicateTagOccurrenceException, JAXBException, CoreException {
		fileAnnotationModel.renameTagOccurrence(tagName, tagOccurrence);
		fireTagsChangedEvent();
	}

	@Override
	public void deleteTag(Tag tag) throws JAXBException, CoreException {
		fileAnnotationModel.deleteTag(tag);
		fireTagsChangedEvent();
	}

	@Override
	public void deleteTagOccurrence(TagOccurrence occurrence) throws JAXBException, CoreException {
		fileAnnotationModel.deleteTagOccurrence(occurrence);
		fireTagsChangedEvent();
	}

	@Override
	public void setShowStickyTooltip(TagOccurrence occurrence, boolean showStickyTooltip)
			throws JAXBException, CoreException {
		fileAnnotationModel.setShowStickyTooltip(occurrence, showStickyTooltip);
		// TODO fire changed event
	}

	@Override
	public void setShowStickyTooltip(Tag tag, boolean showStickyTooltip, boolean applyToAllOccurrences)
			throws JAXBException, CoreException {
		fileAnnotationModel.setShowStickyTooltip(tag, showStickyTooltip, applyToAllOccurrences);
		// TODO fire changed event
	}

	@Override
	public void setColour(Tag tag, String colourID) throws JAXBException, CoreException {
		fileAnnotationModel.setColour(tag, colourID);
		// TODO fire changed event
		fireTagsChangedEvent();
	}

	@Override
	public Comment getComment(String groupName, int line, int character) {
		return fileAnnotationModel.getComment(groupName, line, character);
	}

	@Override
	public Collection<RegionModel> getRegions() {
		return fileAnnotationModel.getRegions();
	}

	@Override
	public void saveRegionData() {
		// Need to update the file's regions data since the collapsed/expanded
		// state will have changed
		try {
			XMLUtil.writeRegionData(fileAnnotationModel);
		} catch (JAXBException e) {
			BigFileApplication.showErrorDialog("Error collapsing/expanding region",
					"Could not update file's regions file", e);
		} catch (CoreException e) {
			BigFileApplication.showErrorDialog("Error collapsing/expanding region",
					"Problem refreshing file's regions file", e);
		}
	}

	private void fireTagsChangedEvent() {
		for (AnnotationsChangedListener listener : annotationChangedListeners) {
			listener.handleAnnotationChanged(TagsChanged);
		}
	}

	private void fireCommentsChangedEvent() {
		for (AnnotationsChangedListener listener : annotationChangedListeners) {
			listener.handleAnnotationChanged(CommentsChanged);
		}
	}

	private void fireMessageTypesChangedEvent() {
		for (AnnotationsChangedListener listener : annotationChangedListeners) {
			listener.handleAnnotationChanged(MessagesChanged);
		}
	}

	@Override
	public String getFileDelimiter() {
		return fileBackend.getFileDelimiter();
	}

	@Override
	public void dispose() {
	}

	@Override
	public Collection<MessageType> getMessageTypes(boolean forRead) {
		return fileAnnotationModel.getMessageTypes(forRead);
	}

	@Override
	public void deleteMessageType(MessageType type) throws JAXBException, CoreException {
		// TODO Auto-generated method stub
		fileAnnotationModel.deleteMessageType(type);
		fireMessageTypesChangedEvent();
		
		
	}

	@Override
	public MessageType getMessageType(String messageTypeName) {
		return fileAnnotationModel.getMessageType(messageTypeName);
	}

	@Override
	public void addMessageType(MessageType messagetype) throws JAXBException, CoreException {
		fileAnnotationModel.addMessageType(messagetype);
		fireMessageTypesChangedEvent();
		
	}


	@Override
	public boolean isUniqueMessageTypeName(String trim) {
		return fileAnnotationModel.isUniqueMessageTypeName(trim);
	}

	@Override
	public void renameMessageType(MessageType type, String newName) throws JAXBException, CoreException, DuplicateMessageOccurrenceException{
	    fileAnnotationModel.renameMessageType(type, newName);
	    fireMessageTypesChangedEvent();
	}


	@Override
	public void setMessageTypes(SortedMap<String, MessageType> messageTypes) {
		fileAnnotationModel.setMessageTypes(messageTypes);
	}



}