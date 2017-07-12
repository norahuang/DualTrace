package ca.uvic.chisel.atlantis.tracedisplayer;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.eventtracevisualization.AssemblyTraceVisualizationView;
import ca.uvic.chisel.atlantis.eventtracevisualization.ThreadEventTraceVisualizationView;
import ca.uvic.chisel.atlantis.handlers.GenerateMemoryViewHandler;
import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.atlantis.views.FunctionsView;
import ca.uvic.chisel.atlantis.views.HexVisualization;
import ca.uvic.chisel.atlantis.views.MemoryVisualization;
import ca.uvic.chisel.atlantis.views.RegistersView;
import ca.uvic.chisel.atlantis.views.ThreadFunctionsView;
import ca.uvic.chisel.atlantis.views.WatchedView;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

public class AtlantisTraceEditor extends BigFileEditor {
	
	public static final String ID = "ca.uvic.chisel.atlantis.tracedisplayer.TraceDisplayer";
	public static final String CONTEXT_MENU_ID = "#TraceDisplayerContext";
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		FileEditorInput fileEditorInput = (FileEditorInput) input;

		this.emptyFile = BfvFileUtils.convertFileIFile(fileEditorInput.getFile());
		
		super.init(site, input);
	}
	
	public boolean hasExecutionTrace() {
		return this.hasReadableFile;
	}
	
	
	private void syncTraceVisualizations() {
		AssemblyTraceVisualizationView assemblyVis = (AssemblyTraceVisualizationView) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(AssemblyTraceVisualizationView.ID);
		
		if (assemblyVis != null) {
			assemblyVis.selectMarker(viewer.getCurrentLineNumber());
		}
		
		ThreadEventTraceVisualizationView threadEventVis = (ThreadEventTraceVisualizationView) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ThreadEventTraceVisualizationView.ID);
		
		if (threadEventVis != null) {
			threadEventVis.selectMarker(viewer.getCurrentLineNumber());
		}
	}
	
	public void clearMemoryViewContents() {
		if(getMemoryView() != null){
			getMemoryView().clearDataContents();
		}
		if(getRegistersView() != null){
			getRegistersView().clearContents();
		}
		if(getWatchedView() != null){
			getWatchedView().clearContents();
		}
		// hex view cleared by memory view clear call,
		// as the reference the same data
	}
	
	public void syncMemoryVisualization(boolean performRefreshes) {

		MemoryVisualization memoryVisualizationView = getMemoryView();
		if (memoryVisualizationView != null) {
			
			memoryVisualizationView.updateMemoryView();
			
			
			/**
			 * This *refresh* gets done with complete data available in {@link MemoryVisualization#visualRefreshAfterUpdateMemoryView},
			 * and calling this from {@link AtlantisTraceEditor#handleCursorPositionChanged} does not appear to have any importance.
			 */
			if(performRefreshes){
				memoryVisualizationView.getViewer().refresh();
				
				// update the registers view
				RegistersView registersView = getRegistersView();
				if(registersView != null) {
					registersView.refresh(); // suspicious, this does not occur when the new line's memory data is available
				}
				
				// update the hex view
				HexVisualization hexView = getHexView();
				if(hexView != null) {
					hexView.refresh();
				}
				
				// update the watched view
				WatchedView.refreshCurrentWatchedView();
			}
			
			// update the functions view
			FunctionsView functionsView = getFunctionsView();
			if(functionsView != null) {
				functionsView.updateForLineChange();
				functionsView.refresh();
			}
			
			// update the thread functions view
			ThreadFunctionsView threadFunctionsView = getThreadFunctionsView();
			if(threadFunctionsView != null) {
				threadFunctionsView.refresh();
			}
		}
	}	
	
	public void highlightInMemoryView(String address) {
		MemoryVisualization memoryVisualizationView = getMemoryView();
		int index = memoryVisualizationView.findAndSelectAddress(address);
		if(index != -1) {
			memoryVisualizationView.getViewer().getTable().setSelection(index);
			memoryVisualizationView.getViewer().getTable().setTopIndex(index);
			memoryVisualizationView.setFocus();
		}
	}
	
	private MemoryVisualization getMemoryView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()){
			return null;
		}
		return (MemoryVisualization) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(MemoryVisualization.ID);
	}
	
	private RegistersView getRegistersView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()){
			return null;
		}
		return (RegistersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RegistersView.ID);
	}
	
	private HexVisualization getHexView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()){
			return null;
		}
		return (HexVisualization) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(HexVisualization.ID);
	}
	
	public WatchedView getWatchedView(){
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()){
			return null;
		}
		return (WatchedView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(WatchedView.ID);
	}
	
	public FunctionsView getFunctionsView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()) {
			return null;
		}
		
		return (FunctionsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(FunctionsView.ID);
	}
	
	public ThreadFunctionsView getThreadFunctionsView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()) {
			return null;
		}
		
		return (ThreadFunctionsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ThreadFunctionsView.ID);
	}
	
	int prevCursorPositionHandled = -1;
	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		ITextSelection textSelection;
		if (selection instanceof ITextSelection){
			textSelection = (ITextSelection) selection;
		}else{
			return;
		}
		
		boolean rangeChange = false;
		if(textSelection != null){
			int startLine = textSelection.getStartLine();
			int endLine = textSelection.getEndLine();
			
			Point prevSelected = viewer.getSelectedRange();
			if(prevSelected.x != textSelection.getOffset() || prevSelected.y != textSelection.getLength()){
				rangeChange = true;
			}
			
//		When Triple clicked, set the Caret to be at the end of the line
//		selection length will be 1 more than the lines length because of carriage return
			if((startLine - endLine == 0) && (textSelection.getLength() - viewer.getLineLength(endLine) == 1)){
				viewer.setSelectedRange(textSelection.getOffset(), textSelection.getLength() - 1);
			}		
		}
		AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		
		syncTraceVisualizations(); 
		if(GenerateMemoryViewHandler.getCurrentState()
				&& (prevCursorPositionHandled != viewer.getCurrentLineNumber() || rangeChange)
				&& !activeTraceDisplayer.getProjectionViewer().pagingOngoing()
				) {
			syncMemoryVisualization(false);
			prevCursorPositionHandled = viewer.getCurrentLineNumber();
		}
	}
	
	@Override
	public void triggerCursorPositionChanged() {
		handleCursorPositionChanged();
	}
	
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		// If the changed preference was a syntax highlighting colour preference, update the syntax highlighting colours
		SyntaxHighlightingPreference preference = SyntaxHighlightingPreference.getByName(event.getProperty());
		if (preference != null) {
			AtlantisActivator.getDefault().getColorRegistry().put(preference.getColourID(), TraceDisplayerUtil.getColourPreference(event));
			// Need these lines to update the colours without restarting the editor
			TraceDisplayerConfiguration config = (TraceDisplayerConfiguration) this.getSourceViewerConfiguration();
			config.updateSyntaxHighlightingColour(preference);
			viewer.invalidateTextPresentation();
			
		// Otherwise, check if the highlight hex values preference was changed and act accordingly
		}		
		
		super.handlePreferenceStoreChanged(event);
	}
	
	@Override
	protected boolean isReadableFile() {
		if(null != AtlantisBinaryFormat.getRegisteredBinaryDataFormat(this.emptyFile)){
			return true;
		} else {
			return TraceDisplayerUtil.isExecutionTrace(AtlantisFileUtils.inst.convertBlankFileToActualFile(this.emptyFile));
		}
	}
	
}
