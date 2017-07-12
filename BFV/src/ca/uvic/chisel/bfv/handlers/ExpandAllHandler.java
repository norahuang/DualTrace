package ca.uvic.chisel.bfv.handlers;

import ca.uvic.chisel.bfv.annotations.*;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

import java.util.Collection;

import org.eclipse.core.commands.*;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for expanding all regions of a file. Invoked when the Expand All command is executed.
 * @author Laura Chan
 */
public class ExpandAllHandler extends AbstractHandler {

	private IFileModelDataLayer fileModel;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor != null && editor instanceof BigFileEditor) {
			BigFileEditor bigFileEditor = (BigFileEditor) editor;
			fileModel = bigFileEditor.getProjectionViewer().getFileModel();
			
			// Expand all of the regions
			Collection<RegionModel> regions = bigFileEditor.getProjectionViewer().getFileModel().getRegions();
			for (RegionModel r : regions) {
				expandAll(r);
				r.markExpanded();
			}
			
			// Update the file's regions data to reflect the fact that all regions are now collapsed
			bigFileEditor.getProjectionViewer().getFileModel().saveRegionData();
		}	
		return null;
	}

	/**
	 * Recursively expand this region and all of its children
	 * @param region region to expand
	 */
	private void expandAll(RegionModel region) {
		if (region.isCollapsed()) {
			fileModel.expandRegion(region);
		}
		
		Collection<RegionModel> children = region.getChildren();
		for (RegionModel child : children) {
			expandAll(child);
			child.markExpanded();
		}
	}
}
