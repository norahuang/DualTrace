package ca.uvic.chisel.bfv.views;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.bfv.BigFileActivator;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.ImageConstants;
import ca.uvic.chisel.bfv.annotations.InvalidRegionException;
import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.FileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.RegionChangeListener;
import ca.uvic.chisel.bfv.dialogs.NameRegionDialog;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

/**
 * View that lists all of the collapsible code regions in the file. Allows the user to do the following:
 * <li>Expand, collapse or jump to a specific a region.</li>
 * <li>Rename or remove regions.</li>
 * </ul>
 * @author Laura Chan
 */
public class RegionsView extends ViewPart implements MenuListener, IPartListener2, RegionChangeListener {
	public static final String ID = "ca.uvic.chisel.bfv.views.RegionsView";
	
	/**
	 * Provides text labels and icons for the regions shown in this view.
	 * @author Laura Chan
	 */
	private class RegionsLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			RegionModel region = (RegionModel) element;
			return region.toString();
		}
		
		@Override
		public Image getImage(Object element) {
			RegionModel region = (RegionModel) element;
			// TODO: Dummy images for now, replace later
			if (region.isCollapsed()) {
				if (region.getChildren() != null && region.getChildren().size() > 0) {
					return BigFileActivator.getDefault().getImageRegistry().get(ImageConstants.ICON_PARENT_REGION_COLLAPSED);
				} else {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
				}
			} else {
				if (region.getChildren() != null && region.getChildren().size() > 0) {
					return BigFileActivator.getDefault().getImageRegistry().get(ImageConstants.ICON_PARENT_REGION_EXPANDED);
				} else {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
				}
			}
		}
	}
	
	/**
	 * Content provider for populating the this view with the active file's regions.
	 * @author Laura Chan
	 */
	private class RegionsContentProvider implements ITreeContentProvider {
		Collection<RegionModel> regions;
		
		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				regions = new ArrayList<RegionModel>(); // clear out the old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					regions.add((RegionModel) o);
				}
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return regions.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			RegionModel region = (RegionModel) parentElement;
			return region.getChildren().toArray();
		}

		@Override
		public Object getParent(Object element) {
			RegionModel region = (RegionModel) element;
			return region.getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			RegionModel region = (RegionModel) element;
			return !region.getChildren().isEmpty();
		}
	}
	
	private BigFileEditor activeEditor;
	private IFileModelDataLayer fileModel;
	
	private TreeViewer treeViewer;
	private MenuItem expandOrCollapse;
	private MenuItem edit;
	private MenuItem remove;

	@Override
	public void createPartControl(Composite parent) {
		final Shell shell = parent.getShell(); // one of the menu item handlers needs this 
		
		// Tree of regions in the file
		treeViewer = new TreeViewer(parent, SWT.V_SCROLL);
		treeViewer.setContentProvider(new RegionsContentProvider());
		treeViewer.setLabelProvider(new RegionsLabelProvider());
		
		// Double click on a region in this view to go to it (automatically expands that region if collapsed)
		final Tree tree = treeViewer.getTree();
		Listener goToLineListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				RegionModel region = (RegionModel) selection.getFirstElement();
				if (region != null) {
					try {
						// if the parent of this region is collapsed, then this one is invisible.  Just go to the parent region.
						while(region.getParent() != null && region.getParent().isCollapsed()) {
							region = region.getParent();
						}
						
						activeEditor.getProjectionViewer().gotoLineAtOffset(region.getStartLine(), 0);
					} catch (Exception e) {
						BigFileApplication.showErrorDialog("Error navigating to region", "Could not go to region " + region.getName(), e);
					} finally {
						activeEditor.setFocus();
					}
				}
			}
		};
		tree.addListener(SWT.MouseDoubleClick, goToLineListener);

		// Right click menu for this view
		Menu treeMenu = new Menu(tree);
		tree.setMenu(treeMenu);
		treeMenu.addMenuListener(this);
		
		// Menu item for expanding/collapsing the selected region
		expandOrCollapse = new MenuItem(treeMenu, SWT.CASCADE);
		expandOrCollapse.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				RegionModel region = (RegionModel) selection.getFirstElement();
				if (region != null) {
					if(region.isCollapsed()) {
						fileModel.expandRegion(region);
					} else {
						fileModel.collapseRegion(region);
					}
				}
			}
		});
		
		// Menu item for renaming the selected region
		// TODO THIS IS JUST A RENAME
		edit = new MenuItem(treeMenu, SWT.CASCADE);
		edit.setText("Edit");
		edit.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				RegionModel region = (RegionModel) selection.getFirstElement();
				if (region != null) {
					NameRegionDialog nameDialog = new NameRegionDialog(shell, region.getName(), region.getStartLine(), region.getEndLine());
					nameDialog.create();
					if (nameDialog.open() == Window.OK) {
						try {
							String name = nameDialog.getName();
							int start = nameDialog.getStartLine();
							int end = nameDialog.getEndLine();
							if(start <= 0) {
								start = 0;
							}
							if(end >= fileModel.getNumberOfLines()) {
								end = (int)fileModel.getNumberOfLines() - 1;
							}
							
							if(start >= end) {
								throw new InvalidRegionException("Start line was larger then the end Line.  Please ensure that the region start line is smaller than the region end line.");
							}
							
							RegionModel newRegion = new RegionModel(region.getName(), start, end);
							newRegion.setName(name);
							// Add/Remove so that parent/child relations are dealt with
							fileModel.removeRegion(region);
							try{
								fileModel.addRegion(newRegion);
							} catch(InvalidRegionException e){
								// Re-add original region
								fileModel.addRegion(region);
								BigFileApplication.showErrorDialog("Error editing region", "The specified region is not valid.", e);
								e.printStackTrace();
							}
							
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error renaming region", "Could not update file's regions file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error renaming region", "Problem refreshing file's regions file", e);
						} catch (InvalidRegionException e) {
							BigFileApplication.showErrorDialog("Error editing region", "The specified region is not valid.", e);
							e.printStackTrace();
						}
						updateView();
					}
				}
			}
		});
		
		// Menu item for removing the selected region from the regions data file
		remove = new MenuItem(treeMenu, SWT.CASCADE);
		remove.setText("Remove");
		remove.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				RegionModel region = (RegionModel) selection.getFirstElement();
				if (region != null) {
					boolean remove = MessageDialog.openQuestion(shell, "Remove region", 
							"Do you want to remove this region?\n\nThis will only remove it from the Regions view and make the corresponding " + 
							"section of the file unfoldable; it will not remove anything from the file itself. Any sub-regions will not be deleted.");
					
					if (remove) {
						try {
							fileModel.removeRegion(region);
						} catch(InvalidRegionException e){
							// Shouldn't really be able to hit this...but the child regions might not be reassignable.
							BigFileApplication.showErrorDialog("Error editing region", "The specified region is not valid.", e);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error removing region", "Could not update file's regions file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error removing region", "Problem refreshing file's regions file", e);
						}
						updateView();
					}
				}
			}
		});
		
		// Enables this view to listen for when the active editor changes or is closed
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	/**
	 * Updates this view with the current region data from the active File Editor
	 */
	public void updateView() {
		if(activeEditor != null) {
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getRegions());
			treeViewer.expandAll();
		}
	}

	@Override
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}
	
	@Override
	public void dispose() {
		this.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	@Override
	public void menuHidden(MenuEvent e) {}

	@Override
	public void menuShown(MenuEvent e) {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		RegionModel region = (RegionModel) selection.getFirstElement();		
		expandOrCollapse.setText("Expand/Collapse...");
		expandOrCollapse.setEnabled(region != null);
		edit.setEnabled(region != null);
		remove.setEnabled(region != null);
		
		// Set the expand/collapse menu item's text according to whether the selected region is expanded or collapsed
		if (region != null) {
			if (region.isCollapsed()) {
				expandOrCollapse.setText("Expand");
			} else {
				expandOrCollapse.setText("Collapse");
			}
		} 
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if(editor instanceof BigFileEditor) {
			BigFileEditor newEditor = (BigFileEditor) editor;
			
			if(newEditor == activeEditor) {
				return;
			}
			
			activeEditor = newEditor;
			fileModel = activeEditor.getProjectionViewer().getFileModel();
			
			fileModel.registerRegionChangedListener(this);
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getRegions());
			treeViewer.expandAll();	
		}
	}
	
	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		// If the active File Editor was closed, remove its regions data from this view
		if (activeEditor != null && activeEditor == part) {
			activeEditor = null;
			
			if(fileModel != null) {
				fileModel.deregisterRegionChangedListener(this);
			}
			
			fileModel = null;
			treeViewer.setInput(null);
		} // Otherwise, it was some other editor that doesn't have any regions data being shown in this view, so we can ignore it
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {}

	@Override
	public void handleRegionChanged(RegionEventType eventType, RegionModel model) {
		this.updateView();
	}
}

