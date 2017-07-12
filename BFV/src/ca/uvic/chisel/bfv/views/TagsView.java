package ca.uvic.chisel.bfv.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.bfv.BigFileActivator;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.ImageConstants;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.dialogs.AddTagDialog;
import ca.uvic.chisel.bfv.dialogs.TooltipColourPicker;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

/**
 * View that lists all of the tags in the file. Allows the user to do the following:
 * <ul>
 * <li>Jump to a specific tag occurrence or navigate through all occurrences of a tag.</li>
 * <li>Select which tags or tag occurrences should be shown or hidden when the tags layer is turned on.</li>
 * <li>Delete tags or tag occurrences</li>
 * <li>Change the colour used for the sticky tooltips of a particular tag.</li>
 * </ul>
 * @author Laura Chan
 */
public class TagsView extends ViewPart implements IPartListener2, MenuListener {
	public static final String ID = "ca.uvic.chisel.bfv.views.TagsView";

	/**
	 * Content provider for populating the this view's checkbox tree with the active file tags.
	 * @author Laura Chan
	 */
	private class TagsContentProvider implements ITreeContentProvider {
		private Collection<Tag> tags;
		
		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				tags = new ArrayList<Tag>(); // clear out the old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					tags.add((Tag) o);
				}
			}	
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return tags.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Tag) {
				Tag tag = (Tag) parentElement;
				return tag.getOccurrences().toArray();
			} else {
				return null;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof TagOccurrence) {
				TagOccurrence occurrence = (TagOccurrence) element;
				return occurrence.getTag();
			} else {
				return null;
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof Tag) {
				Tag tag = (Tag) element;
				return !tag.getOccurrences().isEmpty();
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Provides text labels and icons for the tags shown in this view.
	 * @author Laura Chan
	 */
	private class TagsLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Tag) {
				Tag tag = (Tag) element;
				return tag.toString();
			} else if (element instanceof TagOccurrence) {
				TagOccurrence occurrence = (TagOccurrence) element;
				return "" + occurrence.getStartLine(true) + " : " + occurrence.getStartChar(true) + 
						" to " + occurrence.getEndLine(true) + " : "+ occurrence.getEndChar(true);
			} else {
				return super.getText(element);
			}
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof Tag) {
				return BigFileActivator.getDefault().getImageRegistry().get(ImageConstants.ICON_TAG);
			} else if (element instanceof TagOccurrence) {
				return BigFileActivator.getDefault().getImageRegistry().get(ImageConstants.ICON_TAG_OCCURRENCE);
			} else {
				return null;
			}
		}
	}
	
	private BigFileEditor activeEditor;
	
	private CheckboxTreeViewer treeViewer;
	private Menu menu;
	private MenuItem colourItem;
	private MenuItem deleteItem;
	private MenuItem editItem;
	
	// Variables for navigating through tag occurrences
	private Tag selectedTag = null;
	private int currentSelectedTagOccurrence = 0;

	@Override
	public void createPartControl(final Composite parent) {
		treeViewer = new CheckboxTreeViewer(parent, SWT.V_SCROLL);
		treeViewer.setContentProvider(new TagsContentProvider());
		treeViewer.setLabelProvider(new TagsLabelProvider()); 
		
		// Add a listener to handle items in the tree being checked or unchecked
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				try {
					if (event.getElement() instanceof Tag) {
						Tag tag = (Tag) event.getElement();
						activeEditor.getProjectionViewer().showOrHideStickyTooltip(tag, event.getChecked(), true);						
						// Also apply the same checked value to every occurrence of the tag
						for (TagOccurrence occurrence : tag.getOccurrences()) {
							treeViewer.setChecked(occurrence, event.getChecked());
						}
					} else if (event.getElement() instanceof TagOccurrence) {
						TagOccurrence occurrence = (TagOccurrence) event.getElement();
						activeEditor.getProjectionViewer().showOrHideStickyTooltip(occurrence, event.getChecked());
						Tag tag = occurrence.getTag();
						treeViewer.setChecked(tag, tag.getShowStickyTooltip()); // make sure the underlying tag's check is kept up-to-date
					}
				} catch (JAXBException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding tag tooltips", "Could not update file's tags file", e);
				} catch (CoreException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding tag tooltips", "Problem refreshing file's tags file", e);
				}
			}
		});
		
		final Tree tree = treeViewer.getTree();
		SelectionListener tagSelectedListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent  event) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				updateSelectedTag();
			}
		};
		Listener goToLineListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				gotoCurrentSelectedTagOccurrence();
			}
		};
		KeyListener enterKeyGoToTagListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				if(e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR){
					gotoCurrentSelectedTagOccurrence();
				}
				
			}
		};
		tree.addSelectionListener(tagSelectedListener);
		tree.addListener(SWT.MouseDoubleClick, goToLineListener);
		tree.addKeyListener(enterKeyGoToTagListener);
		// Tried adding SWT.CR and SWT.LF listeners, but those were leading to single-click
		// events firing onto them (likely due to single-click support in Eclipse).
		// If we can prevent that, we can add them back in...
		
		menu = new Menu(tree);
		tree.setMenu(menu);
		menu.addMenuListener(this);
		
		editItem = new MenuItem(menu, SWT.CASCADE);
		editItem.setText("Edit");
		editItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if(selection.getFirstElement() instanceof Tag) {
					Tag selected = (Tag)selection.getFirstElement();
					AddTagDialog editTagDialog = new AddTagDialog(parent.getShell(), null);
					editTagDialog.create(selected);
					if(editTagDialog.open() == Window.OK) {
						try {
							IFileModelDataLayer fileModel = activeEditor.getProjectionViewer().getFileModel();
							String newTagName = editTagDialog.getSelectedTag();
							List<TagOccurrence> occurrences = new ArrayList(selected.getOccurrences());
							fileModel.deleteTag(selected);
							activeEditor.getProjectionViewer().updateAllTagOccurrence(selected, newTagName);
							for(TagOccurrence occ: occurrences){
								fileModel.editTagOccurrence(occ, newTagName);
							}
							updateView();
						} catch (JAXBException | CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}			
		});
		
		// Menu item for colour coding tags
		colourItem = new MenuItem(menu, SWT.CASCADE);
		colourItem.setText("Choose Colour");
		colourItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof Tag) {
					Tag tag = (Tag) selected;
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					TooltipColourPicker colourPicker = new TooltipColourPicker(shell, Tag.DEFAULT_COLOUR);
					colourPicker.setColour(tag.getColour());
					colourPicker.create();
					
					if (colourPicker.open() == Window.OK) {
						try {
							activeEditor.getProjectionViewer().setColour(tag, colourPicker.getColour());
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error setting tag colour", "Could not update file's tags file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error setting tag colour", "Problem refreshing file's tags file", e);
						}
					}
				}
			}
		});
		
		// Menu item for deleting tags or tag occurrences
		deleteItem = new MenuItem(menu, SWT.CASCADE);
		deleteItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (selected instanceof Tag) {
					Tag tag = (Tag) selected;
					boolean delete = MessageDialog.openQuestion(shell, "Delete Tag", 
							"This will delete all occurrences of tag '" + tag.getName() + "'. Are you sure you want to do this?");
					if (delete) {
						try {
							activeEditor.getProjectionViewer().deleteTag(tag);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error deleting tag", "Could not update file's tags file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error deleting tag", "Problem refreshing file's tags file", e);
						}
						updateView();
						selectedTag = null;
						currentSelectedTagOccurrence = -1;
					}
				} else if (selected instanceof TagOccurrence) {
					TagOccurrence occurrence = (TagOccurrence) selected;
					boolean delete = MessageDialog.openQuestion(shell, "Delete Tag Occurrence", 
							"Are you sure you want to delete " + occurrence.toString() + "?");
					if (delete) {
						try {
							activeEditor.getProjectionViewer().deleteTagOccurrence(occurrence);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error deleting tag occurrence", "Could not update file's tags file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error deleting tag occurrence", "Problem refreshing file's tags file", e);
						}
						updateView();
						selectedTag = null;
						currentSelectedTagOccurrence = -1;
					}
				}
			}
		});
		
		// Enables this view to listen for when the active editor changes or is closed
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	/**
	 * Update this view with the current tags data from the File Editor
	 */
	public void updateView() {
		if(activeEditor != null) {
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getTags());
			setInitiallyCheckedItems();			
		}
	}
	
	/**
	 * Set which tree items are initially checked based on the value of the item's showStickyTooltip property.
	 */
	private void setInitiallyCheckedItems() {
		Collection<Tag> tags = activeEditor.getProjectionViewer().getFileModel().getTags();
		for (Tag tag : tags) {
			treeViewer.setChecked(tag, tag.getShowStickyTooltip());
			for (TagOccurrence occurrence : tag.getOccurrences()) {
				treeViewer.setChecked(occurrence, occurrence.getShowStickyTooltip());
			}
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
	
	/**
	 * Selects and navigates to the previous occurrence of the tag that is currently selected. If the first occurrence was originally
	 * selected, this method wraps to the last occurrence of the tag.
	 */
	public void previousOccurrenceOfSelectedTag() {
		if (selectedTag != null && selectedTag.getOccurrences().size() >= 1) {
			currentSelectedTagOccurrence--;
			// Wrap around to the last occurrence if we were previously at the first occurrence
			if (currentSelectedTagOccurrence < 0) {
				currentSelectedTagOccurrence = selectedTag.getOccurrences().size() - 1;
			}
			treeViewer.setSelection(new StructuredSelection(selectedTag.getOccurrences().get(currentSelectedTagOccurrence)), true);
			gotoCurrentSelectedTagOccurrence();
		}
	}
	
	/** 
	 * Selects and navigates to the next occurrence of the tag that is currently selected. If the last occurrence was originally 
	 * selected, this method wraps to the first occurrence of the tag.
	 */
	public void nextOccurrenceOfSelectedTag() {
		if (selectedTag != null && selectedTag.getOccurrences().size() >= 1) {
			currentSelectedTagOccurrence++;
			// Wrap around to the first occurrence if we were previously at the last occurrence
			if (currentSelectedTagOccurrence >= selectedTag.getOccurrences().size()) {
				currentSelectedTagOccurrence = 0;
			}
			treeViewer.setSelection(new StructuredSelection(selectedTag.getOccurrences().get(currentSelectedTagOccurrence)), true);
			gotoCurrentSelectedTagOccurrence();
		}
	}
	
	/**
	 * Jumps to the current selected tag occurrence in the active File Editor.
	 */
	private void gotoCurrentSelectedTagOccurrence() {
		TagOccurrence occurrence = selectedTag.getOccurrences().get(currentSelectedTagOccurrence);
		ProjectionViewer viewer = (ProjectionViewer) activeEditor.getProjectionViewer().getViewer();
		StyledText textWidget = viewer.getTextWidget();
		IDocument document = viewer.getDocument();
		try {
			int startOffset = document.getLineOffset(occurrence.getStartLine()) + occurrence.getStartChar();
			activeEditor.getProjectionViewer().gotoLineAtOffset(occurrence.getStartLine(), occurrence.getStartChar());
			textWidget.setCaretOffset(startOffset);
			// activeEditor.getProjectionViewer().setFocus(occurrence);
			// Make editor update memory views and whatnot, then focus back on tag view to allow easy keyboard navigation therein
			activeEditor.triggerCursorPositionChanged();
			// treeViewer.setSelection(new StructuredSelection(selectedTag.getOccurrences().get(currentSelectedTagOccurrence)), true);
			treeViewer.getTree().setFocus();

		} catch (Exception e) {
			BigFileApplication.showErrorDialog("Error navigating to tag occurrence", "Could not go to " + occurrence, e);
		}
	}
	
	private void updateSelectedTag(){
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		Object selected = selection.getFirstElement();
		if (selected instanceof Tag) {
			Tag tag = (Tag) selected;
			selectedTag = tag;					
			if (selectedTag != null && selectedTag.getOccurrences().size() >= 1) {
				// Navigate to the first occurrence of the tag
				currentSelectedTagOccurrence = 0;
			} else {
				currentSelectedTagOccurrence = -1;
			}
		} else if (selected instanceof TagOccurrence) {
			TagOccurrence occurrence = (TagOccurrence) selected;
			selectedTag = occurrence.getTag();
			if (selectedTag != null) {
				currentSelectedTagOccurrence = selectedTag.getOccurrences().indexOf(occurrence);
			}
		}
	}
	
	@Override
	public void menuHidden(MenuEvent e) {}

	@Override
	public void menuShown(MenuEvent e) {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		Object selected = selection.getFirstElement();
		colourItem.setEnabled(selected instanceof Tag);
		deleteItem.setText("Delete");
		deleteItem.setEnabled(selected != null);
		if (selected instanceof Tag) {
			editItem.setEnabled(true);
			deleteItem.setText("Delete Tag");			
		} else if (selected instanceof TagOccurrence) {
			editItem.setEnabled(false);
			deleteItem.setText("Delete Occurrence");
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
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getTags());
			setInitiallyCheckedItems();			
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		// If the active File Editor was the part that was closed, remove its tags data from this view
		if (activeEditor != null && activeEditor == part) {
			activeEditor = null;
			treeViewer.setInput(null);
			selectedTag = null;
			currentSelectedTagOccurrence = -1;
		} // else, something else was closed, so we don't need to do anything
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
}
