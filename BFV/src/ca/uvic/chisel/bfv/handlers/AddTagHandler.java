package ca.uvic.chisel.bfv.handlers;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.dialogs.AddTagDialog;
import ca.uvic.chisel.bfv.dialogs.AddTagDialog.TagRange;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.views.TagsView;

import javax.xml.bind.JAXBException;

import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for adding a new tag to the file. Invoked when the Add Tag command is executed.
 * @author Laura Chan
 */
public class AddTagHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor instanceof BigFileEditor) {
			BigFileEditor activeEditor = (BigFileEditor) editor;
			ITextSelection selection = (ITextSelection) activeEditor.getSelectionProvider().getSelection();
			if (!selection.isEmpty()) {
				// Open the dialog for adding a new tag
				AddTagDialog addTagDialog = new AddTagDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell(), selection);
				addTagDialog.create();				
				try {
					if (addTagDialog.open() == Window.OK) {
						IDocument document = activeEditor.getProjectionViewer().getViewer().getDocument();
						TagRange tagRange = addTagDialog.getTagRange();
						int startLine;
						int startChar;
						int endLine;
						int endChar;
						
						// Calculate how much of the file the tag should be applied to, based on the selected text and the 
						// tag range that the user selected in the dialog
						if (tagRange == TagRange.CURRENT_CHAR) {
							int offset = selection.getOffset();
							startLine = document.getLineOfOffset(offset);
							endLine = startLine;
							startChar = offset - document.getLineOffset(startLine);
							endChar = startChar;
						} else if (tagRange == TagRange.CURRENT_LINE) {
							startLine = selection.getStartLine();
							endLine = startLine;
							int lineLength = document.getLineLength(startLine);
							startChar = 0;
							if (lineLength > 0) {
								endChar = lineLength - 1;
							} else { // Can happen if the user wants to tag an empty line--for instance, one at the end of a file
								endChar = 0;
							}
						} else if (tagRange == TagRange.SELECTED_TEXT) {
							int offset = selection.getOffset();
							startLine = selection.getStartLine();
							startChar = offset - document.getLineOffset(startLine);
							endLine = selection.getEndLine();
							endChar = offset + selection.getLength() - 1 - document.getLineOffset(endLine);
						} else { // must be TagRange.SELECTED_LINES
							startLine = selection.getStartLine();
							startChar = 0;
							endLine = selection.getEndLine();
							endChar = document.getLineLength(endLine) - 1;
						}
						
						// Add the tag and update the tags view
						activeEditor.getProjectionViewer().addTag(addTagDialog.getSelectedTag(), startLine, startChar, endLine, endChar);
						TagsView tagsView = (TagsView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(TagsView.ID);
						if (tagsView != null) {
							tagsView.updateView();
						}
					}
				} catch (BadLocationException e) {
					BigFileApplication.showErrorDialog("Error adding tag", "Error determining tag's start or end location", e);
				} catch (CoreException e) {
					BigFileApplication.showErrorDialog("Error adding tag", "Problem creating or refreshing file's tags file", e);
				} catch (JAXBException e) {
					BigFileApplication.showErrorDialog("Error adding tag", "Could not add new tag to file's tags file", e);
				} catch (DuplicateTagOccurrenceException e) {
					BigFileApplication.showInformationDialog("Unable to add tag", "Add operation cancelled because it would create a duplicate tag", e);
				}
			}
		}
		return null;
	}

}
