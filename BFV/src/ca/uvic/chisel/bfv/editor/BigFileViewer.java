package ca.uvic.chisel.bfv.editor;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISlaveDocumentManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import bfv.org.eclipse.ui.internal.editors.text.EditorsPlugin;
import bfv.org.eclipse.ui.texteditor.MarkerAnnotation;
import ca.uvic.chisel.bfv.BigFileActivator;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.annotations.MarkerAnnotationConstants;
import ca.uvic.chisel.bfv.annotations.RegionAnnotation;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.RegionChangeListener;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrence;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.DocumentContentManager.DocumentChange;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionSupport;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.BfvViewerUtils;
import ca.uvic.chisel.bfv.views.CombinedFileSearchView;
import ca.uvic.chisel.bfv.views.CommentsView;
import ca.uvic.chisel.bfv.views.TagsView;

/**
 * This class is a viewer which is capable of viewing very large files.  By controlling the contents of the view
 * with data paging, it is able to view text files of sizes in the 5G range.  This view does not provide support 
 * for the users to edit the file in any way.
 */
public class BigFileViewer extends ProjectionViewer implements RegionChangeListener, IAnnotationModelListener {
	
	private Map<Comment, StickyTooltip> commentTooltips;
	private Map<TagOccurrence, StickyTooltip> tagTooltips;
	
	private ITextEditor fTextEditor;
	private IDocument document;
	private IFileModelDataLayer fileModel;
	private DocumentContentManager documentManager;
	private boolean loaded;
	private StyledText textWidget;
	private ISyntaxHighlightingManager syntaxHighlightingManager;
	private boolean isDiffView;
	//This is a hack. Is already fixed in another branch.
	private BigFileViewerConfiguration viewerConfiguration;
	
	private boolean showCommentTooltips, showTagTooltips;
	private Set<MarkerAnnotation> currentMarkers = new HashSet<>();
	
	/**
	 * This class will watch the incoming annotationModel change events for added or removed 
	 * MarkerAnnotations.  If any are found, it will keep track of which ones are currently
	 * added (and not deleted) by adding or removing them to the currentMarkers list.
	 */
	private class AnnotationChangeListener implements IAnnotationModelListener, IAnnotationModelListenerExtension {
		@Override
		public void modelChanged(IAnnotationModel model) {
		}

		@Override
		public void modelChanged(AnnotationModelEvent event) {
			for(Annotation annotation : event.getAddedAnnotations()) {
				if(annotation instanceof MarkerAnnotation) {
					MarkerAnnotation markerAnnotation = (MarkerAnnotation) annotation;
					currentMarkers.add(markerAnnotation);
				}
			}
			
			for(Annotation annotation : event.getRemovedAnnotations()) {
				if(annotation instanceof MarkerAnnotation) {
					MarkerAnnotation marker = (MarkerAnnotation) annotation;
					
					if(annotation == null || marker.getMarker().getAttribute(MarkerAnnotationConstants.DELETED, true)) {
						currentMarkers.remove((MarkerAnnotation) annotation);
					}
				}
			}
			
			// this seems to handle all of the cases where redraws are needed.
			redrawMarkerAnnotations();
		}
	}
	
	public BigFileViewer(Composite parent, IVerticalRuler ruler,IOverviewRuler overviewRuler, Boolean OverviewRulerVisible, int styles, Boolean isDiffView, ITextEditor fTextEditor){
		super(parent, ruler, overviewRuler, OverviewRulerVisible, styles);
		this.setEditable(false);
		loaded = false;
		commentTooltips = new HashMap<Comment, StickyTooltip>();
		tagTooltips = new HashMap<TagOccurrence, StickyTooltip>();
		showCommentTooltips = false;
		showTagTooltips = false;
		this.isDiffView = isDiffView;
		this.fTextEditor = fTextEditor;
	}
		
	/**
	 * Sets the document to null
	 */
	public void disposeDocument(){
		document = null;
	}
	
	/**
	 * Sets the documentManager to null
	 */
	public void disposeDocumentManager(){
		documentManager = null;
	}
	
	/**
	 * Sets the fileModel to null
	 */
	public void disposeFileModel(){
		if(fileModel != null) {
			fileModel.deregisterRegionChangedListener(this);
		}
		fileModel = null;
	}
	
	
	/**
	 * Returns the syntaxHighlightingManager
	 * @return the SyntaxHighlightingManager
	 */
	public ISyntaxHighlightingManager getSyntaxHighlightingManager(){
		return syntaxHighlightingManager;
	}
	
	private ITextEditor getTextEditor(){
		return fTextEditor;
	}
	
	/**
	 * Creates and installs ProjectionSupport. This is used for code folding.
	 */
	public void setProjectionSupport(){
		DefaultMarkerAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
		// Set up code folding for regions
		ProjectionSupport projectionSupport = new ProjectionSupport(this, annotationAccess, EditorsPlugin.getDefault().getSharedTextColors());
		projectionSupport.setAnnotationPainterDrawingStrategy(new RegionDrawingStrategy());
		projectionSupport.install();
		
		this.doOperation(ProjectionViewer.TOGGLE);
	}

	/**
	 * Adds listeners to the textWidget
	 * ControlListener - Handles redrawing of annotations when editor is resized.
	 * SelectionListener - Handles refreshing of content when using scroll bar.
	 * KeyListener - Refreshes content in response to keyboard input.
	 * AnnotationModelListener - Reacts to shrinking/expanding a region. Content is updated and syntax highlighting is redone.
	 * CaretListener - Refreshes content when the caret is moved.
	 */
	public void addListeners(){
		textWidget = this.getTextWidget();
		if(!isDiffView){
			// Listener to redraw tooltips when the editor is moved/resized
			ControlListener controlListener = new ControlListener() { 
				@Override
				public void controlMoved(ControlEvent e) { 
					if(loaded && !textWidget.isDisposed())  {
						BigFileViewer.this.redrawAnnotations();
					}
				}
				@Override
				public void controlResized(ControlEvent e) {
					if(loaded && !textWidget.isDisposed() && !isDiffView) { 
						BigFileViewer.this.redrawAnnotations();
					}
				}
			};
			textWidget.addControlListener(controlListener); 
			
			// Listener to redraw tooltips when the whole application is moved/resized. 
			textWidget.getShell().addControlListener(controlListener); 
		
			textWidget.getVerticalBar().addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if(e.detail == SWT.DRAG) {
						return;
					}
					
					updateTextWidget();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		
			textWidget.addKeyListener(new KeyListener() {
				@Override
				public void keyPressed(KeyEvent e) {}
				@Override
				public void keyReleased(KeyEvent e) {
					if(e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.PAGE_DOWN || 
							e.keyCode == SWT.PAGE_UP || e.keyCode == SWT.HOME || e.keyCode == SWT.END) {
						updateTextWidget();
					}
				}
			});
		
		}
		if(!isDiffView){
			textWidget.addCaretListener(new CaretListener() {
				@Override
				public void caretMoved(CaretEvent event) {
					updateTextWidget(); 
				}
			});
		}
	}

	// XXX this method will currently not work
	public void setDiffFileModel(FileEditorInput fileEditorInput, BigFileViewer viewer){
//		this.viewer = viewer;
		try {
			fileModel = RegistryUtils.getFileModelDataLayerFromRegistry(BfvFileUtils.convertFileIFile(fileEditorInput.getFile()));
			fileModel.readFileIfNeeded();
		
	//		fileModel.getFileIndex();
			documentManager = new DocumentContentManager(fileModel);
			documentManager.initializeDocument();
			//	document = documentManager.getDocument();
			viewerConfiguration = new BigFileViewerConfiguration(fileModel, documentManager);
			viewer.setViewerConfiguration(viewerConfiguration);
			viewer.setDocument(document, new AnnotationModel());
		} catch (Exception e) {
			e.printStackTrace();
		}
	/*	this.fileModel = fileModel;
		this.documentManager = documentManager;
		this.document = document;*/
	}		
		
	/**
	 * Sets up the fileModel, the documentManager and the document for this class.
	 */
	public void setViewerConfiguration(BigFileViewerConfiguration viewerConfiguration){
		this.fileModel = viewerConfiguration.getFileModel();
		
		fileModel.registerRegionChangedListener(this);
		
		this.documentManager = viewerConfiguration.getDocumentManager();
		this.document = documentManager.getDocument();
		
		this.projectionSlaveDocumentManager.setContentManager(documentManager);
	}
		
	/**
	 * Sets the file model, sets the document to the viewer. Sets up projection for code folding and creates event listeners.
	 * @param fileEditorInput The file to be opened.
	 * @param viewer The ProjectionViewer the file will be set to.
	 */
	public void init(FileEditorInput fileEditorInput, ProjectionViewer viewer){
		setProjectionSupport();
		
		syntaxHighlightingManager = RegistryUtils.getSyntaxHighlightingManagerFromRegistry(viewer, fileModel);
		
		addListeners();		
		redrawAnnotations();
		
		syntaxHighlightingManager.setHighlightingDirty();
		syntaxHighlightingManager.adjustHighlighting();
		
		loaded = true;
		syntaxHighlightingManager.beginWatchingViewerTextChanges();
		
		this.getAnnotationModel().addAnnotationModelListener(new AnnotationChangeListener());
	}

	/**
	 * Adds comments, tags, and regions to the document.
	 */
	public void redrawAnnotations() {
		
		// Add annotations and prepare sticky tooltips for any comments that were loaded with the file
		for(CommentGroup group : fileModel.getCommentGroups()) {
			for (Comment comment : group.getComments()) {
				// If the comment is for a line that is paged in, add it.
				if(documentManager.lineLoaded(comment.getLine())) {
					addAndDisplayComment(comment);
				}
			}
		}
		
		// Add annotations and prepare sticky tooltips for any tags that were loaded with the file
		for(Tag tag : fileModel.getTags()) {
			for (TagOccurrence occurrence : tag.getOccurrences()) {
				// If the comment is for a line that is paged in, add it.
				addAndDisplayTagOccurrence(occurrence);
			}
		}				
		
		// Add any regions that were loaded when the file was loaded
		for(RegionModel r : fileModel.getRegions()) {
			addAndDisplayRegion(r, true);
		}
		
		redrawTooltips();
	}

	/**
	 * Redraw all of the current marker annotations that are being tracked by the viewer.
	 */
	protected void redrawMarkerAnnotations() {
		for(MarkerAnnotation marker : currentMarkers) {
			addAndDisplayMarkerAnnotation(marker);
		}
	}
	
	

	/**
	 * This method is needed because the annotation mode doesn't properly remove annotations that
	 * are flagged as to be deleted.  We have to actually iterate through them and force the removal.
	 */
	private void cleanupAnnotations() {
		Iterator annotationIterator = this.getAnnotationModel().getAnnotationIterator();
		while(annotationIterator.hasNext()) {
			Annotation annotation = (Annotation)annotationIterator.next();
			
			if(annotation.isMarkedDeleted()) {
				this.getAnnotationModel().removeAnnotation(annotation);
			}
		}
	}

	private RegionAnnotation getAssociatedRegionAnnotation(RegionModel model) {
		
		Iterator<Annotation> iterator = getProjectionAnnotationModel().getAnnotationIterator();
		
		while(iterator.hasNext()) {
			
			Annotation annotation = (Annotation) iterator.next();
			
			if(annotation instanceof RegionAnnotation && ((RegionAnnotation) annotation).getModel().equals(model)) {
				return (RegionAnnotation) annotation;
			}
		}
		
		return null;
	}
	
	/**
	 * Searches document for given text.
	 * @param selectedText Text to search for.
	 */
	public void doSearch(String selectedText) {
		if(selectedText.length() < 4 || Integer.parseInt(selectedText, 16) == 0) {
			return;
		}
		
		int start = 0;
		for(; start < selectedText.length(); start++) {
			if(selectedText.charAt(start) == '0' || selectedText.charAt(start) == 'x')  {
				continue;
			} else {
				break;
			}					
		}
		
		selectedText = new String(selectedText.substring(start, selectedText.length()));
		CombinedFileSearchView searchView = (CombinedFileSearchView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(CombinedFileSearchView.ID);
		searchView.search(selectedText, false, false);
	}
	
	/**
	 * Navigates to the specified line in the editor
	 * @param line the line to which we want to go
	 * @throws BadLocationException
	 * @throws IOException 
	 */
	public void gotoLineAtOffset(int line, int offsetIntoLine) throws BadLocationException {
		// Need pre and post to work in the GotoLineAction, but also need both combined
		// for usage elsewhere.
		preGotoLineAtOffset(line, offsetIntoLine);
		// Used to have original goto call here, but had off by one error when paging in
		// Might need some things to happen after...if so, call postGoto twice.
		postGotoLineAtOffset(line, offsetIntoLine);
		gotoLineOriginal(line);
		
	}

	/**
	 * Copied from the GotoLineAction class, so that we can have access to that functionality
	 * in additional programmatic ways.
	 * 
	 * @param line
	 */
	private void gotoLineOriginal(long line) {
		// XXX File comparison passes null as ITextEditor to this class's constructor. Might be ok.
		// Can't test because comparison system was compromised during large sets of changes.

		ITextEditor editor = getTextEditor();

		IDocumentProvider provider= editor.getDocumentProvider();
		IDocument document= provider.getDocument(editor.getEditorInput());
		try {

			int start= document.getLineOffset((int)line);
			editor.selectAndReveal(start, 0);

			IWorkbenchPage page= editor.getSite().getPage();
			page.activate(editor);

		} catch (BadLocationException x) {
			// ignore
		}
	}
	
	/**
	 * Navigates to the specified line in the editor
	 * @param line the line to which we want to go
	 * @throws BadLocationException
	 * @throws IOException 
	 */
	public void preGotoLineAtOffset(int line, int offsetIntoLine) throws BadLocationException {
		RegionModel region = RegionModel.getEnclosingRegion(fileModel.getRegions(), line);
		if(null != region && region.isCollapsed()){
			//|| !RegionModel.allAncestorsExpanded(region))){
			fileModel.expandRegion(region);
		}
		
		this.setTopIndex(line);
	}
	
	public void postGotoLineAtOffset(int line, int offsetIntoLine) throws BadLocationException {
		updateTextWidget();
	}
	
	/**
	 * Get the number of the line in which the editor's cursor is currently located.
	 * @return the current line number
	 */
	public int getCurrentLineNumber() {
		int caret = textWidget.getCaretOffset();
		return widgetLine2ModelLine(textWidget.getLineAtOffset(caret));
	}
	
	/**
	 * Get the length of the specified line 
	 * @return the length of the line
	 */
	public int getLineLength(int lineNum){
		return textWidget.getLine(lineNum).length();
	}
	
	/**
	 * Get the contents of the line in which the editor's cursor is currently located.
	 * @return the contents of the editor's current line
	 */
	public String getCurrentLine() {
		int caret = textWidget.getCaretOffset();
		int lineNum = textWidget.getLineAtOffset(caret);
		return textWidget.getLine(lineNum);
	}
	
	/**
	 * Add a comment at the given location to the specified group and show it as an annotation in the editor.
	 * @param groupName name of group to which the comment will be added
	 * @param line in which the comment to add is located
	 * @param character char within the line at which the comment to add is located
	 * @param text text of comment to be added
	 * @throws JAXBException if something goes wrong while updating the file's comments file
	 * @throws CoreException if something goes wrong while creating/refreshing the file's comments file
	 * @throws InvalidCommentLocationException if the comment group already has a comment at that location
	 */
	public void addComment(String groupName, int line, int character, String text) throws JAXBException, CoreException, InvalidCommentLocationException {
		fileModel.addComment(groupName, line, character, text);
		if (groupName == null || "".equals(groupName.trim())) {
			addAndDisplayComment(fileModel.getComment(CommentGroup.NO_GROUP, line, character));
		} else {
			addAndDisplayComment(fileModel.getComment(groupName, line, character));
		}
	}
	
	/**
	 * Add and display a comment in the editor by adding an annotation and a sticky tooltip for it.
	 * @param comment comment to add and display 
	 */
	private void addAndDisplayComment(Comment comment) {
		try {
			
			int offset = document.getLineOffset(comment.getLine());
			
			if(documentManager.lineLoaded(comment.getLine())) {
				offset += comment.getCharacter();
			}
			
			Position position = new Position(offset, 1);
			// This must ensure that the tooltip is deleted, not just hidden
			if(!commentTooltips.containsKey(comment)) {
				StickyTooltip tooltip = createStickyTooltip(comment); 
				commentTooltips.put(comment, tooltip);
			} else {
				StickyTooltip tooltip = commentTooltips.get(comment);
				tooltip.hide();
			}
			this.getAnnotationModel().addAnnotation(comment, position);
			
			showOrHideStickyTooltip(comment);
		} catch (BadLocationException e) {
			BigFileApplication.showErrorDialog("Cannot display comment in editor", "Error while trying to display comment", e);
		}
	}
	
	/**
	 * Add and display a message in the editor by adding an annotation and a sticky tooltip for it.
	 * @param comment comment to add and display 
	 */
	private void addAndDisplayMessage(Comment comment) {
		try {
			
			int offset = document.getLineOffset(comment.getLine());
			
			if(documentManager.lineLoaded(comment.getLine())) {
				offset += comment.getCharacter();
			}
			
			Position position = new Position(offset, 1);
			// This must ensure that the tooltip is deleted, not just hidden
			if(!commentTooltips.containsKey(comment)) {
				StickyTooltip tooltip = createStickyTooltip(comment); 
				commentTooltips.put(comment, tooltip);
			} else {
				StickyTooltip tooltip = commentTooltips.get(comment);
				tooltip.hide();
			}
			this.getAnnotationModel().addAnnotation(comment, position);
			
			showOrHideStickyTooltip(comment);
		} catch (BadLocationException e) {
			BigFileApplication.showErrorDialog("Cannot display comment in editor", "Error while trying to display comment", e);
		}
	}


	/**
	 * Removes a given comment.
	 * @param comment Comment to be removed.
	 */
	private void removeStickyTooltip(Comment comment) {
		if(!commentTooltips.containsKey(comment)) {
			return;
		}
		StickyTooltip tooltip = commentTooltips.get(comment);
		tooltip.hide();
		commentTooltips.remove(comment);
	}
	
	/**
	 * Creates a comment and returns it as a sticky tooltip.
	 * @param comment Comment to be created.
	 * @return Returns comment as a sticky tooltip.
	 */
	private StickyTooltip createStickyTooltip(Comment comment) {
		StickyTooltip tooltip = new StickyTooltip(textWidget, true, comment);
		tooltip.setText(comment.getText());
		String colourID = comment.getCommentGroup().getColour();
		tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(colourID));
		return tooltip;
	}
	
	/**
	 * Edit the specified comment and update its sticky tooltip's text.
	 * @param comment comment to edit
	 * @param newGroupName name of comment's new group
	 * @param newText comment's new text
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 * @throws InvalidCommentLocationException if the comment's group has changed and the new group already has a comment at that location
	 */
	public void editComment(Comment comment, String newGroupName, String newText) throws JAXBException, CoreException, InvalidCommentLocationException {
		fileModel.editComment(comment, newGroupName, newText);
		StickyTooltip tooltip = commentTooltips.get(comment); 
		tooltip.hide();
		tooltip.setText(comment.getText());
		String colourID = comment.getCommentGroup().getColour();
		tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(colourID));
		showOrHideStickyTooltip(comment);
	}
	
	/**
	 * Edits the text of the comment associated with the specified sticky tooltip by replacing it with the tooltip's text.
	 * Allows users to directly edit comments from their associated sticky tooltip.
	 * @param tooltip tooltip whose associated comment is to be edited
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public void editComment(StickyTooltip tooltip) throws JAXBException, CoreException {
		for (Comment comment : commentTooltips.keySet()) {
			if (commentTooltips.get(comment) == tooltip) {
				try {
					editComment(comment, comment.getCommentGroup().getName(), tooltip.getText());
					CommentsView commentsView = 
							(CommentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(CommentsView.ID);
					if (commentsView != null) {
						commentsView.updateView();
					}
				} catch (InvalidCommentLocationException e) {
					// There is no good reason why this should happen since we aren't moving the comment or changing its group...
					BigFileApplication.showErrorDialog("Error editing comment", "Unexpected invalid commment location error received", e);
				}
				return;
			}
		}
	}
	
	public void updateAllTagOccurrence(Tag tag, String newText) throws JAXBException, CoreException {
		for(TagOccurrence occ: tag.getOccurrences()){
			StickyTooltip tooltip = tagTooltips.get(occ);
			tooltip.hide();
			// there's a race condition here, I think. We cannot pull
			// the new text from the tooltip reliably, so we have to
			// pass it along in the update method lower down.
			tooltip.setText(tag.getName());
			String colourID = tag.getColour();
			tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(colourID));
			tooltip.updateText(newText);
			showOrHideStickyTooltip(occ);
		}
	}
	
	public void editTag(StickyTooltip tooltip) throws JAXBException, CoreException {
		for (TagOccurrence occurrence : tagTooltips.keySet()) {
			if (tagTooltips.get(occurrence) == tooltip) {
				fileModel.editTagOccurrence(occurrence, tooltip.getText());
				
				TagsView tagsView = 
						(TagsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(TagsView.ID);
				if (tagsView != null) {
					tagsView.updateView();
				}
				return;
			}
		}
	}
	
	/**
	 * Move the specified comment to a new location and move its sticky tooltip.
	 * @param comment comment to be moved
	 * @param newLine line number of new location
	 * @param newChar index of character within the line of the new location
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 * @throws InvalidCommentLocationException if the comment's group already has another comment at that location
	 */
	public void moveComment(Comment comment, int newLine, int newChar) throws JAXBException, CoreException, InvalidCommentLocationException {
		fileModel.moveComment(comment, newLine, newChar);
		// Remove the annotation and sticky tooltip at the comment's old location and replace them with ones at the new location
		hideAndDeleteComment(comment);
		addAndDisplayComment(comment);
	}
	
	/**
	 * Delete the specified comment group and remove all annotations for its comments.
	 * @param group comment group to delete
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public void deleteCommentGroup(CommentGroup group) throws JAXBException, CoreException {
		fileModel.deleteCommentGroup(group);
		for (Comment comment : group.getComments()) {
			hideAndDeleteComment(comment);
		}
	}
	
	/**
	 * Delete the specified comment and remove its annotation and sticky tooltip.
	 * @param comment comment to delete
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public void deleteComment(Comment comment) throws JAXBException, CoreException {
		fileModel.deleteComment(comment);
		hideAndDeleteComment(comment);
	}
	
	/**
	 * Removes the specified comment's annotation and sticky tooltip from the editor.
	 * @param comment comment whose annotation and sticky tooltip are to be removed
	 */
	private void hideAndDeleteComment(Comment comment) {
		this.getAnnotationModel().removeAnnotation(comment);
		removeStickyTooltip(comment);
	}
	
	/**
	 * Shows or hides the sticky tooltip for all comments in the specified group
	 * @param group comment group whose comments' tooltips are to be shown or hidden
	 * @param showStickyTooltip whether or not to show the comments' sticky tooltips. In other words, true = show, false = hide
	 * @param applyToAllComments whether or not the same value of showStickyTooltip should also be applied to all comments in this group
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file 
	 */
	public void showOrHideStickyTooltip(CommentGroup group, boolean showStickyTooltip, boolean applyToAllComments) throws JAXBException, CoreException {
		fileModel.setShowStickyTooltip(group, showStickyTooltip, applyToAllComments);
		for (Comment comment : group.getComments()) {
			showOrHideStickyTooltip(comment);
		}
	}
	
	/**
	 * Shows or hides the sticky tooltip for the specified comment
	 * @param comment comment whose tooltip is to be shown or hidden
	 * @param showStickyTooltip whether or not to show the comment's sticky tooltip. In other words, true = show, false = hide
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file 
	 */
	public void showOrHideStickyTooltip(Comment comment, boolean showStickyTooltip) throws JAXBException, CoreException {
		fileModel.setShowStickyTooltip(comment, showStickyTooltip);
		showOrHideStickyTooltip(comment);
	}
	
	/**
	 * Helper method to show or hide the specified comment's sticky tooltip, if this File Viewer is currently displaying sticky tooltips 
	 * for comments. Does nothing if the File Viewer is not displaying them at this time.
	 * @param comment comment whose sticky tooltip is to be shown or hidden
	 */
	private void showOrHideStickyTooltip(Comment comment) {
		if (showCommentTooltips) {
			StickyTooltip tooltip = commentTooltips.get(comment);
			try {
				// Only show tooltips for comments that are currently visible in the editor
				int offset = document.getLineOffset(comment.getLine()) + comment.getCharacter();
				for(RegionModel region : fileModel.getRegions()) {
					if(region.getStartLine() <= comment.getLine() && region.getEndLine() >= comment.getLine() && region.isCollapsed()) {
						// If comment in collapsed region...
						tooltip.hide();
						return;
					} else if(region.getEndLine() < comment.getLine() && region.isCollapsed()) {
						// If comment after collapsed region...
						offset -= document.getLineOffset(region.getEndLine() + 1) - document.getLineOffset(region.getStartLine() + 1);
					}
				}
				if (isVisibleInEditor(comment.getLine()) && comment.getShowStickyTooltip()) {					
					tooltip.show(toPoint(offset));
				} else {
					tooltip.hide();
				}
			} catch (BadLocationException e) {
				BigFileApplication.showErrorDialog("Cannot display comment in editor", "Error while trying to display comment", e);
			}
		}
	}
	
	/**
	 * Reveals all invisible sticky tooltips for comments whose showStickyTooltip property is set to true and whose group's 
	 * showStickyTooltip property is set to true, provided that this File Viewer has showing sticky tooltips for tags enabled.
	 */
	public void showCommentTooltips() {
		if(showCommentTooltips) {
			for (Comment comment : commentTooltips.keySet()) {
				showOrHideStickyTooltip(comment);
			}			
		}
	}
	
	/**
	 * Hides all visible sticky tooltips for comments.
	 */
	public void hideCommentTooltips() {
		for (StickyTooltip tooltip : commentTooltips.values()) {
			if (tooltip.isVisible()) {
				tooltip.hide();
			}
		}
	}
	
	/**
	 * Returns whether this File Viewer has showing sticky tooltips for comments enabled.
	 * @return true if showing sticky tooltips for comments is enabled, false otherwise
	 */
	public boolean isShowingCommentTooltips() {
		return showCommentTooltips;
	}
	
	/**
	 * Sets whether this File Viewer will display sticky tooltips for comments
	 * @param showCommentTooltips whether or not to display sticky tooltips for comments
	 */
	public void setShowCommentTooltips(boolean showCommentTooltips) {
		this.showCommentTooltips = showCommentTooltips;
	}
	
	/**
	 * Sets the colour to be used in the sticky tooltips for the comments in the specified comment group
	 * @param group group whose comments' sticky tooltips will use the colour 
	 * @param colourID ID of colour to use for the sticky tooltips
	 * @throws JAXBException if something goes wrong while updating the comments file
	 * @throws CoreException if something goes wrong while refreshing the comments file
	 */
	public void setColour(CommentGroup group, String colourID) throws JAXBException, CoreException {
		fileModel.setColour(group, colourID);
		for (Comment comment : group.getComments()) {
			StickyTooltip tooltip = commentTooltips.get(comment);
			tooltip.hide();
			tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(group.getColour())); 
			tooltip.updateColor();
			showOrHideStickyTooltip(comment);
		}
	}
	
	/**
	 * Focuses on the sticky tooltip for the specified comment
	 * @param comment comment whose tooltip should be focused
	 */
	public void setFocus(Comment comment) {
		StickyTooltip tooltip = commentTooltips.get(comment);
		tooltip.setFocus();
	}
	
	/**
	 * Add and display the specified region as a folding ProjectionAnnotation in the File Viewer.
	 * @param region region to add and display
	 * @param recursive indicates whether this method should also recursively add and display all of this region's child regions as well
	 */
	private void addAndDisplayRegion(RegionModel region, boolean recursive) {
		try {
			
			if (recursive) {
				for (RegionModel child : region.getChildren()) {
					addAndDisplayRegion(child, true);
				}
			}
			
			// NOTE The following block of code must appear after adding the children, 
			// to ensure that they are correctly collapsed within their parents
			
			int offset = document.getLineOffset(region.getStartLine());
			
			// endLine + 1 is invalid if we are at the last line, use length instead.
			int endLine = region.getEndLine();
			int endOffset;
			if(endLine == document.getNumberOfLines() - 1) {
				endOffset = document.getLength();
			} else {
				endOffset = document.getLineOffset(endLine + 1);
			}
			
			// calculate the new position of the annotation
			Position position = new Position(offset, endOffset - offset);

			ProjectionAnnotationModel projectionAnnotationModel = getProjectionAnnotationModel();
			if(projectionAnnotationModel != null){
				RegionAnnotation associatedAnnotation = getAssociatedRegionAnnotation(region);
				
				if(associatedAnnotation != null) {
					if(!position.equals(projectionAnnotationModel.getPosition(associatedAnnotation))) {
						// move the current annotation to the new position
						projectionAnnotationModel.modifyAnnotationPosition(associatedAnnotation, position);
					}
				} else {
					// create a new annotation and use that one
					RegionAnnotation regionAnnotation = new RegionAnnotation(region, fileModel);
					projectionAnnotationModel.addAnnotation(regionAnnotation, position);
					
					if(region.isCollapsed()) {
						projectionAnnotationModel.collapse(regionAnnotation);
					}
				}
			}

		} catch (BadLocationException e) {
			BigFileApplication.showErrorDialog("Could not add foldable region to editor", "Error while trying to create foldable region", e);
		} 
	}
	
	/**
	 *  Given a RegionModel that is being collapsed, collapse its associated RegionAnnotation
	 *  in the projection annotation model.
	 */
	public void collapse(RegionModel region) {
		
		RegionAnnotation regionAnnotation = getAssociatedRegionAnnotation(region);

		if(regionAnnotation == null || regionAnnotation.isCollapsed()) {
			return;
		}
		
		getProjectionAnnotationModel().collapse(regionAnnotation);
	}
	
	/**
	 *  Given a RegionModel that is being expanded, expand its associated RegionAnnotation
	 *  in the projection annotation model.
	 */
	public void expand(RegionModel region) {
		RegionAnnotation regionAnnotation = getAssociatedRegionAnnotation(region);
		
		if(regionAnnotation == null || !regionAnnotation.isCollapsed() || !RegionModel.allAncestorsExpanded(region)) {
			return;
		}
		
		getProjectionAnnotationModel().expand(regionAnnotation);
	}
	
	public IFileModelDataLayer getFileModel() {
		return this.fileModel;
	}
	
	/**
	 * Remove the specified region from the file and from this File Viewer
	 * @param region region to be removed
	 * @throws JAXBException if something goes wrong while updating the file's regions file
	 * @throws CoreException if something goes wrong while refreshing the file's regions file in the workspace
	 */
	public void removeRegion(RegionModel region) {
		RegionAnnotation regionAnnotation = getAssociatedRegionAnnotation(region);
		
		if(regionAnnotation == null) {
			return;
		}
		
		ProjectionAnnotationModel model = getProjectionAnnotationModel();
		model.removeAnnotation(regionAnnotation);
	}
	
	/**
	 * Add an occurrence of the specified tag at the given location to the file and add a marker for the occurrence
	 * @param tagName name of the tag to be added
	 * @param startLine start line of occurrence to add
	 * @param startChar first char within start line of occurrence to add
	 * @param endLine end line of occurrence to add
	 * @param endChar last char within end line of occurrence to add
	 * @throws DuplicateTagOccurrenceException if there is already a tag of that type with the same start and end chars
	 * @throws JAXBException if something goes wrong while updating the file's tags file
	 * @throws CoreException if something goes wrong while creating or refreshing the file's tags file
	 */
	public void addTag(String tagName, int startLine, int startChar, int endLine, int endChar) 
			throws DuplicateTagOccurrenceException, JAXBException, CoreException {
		fileModel.addTag(tagName, startLine, startChar, endLine, endChar);
		addAndDisplayTagOccurrence(fileModel.getTagOccurrence(tagName, startLine, startChar, endLine, endChar));
	}
	
	/**
	 * Add and display a tag occurrence in the editor by adding an annotation and a sticky tooltip for it.
	 * Tags add a Blue squiqqly Style range to the widget automatically through the AnnotationModel
	 * @param occurrence tag occurrence to add
	 */
	private void addAndDisplayTagOccurrence(TagOccurrence occurrence) {
		try {
			// Add the annotation
			int startOffset = document.getLineOffset(occurrence.getStartLine());
			int endOffset = document.getLineOffset(occurrence.getEndLine());
			
			if(documentManager.lineLoaded(occurrence.getStartLine())) {
				startOffset += occurrence.getStartChar();
			}
			
			if(documentManager.lineLoaded(occurrence.getEndLine())) {
				endOffset += occurrence.getEndChar();
			}
			
			Position newPosition = new Position(startOffset, endOffset - startOffset + 1);
			
			// This must ensure that the tooltip is deleted, not just hidden
			if(!tagTooltips.containsKey(occurrence)) {
				// This annotation has no tooltip associated with it, better add one
				StickyTooltip tooltip = new StickyTooltip(textWidget, true, occurrence);
				tooltip.setText(occurrence.getTag().getName());
				String colourID = occurrence.getTag().getColour();
				tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(colourID));
				tagTooltips.put(occurrence, tooltip);
			}

			// XXX I'm not sure if I like this cast here
			AnnotationModel annotationModel = (AnnotationModel) this.getAnnotationModel();
			Position currentPosition = annotationModel.getPosition(occurrence);
			if(currentPosition == null) {
				annotationModel.addAnnotation(occurrence, newPosition);
			} else if(!currentPosition.equals(newPosition)) {
				annotationModel.modifyAnnotationPosition(occurrence, newPosition);
			}
			
			showOrHideStickyTooltip(occurrence);
		} catch (BadLocationException e) {
			BigFileApplication.showErrorDialog("Cannot display tag occurrence in editor", "Error while trying to display tag occurrence", e);
		}
	}
	
	/**
	 * Given a marker annotation that is being tracked by the viewer, it will compute its
	 * location in the view from the its parameters, and move it to that location.  If the
	 * marker annotation is not in the annotation model currently, it will also add it again.
	 * 
	 * XXX FIXME: this method only works with annotations that come from Tours, as it relies on 
	 * it having the correct attributes.  If it does not contain those attributes, the annotation 
	 * will never be added to the view.
	 */
	private void addAndDisplayMarkerAnnotation(MarkerAnnotation markerAnnotation) {
		IMarker marker = markerAnnotation.getMarker();
		
		try {
			int startLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			int endLine = marker.getAttribute(MarkerAnnotationConstants.MARKER_END_LINE_NUMBER, -1);
			int startIntraline = marker.getAttribute(MarkerAnnotationConstants.MARKER_START_INTRA_LINE_OFFSET, -1);
			int endIntraline = marker.getAttribute(MarkerAnnotationConstants.MARKER_END_INTRA_LINE_OFFSET, -1);
			
			if(startLine == -1 || endLine == -1 || startIntraline == -1 || endIntraline == -1) {
				// TODO this could be handled if startIntraline and or endIntraline weren't set.
				return;
			}
			
			int startOffset = document.getLineOffset(startLine);
			int endOffset = document.getLineOffset(endLine);
			
			if(documentManager.lineLoaded(startLine)) {
				startOffset += startIntraline;
			}
			
			if(documentManager.lineLoaded(endLine)) {
				endOffset += endIntraline;
			}
			
			Position newPosition = new Position(startOffset, endOffset - startOffset + 1);
			
			AnnotationModel annotationModel = (AnnotationModel) this.getAnnotationModel();
			Position currentPosition = annotationModel.getPosition(markerAnnotation);
			
			if(currentPosition == null) {
				annotationModel.addAnnotation(markerAnnotation, newPosition);
			} else if(!currentPosition.equals(newPosition)) {
				annotationModel.modifyAnnotationPosition(markerAnnotation, newPosition);
			}
			
		} catch(Exception e) {
			BigFileApplication.showErrorDialog("Cannot redraw marker annotation in editor", "Error while trying to display marker annotation", e);
		}
	}
	
	/**
	 * Delete all occurrences the specified tag from the file and remove all annotations for the tag
	 * @param tag tag to delete
	 * @throws JAXBException if something goes wrong while updating the file's tags file
	 * @throws CoreException if something goes wrong while refreshing the file's tags file
	 */
	public void deleteTag(Tag tag) throws JAXBException, CoreException {
		fileModel.deleteTag(tag);
		for (TagOccurrence occurrence : tag.getOccurrences()) {
			hideAndDeleteTagOccurrence(occurrence);
		}
	}
	
	/**
	 * Delete the specified tag occurrence and its associated annotation.
	 * @param occurrence tag occurrence to delete
	 * @throws JAXBException if something goes wrong while updating the file's tags file
	 * @throws CoreException if something goes wrong while refreshing the file's tags file
	 */
	public void deleteTagOccurrence(TagOccurrence occurrence) throws JAXBException, CoreException {
		Tag tag = occurrence.getTag();
		fileModel.deleteTagOccurrence(occurrence);
		hideAndDeleteTagOccurrence(occurrence);
		if(tag.getOccurrences().isEmpty()) {
			deleteTag(tag);
		}
	}
	
	/**
	 * Removes the specified tag occurrence's annotation and sticky tooltip from the editor.
	 * @param occurrence tag occurrence whose annotation and sticky tooltip are to be removed
	 */
	private void hideAndDeleteTagOccurrence(TagOccurrence occurrence) {
		this.getAnnotationModel().removeAnnotation(occurrence);
		StickyTooltip tooltip = tagTooltips.get(occurrence);
		if(null != tooltip){
			tooltip.hide();
		}
		tagTooltips.remove(occurrence);
	}
	
	/**
	 * Shows or hides the sticky tooltip for all occurrences of the specified tag.
	 * @param tag tag whose occurrences' tooltips are to be shown or hidden
	 * @param showStickyTooltip whether or not to show the occurrences' sticky tooltips. In other words, true = show, false = hide
	 * @param applyToAllOccurrences whether or not the same value of showStickyTooltip should also be applied to all occurrences of this tag
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file 
	 */
	public void showOrHideStickyTooltip(Tag tag, boolean showStickyTooltip, boolean applyToAllOccurrences) throws JAXBException, CoreException {
		fileModel.setShowStickyTooltip(tag, showStickyTooltip, applyToAllOccurrences);
		for (TagOccurrence occurrence : tag.getOccurrences()) {
			showOrHideStickyTooltip(occurrence);
		}
	}
	
	/**
	 * Shows or hides the sticky tooltip for the specified tag occurrence
	 * @param occurrence tag occurrence whose tooltip is to be shown or hidden
	 * @param showStickyTooltip whether or not to show the occurrence's sticky tooltip. In other words, true = show, false = hide
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file 
	 */
	public void showOrHideStickyTooltip(TagOccurrence occurrence, boolean showStickyTooltip) throws JAXBException, CoreException {
		fileModel.setShowStickyTooltip(occurrence, showStickyTooltip);
		showOrHideStickyTooltip(occurrence);
	}
	
	/**
	 * Helper method to show or hide the specified tag occurrence's sticky tooltip, if this File Viewer is currently displaying sticky tooltips 
	 * for tags. Does nothing if the File Viewer is not displaying them at this time.
	 * @param occurrence tag occurrence whose sticky tooltip is to be shown or hidden
	 */
	private void showOrHideStickyTooltip(TagOccurrence occurrence) {
		if (showTagTooltips) {
			StickyTooltip tooltip = tagTooltips.get(occurrence);
			try {				
				// Only show tooltips for tag occurrences that are currently visible in the editor
				// TODO: this only works if the tag's end location is visible. What should we do if the start location is visible but
				// the end location isn't? What if neither endpoint is visible but some middle character is? 
				int offset = document.getLineOffset(occurrence.getEndLine()) + occurrence.getEndChar();
				for(RegionModel region : fileModel.getRegions()) {
					if(region.getStartLine() <= occurrence.getStartLine() && region.getEndLine() >= occurrence.getEndLine() && region.isCollapsed()) {
						// If contained in collapsed region...
						tooltip.hide();
						return;
					} else if(region.getEndLine() < occurrence.getStartLine() && region.isCollapsed()) {
						// If following a collapsed region
						offset -= document.getLineOffset(region.getEndLine() + 1) - document.getLineOffset(region.getStartLine() + 1);
					}
				}
				if (isVisibleInEditor(occurrence.getEndLine()) && occurrence.getShowStickyTooltip()) {					
					tooltip.show(toPoint(offset));
				} else {
					tooltip.hide();
				}
			} catch (BadLocationException e) {
				BigFileApplication.showErrorDialog("Cannot display tooltip for tag occurrence", 
						"Error while trying to display tooltip for tag occurrence", e);
			}
		}
	}
	
	/**
	 * Reveals all invisible sticky tooltips for tag occurrences whose showStickyTooltip property is set to true and whose underlying tag's 
	 * showStickyTooltip property is set to true, provided that this File Viewer has showing sticky tooltips for tags enabled.
	 */
	public void showTagTooltips() {
		if(showTagTooltips) {
			for (TagOccurrence occurrence : tagTooltips.keySet()) {
				showOrHideStickyTooltip(occurrence);
			}			
		}
	}
	
	/**
	 * Hides all visible sticky tooltips for tags.
	 */
	public void hideTagTooltips() {
		for (StickyTooltip tooltip : tagTooltips.values()) {
			if (tooltip.isVisible()) {
				tooltip.hide();
			}
		}
	}
	
	/**
	 * Returns whether this File Viewer has showing sticky tooltips for tags enabled.
	 * @return true if showing sticky tooltips for tags is enabled, false otherwise
	 */
	public boolean isShowingTagTooltips() {
		return showTagTooltips;
	}
	
	/**
	 * Sets whether this File Viewer will display sticky tooltips for comments
	 * @param showTagTooltips whether or not to display sticky tooltips for comments
	 */
	public void setShowTagTooltips(boolean showTagTooltips) {
		this.showTagTooltips = showTagTooltips;
	}
	
	/**
	 * Sets the colour to be used in the sticky tooltips for occurrences of the specified tag.
	 * @param tag tag whose occurrences' sticky tooltips will use the colour 
	 * @param colourID ID of colour to use for the sticky tooltips
	 * @throws JAXBException if something goes wrong while updating the tags file
	 * @throws CoreException if something goes wrong while refreshing the tags file
	 */
	public void setColour(Tag tag, String colourID) throws JAXBException, CoreException {
		fileModel.setColour(tag, colourID);
		for (TagOccurrence occurrence : tag.getOccurrences()) {
			StickyTooltip tooltip = tagTooltips.get(occurrence);
			tooltip.hide();
			tooltip.setBackgroundColor(BigFileActivator.getDefault().getColorRegistry().get(tag.getColour())); 
			tooltip.updateColor();
			showOrHideStickyTooltip(occurrence);
		}
	}
	
	/**
	 * Focuses on the sticky tooltip for the specified tag occurrence
	 * @param occurrence tag occurrence whose tooltip should be focused
	 */
	public void setFocus(TagOccurrence occurrence) {
		StickyTooltip tooltip = tagTooltips.get(occurrence);
		tooltip.setFocus();
	}
	
	/**
	 * Redraws all visible sticky tooltips for this file. Used when the sticky tooltip's location on the screen changes.
	 */
	private void redrawTooltips() {
		for (Comment comment : commentTooltips.keySet()) {
			commentTooltips.get(comment).hide();
			showOrHideStickyTooltip(comment);
		}
		for (TagOccurrence occurrence : tagTooltips.keySet()) {
			tagTooltips.get(occurrence).hide();
			showOrHideStickyTooltip(occurrence);
		}
	}
	
	/**
	 * Retrieve a Point corresponding to the specified location in the file.
	 * @param offset location in the file in offset representation
	 * @return a Point for the offset's location
	 * @throws BadLocationException 
	 */
	private Point toPoint(int offset) throws BadLocationException {
		Point editorLocation = BfvViewerUtils.getAbsoluteLocation(textWidget);
		Point commentLocation = textWidget.getLocationAtOffset(offset);
		return new Point(editorLocation.x + commentLocation.x, editorLocation.y + commentLocation.y);
	}
	
	/**
	 * Determines if the specified location in the file is currently visible in the editor.
	 * @param lineOffset location in the file in offset representation
	 * @return true if that offset is visible in the editor's text widget, false otherwise
	 */
	private boolean isVisibleInEditor(int lineNumber) {
		int top = this.getTopIndex(), bottom = this.getBottomIndex();
		return (lineNumber >= top && lineNumber <= bottom);
	}
	
	/**
	 * Determine whether a line and character index describe a valid location in the File Viewer's document.
	 * @param line line number (0-indexed)
	 * @param character index of character within line (0-indexed)
	 * @return true if the location is valid, false otherwise
	 */
	public boolean isValidLocation(int line, int character) {
		try {
			int length = document.getLineLength(line);
			// Note: the second case after || is for lines of length 0, like the blank line at the end of a file: char 0 is fine, anything else isn't
			// (included since it is currently possible to add a comment on these lines)
			return (length > 0 && character < length) || (length == 0 && character == 0);			
		} catch (BadLocationException e) {
			return false;
		}
	}	
	
	public Map<Comment, StickyTooltip> getCommentTooltips() {
		return commentTooltips;
	}
	
	public Map<TagOccurrence, StickyTooltip> getTagTooltips() {
		return tagTooltips;
	}
	
	public ISourceViewer getViewer() { 
		return this;
	}
	
	private boolean updating = false;
	private BigFileSlaveDocumentManager projectionSlaveDocumentManager;
	
	public void updateTextWidget() {
		updateTextWidget(false);
	}
	
	/**
	 * This method is responsible for updating the BigFileView for any change that might require either a
	 * redraw or a repaging in.  This method will handle:
	 * - paging out and paging in parts of the document
	 * - reapplying syntax highlighting
	 * - readding annotations (including marker annotations)
	 * - redrawing tooltips
	 * @param highlightingAndDocumentReset
	 */
	public void updateTextWidget(boolean highlightingAndDocumentReset) {
		
		// Make sure that we don't call update when we are already updating, as it will not accomplish anything.
		if(updating) {
			return;
		}
		
		updating = true;
		
		try {
			// use the viewer as it converts the widget offset to model offset automatically for us
			int startIndex = this.getTopIndex();
			int endIndex = this.getBottomIndex();
			
			// Turn off the automatic highlighting for a bit to avoid extra syntax highlighting events.
			syntaxHighlightingManager.setHighlightOnTextChange(false);
			
			if(highlightingAndDocumentReset) {
				syntaxHighlightingManager.setHighlightingDirty();
				documentManager.resetLoadedIntervals();
			}
			
			DocumentChange pagingChanges = documentManager.getDocumentChangesForVisibleInterval(startIndex, endIndex);
			boolean willChange = documentManager.willUpdateChangeDocument(startIndex, endIndex);
			
			if(willChange) {
				documentManager.updateDocument(startIndex, endIndex, pagingChanges);
				cleanupAnnotations();
				redrawAnnotations();
			}
			/**
			 * these both both need to be outside the conditional.
			 * redrawTooltips to update their positions.
			 * syntax highlighting to ensure that it gets updated if we scroll past the end of our highlighted interval
			 */
			redrawTooltips();
			syntaxHighlightingManager.adjustHighlighting();
			syntaxHighlightingManager.setHighlightOnTextChange(true);
			redrawMarkerAnnotations();
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			updating = false;
		}
		
		// ensure that the vertical scroll bar is pointing to the correct value
		textWidget.getVerticalBar().setSelection(textWidget.getTopPixel());
		
	}
	
	/**
	 * This method will should be called whenever there is a change to the regions in the FileModelDAO.
	 */
	@Override
	public void handleRegionChanged(RegionEventType eventType, RegionModel model) {
		switch(eventType) {
		case REGION_ADDED:
			handleRegionAdded(model);
			break;
			
		case REGION_COLLAPSED:
			handleRegionCollapsed(model);
			break;
			
		case REGION_EXPANDED:
			handleRegionExpanded(model);
			break;
			
		case REGION_REMOVED:
			handleRegionRemoved(model);
			break;
			
		case REGION_RENAMED:
			break;
		}
		
		try {
			updateTextWidget(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean pagingOngoing(){
		return this.documentManager.pagingOngoing;
	}
	
	/**
	 * TODO XXX FIXME I dont think that this method is  currently ever called.  I believe that it is related
	 * to the Text Diff stuff, which is not currently working. 
	 */
	public void synchronizedUpdateTextWidget(int startIndex, int endIndex, boolean delaySyntaxHighlighting) {
		
		// Make sure that we don't call update when we are already updating, as it will not accomplish anything.
		if(updating) {
			return;
		}
		
		updating = true;
		
		try {
			DocumentChange pagingChanges = documentManager.getDocumentChangesForVisibleInterval(startIndex, endIndex);
			boolean changed = documentManager.updateDocument(startIndex, endIndex, pagingChanges);
			
			if(changed) {
				if(!delaySyntaxHighlighting){
					syntaxHighlightingManager.setHighlightingDirty();
				}
				redrawAnnotations();
			}
			
			redrawTooltips();
			
			if(!delaySyntaxHighlighting){
				syntaxHighlightingManager.adjustHighlighting();
			}
			
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			updating = false;
		}
		
		// ensure that the vertical scroll bar is pointing to the correct value
		textWidget.getVerticalBar().setSelection(textWidget.getTopPixel());
	}
	
	private boolean handleRegionAdded(RegionModel model) {
		addAndDisplayRegion(model, false);
		return true;
	}
	
	private boolean handleRegionRemoved(RegionModel model) {
		removeRegion(model);
		return true;
	}
	
	public boolean isLoaded(){
		return loaded;
	}

	private boolean handleRegionExpanded(RegionModel model) {
		expand(model);
		return true;
	}

	private boolean handleRegionCollapsed(RegionModel model) {
		collapse(model);
		return true;
	}
	
	/**
	 * We are overriding this method so that we can inject our own document manager into this class.
	 * The document content manager is used so that we gain the ability to control which line 
	 * trackers are used in the document and the projection document, which allows us to
	 * greatly reduce our memory footprint.
	 */
	@Override
	protected ISlaveDocumentManager createSlaveDocumentManager() {
		projectionSlaveDocumentManager = new BigFileSlaveDocumentManager();
		return projectionSlaveDocumentManager;
	}
	
	/**
	 * We are overriding this method so that we can inject our own projection annotation model.
	 * For one, we want to never call the expandAll method, because it is stupid.  Secondly, we needed 
	 * the ability to alert the BigFileViewer to the fact that an annotation change had occurred.
	 */
	@Override
	protected IAnnotationModel createVisualAnnotationModel(IAnnotationModel annotationModel) {
		IAnnotationModel model= super.createVisualAnnotationModel(annotationModel);
		annotationModel.addAnnotationModelListener(this);
		fProjectionAnnotationModel= new BigFileProjectionAnnotationModel(this);
		return model;
	}
	
	public void handleAnnotationChangeCommitted() {
		if(null != syntaxHighlightingManager){
			syntaxHighlightingManager.setHighlightingDirty();
			syntaxHighlightingManager.adjustHighlighting();
		}
	}

	/**
	 * There is a listener in this BigFileViewer class already for this, but this offers different responsibilities.
	 * 
	 * @param model
	 */
	@Override
	public void modelChanged(IAnnotationModel model) {
		// Only appears to get called once, probably when constructing annotations.
		// Originally called in BigFileProjectionAnnotation...but this doesn't work
		// for collapse/expand calls...but we do get a thread access error!
		// System.out.println("here in modelChanged() once");
		// this.handleAnnotationChangeCommitted();
	}

	public void updateAllMessageOccurrence(MessageType selected, String newTypeName) {
		// TODO Auto-generated method stub
		
	}

	public void deleteMessageType(MessageType type) throws JAXBException, CoreException {
		fileModel.deleteMessageType(type);
/*		for (MessageOccurrence occurrence : type.getOccurrences()) {
			hideAndDeleteMessageOccurrence(occurrence);
		}*/
		
	}

	public void deleteMessageOccurrence(MessageOccurrence occurrence) throws JAXBException, CoreException {
		// TODO Auto-generated method stub
		fileModel.deleteMessageOccurrence(occurrence);
	}


}
