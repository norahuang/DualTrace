package ca.uvic.chisel.bfv.editor;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

public class DefaultSyntaxHighlightingManagerFactory implements ISyntaxHighlightingManagerFactory {

	public DefaultSyntaxHighlightingManagerFactory() {}
	
	@Override
	public ISyntaxHighlightingManager createHighlightingManager(ProjectionViewer viewer, IFileModelDataLayer fileModel) {
		return new DefaultSyntaxHighlightingManager(viewer, fileModel);
	}
}
