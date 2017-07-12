package ca.uvic.chisel.atlantis.models;

import org.eclipse.swt.graphics.Color;

public interface IEventMarkerModel {

	/**
	 * @return the color that the marker should be when it is not selected
	 */
	Color getDefaultColor();
	
	/**
	 * @return The color that the marker should be when it is selected
	 */
	Color getSelectedColor();
	
	/**
	 * @return The line number of the file that the event occurred on
	 */
	int getLineNumber();
	
	/**
	 * @return The number of lines that the event occurred for. This will be mapped to the marks width.
	 */
	int getNumLines();
	
	/**
	 * @return A unique identifier for this element.
	 */
	public String getIdentifier();
	
	/**
	 * @return Per-computed starting pixel x coordinate for the marker. Added for pixel based database lookup.
	 */
	public int getPixelStart();
	
	/**
	 * @return Per-computed ending pixel x coordinate for the marker. Added for pixel based database lookup.
	 */
	public int getPixelEnd();
}
