package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.atlantis.views.FunctionsView;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

public class ShowFunctionParentHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		try {

			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			FunctionsView funcView;
				funcView = (FunctionsView) 
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FunctionsView.ID);
			if (editor instanceof BigFileEditor && funcView != null) {
				BigFileEditor activeEditor = (BigFileEditor) editor;
				ITextSelection selection = (ITextSelection) activeEditor.getSelectionProvider().getSelection();
						
				IDocument document = activeEditor.getProjectionViewer().getViewer().getDocument();
				int currentLine = selection.getStartLine();
				
				funcView.selectParentFunctionOfCurrentEditorLineSelection(currentLine);
				
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		
		return null;
	}
}
