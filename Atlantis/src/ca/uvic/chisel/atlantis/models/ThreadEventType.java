package ca.uvic.chisel.atlantis.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Color;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.datacache.TraceLine;

/**
 * Enum of thread event types, containing the colours to be used for their markers.
 * @author Laura Chan
 */
public enum ThreadEventType {
	THREAD_BEGIN(0, AtlantisColourConstants.THREAD_BEGIN_DEFAULT, AtlantisColourConstants.THREAD_BEGIN_SELECTED),
	THREAD_END(1, AtlantisColourConstants.THREAD_END_DEFAULT, AtlantisColourConstants.THREAD_END_SELECTED),
	@Deprecated
	OLD_EXPLICIT_THREAD_SWITCH(2, AtlantisColourConstants.THREAD_SWITCH_DEFAULT, AtlantisColourConstants.THREAD_SWITCH_SELECTED),
	IMPLICIT_THREAD_SWITCH(3, AtlantisColourConstants.THREAD_SWITCH_DEFAULT, AtlantisColourConstants.THREAD_SWITCH_SELECTED),
	APPLICATION_END(4, AtlantisColourConstants.THREAD_END_DEFAULT, AtlantisColourConstants.THREAD_END_SELECTED);
	
	private int id;
	private String defaultColour;
	private String selectedColour;
	// Old format had explicit thread switches with the switch keyword, but the new format
	// switches whenever the second token thread id has changed relative to the previous line.
	private static Pattern TID_PATTERN = Pattern.compile("^[0-9a-fA-F]+\\s([0-9a-fA-F]+)\\s\\{");
	private static Pattern threadEvents = Pattern.compile(".+(Thread(Begin)|Thread(End))|(switch)");
	
	/**
	 * Creates a new thread event type.
	 * @param defaultColour ID of the default colour to use for markers of this type
	 * @param selectedColour ID of the colour to use for markers of this type when selected
	 */
	private ThreadEventType(int id, String defaultColour, String selectedColour) {
		this.id = id;
		this.defaultColour = defaultColour;
		this.selectedColour = selectedColour;
	}
	
	/**
	 * Gets the default colour to be used for markers of this type.
	 * @return the default colour
	 */
	public Color getDefaultColour() {
		return AtlantisActivator.getDefault().getColorRegistry().get(defaultColour);
	}
	
	/**
	 * Gets the colour to be used for markers of this type when selected.
	 * @return the colour to use when a marker of this type is selected
	 */
	public Color getSelectedColour() {
		return AtlantisActivator.getDefault().getColorRegistry().get(selectedColour);
	}
	
	public static String getNewFormatThreadIdFromLine(String line){
		Matcher matcher = ThreadEventType.TID_PATTERN.matcher(line);
		if(matcher.find()){
			return matcher.group(1);
		}
		return null;
	}
	
	/**
	 * Trying to support legacy formats, so we can generate our own with the PIN Tool, as well as making use of better info available
	 * in the 2015 formats.
	 * 
	 * @param line
	 * @param previousThreadId
	 * @param lastEvent
	 * @param lineData
	 * @return
	 */
	public static ThreadEventType getThreadEventType(String line, String previousThreadId, TraceThreadEvent lastEvent, TraceLine lineData) {
		if(null == lastEvent){
			return getThreadEventTypeLegacy(line, previousThreadId, lastEvent);
		} else {
			return lineData.threadEventType;
		}
	}
	
	public static ThreadEventType getThreadEventTypeLegacy(String line, String previousThreadId, TraceThreadEvent lastEvent) {
		// The new format doesn't have the S: token and other stuff. It uses ThreadBegin and ThreadEnd
		// without Switch (ThreadBegin is a switch, essentially). They always follow the flags curly braces {stuff},
		// so we will check for the two thread tokens, preceded by the brace.
		if(line.contains("} ThreadBegin ")){
			return THREAD_BEGIN;
		} else if(line.contains("} ThreadEnd ")){
			return THREAD_END;
		} else if(line.contains("} ApplicationEnd ")){
			return APPLICATION_END;
		}
		
		Matcher matches = TID_PATTERN.matcher(line);
		String newTid = matches.find() ? matches.group(1) : null;
		// Either it was a true implicit switch, or it was a thread begin followed by the same thread,
		// which we want to visualize as a switch in terms of color.
		if(null != newTid && (!newTid.equals(previousThreadId) || (newTid.equals(previousThreadId))
				&& null != lastEvent && lastEvent.getEventType() == THREAD_BEGIN))  {
			return IMPLICIT_THREAD_SWITCH;
		}
		
		// The following are detection for the old data format, prior to Sept 2013.
		int firstSpace = line.indexOf(' ');
		if(line.charAt(0) != 'T' && line.charAt(0) != 'S' && line.charAt(firstSpace + 1) != 'T' && line.charAt(firstSpace + 1) != 'S'){
			return null;
		}
		Matcher threadMatcher = threadEvents.matcher(line);
		if(threadMatcher.find()) {
			if(threadMatcher.group(2) != null)  {
				return THREAD_BEGIN;
			}
			if(threadMatcher.group(3) != null)  {
				return THREAD_END;
			}
			if(threadMatcher.group(4) != null)  {
				return OLD_EXPLICIT_THREAD_SWITCH;
			}
		}
		
		return null;
	}
	
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the EventType with the corresponding id or null if one cannot be found
	 */
	public static ThreadEventType getFromId(int id) {
	
		for(ThreadEventType value : ThreadEventType.values()) {
			if(value.getId() == id) {
				return value;
			}
		}
		
		return null;
	}
}