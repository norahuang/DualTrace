package ca.uvic.chisel.atlantis.tracedisplayer;

import java.util.*;

import org.eclipse.jface.text.rules.*;

/**
 * Rule-based partition scanner for partitioning traces. This scanner is used on both execution and instrumentation traces.
 * Sections beginning with S: and continuing to the end of a line are classified as system event partitions. Instrumentation begin/end
 * events in instrumentation traces are included in this category.
 * Sections beginning with T: and continuing to the end of a line are classified as thread partitions.
 * Sections beginning with L: or U: and continuing to the end of a line are classified as module load/unload partitions.
 * All other sections are classified as partitions with the default content type.
 * @author Laura Chan
 */
public class TracePartitionScanner extends RuleBasedPartitionScanner {
	public static final String PARTITION_MODULE_LOAD_UNLOAD = "__execution_module_load_unload";
	public static final String PARTITION_SYSTEM_EVENT = "__execution_trace_system_event"; // includes instrumentation events
	public static final String PARTITION_THREAD = "__execution_trace_thread";
	public static final String[] PARTITION_TYPES = {PARTITION_MODULE_LOAD_UNLOAD, PARTITION_SYSTEM_EVENT, PARTITION_THREAD};

	/**
	 * Creates a new scanner and configures its partitioning rules.
	 */
	public TracePartitionScanner() {
		super();
		
		// Define tokens for partition types
		Token moduleLoadUnload = new Token(PARTITION_MODULE_LOAD_UNLOAD);
		Token systemEvent = new Token(PARTITION_SYSTEM_EVENT);
		Token thread = new Token(PARTITION_THREAD);
		
		// Define rules for classifying sections of the trace and partitioning accordingly
		List<IPredicateRule> rules = new ArrayList<IPredicateRule>();
		rules.add(new EndOfLineRule("S:", systemEvent));
		rules.add(new EndOfLineRule("T:", thread));
		rules.add(new EndOfLineRule("L:", moduleLoadUnload));
		rules.add(new EndOfLineRule("U:", moduleLoadUnload));
		setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}
}
