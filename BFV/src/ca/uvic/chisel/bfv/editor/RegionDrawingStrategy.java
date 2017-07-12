package ca.uvic.chisel.bfv.editor;

import ca.uvic.chisel.bfv.annotations.RegionAnnotation;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionAnnotationsPainter.IDrawingStrategy;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.graphics.*;

/**
 * Drawing strategy that determines how collapsed regions are displayed in the File Viewer. The code for this class was copied 
 * from org.eclipse.jface.text.source.projection.ProjectionSupport.ProjectionDrawingStrategy with slight modifications.
 * @author Laura Chan
 */
class RegionDrawingStrategy implements IDrawingStrategy {
	
	@Override
	public void draw(Annotation annotation, GC gc, StyledText textWidget, int offset, int length, Color color) {
		if (annotation instanceof RegionAnnotation) {
			RegionAnnotation region = (RegionAnnotation) annotation;
			if (region.isCollapsed()) {
				if (gc != null) {
					
					StyledTextContent content= textWidget.getContent();
					int line= content.getLineAtOffset(offset);
					int lineStart= content.getOffsetAtLine(line);
					String text= content.getLine(line);
					int lineLength= text == null ? 0 : text.length();
					int lineEnd= lineStart + lineLength;
					Point p= textWidget.getLocationAtOffset(lineEnd);

					Color c= gc.getForeground();
					gc.setForeground(color);

					FontMetrics metrics= gc.getFontMetrics();

					// baseline: where the dots are drawn
					int baseline= textWidget.getBaseline(offset);
					// descent: number of pixels that the box extends over baseline
					int descent= Math.min(2, textWidget.getLineHeight(offset) - baseline);
					// ascent: so much does the box stand up from baseline.  
					int ascent= metrics.getAscent();
					// leading: free space from line top to box upper line
					int leading= baseline - ascent;
					// height: height of the box
					int height= ascent + descent;

					// Modified to draw a box over the entire line, and to draw three dots instead of just two
					Point lineStartPoint = textWidget.getLocationAtOffset(lineStart);
					int width= 2 * metrics.getAverageCharWidth();
					int lineWidth = lineLength * metrics.getAverageCharWidth();
					gc.drawRectangle(lineStartPoint.x, lineStartPoint.y + leading, lineWidth + width, height);
					int third= width/3;
					int dotsVertical= p.y + baseline - 1;
					gc.drawPoint(p.x + third, dotsVertical);
					gc.drawPoint(p.x + width - third, dotsVertical);
					int midpoint = ((p.x + third) + (p.x + width - third)) / 2;
					gc.drawPoint(midpoint, dotsVertical);
					gc.setForeground(c);

				} else {
					textWidget.redrawRange(offset, length, true);
				}
			}
		}
	}
}
