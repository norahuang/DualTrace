package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;

public class AtlantisFindOccurrences extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor != null && editor instanceof AtlantisTraceEditor) {
			AtlantisTraceEditor traceDisplayer = (AtlantisTraceEditor) editor;
			StyledText textWidget = traceDisplayer.getProjectionViewer().getViewer().getTextWidget();
			String selectedText = textWidget.getSelectionText();
			if(selectedText.length() >= 4 || selectedText.length() <= 16) {
				traceDisplayer.getProjectionViewer().doSearch(selectedText);
				traceDisplayer.highlightInMemoryView(selectedText);
				traceDisplayer.setFocus();
			}
		}	
		return null;
	}

}
