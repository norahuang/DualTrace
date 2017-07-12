package ca.uvic.chisel.atlantis.eventtracevisualization;

import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.MARKER_HEIGHT;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.MIN_MARKER_WIDTH;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.ROW_HEIGHT;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.VERTICAL_SHIFT;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalElement;

public class MarkerLinkVisualElement implements VisualElement {

	private int startX;
	private int startY;
	private int rowNum;
	private int endX;

	public MarkerLinkVisualElement(int startX, int startY, int rowNum) {
		this.startX = startX;
		this.startY = startY;
		this.rowNum = rowNum;
		this.endX = startX  + MIN_MARKER_WIDTH / 2;;
	}
	
	public static int drawOrder = 0;

	/**
	 * @param gc
	 * the graphical component that will be used to draw this element
	 * 
	 * @param currentOffset
	 * the offset into the virtual space that is currently being displayed.  This number
	 * should always be negative in both the X and the Y direction.
	 * 
	 * @param virtualStartX
	 * The smallest x value in the virtual space that this element is allowed to occupy.
	 */
	@Override
	public void draw(GC gc, Point currentOffset, int virtualStartX) {
		// Draw thread spawn line in the old thread's row
		int yMidpoint = (startY + MARKER_HEIGHT / 2 + VERTICAL_SHIFT) + currentOffset.y; ;
				
		Point point1 = new Point(startX + currentOffset.x, yMidpoint);
		Point point2 = new Point(endX + currentOffset.x, yMidpoint);
		Point point3 = new Point(endX + currentOffset.x, (rowNum * ROW_HEIGHT + VERTICAL_SHIFT) + currentOffset.y);
		
		gc.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		
		if(virtualStartX <=  startX) {
			gc.drawLine(point1.x, point1.y, point2.x, point2.y);
			gc.drawLine(point2.x, point2.y, point3.x, point3.y);
		}
		
		else if(virtualStartX > startX && virtualStartX <= endX) {
			gc.drawLine(virtualStartX + currentOffset.x, point1.y, point2.x, point2.y);
			gc.drawLine(point2.x, point2.y, point3.x, point3.y);
		}
	}
	
	/**
	 * Since you cannot move lines, simply redraw it in it's new location
	 */
	@Override
	public void moveTo(GC gc, Point currentOffset, int virtualStartX) {
		draw(gc, currentOffset, virtualStartX);
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public IntervalElement<VisualElement> asInterval() {
		return new IntervalElement<VisualElement>(new Interval(startX, endX), this);
	}

}
