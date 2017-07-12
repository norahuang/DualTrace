package ca.uvic.chisel.bfv.handlers;

import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

/**
 * Handler for collapsing all regions of a file. Invoked when the Collapse All command is executed.
 * @author Laura Chan
 */
public class CollapseAllHandler extends AbstractHandler {

	private IFileModelDataLayer fileModel;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor != null && editor instanceof BigFileEditor) {
			BigFileEditor bigFileEditor = (BigFileEditor) editor;
			fileModel = bigFileEditor.getProjectionViewer().getFileModel();
			
			// Collapse all of the regions
			Collection<RegionModel> regions = bigFileEditor.getProjectionViewer().getFileModel().getRegions();
			for (RegionModel r : regions) {
				collapseAll(r);
			}
			
			// Update the file's regions data to reflect the fact that all regions are now collapsed
			bigFileEditor.getProjectionViewer().getFileModel().saveRegionData();
		}
		
		return null;
	}

	/**
	 * Recursively collapse this region and all of its children
	 * @param region region to collapse
	 */
	private void collapseAll(RegionModel region) {
		if (!region.isCollapsed()) {
			fileModel.collapseRegion(region);
		}
		
		Collection<RegionModel> children = region.getChildren();
		for (RegionModel child : children) {
			collapseAll(child);
		}
	}
}
