package ca.uvic.chisel.bfv.editor;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import bfv.org.eclipse.ui.editors.text.EditorsUI;
import bfv.org.eclipse.ui.editors.text.TextEditor;
import bfv.org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import bfv.org.eclipse.ui.texteditor.NewLineNumberChangeRulerColumn;
import ca.uvic.chisel.bfv.BigFileActivator;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.RegionChangeListener;
import ca.uvic.chisel.bfv.dialogs.GotoLineAction;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.bfv.utils.RegionUtils;

public class BigFileEditor extends TextEditor implements IPropertyChangeListener, RegionChangeListener  {

	protected static final int VERTICAL_RULER_WIDTH = 12;
	public static final String ID = "ca.uvic.chisel.bfv.editor.Editor";
	public static final String CONTEXT_MENU_ID = "#BigFileEditorContext";
	
	protected BigFileViewer viewer;
	private long cursorTimeOld;
	private FileEditorInput fileEditorInput;
	private BigFileViewerConfiguration viewerConfiguration;
	private IFileModelDataLayer fileModel;
	protected File emptyFile;
	private FileEditorInput emptyFileEditorInput;
	
	protected boolean hasReadableFile;
	private BfvDocumentProvider documentProvider;

	@Override
	protected BigFileViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		// Need a projection viewer to support collapsible regions
		// Possible early leak when passing 'this' to constructor
		BigFileViewer viewer = new BigFileViewer(parent, ruler, this.getOverviewRuler(), this.isOverviewRulerVisible(), styles, false, this);
		getSourceViewerDecorationSupport(viewer);
		viewer.setEditable(false);	
		this.viewer = viewer;
		
		this.setEditorContextMenuId(BigFileEditor.CONTEXT_MENU_ID);
		
		String lineNumbers = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER;
		EditorsUI.getPreferenceStore().setValue(lineNumbers, true);
		
		return viewer;
	}

	public BigFileViewer getProjectionViewer() {
		return viewer;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		viewer.setViewerConfiguration(viewerConfiguration);
		viewer.init(fileEditorInput, viewer);
		
			
		// Listen for changes to the Activator's preference store so the editor can handle changes to the syntax highlighting colours
		BigFileActivator.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof FileEditorInput)) {
			throw new PartInitException("The BigFileViewer requires FileEditorInput, got instance of " + input.getClass());
		}
		try {
			FileEditorInput fileEditorInput = (FileEditorInput) input;
			IFileUtils fileUtils = RegistryUtils.getFileUtils();
			IFile eFile = fileEditorInput.getFile();
			this.emptyFile = BfvFileUtils.convertFileIFile(eFile);
			hasReadableFile = isReadableFile();
			
			if(!hasReadableFile) {
				BigFileApplication.showInformationDialog("Could not open file", "The file found can not be opened by the selected editor.");
				throw new PartInitException("Could not open file");
			} else {
				// Create empty file in a hidden directory with project parallel structure, and point editor at it.
				// Once I have an IFile, create it by using IFile.setContents() or create(), or there might be sync issues.
				eFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
				if(!emptyFile.exists()){
					fileUtils.createEmptyFile(eFile);
				}
				
				emptyFileEditorInput = new FileEditorInput(eFile);
				fileModel = RegistryUtils.getFileModelDataLayerFromRegistry(this.getCurrentBlankFile());
				fileModel.readFileIfNeeded();
				
				if(!fileModel.fileReadSuccessfully()){
					RegistryUtils.clearFileModelDataLayerFromRegistry(fileModel);
					// this.close(false);
					// this.dispose();
					throw new PartInitException("File opening cancelled.");
				}
				
				DocumentContentManager documentManager = new DocumentContentManager(fileModel);
				documentManager.initializeDocument();
				
				documentProvider = new BfvDocumentProvider(documentManager);
				
				viewerConfiguration = new BigFileViewerConfiguration(fileModel, documentManager);
				this.fileEditorInput = emptyFileEditorInput;
				setSite(site);
				setInput(emptyFileEditorInput);
				
				fileModel.registerRegionChangedListener(this);
				
			}
		} catch (Exception e) {
			throw new PartInitException(e.getMessage(), e);
		}
	}

	protected boolean isReadableFile() {
		return true;
	}

	@Override
	protected void createActions() {
		super.createActions();
		GotoLineAction action = new GotoLineAction(this, viewer); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.GOTO_LINE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
		setAction(ITextEditorActionConstants.GOTO_LINE, action);
	}
	
	@Override
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}

	@Override
	protected void handleCursorPositionChanged() {
		// XXX TODO FIXME we may want to test to see if this can be removed (used to handle fast scrolling)
		if((System.currentTimeMillis() - cursorTimeOld) < 200) {
			cursorTimeOld = System.currentTimeMillis();		
			return;
		}
		super.handleCursorPositionChanged();
		this.getStatusLineManager().setMessage(this.getCursorPosition()); // display cursor position in status bar
		cursorTimeOld = System.currentTimeMillis();	
	}
	
	@Override
	public void setHighlightRange(int offset, int length, boolean moveCursor) {
		try {
			super.setHighlightRange(offset, length, moveCursor);
			int lineNum = viewer.getDocument().getLineOfOffset(offset);
			
			viewer.updateTextWidget();
			viewer.gotoLineAtOffset(lineNum, 0);
			
			if(moveCursor) {
				viewer.getTextWidget().setCaretOffset(viewer.getDocument().getLineOffset(lineNum));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public IEditorInput getEditorInput() {
		return super.getEditorInput();
	}
	
	public void triggerCursorPositionChanged() {
		handleCursorPositionChanged();
	}

	/**
	 * Does nothing--this editor does not support text drag and drop since files are not to be edited.
	 */
	@Override
	protected void installTextDragAndDrop(ISourceViewer viewer) {}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		handlePreferenceStoreChanged(event);
	}

	/**
	 * We must override this method so that we ensure that the document never gets set to editable.
	 * Alternatively, we could create an event filter and just call super.handlePreferenceStoreChanged()
	 */
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		updateLineNumberColumnForPropertyChange(event);
	}

	/**
	 * Always returns false since files are not to be edited.
	 */
	@Override
	public boolean isEditable() {
		return false;
	}

	/**
	 * Gets rid of any sticky tooltips and disposes this editor.
	 */
	@Override
	public void dispose() {
		BigFileActivator.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		if(null != viewer){
			viewer.disposeDocument();
			viewer.disposeDocumentManager();
			viewer.disposeFileModel();
		}
		emptyFileEditorInput = null;
		
		if(fileModel != null) {
			fileModel.deregisterRegionChangedListener(this);
			fileModel = null;
		}
		viewerConfiguration = null;
		super.dispose();
	}

	@Override
	public void handleRegionChanged(RegionEventType eventType, RegionModel model) {
		updateLineNumberColumnRegions();
	}

	private void updateLineNumberColumnRegions() {
		List<ILineRange> collapsedRanges = RegionUtils.getCollapsedRegionLineRanges(fileModel.getRegions());
		
		if(fLineNumberRulerColumn instanceof NewLineNumberChangeRulerColumn) {
			((NewLineNumberChangeRulerColumn)fLineNumberRulerColumn).setCurrentCollapsedRegions(collapsedRanges);
		}
	}

	/**
	 * This method exposes the document which is contained in the DocumentContentManager.  
	 * It would be better to not expose it this way.
	 */
	public IDocument getDocument() {
		return viewer.getDocument();
	}

	@Override
	protected IVerticalRulerColumn createLineNumberRulerColumn() {
		IVerticalRulerColumn createLineNumberRulerColumn = super.createLineNumberRulerColumn();
		updateLineNumberColumnRegions();
		return createLineNumberRulerColumn;
	}
	
	public File getCurrentBlankFile() {
		return this.emptyFile;
	}

	public File getEmptyFile() {
		return BfvFileUtils.convertFileIFile(emptyFileEditorInput.getFile());
	}

}