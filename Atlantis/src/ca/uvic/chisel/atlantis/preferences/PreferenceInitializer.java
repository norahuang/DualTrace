package ca.uvic.chisel.atlantis.preferences;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.database.DbConnectionManager;

import static ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference.*;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.ui.PlatformUI;

/**
 * Class used to initialize default trace analysis preference values for the application.
 * @author Laura Chan
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
			initializeNonSecurePreferences();
			initializeSecurePreferences();
	}

	private void initializeSecurePreferences() {
		// Do DB connect defaults. Do all as secure.
		IPreferenceStore secureStore = AtlantisActivator.getDefault().getSecurePreferenceStore();
		
//		// TODO This seems wrong. If the secure store is cleared, does it clears the database and urls too??
		// Needed to comment this out because of the re-design to (temporaily) allow MySQL and SQLite to be toggled between, across runs.
//		secureStore.setDefault(DatabaseConnectionPreferencePage.PREF_DB_DATABASE, .getDEFAULT_DATABASE());
//		secureStore.setDefault(DatabaseConnectionPreferencePage.PREF_DB_URL, .getDEFAULT_URL());
//		secureStore.setDefault(DatabaseConnectionPreferencePage.PREF_DB_PASSWORD, .getDEFAULT_PASSWORD());
//		secureStore.setDefault(DatabaseConnectionPreferencePage.PREF_DB_USERNAME, .getDEFAULT_USERNAME());
	}

	private void initializeNonSecurePreferences() {
		IPreferenceStore store = AtlantisActivator.getDefault().getPreferenceStore();
		store.setDefault(TraceAnalysisPreferencePage.PREF_HEX_HIGHLIGHTING, true);
		
		// Set default colours for syntax highlighting preferences
		Color colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		PreferenceConverter.setDefault(store, SYSTEM_EVENT_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLUE);
		PreferenceConverter.setDefault(store, THREAD_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
		PreferenceConverter.setDefault(store, MODULE_LOAD_UNLOAD_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		PreferenceConverter.setDefault(store, OPERATION_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
		PreferenceConverter.setDefault(store, REGISTER_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN);
		PreferenceConverter.setDefault(store, KEYWORD_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
		PreferenceConverter.setDefault(store, PROBLEM_COLOUR.getName(), colour.getRGB());
		colour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_MAGENTA);
		PreferenceConverter.setDefault(store, NOT_EXECUTED_COLOUR.getName(), colour.getRGB());
	}
}
