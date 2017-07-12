package ca.uvic.chisel.atlantis.eventtracevisualization;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import ca.uvic.chisel.bfv.intervaltree.IntervalElement;

public interface VisualElement {

	/**
	 * A method that will draw the visual element that you are trying to draw
	 */
	void draw(GC gc, Point currentOffset, int virtualStartX);
	
	/**
	 * A method that will dispose this visual element if it has already been drawn
	 */
	void dispose();
	
	/**
	 * @return An interval containing both the start and end x value for the element, as well as
	 * 			the element itself.
	 */
	IntervalElement<VisualElement> asInterval();
	
	
	/**
	 * @param gc
	 * the graphical component that will be used to draw this element
	 * 
	 * @param xOffset
	 * the offset into the virtual space that is currently being displayed.  This number
	 * should always be negative.
	 * 
	 * @param virtualStartX
	 * The smallest x value in the virtual space that this element is allowed to occupy.
	 */
	void moveTo(GC gc, Point currentOffset, int virtualStartX);
}
