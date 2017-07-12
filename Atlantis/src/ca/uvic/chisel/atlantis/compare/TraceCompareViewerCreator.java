package ca.uvic.chisel.atlantis.compare;

import org.eclipse.compare.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * Factory class for creating a new TraceCompareViewer when comparing two trace files.
 * @author Laura Chan
 */
public class TraceCompareViewerCreator implements IViewerCreator {
	@Override
	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		config.setLeftEditable(false);
		config.setRightEditable(false);
		return new TraceCompareViewer(parent, config);
	}
}
