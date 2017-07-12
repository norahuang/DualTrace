package ca.uvic.chisel.bfv.views;

import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.StickyTooltip;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

/**
 * View for turning the comments or tags layers of the active file on and off.
 * Turning on the comments layer shows the sticky tooltips for the comments in the active file; likewise with the tags layer.
 * Turning off these layers hides the sticky tooltips.
 * @see StickyTooltip
 * @author Laura Chan
 */
public class LayersView extends ViewPart implements IPartListener2 {
	public static final String ID = "ca.uvic.chisel.bfv.views.LayersView";
	
	private BigFileEditor activeEditor;
	
	private Button commentsCheck;
	private Button tagsCheck;
	
	@Override
	public void createPartControl(Composite parent) {
		RowLayout layout = new RowLayout(2);
		parent.setLayout(layout);
		
		commentsCheck = new Button(parent, SWT.CHECK);
		commentsCheck.setText("Comments");
		commentsCheck.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (activeEditor != null) {
					activeEditor.getProjectionViewer().setShowCommentTooltips(commentsCheck.getSelection());
					showOrHideCommentTooltips();
					activeEditor.setFocus();
				}
			}
		});
		
		tagsCheck = new Button(parent, SWT.CHECK);
		tagsCheck.setText("Tags"); 
		tagsCheck.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (activeEditor != null) {
					activeEditor.getProjectionViewer().setShowTagTooltips(tagsCheck.getSelection());
					showOrHideTagTooltips();
					activeEditor.setFocus();
				}
			}
		});
		
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	@Override
	public void setFocus() {
		commentsCheck.setFocus();
	}
	
	@Override
	public void dispose() {
		this.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	/**
	 * Returns whether the comments check in this view is selected
	 * @return true if the comments check is selected, false otherwise
	 */
	public boolean commentsSelected() {
		return commentsCheck.getSelection();
	}
	
	/**
	 * Returns whether the tags check in this view is selected
	 * @return true if the tags check is selected, false otherwise
	 */
	public boolean tagsSelected() {
		return tagsCheck.getSelection();
	}
	
	/**
	 * Make the active File Editor show or hide its sticky tooltips for comments, according to whether the 
	 * comments check is selected in this view.
	 */
	private void showOrHideCommentTooltips() {
		if (commentsCheck.getSelection()) {
			activeEditor.getProjectionViewer().showCommentTooltips();
		} else {
			activeEditor.getProjectionViewer().hideCommentTooltips();
		}
	}
	
	/**
	 * Make the active File Editor show or hide its sticky tooltips for tags, according to whether the
	 * tags check is selected in this view.
	 */
	private void showOrHideTagTooltips() {
		if (tagsCheck.getSelection()) {
			activeEditor.getProjectionViewer().showTagTooltips();
		} else {
			activeEditor.getProjectionViewer().hideTagTooltips();
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if(editor instanceof BigFileEditor) {
			BigFileEditor newEditor  = (BigFileEditor) editor;
			
			if(newEditor == activeEditor) {
				return;
			}
			activeEditor = newEditor;		
			commentsCheck.setSelection(activeEditor.getProjectionViewer().isShowingCommentTooltips());
			showOrHideCommentTooltips();
			tagsCheck.setSelection(activeEditor.getProjectionViewer().isShowingTagTooltips());			
			showOrHideTagTooltips();
		} else {
			commentsCheck.setSelection(false);
			tagsCheck.setSelection(false);
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (activeEditor != null && activeEditor == part) {
			activeEditor = null;
			commentsCheck.setSelection(false);
			tagsCheck.setSelection(false);
		} // else, something else was closed, so we don't need to do anything
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (activeEditor != null && activeEditor == part) {
			// Hide any sticky tooltips
			activeEditor.getProjectionViewer().hideCommentTooltips();
			activeEditor.getProjectionViewer().hideTagTooltips();
			
			activeEditor = null;
			commentsCheck.setSelection(false);
			tagsCheck.setSelection(false);
		} // else, something else was hidden, so we don't need to do anything
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {}
}
