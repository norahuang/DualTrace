package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;

import org.eclipse.jface.text.rules.*;

/**
 * Abstract superclass for the various rule-based scanners used to perform syntax highlighting. This class primarily serves as a 
 * type declaration for specifying the abstract methods usesSyntaxHighlightingPref() and updateColour().
 * 
 * Subclasses must override the default constructor to implement appropriate tokens and rules. The only thing that this class 
 * provides for its subclasses is a whitespace rule implementation. However, subclasses still must explicitly include this rule 
 * in any calls to setRules() if they need to use it.
 * @author Laura Chan
 */
public abstract class TraceDisplayerScanner extends RuleBasedScanner {
	
	private WhitespaceRule whitespaceRule = null;
	
	/**
	 * Gets the whitespace rule that this scanner should use.
	 * @return the scanner's whitespace rule
	 */
	protected WhitespaceRule getWhitespaceRule() {
		if (whitespaceRule == null) {
			whitespaceRule = new WhitespaceRule(new IWhitespaceDetector() {
				@Override
				public boolean isWhitespace(char c) {
					return Character.isWhitespace(c);
				}
			});
		}
		return whitespaceRule;
	}

	/**
	 * Returns whether this scanner has a token that uses the colour associated with the specified syntax highlighting preference
	 * @param preference syntax highlighting preference to test for
	 * @return true if at least one of this scanner's token uses the preference's associated colour, false otherwise
	 */
	protected abstract boolean usesSyntaxHighlightingPref(SyntaxHighlightingPreference preference);
	
	/**
	 * Updates any tokens that use the specified syntax highlighting preference so that they use the preference's associated colour.
	 * @param preference syntax highlighting preference whose colour should be used by any affected tokens
	 */
	protected abstract void updateColour(SyntaxHighlightingPreference preference);
}
