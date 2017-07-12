package ca.uvic.chisel.atlantis.preferences;

import ca.uvic.chisel.atlantis.AtlantisActivator;

import static ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference.*;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;

/**
 * Preference page for selecting the colours to be used for syntax highlighting in the Trace Displayer
 * @author Laura Chan
 */
public class SyntaxHighlightingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Creates a new syntax highlighting preference page.
	 */
	public SyntaxHighlightingPreferencePage() {
		super(GRID);
		setPreferenceStore(AtlantisActivator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {}

	/**
	 * Creates the field editors for selecting the colours.
	 */
	@Override
	protected void createFieldEditors() {
		this.addField(createColorFieldEditor(SYSTEM_EVENT_COLOUR.getName(), "System events"));
		this.addField(createColorFieldEditor(THREAD_COLOUR.getName(), "Threads"));
		this.addField(createColorFieldEditor(MODULE_LOAD_UNLOAD_COLOUR.getName(), "Module load/unload"));
		this.addField(createColorFieldEditor(OPERATION_COLOUR.getName(), "Assembly operations"));
		this.addField(createColorFieldEditor(REGISTER_COLOUR.getName(), "Registers"));
		this.addField(createColorFieldEditor(KEYWORD_COLOUR.getName(), "Keywords"));
		this.addField(createColorFieldEditor(PROBLEM_COLOUR.getName(), "Problems"));
		this.addField(createColorFieldEditor(NOT_EXECUTED_COLOUR.getName(), "Not Executed marker"));
	}
	
	/**
	 * Helper method for creating ColorFieldEditors for this preference page's colour preferences.
	 * @param preferenceName name of the colour preference for which to create the ColorFieldEditor
	 * @param labelText label text that the ColorFieldEditor should use
	 * @return a ColorFieldEditor for the specified preference with the given label text
	 */
	private ColorFieldEditor createColorFieldEditor(String preferenceName, String labelText) {
		ColorFieldEditor colorFieldEditor = new ColorFieldEditor(preferenceName, labelText, getFieldEditorParent());
		colorFieldEditor.setPreferenceStore(this.getPreferenceStore());
		return colorFieldEditor;
	}
}
