package ca.uvic.chisel.bfv.handlers;

import javax.xml.bind.JAXBException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.dialogs.AddOrEditCommentDialog;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.views.CommentsView;

/**
 * Handler for adding a new comment to the file. Invoked when the Add Commment command is executed. 
 * @author Laura Chan
 */
public class AddCommentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor instanceof BigFileEditor) {
			BigFileEditor activeEditor = (BigFileEditor) editor;
			ITextSelection selection = (ITextSelection) activeEditor.getSelectionProvider().getSelection();
			if (!selection.isEmpty()) {
				// Open the dialog for adding a new comment
				AddOrEditCommentDialog addCommentDialog = new AddOrEditCommentDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell(), null);
				addCommentDialog.create();
				
				if (addCommentDialog.open() == Window.OK) {
					try {
						// Add the comment and update the comments view
						IDocument document = activeEditor.getProjectionViewer().getViewer().getDocument();
						int offset = selection.getOffset();
						int line = document.getLineOfOffset(offset);
						int character = offset - document.getLineOffset(line);
						activeEditor.getProjectionViewer().addComment(addCommentDialog.getCommentGroup(), line, character, addCommentDialog.getCommentText());
						CommentsView commentsView = (CommentsView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(CommentsView.ID);
						if (commentsView != null) {
							commentsView.updateView();
						}
					} catch (CoreException e) {
						BigFileApplication.showErrorDialog("Error adding comment", "Problem creating or refreshing file's comments file", e);
					} catch (JAXBException e) {
						BigFileApplication.showErrorDialog("Error adding comment", "Could not add new comment to file's comments file", e);
					} catch (InvalidCommentLocationException e) {
						BigFileApplication.showInformationDialog("Unable to add comment", "The specified comment group already has a comment at that location", e);
					} catch (BadLocationException e) {
						BigFileApplication.showErrorDialog("Error adding comment", "Error determining comment's location", e);
					}
				}
			}
		}
		return null;
	}
}
