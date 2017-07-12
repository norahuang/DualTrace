package ca.uvic.chisel.bfv.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.projection.ProjectionDocument;

/**
 * This class behaves exactly like the ProjectionDocument class, except that instead of using
 * the DefaultLineTracker, which is incredibly memory inefficient, it uses our custom
 * IntervalLineTracker.
 */
public class BigFileProjectionDocument extends ProjectionDocument {

	private IntervalLineTracker lineTracker;

	public BigFileProjectionDocument(IDocument masterDocument, DocumentContentManager contentManager) {
		super(masterDocument);
		
		lineTracker = contentManager.createLineTracker();
		setLineTracker(lineTracker);
	}
}
