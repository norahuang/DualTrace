package ca.uvic.chisel.atlantis.eventtracevisualization;

public class TraceVisualizationConstants {

	public static final int MIN_MARKER_WIDTH = 13;
	public static final int MARKER_HEIGHT = 9;
	// TODO make this dynamic
	public static final int DEFAULT_ROW_LABEL_LENGTH = 6; // determines how much space we leave for writing the row labels 
	public static final int SCALE_FACTOR = 20; // Adjust with caution--for some reason, 15 makes parts of the visualization incorrect; 25 is ok
	// TODO hardcoding row height and vertical shift for now, but look for a less hacky way to implement these in the future
	public static final int ROW_HEIGHT = 20;
	public static final int VERTICAL_SHIFT = 5;
	public static final int LABEL_SECTION_PADDING = 10;
	// XXX For some reason, total width - canvas width is not quite right for the horizontal scrollBar max
	public static final int SCROLLBAR_RIGHT_PADDING = 20;

}
