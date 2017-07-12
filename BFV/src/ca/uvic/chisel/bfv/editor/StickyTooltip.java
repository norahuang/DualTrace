package ca.uvic.chisel.bfv.editor;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.views.CommentsView;
import ca.uvic.chisel.bfv.views.TagsView;

/**
 * Sticky tooltips display tags and comments in separate "layers" above the file contents. This allows users to add or view
 * locations of interest and commentary without needing to modify the file itself, since files are not to be edited.
 * Tooltips can be made editable (which is allowed for comments, but not tags), allowing the user to directly modify the
 * tooltip's text and update the text of the underlying comment.
 * 
 * As their name implies, sticky tooltips do not disappear when the mouse is moved or clicked or if a key is pressed--they must
 * be shown and hidden manually. Unlike with normal tooltips, more than one sticky tooltip can be visible at the same time. 
 * 
 * @author Laura Chan
 */
public class StickyTooltip extends DefaultToolTip implements MenuListener {

	private Control control;
	private Shell tooltip;
	private Text textWidget;
	private boolean editable;
	private boolean textChanged;
	private Object type;
	private MenuItem hide, delete;
	
	/**
	 * Creates a new sticky tooltip for the specified control.
	 * @param control the control in which the sticky tooltip will be displayed when made visible
	 * @param editable whether or not the tooltip's text should be editable. This should be true for comment tooltips and false for tag tooltips.
	 */
	public StickyTooltip(Control control, boolean editable, Object type) {
		super(control);
		super.setHideOnMouseDown(false);
		this.control = control;
		tooltip = null;
		textWidget = null;
		this.editable = editable;
		textChanged = false;
		this.type = type;
	}
	
	/**
	 * Does nothing; this prevents creating two tooltips (one from this and the ordinary one from BigFileViewerConfiguration)
	 * every time we mouse over an annotation
	 */
	@Override
	public void activate() {}
	
	/**
	 * Does nothing; this prevents creating two tooltips (one from this and the ordinary one from BigFileViewerConfiguration)
	 * every time we mouse over an annotation
	 */
	@Override
	public void deactivate() {}
	
	/**
	 * Returns whether this sticky tooltip is currently visible
	 * @return true if visible, false otherwise
	 */
	public boolean isVisible() {
		return tooltip != null && !tooltip.isDisposed() && tooltip.isVisible();
	}
	
	/**
	 * Does nothing since hideOnMouseDown is always going to be false.
	 */
	@Override
	public void setHideOnMouseDown(boolean hideOnMouseDown) {}
	
	/**
	 * Always returns false for this implementation.
	 */
	@Override
	public boolean isHideOnMouseDown() {
		return false;
	}
	
	public void updateColor() {
		
		if(textWidget == null || textWidget.isDisposed()) {
			return;
		}
		
		Event nullEvent = new Event();
		nullEvent.widget = control;
		
		Color fgColor = getForegroundColor(nullEvent);
		Color bgColor = getBackgroundColor(nullEvent);
		
		if (fgColor != null) {
			tooltip.setForeground(fgColor);
			textWidget.setForeground(fgColor);
		}

		if (bgColor != null) {
			tooltip.setBackground(bgColor);
			textWidget.setBackground(bgColor);
		}
	}
	
	public void updateText(String newText) {
		
		if(textWidget == null || textWidget.isDisposed()) {
			return;
		}
		
		Event nullEvent = new Event();
		nullEvent.widget = control;
		// Cannot use getText() for this....I am not sure why!
		// I believe it is due to race conditions when this method is used.
		tooltip.setText(newText);
		textWidget.setText(newText);
	}
	
	/**
	 * Creates the content area of this tooltip. The content area is composed of a text widget that will allow the user to change 
	 * the tooltip's text if this tooltip is editable. Any changes to the text will be saved as soon as this tooltip loses focus.
	 */
	@Override
	protected Composite createToolTipContentArea(Event event, final Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		parent.addFocusListener(new FocusListener() { // Transfers focus to the text widget when the user clicks on any part of the tooltip
			@Override
			public void focusGained(FocusEvent e) {
				textWidget.setFocus();
			}

			@Override
			public void focusLost(FocusEvent e) {
				// Nothing to do here...
			}
		});
		
		// Following lines adapted from org.eclipse.jface.window.DefaultToolTip.createToolTipContentArea()
		Color fgColor = getForegroundColor(event);
		Color bgColor = getBackgroundColor(event);
		if (fgColor != null) {
			parent.setForeground(fgColor);
		}
		if (bgColor != null) {
			parent.setBackground(bgColor);
		}
		
		// Text widget for this sticky tooltip
		textWidget = new Text(parent, SWT.MULTI);
		textWidget.setEditable(editable);
		textWidget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Following lines adapted from org.eclipse.jface.window.DefaultToolTip.createToolTipContentArea()
		String text = getText(event);
		Font font = getFont(event);
		if (text != null) {
			textWidget.setText(text);
		}
		if (fgColor != null) {
			textWidget.setForeground(fgColor);
		}

		if (bgColor != null) {
			textWidget.setBackground(bgColor);
		}
		if (font != null) {
			textWidget.setFont(font);
		}
		
		Menu menu = new Menu(textWidget);
		textWidget.setMenu(menu);
		menu.addMenuListener(this);
		
		hide = new MenuItem(menu, SWT.CASCADE);
		hide.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				BigFileEditor editor = 
						(BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				try {
					if(type instanceof Comment) {
						editor.getProjectionViewer().showOrHideStickyTooltip((Comment) type, false);
						((CommentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(CommentsView.ID)).updateView();
					} else if(type instanceof TagOccurrence) {
						editor.getProjectionViewer().showOrHideStickyTooltip((TagOccurrence) type, false);
						((TagsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(TagsView.ID)).updateView();
					}
				} catch (JAXBException | CoreException e) {
					e.printStackTrace();
				}
			}
		});
		hide.setText("Hide");
		
		delete = new MenuItem(menu, SWT.CASCADE);
		delete.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				BigFileEditor editor = 
						(BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				try {
					boolean delete = MessageDialog.openQuestion(shell, "Delete selected item", 
							"Are you sure you want to delete the selected item?");
					if(type instanceof Comment && delete) {
						editor.getProjectionViewer().deleteComment((Comment) type);
						((CommentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(CommentsView.ID)).updateView();
					} else if(type instanceof TagOccurrence && delete) {
						editor.getProjectionViewer().deleteTagOccurrence((TagOccurrence) type);
						((TagsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(TagsView.ID)).updateView();
					}
				} catch (JAXBException | CoreException e) {
					e.printStackTrace();
				}
			}
		});
		delete.setText("Delete");
		
		// If there is text highlighted, this listener will make it so that it isn't highlighted anymore after the text widget loses focus.
		// More importantly, if the tooltip is editable, this listener saves any changes when the tooltip loses focus
		textWidget.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				
			}

			@Override
			public void focusLost(FocusEvent event) {
				if (editable && textChanged) {
					// Update the associated comment.
					BigFileEditor editor = 
							(BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					try {
						if(type instanceof Comment) {
							editor.getProjectionViewer().editComment(StickyTooltip.this);
							textChanged = false;
						}
						if(type instanceof TagOccurrence) {
							editor.getProjectionViewer().editTag(StickyTooltip.this);
							textChanged = false;
						}
					} catch (JAXBException e) {
						BigFileApplication.showErrorDialog("Error editing comment", "Could not update file's comments file", e);
					} catch (CoreException e) {
						BigFileApplication.showErrorDialog("Error editing comment", "Problem refreshing file's comments file", e);
					} 
				}				
				// Removes any text highlighting
				textWidget.setSelection(0, 0);
			}
		});
			
		if (editable) {	
			textWidget.addModifyListener(new ModifyListener() { // resizes the sticky tooltip to fit the text as the text grows or shrinks
				@Override
				public void modifyText(ModifyEvent e) {
					textChanged = true;
					parent.pack(); // resizes the tooltip
				}
			});
		}
		return parent;
	}
	
	/**
	 * Makes this tooltip visible. Clients must call this method to make the tooltip appear.
	 */
	@Override
	public void show(Point location) {
		
		Point displayLocation = new Point(location.x + 10, location.y + 70);
		
		if(tooltip == null || tooltip.isDisposed()) {
			initializeToolTip(displayLocation);
		}
		
		tooltip.setLocation(displayLocation);
		tooltip.setVisible(true);
	}

	private void initializeToolTip(Point location) {
		// Following lines copied from the method in org.eclipse.jface.window.ToolTip that this method overrides
		Event event = new Event();
		
		event.x = location.x;
		event.y = location.y;
		event.widget = this.control;
		
		// Following lines adapted from org.eclipse.jface.window.ToolTip.toolTipCreate()
		tooltip = new Shell(control.getShell(), SWT.TOOL);
		tooltip.setLayout(new FillLayout());
		
		// Following lines adapted from org.eclipse.jface.window.ToolTip.toolTipShow()
		this.createToolTipContentArea(event, tooltip);
		tooltip.pack();
	}
	
	/**
	 * Hides this tooltip. Clients must call this method to make the tooltip disappear (and for its underlying widgets to be disposed).
	 */
	@Override
	public void hide() {
		if (tooltip != null && !tooltip.isDisposed()) {
			tooltip.setVisible(false);
		}
	}
	
	/**
	 * Causes this tooltip to receive focus.
	 * @return true if this tooltip got focus, false otherwise
	 */
	public boolean setFocus() {
		if (this.isVisible()) {
			// TODO: is there a good way of making the focused tooltip more obvious?
			return tooltip.setFocus();
		} else {
			return false;
		}
	}
	
	/**
	 * Returns this tooltip's text
	 * @return the tooltip text
	 */
	public String getText() {		
		return textWidget == null ? "" : textWidget.getText();
	}

	@Override
	public void menuHidden(MenuEvent e) {
				
	}

	@Override
	public void menuShown(MenuEvent e) {
		if(type instanceof Comment) {
			delete.setText("Delete Comment");
		} else if(type instanceof TagOccurrence) {
			delete.setText("Delete Occurrence");
		}
	}
}
