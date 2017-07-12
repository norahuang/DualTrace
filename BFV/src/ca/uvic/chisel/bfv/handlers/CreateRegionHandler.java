package ca.uvic.chisel.bfv.handlers;

import javax.xml.bind.JAXBException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

// needed for resolving several ambiguities
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.InvalidRegionException;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.FileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.dialogs.NameRegionDialog;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.views.RegionsView;

/**
 * Handler for creating a new region. Invoked when the Create Region command is executed.
 * @author Laura Chan
 */
public class CreateRegionHandler extends AbstractHandler {
	
	BigFileEditor bigFileEditor;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor instanceof BigFileEditor) {
			
			bigFileEditor = (BigFileEditor) editor;
			
			ISelection selection = bigFileEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;				
				if (!textSelection.isEmpty()) {
					IAnnotationModel model= bigFileEditor.getProjectionViewer().getProjectionAnnotationModel();
					if (model != null) {
						int start = textSelection.getStartLine();
						int end;
						String regionName = bigFileEditor.getProjectionViewer().getCurrentLine();
						if(regionName.contains("call")) {
							end = createCallRegion(start);
							regionName = new String(regionName.substring(regionName.indexOf("call"), regionName.indexOf(" | ")));
						} else {
							regionName = null;
							end = textSelection.getEndLine();
						}
						
						// sanity check, we should have handled this anyway
						if (start <= end) { 
							try {					
								// Prompt the user to name the region
								NameRegionDialog nameDialog = new NameRegionDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell(), regionName, start, end);
								nameDialog.create();								
								if (nameDialog.open() == Window.OK) {
									start = nameDialog.getStartLine();
									end = nameDialog.getEndLine();
									if(start < 0) {
										start = 0;
									}
									
									if(start >= end) {
										throw new InvalidRegionException("Start line was larger then the end Line.  Please ensure that the region start line is smaller than the region end line.");
									}

									RegionModel region = new RegionModel(regionName, start, end);
									
									// Validate the region's bounds first
									IFileModelDataLayer fileModel = bigFileEditor.getProjectionViewer().getFileModel();
									
									fileModel.validateRegionBounds(region);
									// Set the name and add the region
									region.setName(nameDialog.getName());
									// Check if it's being made around another region
									fileModel.addRegion(region); 
									// Update the regions view to show the new region
									RegionsView regionsView = (RegionsView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().
											findView(RegionsView.ID);
									if (regionsView != null) {
										regionsView.updateView();
									}
								} // else the user didn't enter a name, so no region gets created
								
							} catch (InvalidRegionException e) {
								BigFileApplication.showInformationDialog("Unable to create region", "The specified region is not valid.", e);
							} catch (JAXBException e) {
								BigFileApplication.showErrorDialog("Error creating region", 
										"Could not add new region to file's regions file", e);
							} catch (CoreException e) {
								BigFileApplication.showErrorDialog("Error creating region", 
										"Problem creating or refreshing file's regions file", e);
							}
						} else {
							BigFileApplication.showInformationDialog("Unable to create region", "Regions must span at least two consecutive lines.");
						}
					} 
				}
			}
		} 
		return null;
	}
	
	private int createCallRegion(int start) {
		IDocument doc = bigFileEditor.getProjectionViewer().getViewer().getDocument();
		try {
			String regionName = doc.get(doc.getLineOffset(start), doc.getLineLength(start)).replace("\n", "").replace("\r", "");
			String address = new String("(" + regionName.substring(regionName.length() - 8) + ")");
			int end;
			for(int i = start; i < doc.getNumberOfLines(); i++) {	
				String str = doc.get(doc.getLineOffset(i), doc.getLineLength(i)).replace("\n", "").replace("\r", "");				
				if(str.contains(address)) {					
					end = i - 1;
					if(end - start > 1) {
						return end;
					} else {
						return start + 1;
					}
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();				
		}
		return start + 1;
	}
}
