package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;
import ca.uvic.chisel.bfv.*;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;

/**
 * Rule-based scanner for classifying and applying syntax highlighting to tokens in a system event partition of a trace 
 * being displayed in a TraceDisplayer.
 * 
 * This scanner does some work that doesn't apply to the new data format, provided for September 2013. This format
 * has the syscalls in a separate syscall file, which we do not need for the thread, assembly, or memory views.
 * We can leave the syscall highlighting here, but it isn't needed for the new format.
 * 
 * @author Laura Chan
 */
public class SystemEventScanner extends TraceDisplayerScanner {
	
	// Keywords and variables from system events in execution traces
	private static final String[] SYSTEM_EVENT_KEYWORDS_OLD_FORMAT = {"ThreadBegin", "ThreadEnd", "SyscallEntry", "SyscallExit", "SyscallAbandon", 
			"ContextChange", "ApplicationEnd"};
	private static final String[] SYSTEM_EVENT_KEYWORDS_NEW_FORMAT = {};
	private static final String[] SYSTEM_EVENT_KEYWORDS = ArrayUtils.addAll(SYSTEM_EVENT_KEYWORDS_OLD_FORMAT, SYSTEM_EVENT_KEYWORDS_NEW_FORMAT);
	
	// Old: 01 S:ThreadBegin TID=2184
	// New: 0 01 {64 OFF} ThreadBegin | TIB=000007fffffde000 WindowsTID=00001b0c | ...
	// TIB is thread information block. TID in new format is called WindowsTID, and the trace thread id is a hex number that is in
	// the second column of each line (including the ThreadBegin line).
	private static final String[] SYSTEM_EVENT_VARIABLES_OLD_FORMAT = {"TID", "Code"};
	// XXX This tokenizer cannot make use of regex that needs context, like the new format requires.
	private static final String THREAD_ID_REGEX = "^[0-9a-fA-F]+\\s([0-9a-fA-F]+)\\s\\{"; // hex line number followed by hex thread number
	private static final String[] SYSTEM_EVENT_VARIABLES_NEW_FORMAT = {"TIB", "WindowsTID"}; // Might still use old format elements
	private static final String[] SYSTEM_EVENT_VARIABLES = ArrayUtils.addAll(SYSTEM_EVENT_VARIABLES_OLD_FORMAT, SYSTEM_EVENT_VARIABLES_NEW_FORMAT); 

	private static final String[] CONTEXT_CHANGE_TYPES = {"CallBack", "Exception", "APC"};
	private static final String UNMATCHED_SYSCALL = "???";
	
	// Keywords and variables from instrumentation records in instrumentation traces
	private static final String[] INSTRUMENTATION_KEYWORDS = {"InstrumentationBegin", "InstrumentationEnd"};
	private static final String[] INSTRUMENTATION_VARIABLES = {"HasXMM", "HasYMM", "64bits", "OS"};
	
	private Map<SyntaxHighlightingPreference, List<Token>> tokens;
	private Map<String, TextAttribute> ruleMap;
	private Token defaultToken;
	
	/**
	 * Creates a new scanner and configures its syntax highlighting rules and token types.
	 */
	public SystemEventScanner() {
		tokens = new HashMap<SyntaxHighlightingPreference, List<Token>>();
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		defaultToken = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_SYSTEM_EVENT)));
		this.setDefaultReturnToken(defaultToken);
		ruleMap = new HashMap<>();
		
		// Present keywords in bold and italics
		Token keyword = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_SYSTEM_EVENT), null, SWT.BOLD | SWT.ITALIC));
		WordRule wordRule = new WordRule(new IWordDetector() {
			@Override
			public boolean isWordStart(char c) {
				return c == '?' || Character.isLetterOrDigit(c);
			}

			@Override
			public boolean isWordPart(char c) {
				return c == '?' || Character.isLetterOrDigit(c);
			}
		}, defaultToken); // defaultToken stops portions of other words from being highlighted if they happen to match a keyword or variable
		
		for (int i = 0; i < SYSTEM_EVENT_KEYWORDS.length; i++) {
			wordRule.addWord(SYSTEM_EVENT_KEYWORDS[i], keyword);
			ruleMap.put("\\b" + (SYSTEM_EVENT_KEYWORDS[i]) + "\\b", (TextAttribute) keyword.getData());
		}
		for (int i = 0; i < INSTRUMENTATION_KEYWORDS.length; i++) {
			wordRule.addWord(INSTRUMENTATION_KEYWORDS[i], keyword);
			ruleMap.put("\\b" + (INSTRUMENTATION_KEYWORDS[i]) + "\\b", (TextAttribute) keyword.getData());
		}
		
		// Present value keywords for thread ID, exit code and instrumentation variables in boldface
		Token variable = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_SYSTEM_EVENT), null, SWT.BOLD));
		
		for (int i = 0; i < SYSTEM_EVENT_VARIABLES.length; i++) {
			wordRule.addWord(SYSTEM_EVENT_VARIABLES[i], variable);
			ruleMap.put("\\b" + (SYSTEM_EVENT_VARIABLES[i]) + "\\b", (TextAttribute) variable.getData());
		}
		
		// TODO Figure out highlighting for new format thread ID
//		// No word boundaries when adding this one...
//		wordRule.addWord(THREAD_ID_REGEX, variable);
//		ruleMap.put(THREAD_ID_REGEX, (TextAttribute) variable.getData());
		
		for (int i = 0; i < INSTRUMENTATION_VARIABLES.length; i++) {
			wordRule.addWord(INSTRUMENTATION_VARIABLES[i], variable);
			ruleMap.put("\\b" + (INSTRUMENTATION_VARIABLES[i]) + "\\b", (TextAttribute) variable.getData());
		}
		
		// Present context change types in boldface
		Token contextChange = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_SYSTEM_EVENT), null, SWT.BOLD));
		for (int i = 0; i < CONTEXT_CHANGE_TYPES.length; i++) {
			wordRule.addWord(CONTEXT_CHANGE_TYPES[i], contextChange);
			ruleMap.put("\\b" + (CONTEXT_CHANGE_TYPES[i]) + "\\b", (TextAttribute) contextChange.getData());
		}
		
		// Show ??? for unmatched system calls in red
		Token unmatched = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_PROBLEM)));
		wordRule.addWord(UNMATCHED_SYSCALL, unmatched);
		
		// Map and store the tokens with their corresponding syntax highlighting preferences. This lets us update the colours
		// should the user change one of those colour preferences later. We need to use lists of tokens since more than one 
		// token type uses the system event colour preference.
		List<Token> systemEventColourTokens = new ArrayList<Token>();
		systemEventColourTokens.add(defaultToken);
		systemEventColourTokens.add(keyword);
		systemEventColourTokens.add(variable);
		systemEventColourTokens.add(contextChange);
		tokens.put(SyntaxHighlightingPreference.SYSTEM_EVENT_COLOUR, systemEventColourTokens);
		
		List<Token> problemColourTokens = new ArrayList<Token>();
		problemColourTokens.add(unmatched);
		tokens.put(SyntaxHighlightingPreference.PROBLEM_COLOUR, problemColourTokens);
		
		this.setRules(new IRule[] {wordRule, this.getWhitespaceRule()});
	}

	@Override
	protected boolean usesSyntaxHighlightingPref(SyntaxHighlightingPreference preference) {
		return tokens.get(preference) != null;
	}

	@Override
	protected void updateColour(SyntaxHighlightingPreference preference) {
		if (!this.usesSyntaxHighlightingPref(preference)) {
			throw new IllegalArgumentException("Scanner does not use preference " + preference);
		}
		
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		List<Token> affectedTokens = tokens.get(preference);
		for (Token token : affectedTokens) {
			TextAttribute old = (TextAttribute) token.getData();
			token.setData(new TextAttribute(colours.get(preference.getColourID()), old.getBackground(), old.getStyle()));
		}
	}

	public Map<String, TextAttribute> getRuleMap() {
		return ruleMap;
	}
}
