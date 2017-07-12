package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;
import ca.uvic.chisel.bfv.*;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;

/**
 * Rule-based scanner for classifying and applying syntax highlighting to tokens in a module load/unload partition of a trace 
 * being displayed in a TraceDisplayer.
 * @author Laura Chan
 */
public class ModuleLoadUnloadScanner extends TraceDisplayerScanner {
	private static final String[] keywords = {"From", "To", "File"};
	
	private Token defaultToken;
	private Token keyword;
	
	/**
	 * Creates a new scanner and configures its syntax highlighting rules and token types.
	 */
	public ModuleLoadUnloadScanner() {
		ColorRegistry colours = AtlantisActivator.getDefault().getColorRegistry();
		defaultToken = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_MODULE_LOAD_UNLOAD)));
		this.setDefaultReturnToken(defaultToken);
		
		// Present keywords in boldface
		keyword = new Token(new TextAttribute(colours.get(AtlantisColourConstants.SYNTAX_MODULE_LOAD_UNLOAD),null, SWT.BOLD));
		WordRule wordRule = new WordRule(new IWordDetector() {
			@Override
			public boolean isWordStart(char c) {
				return Character.isLetterOrDigit(c);
			}

			@Override
			public boolean isWordPart(char c) {
				return Character.isLetterOrDigit(c);
			}
		}, defaultToken); // defaultToken stops portions of other words from being highlighted if they happen to match a keyword
		
		for (int i = 0; i < keywords.length; i++) {
			wordRule.addWord(keywords[i], keyword);
		}
		
		this.setRules(new IRule[] {wordRule, this.getWhitespaceRule()});
	}
	
	@Override
	protected boolean usesSyntaxHighlightingPref(SyntaxHighlightingPreference preference) {
		return preference == SyntaxHighlightingPreference.MODULE_LOAD_UNLOAD_COLOUR;
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
}
