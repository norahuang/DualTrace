package ca.uvic.chisel.atlantis.models;

import org.eclipse.swt.graphics.Color;

import ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants;

public class AssemblyChangedEvent implements IEventMarkerModel {

	private int lineNumber;
	
	private int numLines;
	
	private String id;
	
	private AssemblyEventType eventType;
	
	/**
	 * Needed for storage in the database to facilitate queries for graphical paging of markers.
	 * This is view data, but it was forced into the model to allow us to gain performance and storage
	 * benefits from the database backing trace derived data.
	 */
	private int pixelXStartPoint;
	private int pixelXEndPoint;
	
	public AssemblyChangedEvent(String id, int lineNumber, int numLines, int prevEventPixelEndpoint) {
		this(id, lineNumber, numLines, AssemblyEventType.SWITCH, prevEventPixelEndpoint);
	}
	
	public AssemblyChangedEvent(String id, int lineNumber, int numLines, AssemblyEventType eventType, int prevEventPixelEndpoint) {
		this.id = id;
		this.lineNumber = lineNumber;
		this.numLines = numLines;
		this.eventType = eventType;
		this.pixelXStartPoint = prevEventPixelEndpoint;
		int width = Math.max(this.numLines / TraceVisualizationConstants.SCALE_FACTOR, TraceVisualizationConstants.MIN_MARKER_WIDTH);
		this.pixelXEndPoint = this.pixelXStartPoint + width;
	}
	
	@Override
	public Color getDefaultColor() {
		return eventType.getDefaultColor();
	}
	
	/**
	 * Allows updating of the number of lines without keeping file line data around during parsing.
	 * Also updates the pixel end point.
	 * @param numLines
	 */
	public void setNumLines(int numLines) {
		this.numLines = numLines;
		int width = Math.max(this.numLines / TraceVisualizationConstants.SCALE_FACTOR, TraceVisualizationConstants.MIN_MARKER_WIDTH);
		this.pixelXEndPoint = this.pixelXStartPoint + width;
	}

	@Override
	public Color getSelectedColor() {
		return eventType.getSelectedColor();
	}

	@Override
	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public int getNumLines() {
		return numLines;
	}

	@Override
	public String getIdentifier() {
		return id;
	}
	
	public AssemblyEventType getEventType(){
		return this.eventType;
	}

	@Override
	public int getPixelStart() {
		return this.pixelXStartPoint;
	}

	@Override
	public int getPixelEnd() {
		return this.pixelXEndPoint;
	}

	

}
