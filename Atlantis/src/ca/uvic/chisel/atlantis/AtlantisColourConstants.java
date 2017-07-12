package ca.uvic.chisel.atlantis;

import java.util.*;

/**
 * Constants class for the IDs of the various colours in the Activator's colour registry.
 * @author Laura Chan
 */
public class AtlantisColourConstants {
	// Syntax highlighting colours
	public static final String SYNTAX_SYSTEM_EVENT = "ca.uvic.chisel.atlantis.colours.syntaxSystemEvent";
	public static final String SYNTAX_THREAD = "ca.uvic.chisel.atlantis.colours.syntaxThread";
	public static final String SYNTAX_MODULE_LOAD_UNLOAD = "ca.uvic.chisel.atlantis.colours.syntaxModuleLoadUnload";
	public static final String SYNTAX_OPERATION = "ca.uvic.chisel.atlantis.colours.syntaxOperation";
	public static final String SYNTAX_REGISTER = "ca.uvic.chisel.atlantis.colours.syntaxRegister";
	public static final String SYNTAX_KEYWORD = "ca.uvic.chisel.atlantis.colours.syntaxKeyword";
	public static final String SYNTAX_PROBLEM = "ca.uvic.chisel.atlantis.colours.syntaxProblem";
	public static final String SYNTAX_NOT_EXECUTED = "ca.uvic.chisel.atlantis.colours.syntaxNotExecuted";
	
	// Trace Visualization thread colours
	public static final String THREAD_ROW = "ca.uvic.chisel.atlantis.colours.threadRow";
	public static final String THREAD_BEGIN_DEFAULT = "ca.uvic.chisel.atlantis.colours.threadBeginDefault";
	public static final String THREAD_BEGIN_SELECTED = "ca.uvic.chisel.atlantis.colours.threadBeginSelected";
	public static final String THREAD_END_DEFAULT = "ca.uvic.chisel.atlantis.colours.threadEndDefault";
	public static final String THREAD_END_SELECTED = "ca.uvic.chisel.atlantis.colours.threadEndSelected";
	public static final String THREAD_SWITCH_DEFAULT = "ca.uvic.chisel.atlantis.colours.threadSwitchDefault";
	public static final String THREAD_SWITCH_SELECTED = "ca.uvic.chisel.atlantis.colours.threadSwitchSelected";
	
	// Trace Visualization Assembly Colors
	public static final String ASSEMBLY_ROW = "ca.uvic.chisel.atlantis.colours.assemblyRow";
	public static final String ASSEMBLY_BEGIN_DEFAULT = "ca.uvic.chisel.atlantis.colours.assemblyBeginDefault";
	public static final String ASSEMBLY_BEGIN_SELECTED = "ca.uvic.chisel.atlantis.colours.assemblyBeginSelected";
	public static final String ASSEMBLY_END_DEFAULT = "ca.uvic.chisel.atlantis.colours.assemblyEndDefault";
	public static final String ASSEMBLY_END_SELECTED = "ca.uvic.chisel.atlantis.colours.assemblyEndSelected";
	public static final String ASSEMBLY_SWITCH_DEFAULT = "ca.uvic.chisel.atlantis.colours.assemblySwitchDefault";
	public static final String ASSEMBLY_SWITCH_SELECTED = "ca.uvic.chisel.atlantis.colours.assemblySwitchSelected";
	
	/**
	 * Returns a list of IDs for all thread-related colours used in the trace visualization. Currently this is here for unit-testing purposes, but
	 * if it is needed elsewhere in the future feel free to make this a public method.
	 * @return list of IDs of thread-related trace visualization colours
	 */
	static Collection<String> getTraceVisualizationThreadColours() {
		Collection<String> threadColourList = new ArrayList<String>();
		threadColourList.add(THREAD_ROW);
		threadColourList.add(THREAD_BEGIN_DEFAULT);
		threadColourList.add(THREAD_BEGIN_SELECTED);
		threadColourList.add(THREAD_END_DEFAULT);
		threadColourList.add(THREAD_END_SELECTED);
		threadColourList.add(THREAD_SWITCH_DEFAULT);
		threadColourList.add(THREAD_SWITCH_SELECTED);
		return threadColourList;
	}
}
