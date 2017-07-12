package ca.uvic.chisel.atlantis.preferences;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisActivator;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;

/**
 * Preference page for general trace analysis settings.
 * @author Eric Verbeek
 */
public class DatabaseConnectionPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PREF_DB_PASSWORD = "PasswordPreference";
	public static final String PREF_DB_USERNAME = "UsernamePreference";
	public static final String PREF_DB_URL = "URLPreference";
	public static final String PREF_DB_DATABASE = "DatabaseNamePreference";
	
	/**
	 * Creates a new Trace Analysis preference page.
	 */
	public DatabaseConnectionPreferencePage() {
		super(GRID);
		
		// TODO This seems wrong. If the secure store is cleared, it clears the database and urls too??
		SecurePreferenceStore secureStore = AtlantisActivator.getDefault().getSecurePreferenceStore();
		secureStore.setDoEncryptPreference(DatabaseConnectionPreferencePage.PREF_DB_PASSWORD);
		setPreferenceStore(secureStore);
		
		this.setDescription("Database connection details for use with trace database. ");
	}
	
	/**
	 * Creates the field editors for editing the preferences on this page.
	 */
	@Override
	public void createFieldEditors() {
		this.addField(new StringFieldEditor(PREF_DB_URL, "MySQL Database URL", getFieldEditorParent()));
		this.addField(new StringFieldEditor(PREF_DB_DATABASE, "MySQL Database Name", getFieldEditorParent()));

		this.addField(new StringFieldEditor(PREF_DB_USERNAME, "Username", getFieldEditorParent()));

		StringFieldEditor passFieldEditor = new StringFieldEditor(PREF_DB_PASSWORD, "Password", getFieldEditorParent());
		passFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		this.addField(passFieldEditor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {}
}