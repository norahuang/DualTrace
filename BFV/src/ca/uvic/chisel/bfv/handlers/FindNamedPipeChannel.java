package ca.uvic.chisel.bfv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.impl.PartSashContainerImpl;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.editor.BigFileEditor;


public class FindNamedPipeChannel extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		MPart container = (MPart) editorPart.getSite().getService(MPart.class);
		MElementContainer m = container.getParent();
		if(!(m instanceof PartSashContainerImpl)){
			Throwable throwable = new Throwable("This is not a dual-trace");
			BigFileApplication.showErrorDialog("This is not a dual-trace!","Open a dual-trace First",throwable);
			return null;
		}
		
		if (editorPart != null && editorPart instanceof BigFileEditor) {
			BigFileEditor bigFileEditor = (BigFileEditor) editorPart;
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
