package ca.uvic.chisel.atlantis.tracedisplayer;

import java.io.File;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

/**
 * Class with static utility methods related to TraceDisplayer.
 * @author Laura Chan
 */
public class TraceDisplayerUtil {

	/**
	 * Returns whether the specified character could be a hex digit 
	 * @param c character to be tested
	 * @return true if c could be a hex digit, false otherwise
	 */
	public static boolean isHexDigit(char c) {
		return Character.toString(c).matches("[a-fA-F0-9]");
	}

	/**
	 * Returns whether the character at the specified location in the given line could be a lowercase 'h' suffix for some hex address
	 * @param currentChar index of the character to check
	 * @param line line in which the character appears
	 * @return true if the character is 'h' and does not proceed an alphanumeric character; false otherwise
	 */
	public static boolean isHexAddressSuffix(int currentChar, String line) {
		if (currentChar < 0 || currentChar >= line.length()) {
			return false;
		}
		
		int nextChar = currentChar + 1;
		return line.charAt(currentChar) == 'h' && (nextChar >= line.length() || !Character.isLetterOrDigit(line.charAt(nextChar)));
	}

	/**
	 * Determines whether the specified trace file is an execution trace. This is done by checking its filename; execution traces
	 * have file names that would match the regex A[0-9]+.trace. In comparison, instrumentation traces are always named I.trace,
	 * while old-format traces can have just about any name.
	 * @param traceFile trace file resource to test
	 * @return true if the trace file is an execution trace, false if it is an instrumentation or old-format trace
	 */
	@Deprecated
	public static boolean isExecutionTrace(File traceFile) {
		// For the new binary format, this logic does not apply.
		// I preferred that logic outside of this utility method,
		// where the sole caller already knew if it had a binary format file...
		// Move in here if necessary.
		Pattern executionTraceName = Pattern.compile(".+.trace");
		Matcher matcher = executionTraceName.matcher(traceFile.getName());
		return matcher.matches();
	}

	/**
	 * Helper method used to get the new value for a colour preference that was changed
	 * @param event PropertyChangeEvent for the colour preference that was changed
	 * @return the new value for the colour preference
	 */
	public static RGB getColourPreference(PropertyChangeEvent event) {
		Object newValue = event.getNewValue();
		if (newValue instanceof RGB) {
			return (RGB) newValue;
		} else if (newValue instanceof String) {
			return StringConverter.asRGB((String) newValue);
		} else {
			throw new IllegalArgumentException("Invalid new value for property " + event.getProperty() + ": " + newValue);
		}
	}

	/**
	 * Helper method used to get the new value for a boolean preference that was changed
	 * @param event PropertyChangeEvent for the boolean preference that was changed
	 * @return the new value for the boolean preference
	 */
	public static boolean getBooleanPreference(PropertyChangeEvent event) {
		Object newValue =  event.getNewValue();
		if (newValue instanceof Boolean) {
			return (Boolean) newValue;
		} else if (newValue instanceof String) {
			return StringConverter.asBoolean((String) newValue);
		} else {
			throw new IllegalArgumentException("Invalid new value for property " + event.getProperty() + ": " + newValue);
		}
	}
	
	// XXX TODO FIXME This method is a hack to make execution sequence view compile
	public static String createSequenceNumber(String line, int lineNumber, IFileModelDataLayer fileModel) {
		StringTokenizer token;
		
		String currentLine = line;
		
		if(!currentLine.matches("[a-fA-F0-9].+")) {
			currentLine = lineNumber + " " + currentLine;
		}
		
		// TODO is this what we should return if we cannot match a sequence number
		if(!currentLine.matches("[a-fA-F0-9].+")) {
			return " ";
		}
		
		token = new StringTokenizer(currentLine);
		return Integer.toString((Integer.parseInt(token.nextToken(), 16)), 16);	
	}
}
