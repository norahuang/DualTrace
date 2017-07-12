package ca.uvic.chisel.atlantis.models;
import org.eclipse.swt.graphics.Color;

import ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants;

public class TraceThreadEvent implements IEventMarkerModel{
	
	private String tid;

	private int lineNum;
	
	private int numLines;
	
	/**
	 * Needed for storage in the database to facilitate queries for graphical paging of markers.
	 * This is view data, but it was forced into the model to allow us to gain performance and storage
	 * benefits from the database backing trace derived data.
	 */
	private int pixelXStartPoint;
	private int pixelXEndPoint;

	private ThreadEventType eventType;
	
	/**
	 * 
	 * @param tid must be an integer >= 0
	 * @param lineNum
	 * @param eventType
	 */
	public TraceThreadEvent(String tid, int lineNum, ThreadEventType eventType, int pixelXStartPoint) {
		this.tid = tid;
		this.eventType = eventType;
		this.lineNum = lineNum;
		this.pixelXStartPoint = pixelXStartPoint;
		int width = Math.max(this.numLines / TraceVisualizationConstants.SCALE_FACTOR, TraceVisualizationConstants.MIN_MARKER_WIDTH);
		this.pixelXEndPoint = this.pixelXStartPoint + width;
	}
	
	public String getTid() {
		return tid;
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
	
	public ThreadEventType getEventType() {
		return eventType;
	}
	
	@Override
	public int getNumLines() {
		return numLines;
	}

	@Override
	public Color getDefaultColor() {
		return this.eventType.getDefaultColour();
	}

	@Override
	public Color getSelectedColor() {
		return this.eventType.getSelectedColour();
	}

	@Override
	public int getLineNumber() {
		return lineNum;
	}
	
	@Override
	public String getIdentifier() {
		return getTid();
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


