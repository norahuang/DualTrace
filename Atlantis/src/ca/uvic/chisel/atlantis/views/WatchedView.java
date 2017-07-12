package ca.uvic.chisel.atlantis.views;

import static ca.uvic.chisel.atlantis.views.MemoryVisualization.convertHexToString;
import static ca.uvic.chisel.atlantis.views.MemoryVisualization.createMemoryReferenceList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.BigFileApplication;

public class WatchedView extends ViewPart {

	public static final String ID = "ca.uvic.chisel.atlantis.views.WatchedView";
	
	private TableViewer viewer;
	
	private Action addItemAction;
	private Action jumpAction;
	private Action unWatchItemAction;
	
	private Table table;
	
	public WatchedView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		createViewer(parent);
		createContextMenu();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	private void createContextMenu() {

		createActions();

		// Create menu manager.
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});

		// Create menu.
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager mgr) {
		mgr.add(addItemAction);
		mgr.add(jumpAction);
		mgr.add(unWatchItemAction);
	}

	public void createActions() {
		addItemAction = new Action("Copy") {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(WatchedView.ID);
				Clipboard cb = new Clipboard(Display.getDefault());
				ISelection selection = view.getSite().getSelectionProvider().getSelection();

				if (selection != null && selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					List<Entry<String, MemoryReference>> referenceList = createMemoryReferenceList(sel);

					StringBuilder sb = new StringBuilder();
					for (Entry<String, MemoryReference> reference : referenceList) {
						sb.append(reference.getKey());
						sb.append(" ");
						sb.append(reference.getValue().getMemoryContent());
						sb.append(" ");
						sb.append(MemoryVisualization.convertHexToString(reference.getValue().getMemoryContent().getMemoryValue()));
						sb.append("\n");
					}

					TextTransfer textTransfer = TextTransfer.getInstance();
					cb.setContents(new Object[] { sb.toString() },
							new Transfer[] { textTransfer });
				}
			}
		};

		unWatchItemAction = new Action("Un-Watch") {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(WatchedView.ID);
				ISelection selection = view.getSite().getSelectionProvider().getSelection();
				if (selection != null && selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					List<Entry<String, MemoryReference>> referenceList = createMemoryReferenceList(sel);
					for (Entry<String, MemoryReference> reference : referenceList) {
						ModelProvider.INSTANCE.removeWatchedEntry(reference.getKey());
						MemoryVisualization.removeWatchedLocation(reference.getKey());
					}
					refreshCurrentWatchedView();
				}
			}
		};	
		
		jumpAction = new Action("Jump To") {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(WatchedView.ID);
				ISelection selection = view.getSite().getSelectionProvider()
						.getSelection();

				ArrayList<MemoryReference> referenceList = new ArrayList<MemoryReference>();

				if (selection != null
						&& selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;

					for (@SuppressWarnings("unchecked")
					Iterator<Entry<String, MemoryReference>> iterator = sel
							.iterator(); iterator.hasNext();) {
						Entry<String, MemoryReference> temp = iterator
								.next();
						MemoryReference reference = temp.getValue();
						referenceList.add(reference);
					}

					try {
						int lineNumber = referenceList.get(0).getLineNumber();

						AtlantisTraceEditor activeTraceDisplayer;
						IEditorPart editor = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage()
								.getActiveEditor();
						if (editor instanceof AtlantisTraceEditor) {
							activeTraceDisplayer = (AtlantisTraceEditor) editor;
							if (activeTraceDisplayer != null) {
								try {
									activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(lineNumber, 0);
								} catch (BadLocationException e) {
									BigFileApplication
											.showErrorDialog(
													"Error synchronizing execution sequence view",
													"Unable to navigate to selected line",
													e);
								}
							}
						}
					} catch (Exception ex) {
						System.out
								.println("Error getting line number for memory reference "
										+ ex.toString());
					}
				}
			}
		};
	}

	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns(parent, viewer);
		table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		// Get the content for the viewer, setInput will call getElements in the
		// contentProvider
		viewer.setInput(ModelProvider.INSTANCE.getWatchedList());
		// Make the selection available to other views
		getSite().setSelectionProvider(viewer);
		// Set the sorter for the table

		// Layout the viewer
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
	}

	public TableViewer getViewer() {
		return viewer;
	}

	// This will create the columns for the table
	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = { "Address", "Line", "Value", "ASCII" };
		int[] bounds = { 69, 67, 69, 78 }; // Trying to prevent horizontal scrollbar with default size

		// First column is for the memory address affected by the instruction
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<String, MemoryReference> p = (Entry<String, MemoryReference>) element;
				return p.getKey();
			}
		});
		
		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<String, MemoryReference> p = (Entry<String, MemoryReference>) element;
				return (p == null) ? "" : (p.getValue().getLineNumber() + 1)+"";
			}
		});

		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<String, MemoryReference> p = (Entry<String, MemoryReference>) element;
				return p.getValue().getMemoryContent().getMemoryValue();
			}
		});

		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<String, MemoryReference> p = (Entry<String, MemoryReference>) element;
				String temp = convertHexToString(p.getValue().getMemoryContent().getMemoryValue());
				return temp;
			}
		});
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int bound,
			final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	
	public static void refreshCurrentWatchedView() {
		WatchedView watchedView = (WatchedView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(WatchedView.ID);
		
		if(watchedView == null)  {
			return; 
		}
		
		watchedView.getViewer().refresh();
	}

	public void clearContents() {
		ModelProvider.INSTANCE.clearWatchedEntries();
		table.removeAll();
		this.getViewer().refresh();
	}
}
