package ca.uvic.chisel.bfv.dialogs;

import ca.uvic.chisel.bfv.annotations.*;
import ca.uvic.chisel.bfv.editor.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog for adding a new comment to the file or editing an existing one.
 * @author Laura Chan
 */
public class AddOrEditCommentDialog extends TitleAreaDialog {

	private ComboViewer comboViewer;
	private Text commentTextField;
	
	private String commentGroup;
	private String commentText;
	private Comment commentToEdit;
	
	/**
	 * Constructs a new dialog for adding or editing a comment.
	 * @param parentShell parent shell widget for this dialog
	 * @param commentToEdit comment to edit if this should be an edit dialog, or null if a new comment is to be added
	 */
	public AddOrEditCommentDialog(Shell parentShell, Comment commentToEdit) {
		super(parentShell);
		this.commentToEdit = commentToEdit;
	}
	
	@Override
	public void create() {
		super.create();
		if (commentToEdit != null) {
			setTitle("Edit a comment");
			setMessage("Edit this comment's text or change its group.");
		} else {
			setTitle("Add a comment");
			setMessage("Add a comment to this file.");
		}
	}

	/**
	 * Creates the dialog area, which contains a combobox for specifying the comment's group and a text field for entering/editing the comment's text
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		
		Label comboLabel = new Label(parent, SWT.NONE);
		comboLabel.setText("Enter or select a comment group (optional):");
		
		GridData comboGridData = new GridData();
		comboGridData.horizontalAlignment = SWT.FILL;
		comboGridData.grabExcessHorizontalSpace = true;
		comboViewer = new ComboViewer(parent, SWT.BORDER);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new LabelProvider());
		comboViewer.getCombo().setLayoutData(comboGridData);
		
		Label textLabel = new Label(parent, SWT.NONE);
		textLabel.setText("Enter the comment's text:");
		commentTextField = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		commentTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		BigFileEditor fileEditor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		comboViewer.setInput(fileEditor.getProjectionViewer().getFileModel().getCommentGroups());
		
		// If we are editing an existing comment, have the group and text inputs contain its current values for those properties
		if (commentToEdit != null) {
			comboViewer.setSelection(new StructuredSelection(commentToEdit.getCommentGroup()), true);
			commentTextField.setText(commentToEdit.getText());
		}
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridLayout layout = new GridLayout(2, true);
		parent.setLayout(layout);
		
		GridData okGridData = new GridData();
		okGridData.horizontalAlignment = SWT.FILL;
		okGridData.grabExcessHorizontalSpace = true;
		
		final Button ok = new Button(parent, SWT.PUSH); // done manually so we can implement a custom selection listener without clashing with the default one
		ok.setLayoutData(okGridData);
		ok.setText(IDialogConstants.OK_LABEL);
		ok.setData(new Integer(IDialogConstants.OK_ID)); // sets the button's ID; needed since we're not using createButton()
		ok.setEnabled(false);
		commentTextField.addListener(SWT.CHANGED, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if(commentTextField.getText().trim().length() > 0){ // If it has no visible characters, we won't let them set it
					ok.setEnabled(true);
				} else {
					ok.setEnabled(false);
				}
			}
		});
		ok.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				commentGroup = comboViewer.getCombo().getText();
				commentText = commentTextField.getText();
				okPressed();
			}
		});
		// Set the OK button to be the default button
		Shell shell = parent.getShell();
		if (shell != null) {
			shell.setDefaultButton(ok);
		}
		
		Button cancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		GridData cancelGridData = new GridData();
		cancelGridData.horizontalAlignment = SWT.FILL;
		cancelGridData.grabExcessHorizontalSpace = true;
		cancel.setLayoutData(cancelGridData);
	}
	
	/**
	 * Returns the comment group that the user entered
	 * @return the comment group that the user entered
	 */
	public String getCommentGroup() {
		return commentGroup;
	}
	
	/**
	 * Returns the comment text that the user entered
	 * @return the comment text that the user entered
	 */
	public String getCommentText() {
		return commentText;
	}
}
