package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.ISyntaxHighlightingManager;
import ca.uvic.chisel.bfv.editor.ISyntaxHighlightingManagerFactory;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

public class AtlantisSyntaxHighlightingManagerFactory implements ISyntaxHighlightingManagerFactory {
	@Override
	public ISyntaxHighlightingManager createHighlightingManager(ProjectionViewer viewer, IFileModelDataLayer fileModel) {
		return new AtlantisSyntaxHighlightingManager(viewer, fileModel);
	}
}
