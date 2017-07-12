package ca.uvic.chisel.bfv.utils;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class BfvViewerUtils {

	/**
	 * Get the absolute location of a control (i.e.: relative to the display, not to its parent control)
	 * @param control the control whose location is to be calculated
	 * @return a Point giving the absolute location of the control
	 */
	public static Point getAbsoluteLocation(Control control) {
		Point controlLocation = control.getLocation();
		if (control.getParent() == null) {
			return controlLocation;
		} else {
			Point parentLocation = getAbsoluteLocation(control.getParent());
			return new Point(parentLocation.x + controlLocation.x, parentLocation.y + controlLocation.y);
		}
	}
	
}
