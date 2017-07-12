package ca.uvic.chisel.bfv.dialogs;

import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Dialog for naming a new region or renaming an existing one.
 * @author Laura Chan
 */
public class NameRegionDialog extends TitleAreaDialog {

	private Text nameField, startLineField, endLineField;
	private Label nameLabel, startLineLabel, endLineLabel;
	private String name;
	private String oldName;
	private int startLine, endLine;
	
	/**
	 * Constructs a new dialog for naming a new region or renaming an existing one.
	 * @param parentShell parent shell of this dialog
	 * @param oldName region's old name if this should be a rename dialog, or null if the dialog is for naming a new region
	 */
	public NameRegionDialog(Shell parentShell, String oldName, int startLine, int endLine) {
		super(parentShell);
		this.oldName = oldName;
		this.startLine = startLine;
		this.endLine = endLine;
	}
	
	@Override
	public void create() {
		super.create();
		if (oldName != null) {
			this.setTitle("Rename region");
			this.setMessage("Enter a new name for this region.");
		} else {
			this.setTitle("Create a new region");
			this.setMessage("Enter a name for this new region.");
		}
	}
	
	/**
	 * Creates the dialog area, which contains a field for entering the region name.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		parent.setLayout(layout);
		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;		
		nameLabel = new Label(parent, SWT.SINGLE);
		nameLabel.setText("Region Name");
		
		nameField = new Text(parent, SWT.SINGLE | SWT.BORDER);		
		nameField.setLayoutData(gridData);
		if (oldName != null) {
			nameField.setText(oldName);
		}
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		startLineLabel = new Label(parent, SWT.SINGLE);
		startLineLabel.setText("Start Line");
		
		startLineField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		startLineField.setLayoutData(gridData);		
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;		
		endLineLabel = new Label(parent, SWT.SINGLE);
		endLineLabel.setText("End Line");
		
		endLineField = new Text(parent, SWT.SINGLE | SWT.BORDER);		
		endLineField.setLayoutData(gridData);
		
		startLineField.setText(Integer.toString(startLine + 1));
		endLineField.setText(Integer.toString(endLine + 1));
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
					startLine = Integer.parseInt(startLineField.getText().trim()) - 1;
					endLine = Integer.parseInt(endLineField.getText().trim()) - 1;
					okPressed();
				} else {
					setMessage("Name cannot be empty.", IMessageProvider.ERROR);
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
	 * Determines if the name entered in the name field is valid. Region names must be non-null and non-empty.
	 * @return true if the name is valid, false if it is empty or null.
	 */
	private boolean isValidName() {
		return nameField.getText() != null && !"".equals(nameField.getText().trim());
	}
	
	/**
	 * Gets the region name that the user entered in this dialog.
	 * @return the name that the user entered
	 */
	public String getName() {
		return name;
	}
	
	public int getStartLine() { 
		return startLine;
	}
	
	public int getEndLine() { 
		return endLine;		
	}
}
