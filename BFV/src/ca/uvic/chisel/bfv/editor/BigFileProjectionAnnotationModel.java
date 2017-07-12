package ca.uvic.chisel.bfv.editor;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

public class BigFileProjectionAnnotationModel extends ProjectionAnnotationModel {

	protected final BigFileViewer viewer;

	public BigFileProjectionAnnotationModel(BigFileViewer viewer) {
		super();
		this.viewer = viewer;
	}
	
	/**
	 * This messes with a lot of our region and data paging stuff.  It appears that no issues
	 * arise when simply removing this functionality all together.
	 */
	@Override
	protected boolean expandAll(int offset, int length, boolean fireModelChanged) {
		return false;
	}
	
	// XXX FIXME This is a hack to get around the problem that we are currently 
	// listening to markExpanded instead of modify annotation events.
	// Removing this outright led to one less call (of 4) to the highlighting method.
	// It also saves some lines from being blanked out in the trace. It saved no
	// real time in a 1.5GB test with the region from line 4 to the end of file.
	// Also, when adding the BigFileViewer as an annotationModelListener
	// in BigFileViewer#createVisualAnnotationModel(), it didn't lead to events
	// triggering on our listener method, except when first creating the annotations.
	// So that listener can't act as our collapse/expand listener?
	@Override
	protected void modifyAnnotation(Annotation annotation, boolean fireModelChanged) {
		super.modifyAnnotation(annotation, fireModelChanged);
		viewer.handleAnnotationChangeCommitted();
	}
}
