package ca.uvic.chisel.bfv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.bfv.editor.BigFileEditor;


public class FindUDPChannel extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor != null && editor instanceof BigFileEditor) {
			BigFileEditor bigFileEditor = (BigFileEditor) editor;
			StyledText textWidget = bigFileEditor.getProjectionViewer().getViewer().getTextWidget();
			String selectedText = textWidget.getSelectionText();
			if(selectedText.length() >= 4 || selectedText.length() <= 16) {
				bigFileEditor.getProjectionViewer().doSearch(selectedText);
				bigFileEditor.setFocus();
			}
		}	
		return null;
	}

}
