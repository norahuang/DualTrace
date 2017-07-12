package ca.uvic.chisel.atlantis.eventtracevisualization;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalElement;

public class AnnotationVisualElement implements VisualElement {

	private static final int ANNOTATION_MARKER_WIDTH = 3;
	private int x;
	private int startY;
	private int endY;
	
	private Color markColor;
	
	public AnnotationVisualElement(int x, int startY, int endY, Color c) {
		this.x = x;
		this.startY = startY;
		this.endY = endY;
		this.markColor = c;
	}
	
	@Override
	public void draw(GC gc, Point currentOffset, int virtualStartX) {
		
		if(virtualStartX > x) {
			return;
		}
		
		gc.setBackground(markColor);
		
		int drawX = x + currentOffset.x;
		int drawStartY = startY + currentOffset.y;
		int drawEndY = endY + currentOffset.y;
		
		gc.fillPolygon(new int[]{drawX, drawStartY, drawX - ANNOTATION_MARKER_WIDTH, drawEndY, drawX + ANNOTATION_MARKER_WIDTH, drawEndY});
	}

	@Override
	public void dispose() {
		//nothing to do here
	}

	@Override
	public IntervalElement<VisualElement> asInterval() {
		return new IntervalElement<VisualElement>(new Interval(x, x), this);
	}

	@Override
	public void moveTo(GC gc, Point currentOffset, int virtualStartX) {
		//polygons are not moveable, so we simply have to redraw
		draw(gc, currentOffset, virtualStartX);
	}

}
