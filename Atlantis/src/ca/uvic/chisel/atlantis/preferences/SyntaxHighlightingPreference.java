package ca.uvic.chisel.atlantis.preferences;

import ca.uvic.chisel.atlantis.AtlantisColourConstants;

import java.util.*;

/**
 * Enum containing all of the different syntax highlighting preferences. Each preference contains its name (a String) and
 * the ID of its corresponding colour for easy retrieval from the plugin's colour registry.
 * @author Laura Chan
 */
public enum SyntaxHighlightingPreference {
	SYSTEM_EVENT_COLOUR("systemEventColourPreference", AtlantisColourConstants.SYNTAX_SYSTEM_EVENT),
	THREAD_COLOUR("threadColourPreference", AtlantisColourConstants.SYNTAX_THREAD),
	MODULE_LOAD_UNLOAD_COLOUR("moduleLoadUnloadColourPreference", AtlantisColourConstants.SYNTAX_MODULE_LOAD_UNLOAD),
	OPERATION_COLOUR("operationColourPreference", AtlantisColourConstants.SYNTAX_OPERATION),
	REGISTER_COLOUR("registerColourPreference", AtlantisColourConstants.SYNTAX_REGISTER),
	KEYWORD_COLOUR("keywordColourPreference", AtlantisColourConstants.SYNTAX_KEYWORD),
	PROBLEM_COLOUR("problemColourPreference", AtlantisColourConstants.SYNTAX_PROBLEM),
	NOT_EXECUTED_COLOUR("notExecutedColourPreference", AtlantisColourConstants.SYNTAX_NOT_EXECUTED);
	
	// Map for easily (and statically) retrieving preferences by their names
	private static Map<String, SyntaxHighlightingPreference> prefsByName;
	static {
		prefsByName = new HashMap<String, SyntaxHighlightingPreference>();
		SyntaxHighlightingPreference[] values = SyntaxHighlightingPreference.values();
		for (int i = 0; i < values.length; i++) {
			prefsByName.put(values[i].getName(), values[i]);
		}
	}

	private String name;
	private String colourID; // ID of associated colour in the colour registry
	
	/**
	 * Creates a new syntax highlighting preference with the given name and associated colour.
	 * @param name preference name
	 * @param colourID ID of associated colour
	 */
	private SyntaxHighlightingPreference(String name, String colourID) {
		this.name = name;
		this.colourID = colourID;
	}
	
	/**
	 * Returns the name of this preference. 
	 * @return the name of this preference
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the ID of the colour corresponding to this preference.
	 * @return the ID of the corresponding colour
	 */
	public String getColourID() {
		return colourID;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	/**
	 * Retrieves the SyntaxHighlightingPreference with the given name. Use this to convert between the preference name strings
	 * issued by PropertyChangeEvents and instances of this enum.
	 * @param name name of the preference to retrieve
	 * @return the preference with that name, or null of the name did not correspond to a valid syntax highlighting preference
	 */
	public static SyntaxHighlightingPreference getByName(String name) {
		return prefsByName.get(name);
	}
}
