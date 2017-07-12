package ca.uvic.chisel.bfv.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.editor.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog for adding a tag to the file.
 * @author Laura Chan
 */
public class AddTagDialog extends TitleAreaDialog {
	
	private class TagComboLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			Tag tag = (Tag) element;
			return tag.getName();
		}
	}
	
	/**
	 * Values for describing the tag range, that is, the portion of the file to which the tag will be applied.
	 * @author Laura Chan
	 */
	public static enum TagRange {
		CURRENT_CHAR, CURRENT_LINE, SELECTED_TEXT, SELECTED_LINES;
	}
	
	private ITextSelection selection;
	
	private ComboViewer comboViewer;
	private Button currentCharRadio;
	private Button currentLineRadio;
	private Button selectedTextRadio;
	private Button selectedLinesRadio;
	private boolean edit;
	private String selectedTag;
	private TagRange tagRange;

	private Tag tagToEdit; 
	
	/**
	 * Creates a new dialog for adding a tag.
	 * @param parentShell parent shell, or null to create a top-level shell
	 * @param selection text selection to which the tag will be applied. Depending on the selected tag range, the tag may also be applied to surrounding text.
	 */
	public AddTagDialog(Shell parentShell, ITextSelection selection) {
		super(parentShell);
		this.selection = selection;
	}
	
	@Override
	public void create() {
		super.create();
		this.setTitle("Add a tag");
		this.setMessage("Select an existing tag or enter a new one.");
	}
	
	public void create(Tag tagToEdit) {
		this.tagToEdit = tagToEdit;
		this.edit = true;
		create();
		this.setTitle("Edit a tag");
	}

	/**
	 * Creates the dialog area, which contains a combobox for choosing a tag or specifying a new one, and a radio group for selecting
	 * the range of text that will have the tag applied
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		parent.setLayout(layout);
		
		GridData comboGridData = new GridData();
		comboGridData.horizontalAlignment = SWT.FILL;
		comboGridData.grabExcessHorizontalSpace = true;
		comboViewer = new ComboViewer(parent, SWT.BORDER);
		comboViewer.getCombo().setLayoutData(comboGridData);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new TagComboLabelProvider());
		comboViewer.getCombo().setFocus();
				
		if(!edit) {
			// Radio group for selecting the tag range
			Label label = new Label(parent, SWT.NONE);
			label.setText("Tag range: apply tag to...");
			
			currentCharRadio = new Button(parent, SWT.RADIO);
			currentCharRadio.setText("Current character");
			currentCharRadio.setToolTipText("Tags the highlighted character or the character after the cursor if none are highlighted.");
			
			currentLineRadio = new Button(parent, SWT.RADIO);
			currentLineRadio.setText("Current line");
			currentLineRadio.setToolTipText("Tags the entire line in which the cursor or highlighted text is located.");
			
			selectedTextRadio = new Button(parent, SWT.RADIO);
			selectedTextRadio.setText("Selected text");
			selectedTextRadio.setToolTipText("Tags only the highlighted text.");
			
			selectedLinesRadio = new Button(parent, SWT.RADIO);
			selectedLinesRadio.setText("Selected lines");
			selectedLinesRadio.setToolTipText("Tags the entirety of every line covered by the highlighted region.");
			
			// To avoid confusion, enable/disable tag range options based on how many lines/characters are highlighted
			currentCharRadio.setEnabled(selection.getLength() == 0 || selection.getLength() == 1); // cannot have more than one character highlighted
			currentLineRadio.setEnabled(selection.getStartLine() == selection.getEndLine()); // cannot have more than one line highlighted
			selectedTextRadio.setEnabled(selection.getLength() > 0); // must have at least one character highlighted
			selectedLinesRadio.setEnabled(selection.getLength() > 0); // must have at least one character highlighted	
			
			// Set default, in this preferential order.
			if(currentLineRadio.isEnabled()){
				currentLineRadio.setSelection(true);
			} else if(selectedLinesRadio.isEnabled()) {
				selectedLinesRadio.setSelection(true);
			} else if(selectedTextRadio.isEnabled()){
				selectedTextRadio.setSelection(true);
			} else if(currentCharRadio.isEnabled()){
				currentCharRadio.setSelection(true);
			}
		}
		
		BigFileEditor editor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		Collection<Tag> tags = editor.getProjectionViewer().getFileModel().getTags();
		Tag[] arrayOfTags = new Tag[]{};
		arrayOfTags = tags.toArray(arrayOfTags);
		comboViewer.setInput(arrayOfTags);
		comboViewer.getCombo().select(Arrays.asList(arrayOfTags).indexOf(tagToEdit));
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
				if (tagSelectionEmpty()) {
					setMessage("Selected tag cannot be empty.", IMessageProvider.ERROR);
				} else if (!edit && !isValidTagRange()) {
					setMessage("Please specify where the tag should be applied.", IMessageProvider.ERROR); 
				} else if (!edit) {
					selectedTag = comboViewer.getCombo().getText();
					if (currentCharRadio.getSelection()) {
						tagRange = TagRange.CURRENT_CHAR;
					} else if (currentLineRadio.getSelection()) {
						tagRange = TagRange.CURRENT_LINE;
					} else if (selectedTextRadio.getSelection()){ 
						tagRange = TagRange.SELECTED_TEXT;
					} else { // selected lines radio must have been selected
						tagRange = TagRange.SELECTED_LINES;
					}
					okPressed();
				} else {
					selectedTag = comboViewer.getCombo().getText();
					okPressed();
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
	 * Checks whether the user specified a valid tag range by ensuring that one of the radio buttons was selected.
	 * @return true if a tag range was specified, false otherwise
	 */
	private boolean isValidTagRange() {
		return currentCharRadio.getSelection() || currentLineRadio.getSelection() 
				|| selectedTextRadio.getSelection() || selectedLinesRadio.getSelection();
	}
	
	/**
	 * Checks whether the selected tag is empty.
	 * @return true if the combobox's value is null or empty, false otherwise
	 */
	private boolean tagSelectionEmpty() {
		return comboViewer.getCombo().getText() == null || "".equals(comboViewer.getCombo().getText().trim());
	}
	
	/**
	 * Gets the name of the tag that the user selected.
	 * @return name of the selected tag
	 */
	public String getSelectedTag() {
		return selectedTag;
	}
	
	/**
	 * Get the tag range that the user selected.
	 * @return tag range that the user selected.
	 */
	public TagRange getTagRange() {
		return tagRange;
	}
}
