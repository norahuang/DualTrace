package ca.uvic.chisel.atlantis.eventtracevisualization;

import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.ROW_HEIGHT;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.VERTICAL_SHIFT;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalElement;

public class RowLabelVisualElement implements VisualElement {

	private Color rowColour;
	private int rowNum;
	private Canvas parent;
	private String label;
	private int totalCanvasWidth;
	
	public RowLabelVisualElement(Color rowColor, int rowNum, Canvas parent, String label, int totalCanvasWidth) {
		this.rowColour = rowColor;
		this.rowNum = rowNum;
		this.parent = parent;
		this.label = label;
		this.totalCanvasWidth = totalCanvasWidth;
	}
	
	public RowLabelVisualElement(Color rowColor, int rowNum, Canvas parent, int totalCanvasWidth) {
		this.rowColour = rowColor;
		this.rowNum = rowNum;
		this.parent = parent;
		this.totalCanvasWidth = totalCanvasWidth;
	}
	
	@Override
	public void draw(GC gc, Point currentOffset, int startX) {
		gc.setBackground(rowColour);
		gc.fillRectangle(0, rowNum * ROW_HEIGHT + currentOffset.y, parent.getClientArea().width, ROW_HEIGHT);
		
		if(label != null) {
			gc.drawString(label, 0, (rowNum * ROW_HEIGHT + VERTICAL_SHIFT) + currentOffset.y);
		}
	}

	@Override
	public void dispose() {
		// do nothing, we will always have to redraw this
	}

	@Override
	public IntervalElement<VisualElement> asInterval() {
		return new IntervalElement<VisualElement>(new Interval(0, totalCanvasWidth), this);
	}

	@Override
	public void moveTo(GC gc, Point currentOffset, int startX) {
		draw(gc, currentOffset, startX);
	}

}
