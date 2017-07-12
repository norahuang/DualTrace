package ca.uvic.chisel.bfv.dialogs;

import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog for moving a comment to a different location in the file.
 * @author Laura Chan
 */
public class MoveCommentDialog extends TitleAreaDialog {

	private Text lineField;
	private Text charField;
	
	private Comment comment;
	private int line;
	private int character;
	
	/**
	 * Create a new dialog for moving the specified comment.
	 * @param parentShell parent shell or null to create a top-level shell
	 * @param comment the comment to be moved
	 */
	public MoveCommentDialog(Shell parentShell, Comment comment) {
		super(parentShell);
		this.comment = comment;
		
		// Use 1-indexing since these values will be displayed to the user (just remember to convert back to 0-indexing later!)
		line = comment.getLine(true);
		character = comment.getCharacter(true);
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Move Comment");
		setMessage("Enter a new location in the file for this comment.");
	}
	
	/**
	 * Creates the dialog area, which contains fields for entering the new comment's location.
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		
		// Field for entering a new line number
		Composite upper = new Composite(parent, SWT.NONE);
		upper.setLayout(new GridLayout(2, true));
		Label lineLabel = new Label(upper, SWT.NONE);
		lineLabel.setText("Line ");
		lineField = new Text(upper, SWT.BORDER);
		lineField.setText("" + line);
		GridData lineData = new GridData();
		lineData.widthHint = 30;
		lineField.setLayoutData(lineData);
		
		// Field for entering a new character index
		Composite lower = new Composite(parent, SWT.NONE);
		lower.setLayout(new GridLayout(2, true));
		Label charLabel = new Label(lower, SWT.NONE);
		charLabel.setText("Char ");
		charField = new Text(lower, SWT.BORDER);
		charField.setText("" + character);
		GridData charData = new GridData();
		charData.widthHint = 30;
		charField.setLayoutData(charData);
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridLayout layout = new GridLayout(2, true);
		parent.setLayout(layout);
		
		GridData okGridData = new GridData();
		okGridData.horizontalAlignment = SWT.FILL;
		okGridData.grabExcessHorizontalSpace = true;
		
		Button ok = new Button(parent, SWT.PUSH); // done manually so we can implement a custom selection listener without clashing with the default one
		ok.setLayoutData(okGridData);
		ok.setText(IDialogConstants.OK_LABEL);
		ok.setData(new Integer(IDialogConstants.OK_ID)); // sets the button's ID; needed since we're not using createButton()
		ok.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!isValidNumber(lineField.getText())) {
					setMessage("Please enter a valid line number (must be at least 1)", IMessageProvider.ERROR);
				} else if (!isValidNumber(charField.getText())) {
					setMessage("Please enter a valid char number (must be at least 1)", IMessageProvider.ERROR);
				} else {
					line = Integer.parseInt(lineField.getText());
					character = Integer.parseInt(charField.getText());
					if (!isValidCommentLocation()) {
						setMessage("The new location is the same as the old one or the comment's group already has another comment at that location.",
								IMessageProvider.ERROR);
					} else if (!locationExistsInFile()) {
						setMessage("Line " + line + " char " + character + " does not exist in the file.", IMessageProvider.ERROR);
					} else {
						okPressed();
					}
				}
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
	 * Checks whether the value that the user entered is a valid number. The value must be an integer greater than or equal to one.
	 * @param s value to check 
	 * @return true if the value is a valid number, false otherwise. 
	 */
	private boolean isValidNumber(String s) {
		try {
			int number = Integer.parseInt(s);
			return number >= 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Validates the location that the user entered by ensuring that there isn't already a comment from the same group at that location.
	 * @return true if there is no comment at that location, false otherwise
	 */
	private boolean isValidCommentLocation() {
		return comment.getCommentGroup().getCommentAt(line - 1, character - 1) == null; // convert back to 0-indexing!
	}
	
	/**
	 * Checks that the location that user entered is a valid location in the file.
	 * @return true if the location exists in the file, false otherwise
	 */
	private boolean locationExistsInFile() {
		BigFileEditor editor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		return editor.getProjectionViewer().isValidLocation(line - 1, character - 1); // convert back to 0-indexing!
	}

	/**
	 * Returns the line number that the user entered, converting it to 0-indexed form.
	 * @return the line number in 0-indexed form
	 */
	public int getLine() {
		return line - 1; // convert back to 0-indexing!
	}
	
	/**
	 * Returns the char number that the user entered, converting it to 0-indexed form.
	 * @return the char number in 0-indexed form
	 */
	public int getCharacter() {
		return character - 1; // convert back to 0-indexing!
	}
}
