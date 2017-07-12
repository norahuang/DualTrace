package ca.uvic.chisel.bfv.editor;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

public interface ISyntaxHighlightingManagerFactory {
	
	public static final String EXTENSION_ID = "ca.uvic.chisel.bfv.SyntaxHighlightingManagerFactory";
	
	public static final String CLASS_ATTRBIUTE = "class";
	
	public static final String PRIORITY_ATTRBIUTE = "priority";

	public static final int PRIORITY_DEFAULT = 0;

	public abstract ISyntaxHighlightingManager createHighlightingManager(ProjectionViewer viewer, IFileModelDataLayer fileModel);

}