package ca.uvic.chisel.atlantis.preferences;

import ca.uvic.chisel.atlantis.AtlantisActivator;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;

/**
 * Preference page for general trace analysis settings.
 * @author Laura Chan
 */
public class TraceAnalysisPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PREF_HEX_HIGHLIGHTING = "hexHighlightingPreference";

	/**
	 * Creates a new Trace Analysis preference page.
	 */
	public TraceAnalysisPreferencePage() {
		super(GRID);
		setPreferenceStore(AtlantisActivator.getDefault().getPreferenceStore());
		this.setDescription("General editor options can be found in General > Editors > Text Editors."); // TODO make this a link to that preference page
	}
	
	/**
	 * Creates the field editors for editing the preferences on this page.
	 */
	@Override
	public void createFieldEditors() {
		// Don't want text highlighting anymore according to Patrick, so I'm removing the field but leaving the class.
		// this.addField(new BooleanFieldEditor(PREF_HEX_HIGHLIGHTING, "Highlight occurrences of selected hex value", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {}
}