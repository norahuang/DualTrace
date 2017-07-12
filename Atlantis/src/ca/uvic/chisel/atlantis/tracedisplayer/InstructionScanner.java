package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.preferences.*;
import ca.uvic.chisel.bfv.*;

import java.util.*;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;

/**
 * Rule-based scanner for classifying and applying syntax highlighting to tokens of the instruction records of a trace 
 * being displayed in a TraceDisplayer. This is the scanner that highlights assembly operations, registers and 
 * other keywords. Trace errors/placeholders like BADADDR and NO_MODULE are also highlighted.
 * @author Laura Chan
 */
public class InstructionScanner extends TraceDisplayerScanner {
	// TODO find a cleaner way to implement operations, registers and keywords lists in the future. There are enough of them to make hardcoding
	// an unattractive solution, and that won't work anyway if we have to support syntax highlighting for other assembly languages in the future.
	private static final String[] OPERATIONS = { "c?mov(b|(z|s)xd?)?", "a((d|n)d)", "bt(s)?", "p(ush|op)", "s(ub|h(l|r)|ar|et(n?z))", "no(t|o?p)", "x?(or|chg)",
		"(System\\s)?c(mp|all)", "l(ea(ve)?|ock)", "i(nc|mul)", "j(mp|n?(be?|le?|z|s))", "r(e(t|pne)|ol)", "d(ec|ata)", "test",};
	private static final String[] REGISTERS = { "([ERer]([Ss]([Pp]|[Ii])|[Dd]([Ii]|[Xx])|(AX|ax)|[Bb]([Pp]|[Xx])|(CX|cx))\\,?\\s?)+" };

	private static final String[] KEYWORDS = { "[dq]?word|short|offset|ptr" };
	private static final String BAD_ADDRESS_ERROR = "BADADDR"; // TODO left over from old trace format, decide whether to delete this
	private static final String NO_MODULE = "NO_MODULE";
	private static final String NOT_EXECUTED = "N";
	private static final String UNKNOWN_TYPE_BEGIN = "(unknown";
	private static final String UNKNOWN_TYPE_END = "type!)";
	
	private Map<SyntaxHighlightingPreference, Token> tokens;
	private Map<String, TextAttribute> ruleMap;
	
	/**
	 * Creates a new scanner and configures its syntax highlighting rules and token types.
	 */
	public InstructionScanner() {
		// Define token types and the colour/style with which to highlight them
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		Token operation = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_OPERATION), null, SWT.BOLD));
		Token register = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_REGISTER)));
		Token keyword = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_KEYWORD)));
		Token problem = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_PROBLEM)));
		Token notExecuted = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_NOT_EXECUTED)));
		ruleMap = new HashMap<>();
		
		// Stops portions of other words from being highlighted if they happen to match an operation, register, keyword or error
		Token other = new Token(new TextAttribute(null));  
		
		// Map and store the tokens with the corresponding syntax highlighting preferences. This lets us update the colours
		// should the user change one of those colour preferences later. The "other" token can be skipped since it uses no colours.
		tokens = new HashMap<SyntaxHighlightingPreference, Token>();
		tokens.put(SyntaxHighlightingPreference.OPERATION_COLOUR, operation);
		tokens.put(SyntaxHighlightingPreference.REGISTER_COLOUR, register);
		tokens.put(SyntaxHighlightingPreference.KEYWORD_COLOUR, keyword);
		tokens.put(SyntaxHighlightingPreference.PROBLEM_COLOUR, problem);
		tokens.put(SyntaxHighlightingPreference.NOT_EXECUTED_COLOUR, notExecuted);
		
		WordRule wordRule = new WordRule(new IWordDetector() {
			@Override
			public boolean isWordStart(char c) {
				return Character.isLetterOrDigit(c);
			}

			@Override
			public boolean isWordPart(char c) {
				return c == '_' || Character.isLetterOrDigit(c);
			}
		}, other);
		
		// Where applicable, classify words according to the token types
		for (int i = 0; i < OPERATIONS.length; i++) {
			wordRule.addWord(OPERATIONS[i], operation);
			ruleMap.put("\\b" + (OPERATIONS[i]) + "\\b", (TextAttribute) operation.getData());
		}
		for (int i = 0; i < REGISTERS.length; i++) {
			wordRule.addWord(REGISTERS[i], register);
			ruleMap.put("\\b" + (REGISTERS[i]) + "\\b", (TextAttribute) register.getData());
//			wordRule.addWord(REGISTERS[i].toUpperCase(), register);
//			ruleMap.put("\\b" + (REGISTERS[i]) + "\\b".toUpperCase(), (TextAttribute) register.getData());
		}
		for (int i = 0; i < KEYWORDS.length; i++) {
			wordRule.addWord(KEYWORDS[i], keyword);
			ruleMap.put("\\b" + (KEYWORDS[i]) + "\\b", (TextAttribute) keyword.getData());
		}
		wordRule.addWord(BAD_ADDRESS_ERROR, problem);
		ruleMap.put("\\b" + (BAD_ADDRESS_ERROR) + "\\b", (TextAttribute) problem.getData());
		wordRule.addWord(NO_MODULE, problem);
		ruleMap.put("\\b" + (NO_MODULE) + "\\b", (TextAttribute) problem.getData());
		wordRule.addWord(NOT_EXECUTED, notExecuted);
		ruleMap.put("\\b" + (NOT_EXECUTED) + "\\b", (TextAttribute) problem.getData());
		
		// This highlights unknown-type system calls
		SingleLineRule unknownTypeRule = new SingleLineRule(UNKNOWN_TYPE_BEGIN, UNKNOWN_TYPE_END, problem);
		
		setRules(new IRule[] {wordRule, this.getWhitespaceRule(), unknownTypeRule});
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
		Token token = tokens.get(preference);
		TextAttribute old = (TextAttribute) token.getData();
		token.setData(new TextAttribute(colours.get(preference.getColourID()), old.getBackground(), old.getStyle()));
	}

	public Map<String, TextAttribute> getRuleMap() {
		return ruleMap;
	}
}
