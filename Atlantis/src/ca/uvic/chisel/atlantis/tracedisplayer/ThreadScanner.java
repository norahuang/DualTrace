package ca.uvic.chisel.atlantis.tracedisplayer;

import java.util.HashMap;
import java.util.Map;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;
import ca.uvic.chisel.bfv.*;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;

/**
 * Rule-based scanner for classifying and applying syntax highlighting to tokens in a thread partition of a trace 
 * being displayed in a TraceDisplayer. 
 * @author Laura Chan
 */
public class ThreadScanner extends TraceDisplayerScanner {
	// TODO the trace format document we have doesn't actually list these. These are ones that have been seen in the sample
	// trace file they gave us, but whether there are any more keywords is currently uncertain.
	public static final String[] KEYWORDS = {"same", "switch"}; 
	
	private Token defaultToken;
	private Token keyword;
	private Map<String, TextAttribute> ruleMap;
	
	/**
	 * Creates a new scanner and configures its default return token.
	 */
	public ThreadScanner() {
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		defaultToken = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_THREAD)));
		this.setDefaultReturnToken(defaultToken);
		ruleMap = new HashMap<>();
		
		// Present keywords in boldface
		keyword = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_THREAD), null, SWT.BOLD));
		WordRule wordRule = new WordRule(new IWordDetector() {
			@Override
			public boolean isWordStart(char c) {
				return Character.isLetterOrDigit(c);
			}

			@Override
			public boolean isWordPart(char c) {
				return Character.isLetterOrDigit(c);
			}
		}, defaultToken); // defaultToken stops portions of other words from being highlighted if they happen to match a keyword or variable
		
		for (int i = 0; i < KEYWORDS.length; i++) {
			wordRule.addWord(KEYWORDS[i], keyword);
			ruleMap.put("\\b" + (KEYWORDS[i]) + "\\b", (TextAttribute) keyword.getData());
		}
		
		this.setRules(new IRule[]{wordRule, this.getWhitespaceRule()});
	}

	@Override
	protected boolean usesSyntaxHighlightingPref(SyntaxHighlightingPreference preference) {
		return preference == SyntaxHighlightingPreference.THREAD_COLOUR;
	}

	@Override
	protected void updateColour(SyntaxHighlightingPreference preference) {
		if (!this.usesSyntaxHighlightingPref(preference)) {
			throw new IllegalArgumentException("Scanner does not use preference " + preference);
		}
		
		updateColour(defaultToken, preference.getColourID());
		updateColour(keyword, preference.getColourID());
	}
	
	/**
	 * Helper method for updating the specified token to use the colour given by the specified ID
	 * @param token token whose colour is to be updated
	 * @param newColourID new colour that the token should use
	 */
	private void updateColour(Token token, String newColourID) {
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		TextAttribute old = (TextAttribute) token.getData();
		token.setData(new TextAttribute(colours.get(newColourID), old.getBackground(), old.getStyle()));
	}

	public Map<String, TextAttribute> getRuleMap() {
		return ruleMap;
	}
}
