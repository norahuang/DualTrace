package ca.uvic.chisel.bfv.views;

import java.util.ArrayList;
import java.util.Collection;

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
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
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

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.dialogs.AddOrEditCommentDialog;
import ca.uvic.chisel.bfv.dialogs.MoveCommentDialog;
import ca.uvic.chisel.bfv.dialogs.RenameCommentGroupDialog;
import ca.uvic.chisel.bfv.dialogs.TooltipColourPicker;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

/**
 * View that lists all of the comments in the file by group. Allows the user to do the following:
 * <ul>
 * <li>Jump to a specific comment or navigate through all comments in a group.</li>
 * <li>Select which comments or groups should be shown or hidden when the comments layer is turned on.</li>
 * <li>Edit, move, or delete comments.</li>
 * <li>Rename or delete comment groups</li>
 * <li>Change the colour used for the sticky tooltips for the comments of a particular group.</li>
 * </ul>
 * @author Laura Chan
 */
public class CommentsView extends ViewPart implements IPartListener2, MenuListener {
	public static final String ID = "ca.uvic.chisel.bfv.views.CommentsView";

	/**
	 * Content provider for populating the this view's checkbox tree with the active file's comments and comment groups.
	 * @author Laura Chan
	 */
	private class CommentsContentProvider implements ITreeContentProvider {
		private Collection<CommentGroup> commentGroups;
		
		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				commentGroups = new ArrayList<CommentGroup>(); // clear out the old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					commentGroups.add((CommentGroup) o);
				}
			}	
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return commentGroups.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof CommentGroup) {
				CommentGroup group = (CommentGroup) parentElement;
				return group.getComments().toArray();
			} else {
				return null;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Comment) {
				Comment comment = (Comment) element;
				return comment.getCommentGroup();
			} else {
				return null;
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof CommentGroup) {
				CommentGroup group = (CommentGroup) element;
				return !group.getComments().isEmpty();
			} else {
				return false;
			}
		}
	}
	
	private BigFileEditor activeEditor;
	
	private CheckboxTreeViewer treeViewer;
	private MenuItem editItem;
	private MenuItem moveItem;
	private MenuItem colourItem;
	private MenuItem deleteItem;
	
	// Variables for navigating through comments
	private CommentGroup selectedGroup;
	private int currentSelectedComment;
	
	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new CheckboxTreeViewer(parent, SWT.V_SCROLL);
		treeViewer.setLabelProvider(new LabelProvider());
		treeViewer.setContentProvider(new CommentsContentProvider());
		
		// Add a listener to handle items in the tree being checked or unchecked
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				try {
					if (event.getElement() instanceof CommentGroup) {
						CommentGroup group = (CommentGroup) event.getElement();
						activeEditor.getProjectionViewer().showOrHideStickyTooltip(group, event.getChecked(), true);						
						// Also apply the same checked value to every comment in the group
						for (Comment comment : group.getComments()) {
							treeViewer.setChecked(comment, event.getChecked());
						}
					} else if (event.getElement() instanceof Comment) {
						Comment comment = (Comment) event.getElement();
						activeEditor.getProjectionViewer().showOrHideStickyTooltip(comment, event.getChecked());
						CommentGroup group = comment.getCommentGroup();
						treeViewer.setChecked(group, group.getShowStickyTooltip()); // make sure the group's check is up-to-date
					}
				} catch (JAXBException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding comment tooltips", "Could not update file's comments file", e);
				} catch (CoreException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding comment tooltips", "Problem refreshing file's comments file", e);
				}
			}
		});
		
		// Add a listener to handle items in the tree being selected
		final Tree tree = treeViewer.getTree();
		Listener goToLineListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof CommentGroup) {
					selectedGroup = (CommentGroup) selected;					
					if (selectedGroup.getComments().size() >= 1) {
						// Navigate to the first comment in the group
						currentSelectedComment = 0;
						gotoCurrentSelectedComment();
					} else {
						currentSelectedComment = -1;
					}
				} else if (selected instanceof Comment) {
					Comment comment = (Comment) selected;
					selectedGroup = comment.getCommentGroup();
					currentSelectedComment = selectedGroup.getComments().indexOf(comment);
					gotoCurrentSelectedComment();
				}
				activeEditor.setFocus();
			}
		};
		tree.addListener(SWT.MouseDoubleClick, goToLineListener);
		// Tried adding SWT.CR and SWT.LF listeners, but those were leading to single-click
		// events firing onto them (likely due to single-click support in Eclipse).
		// If we can prevent that, we can add them back in...
		
		Menu menu = new Menu(tree);
		tree.setMenu(menu);
		menu.addMenuListener(this);
		
		// Menu item for editing comments or renaming comment groups
		editItem = new MenuItem(menu, SWT.CASCADE);
		editItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				
				if (selected instanceof CommentGroup) {
					CommentGroup group = (CommentGroup) selected;
					RenameCommentGroupDialog renameDialog = new RenameCommentGroupDialog(shell, group.getName());
					renameDialog.create();
					
					if (renameDialog.open() == Window.OK) {
						try {
							activeEditor.getProjectionViewer().getFileModel().renameCommentGroup(group, renameDialog.getName());
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error renaming comment group", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error renaming comment group", "Problem refreshing file's comments file", e);
						}
						updateView();
					}
				} else if (selected instanceof Comment) {
					Comment comment = (Comment) selected;
					AddOrEditCommentDialog editCommentDialog = new AddOrEditCommentDialog(shell, comment);
					editCommentDialog.create();
					
					if (editCommentDialog.open() == Window.OK) {
						try {
							activeEditor.getProjectionViewer().editComment(comment, editCommentDialog.getCommentGroup(), editCommentDialog.getCommentText());
							// Comment group may have changed, so update the selected comment group accordingly
							selectedGroup = comment.getCommentGroup();
							currentSelectedComment = selectedGroup.getComments().indexOf(comment);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error editing comment", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error editing comment", "Problem refreshing file's comments file", e);
						} catch (InvalidCommentLocationException e) {
							BigFileApplication.showInformationDialog("Unable to edit comment", "The comment's new group already has a comment at that location", e);
						}
						updateView();
					}
				}
			}
		});
		
		// Menu item for moving comments
		moveItem = new MenuItem(menu, SWT.CASCADE);
		moveItem.setText("Move");
		moveItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				
				if (selected instanceof Comment) {
					Comment comment = (Comment) selected;
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					MoveCommentDialog moveDialog = new MoveCommentDialog(shell, comment);
					moveDialog.create();
					
					if (moveDialog.open() == Window.OK) {
						try {
							activeEditor.getProjectionViewer().moveComment(comment, moveDialog.getLine(), moveDialog.getCharacter());
							// Move the highlighting to the new location
							currentSelectedComment = selectedGroup.getComments().indexOf(comment);
							gotoCurrentSelectedComment();
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error moving comment", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error moving comment", "Problem refreshing file's comments file", e);
						} catch (InvalidCommentLocationException e) {
							BigFileApplication.showInformationDialog("Unable to move comment", "The comment's group already has a comment at that location", e);
						}
						updateView(); // should do this in case there were changes to the order of the comments in the group
					}
				}
				
			}
		});
		
		// Menu item for colour coding comment groups
		colourItem = new MenuItem(menu, SWT.CASCADE);
		colourItem.setText("Choose Colour");
		colourItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof CommentGroup) {
					CommentGroup group = (CommentGroup) selected;
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					TooltipColourPicker colourPicker = new TooltipColourPicker(shell, CommentGroup.DEFAULT_COLOUR);
					colourPicker.setColour(group.getColour());
					colourPicker.create();
					
					if (colourPicker.open() == Window.OK) {
						try {
							activeEditor.getProjectionViewer().setColour(group, colourPicker.getColour());
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error setting comment group colour", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error setting comment group colour", "Problem refreshing file's comments file", e);
						}
					}
				}
			}
		});
		
		// Menu item for deleting comments or comment groups
		deleteItem = new MenuItem(menu, SWT.CASCADE);
		deleteItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (selected instanceof CommentGroup) {
					CommentGroup group = (CommentGroup) selected;
					boolean delete = MessageDialog.openQuestion(shell, "Delete Comment Group", 
							"This will delete comment group '" + group.getName() + "' and all comments in it. Are you sure you want to do this?");
					
					if (delete) {
						try {
							activeEditor.getProjectionViewer().deleteCommentGroup(group);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error deleting comment group", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error deleting comment group", "Problem refreshing file's comments file", e);
						}
						updateView();
						selectedGroup = null;
						currentSelectedComment = -1;
					}
				} else if (selected instanceof Comment) {
					Comment comment = (Comment) selected;
					boolean delete = MessageDialog.openQuestion(shell, "Delete Comment", "Are you sure you want to delete this comment?");
					
					if (delete) {
						try {
							activeEditor.getProjectionViewer().deleteComment(comment);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error deleting comment", "Could not update file's comments file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error deleting comment", "Problem refreshing file's comments file", e);
						}
						updateView();
						selectedGroup = null;
						currentSelectedComment = -1;
					}
				}
			}
		});
		
		// Enables this view to listen for when the active editor changes or is closed
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	/**
	 * Update this view with the current comments data from the File Editor
	 */
	public void updateView() {
		
		if(activeEditor == null) {
			return;
		}
		
		Collection<CommentGroup> groups = activeEditor.getProjectionViewer().getFileModel().getCommentGroups();
		treeViewer.setInput(groups);
		setInitiallyCheckedItems();
	}
	
	/**
	 * Set which tree items are initially checked based on the value of the item's showStickyTooltip property.
	 */
	private void setInitiallyCheckedItems() {
		Collection<CommentGroup> groups = activeEditor.getProjectionViewer().getFileModel().getCommentGroups();
		for (CommentGroup group : groups) {
			treeViewer.setChecked(group, group.getShowStickyTooltip());
			for (Comment comment : group.getComments()) {
				treeViewer.setChecked(comment, comment.getShowStickyTooltip());
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
	 * Selects and navigates to the previous comment in the currently selected comment group. If the first comment in the group was originally
	 * selected, this method will wrap to the last comment in the group.
	 */
	public void previousCommentInSelectedGroup() {
		if (selectedGroup != null && selectedGroup.getComments().size() >= 1) {
			currentSelectedComment--;
			if (currentSelectedComment < 0) {
				// Wrap around to the last comment if we were previously at the first one
				currentSelectedComment = selectedGroup.getComments().size() - 1;
			}
			treeViewer.setSelection(new StructuredSelection(selectedGroup.getComments().get(currentSelectedComment)), true);
//			gotoCurrentSelectedComment();
		}
	}
	
	/**
	 * Selects and navigates to the next comment in the currently selected comment group. If the last comment in the group was originally
	 * selected, this method will wrap to the first comment in the group.
	 */
	public void nextCommentInSelectedGroup() {
		if (selectedGroup != null && selectedGroup.getComments().size() >= 1) {
			currentSelectedComment++;
			if (currentSelectedComment >= selectedGroup.getComments().size()) {
				// Wrap around to the first comment if we were previously at the last one
				currentSelectedComment = 0;
			}
			treeViewer.setSelection(new StructuredSelection(selectedGroup.getComments().get(currentSelectedComment)), true);
//			gotoCurrentSelectedComment();
		}
	}
	
	/**
	 * Jumps to the current selected comment in the active File Editor.
	 */
	private void gotoCurrentSelectedComment() {
		Comment comment = selectedGroup.getComments().get(currentSelectedComment);
		ProjectionViewer viewer = (ProjectionViewer) activeEditor.getProjectionViewer().getViewer();
		StyledText textWidget = viewer.getTextWidget();
		IDocument document = viewer.getDocument();
		try {
			int offset = document.getLineOffset(comment.getLine()) + comment.getCharacter();
			activeEditor.getProjectionViewer().gotoLineAtOffset(comment.getLine(), comment.getCharacter());
			textWidget.setCaretOffset(offset);
			activeEditor.getProjectionViewer().setFocus(comment);
		} catch (Exception e) {
			BigFileApplication.showErrorDialog("Error navigating to comment", "Error while navigating to comment at line " + (comment.getLine() + 1) + 
					" char " + (comment.getCharacter() + 1), e);
		}
	}

	@Override
	public void menuHidden(MenuEvent e) {}

	@Override
	public void menuShown(MenuEvent e) {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		Object selected = selection.getFirstElement();
		editItem.setText("Edit");
		editItem.setEnabled(selected != null);
		moveItem.setEnabled(selected instanceof Comment);
		colourItem.setEnabled(selected instanceof CommentGroup);
		deleteItem.setText("Delete");
		deleteItem.setEnabled(selected != null);
		
		// Set the menu item's text depending on whether the selected item is a comment group or a comment
		if (selected instanceof CommentGroup) {
			editItem.setText("Rename");
			deleteItem.setText("Delete Comment Group");
			
			// Special case: if the "no group" comment group is selected, don't allow renaming or deleting it
			CommentGroup group = (CommentGroup) selected;
			if (CommentGroup.NO_GROUP.equals(group.getName())) {
				editItem.setEnabled(false);
				deleteItem.setEnabled(false);
			}
		} else if (selected instanceof Comment) {
			editItem.setText("Edit");
			deleteItem.setText("Delete Comment");
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
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getCommentGroups());
			setInitiallyCheckedItems();			
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		// If the active File Editor was the part that was closed, remove its comments data from this view
		if (activeEditor != null && activeEditor == part) {
			activeEditor = null;
			treeViewer.setInput(null);
			selectedGroup = null;
			currentSelectedComment = -1;
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
