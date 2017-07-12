package ca.uvic.chisel.atlantis.views;

import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

import ca.uvic.chisel.bfv.editor.BigFileEditor;

/**
 * Dialog for renaming a comment group.
 * @author Laura Chan
 */
public class RenameMessageTypeDialog extends TitleAreaDialog {

	private Text nameField;
	private String name;
	private String oldName;
	
	/**
	 * Creates a new dialog for renaming a comment group.
	 * @param parentShell parent shell, or null to create a top-level shell
	 * @param oldName comment group's old name, which will initially be displayed in the dialog's name field (must not be null)
	 */
	public RenameMessageTypeDialog(Shell parentShell, String oldName) {
		super(parentShell);
		this.oldName = oldName;
	}
	
	@Override
	public void create() {
		super.create();
		this.setTitle("Rename Message Type");
		this.setMessage("Enter a new name for this Message Type.");
	}
	
	/**
	 * Creates the dialog area, which contains a field for entering a new comment group name.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		parent.setLayout(layout);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		nameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		nameField.setLayoutData(gridData);
		nameField.setText(oldName);
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
				if (isValidName()) {
					name = nameField.getText().trim();
					okPressed();
				} else {
					if (nameFieldEmpty()) {
						setMessage("Name cannot be empty.", IMessageProvider.ERROR);
					} else {
						setMessage("There is already an existing message type with that name. Please choose another name.", IMessageProvider.ERROR);
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
	 * Checks whether the name field is empty.
	 * @return true if the name field is empty, false otherwise
	 */
	private boolean nameFieldEmpty() {
		return nameField.getText() == null || "".equals(nameField.getText().trim());
	}
	
	/**
	 * Checks whether the name that the user entered is valid. The name must be non-null, non-empty, and unique.
	 * @return true if the name is valid, false if not
	 */
	private boolean isValidName() {
		if (nameFieldEmpty()) {
			return false;
		}
		
		BigFileEditor editor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		return editor.getProjectionViewer().getFileModel().isUniqueMessageTypeName(nameField.getText().trim());
	}
	
	/**
	 * Returns the comment group name that the user entered.
	 * @return the name that the user entered
	 */
	public String getName() {
		return name;
	}
}
