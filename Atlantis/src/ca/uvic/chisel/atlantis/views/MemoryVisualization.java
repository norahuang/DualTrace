package ca.uvic.chisel.atlantis.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.datacache.AsyncResult;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class MemoryVisualization extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.atlantis.views.MemoryVisualization";

	private TableViewer viewer;
	private Action copyItemAction;
	private Action jumpAction;
	private Action watchItemAction;

	private List<String> watchedLocations = new LinkedList<String>();

	private AtlantisTraceEditor activeTraceDisplayer;

	private AtlantisFileModelDataLayer fileModel;

	private Table table;

	private MemoryQueryResults newMemoryReferencesResult;
	
	Job lastUpdateJob;
	int lastUpdateJobLineNumber = -1;

	private String currentAddress;

	private final static Color GREEN = new Color(null, 180, 255, 155);
	private final static Color PINK = new Color(null, 255, 155, 180);
	private final static Color WHITE = new Color(null, 255, 255, 255);
	
	/**
	 * This label gets filled with whatever file and line are loaded into the view
	 */
	private Label loadedMemoryLabel;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		loadedMemoryLabel = new Label(parent, SWT.NONE);
		loadedMemoryLabel.setText("");
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		loadedMemoryLabel.setLayoutData(gridData);
		
		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Search Addresses: ");
		final Text searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		searchText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(searchText.getText().isEmpty()){
					searchText.setBackground(WHITE);
					return;
				}
				int index = findAndSelectAddress(searchText.getText());
				if (index != -1) {
					viewer.getTable().setSelection(index);
					searchText.setBackground(GREEN);
				} else {
					searchText.setBackground(PINK);
				}
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					viewer.getTable().setFocus();
				}
			}
		});
		createViewer(parent);
		createContextMenu();
		
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
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
		mgr.add(copyItemAction);
		mgr.add(jumpAction);
		mgr.add(watchItemAction);
	}

	public void createActions() {
		copyItemAction = new Action("Copy") {
			@Override
			public void run() {
				Clipboard cb = new Clipboard(getViewer().getControl().getDisplay());
				ISelection selection = getViewer().getSelection();

				if (selection != null && selection instanceof IStructuredSelection) {
					List<Entry<String, MemoryReference>> referenceList = createMemoryReferenceList((IStructuredSelection) selection);

					StringBuilder sb = new StringBuilder();
					for (Entry<String, MemoryReference> reference : referenceList) {
						sb.append(reference.getKey());
						sb.append(" ");
						sb.append(reference.getValue().getMemoryContent());
						sb.append(" ");
						sb.append(convertHexToString(reference.getValue().getMemoryContent().getMemoryValue()));
					}

					TextTransfer textTransfer = TextTransfer.getInstance();
					String clipBoardContents = sb.toString();

					if (clipBoardContents == null || clipBoardContents.isEmpty()) {
						return;
					}

					cb.setContents(new Object[] { clipBoardContents }, new Transfer[] { textTransfer });
				}
			}
		};

		watchItemAction = new Action("Watch") {
			@Override
			public void run() {
				ISelection selection = getViewer().getSelection();
				List<Entry<String,MemoryReference>> referenceList;
				if (selection != null && selection instanceof IStructuredSelection) {
					referenceList = createMemoryReferenceList((IStructuredSelection) selection);
					for (Entry<String, MemoryReference> reference : referenceList) {
						getWatchedLocations().add(reference.getKey());
					}
				}
				
				updateWatchedLocations();
			}
		};

		jumpAction = new Action("Jump To") {
			@Override
			public void run() {
				ISelection selection = getViewer().getSelection();

				List<Entry<String,MemoryReference>> referenceList;

				if (selection != null && selection instanceof IStructuredSelection) {
					referenceList = createMemoryReferenceList((IStructuredSelection) selection);
					if (referenceList.isEmpty()) {
						return;
					}
					try {
						int lineNumber = referenceList.get(0).getValue().getLineNumber();

						AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().getActiveEditor();
						if (activeTraceDisplayer != null) {
							activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(lineNumber, 0);
						}
					} catch (Exception ex) {
						System.err.println("Error getting line number for memory reference " + ex.toString());
						ex.printStackTrace();
					}
				}
			}
		};
	}

	private void createViewer(Composite parent) {
		
		table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL 
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
		table.setItemCount(0);
		viewer = new TableViewer(table);
		
		createColumns(parent, viewer);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		viewer.setContentProvider(new ArrayContentProvider());
		
		viewer.setInput(ModelProvider.INSTANCE.getMemoryList());
		
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
				Entry<Long, MemoryReference> p = (Entry<Long, MemoryReference>) element;
				return (p == null) ? "" : BinaryFormatParser.toHex(p.getKey());
			}
		});
		
		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<Long, MemoryReference> p = (Entry<Long, MemoryReference>) element;
				return (p == null) ? "" : (p.getValue().getLineNumber() + 1)+"";
			}
		});

		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<Long, MemoryReference> p = (Entry<Long, MemoryReference>) element;
				return (p == null) ? "" : p.getValue().getMemoryContent().getMemoryValue();
			}
		});

		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<String, MemoryReference> p = (Entry<String, MemoryReference>) element;
				
				if(p == null) {
					return "";
				}
				
				String temp = convertHexToString(p.getValue().getMemoryContent().getMemoryValue());
				return temp;
			}
		});
	}

	/**
	 * Utility method to convert a given hex string to ascii
	 * 
	 * @param hex
	 * @return
	 */
	public static String convertHexToString(String hex) {
		StringBuilder sb = new StringBuilder();
		hex = hex.replaceAll("\\?", "");
		
		while (hex.startsWith("00")) {
			hex = hex.substring(2);
		}
		
		// TODO it looks like the occasionally use 16 bit encodings in their example files.
		// We should see if we can detect such a situation and adjust accordingly.
		// 49204c6f7665204a617661 will be split into two characters 49, 20, 4c...
		for (int i = 0; i < hex.length(); i += 2) {
			try {
				// grab the hex in pairs
				String output = hex.substring(i, i + 2);
				output = output.replace("(", "");
				output = output.replace(")", "");
				
				// convert hex to decimal
				int decimal = Integer.parseInt(output, 16);
				
				if(decimal == 0) {
					sb.append("\\0");
				} else {
					sb.append((char) decimal);
				}
				
				// convert the decimal to character
			} catch (NumberFormatException ex) {
				System.out.println("Error converting to ACSII " + ex.toString());
			}
		}

		return sb.toString();
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

	/** * Passing the focus request to the viewer's control. */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/**
	 * Allows canceling of UI updates when they are stale; it takes time to get the memory results, and the
	 * UI flickers and takes time to resolve when we move quickly (like with scroll keys).
	 */
	private int mostRecentRequestedLineNumberForCancellation = -1;
	/**
	 * This method will parse memory events form each line of the trace from the
	 * start to the given line number.
	 */
	public void updateMemoryView() {	
		// When only done in partActivate(), it fails if the part is hidden behind other things!
		// So that whole approach seen in a lot of views is faulty! Oh no!
		// TODO Fix this incorrect approach all over the place
		if (activeTraceDisplayer == null) {
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
			fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
		}

		int topIndex = table.getTopIndex();
		currentAddress = null;
		
		if(table.getItemCount() > 0 && table.getItem(topIndex) != null) {
			TableItem topItem  = table.getItem(topIndex);
			Map.Entry<String, MemoryReference> data = (Map.Entry<String, MemoryReference>)topItem.getData();
			
			if(data != null && data.getValue() != null) {
				currentAddress = data.getValue().getAddressAsHexString();
			}
		}

		final int lineNumber = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber();
		
		// execRec flag, lowest bit, tells us whether the instruction was in
		// 64-bit (Long Mode). 0x0 or false is 32-bit Protected Mode.
		// Don't fetch the entire ExecRec, just request this bit of information
		final boolean is64BitExecutionLine = fileModel.isExecutionMode64Bit(lineNumber);
		
		// This allows us to cancel queries and processing of results separately.
		mostRecentRequestedLineNumberForCancellation = lineNumber;
		
		loadedMemoryLabel.setText("After line "+(lineNumber+1)+" in "+fileModel.getOpenFileRelativePath().toString());
				
		Job memoryUpdateJob = new Job("Memory Update Job"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// These will cancel themselves and return null if cancelled
				final AsyncResult<MemoryQueryResults> newMemoryEventsAsync = fileModel.getMemoryEventsAsync(lineNumber+1, monitor);
				
				if(newMemoryEventsAsync.isCancelled()) {
					return Status.CANCEL_STATUS;
				}
				
				if(!newMemoryEventsAsync.isCancelled() && mostRecentRequestedLineNumberForCancellation == lineNumber){
		  			  clearDataContents();
				}
								
				newMemoryReferencesResult = newMemoryEventsAsync.getResult();
				
				Display.getDefault().asyncExec(new Runnable() {
				      @Override
				      public void run() {
				  		if(!newMemoryEventsAsync.isCancelled() && mostRecentRequestedLineNumberForCancellation == lineNumber){
//				  		  clearGraphicalContents(); // text removing this, see if it matters to behavior...seems to improve UI responsivity a lot when removed
							visualRefreshAfterUpdateMemoryView(lineNumber, is64BitExecutionLine);
				  		}
				      }
			    });
				
				return Status.OK_STATUS;
			}
		};
		
		if(lastUpdateJob != null && (lastUpdateJob.getState() == Job.RUNNING || lastUpdateJob.getState() == Job.WAITING)) {
			lastUpdateJob.cancel();
			boolean cancelled = fileModel.cancelMemoryRegisterEventsAsync(lastUpdateJobLineNumber);
		}
		
		if(lastUpdateJobLineNumber != lineNumber){
			memoryUpdateJob.schedule();
			lastUpdateJob = memoryUpdateJob;
			lastUpdateJobLineNumber = lineNumber;
		}
		
	}
	
	/**
	 * One data for memory view is retrieved, 
	 * @param lineNumber
	 */
	private void visualRefreshAfterUpdateMemoryView(int lineNumber, boolean is64BitExecutionLine){
//		table.setItemCount(memoryReferencesResult.size());
		
		ModelProvider.INSTANCE.setMemoryQueryResults(newMemoryReferencesResult, lineNumber);
		
		// Might want to leave to situation where search, etc., is performed, so that the time cost
		// is not paid on each memory update
		// Might also want to move this closer to MemoryDBConnection.getMemRefsAsync(), where the rubber meets the road.
		// That is counter to the idea of lazy-loading this for search only when needed by search.
		newMemoryReferencesResult.collateMemoryAndRegisterResults(lineNumber);
		
		// update the watched locations
		updateWatchedLocations();
		updateTopIndex();
		
		this.getViewer().refresh();
				
		RegistersView registersView = (RegistersView) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RegistersView.ID);
		if(registersView != null) {
			registersView.setCurrentLine64BitExecutionMode(is64BitExecutionLine);
			registersView.refresh(); // this gets called when the new memory data is actually available, good.
		}
		
		// update the hex view
		HexVisualization hexView = (HexVisualization) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(HexVisualization.ID);
		if(hexView != null) {
			// recompute the info for the hex view
			// we only need to do this here
			hexView.rebuildMemoryInfo();
			
			// refresh the display
			hexView.refresh();
		}
	}


	private void updateTopIndex() {
		
		if(currentAddress == null) {
			return;
		}
		
		int finalIndex = -1;
		int count = 0;
		
		for(Entry<Long, MemoryReference> kvp : ModelProvider.INSTANCE.getMemoryList()) {
			int compareResult = kvp.getKey().compareTo(Long.parseLong(currentAddress, 16));
			
			if(compareResult == 0) {
				finalIndex = count;
				break;
			} else if(compareResult > 0) {
				finalIndex = Math.min(0, count - 1);
				break;
			}
			count++;
		}
		
		table.setTopIndex(finalIndex);
		
	}

	public void updateWatchedLocations() {
		// clear out the watched map so that old values that are not reset don't stay
		ModelProvider.INSTANCE.clearWatchedEntries();
		ModelProvider.INSTANCE.updateWatchedEntries(getWatchedLocations());
		
		WatchedView.refreshCurrentWatchedView();
	}

	public List<String> getWatchedLocations() {
		return watchedLocations;
	}

	// TODO this implementation isn't good.  There isn't even partial matching.
	public int findAndSelectAddress(String address) {
		// DOn't really need to search the map at all...
		// TreeMap<String, MemoryReference> map = ModelProvider.INSTANCE.getMemoryMap();
		TableItem[] items = viewer.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getText(0).startsWith(address)) {
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings({ "rawtypes"})
	public static List<Entry<String,MemoryReference>> createMemoryReferenceList(IStructuredSelection selection) {
		List<Entry<String, MemoryReference>> referenceList = new ArrayList<>();
		Iterator<?> itr = selection.iterator();
		while (itr.hasNext()) {
			@SuppressWarnings("unchecked")
			Entry<String, MemoryReference> entry = (Entry<String, MemoryReference>) itr.next();
			referenceList.add(entry);
		}
		((ArrayList) referenceList).trimToSize();
		return referenceList;
	}
	
	public void clearContents(){
		clearDataContents();
		clearGraphicalContents();
	}

	public void clearDataContents() {	
		// clear whats in the memorymap
		ModelProvider.INSTANCE.clearMemoryAndRegisterData();
		// used to think I needed to clearRegisterEntries() here too, but didn't.
		
	}
	
	public void clearGraphicalContents(){
		table.removeAll();
		
		this.getViewer().refresh();
		
		// update the registers view
		RegistersView registersView = (RegistersView) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RegistersView.ID);
		if(registersView != null) {
			registersView.refresh(); // suspicious, this does not occur before new memory is available, and it does more work than just clearing...
		}
		
		// update the hex view
		HexVisualization hexView = (HexVisualization) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(HexVisualization.ID);
		if(hexView != null) {
			hexView.refresh();
		}
		
		// update the watched view
		WatchedView.refreshCurrentWatchedView();
	}
	
	public static void removeWatchedLocation(String watchedLocation) {
		MemoryVisualization memoryView = (MemoryVisualization) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(MemoryVisualization.ID);
		
		if(memoryView == null)  {
			return; 
		}
		
		memoryView.getWatchedLocations().remove(watchedLocation);
		WatchedView.refreshCurrentWatchedView();
	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				if (part != activeTraceDisplayer) { // don't need to redraw the visualization if the active Trace Displayer hasn't changed
					activeTraceDisplayer = (AtlantisTraceEditor) part;
					fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
					activeTraceDisplayer.clearMemoryViewContents();
					activeTraceDisplayer.syncMemoryVisualization(true);
				}
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (activeTraceDisplayer != null && activeTraceDisplayer == part) {
			activeTraceDisplayer = null;
			clearDataContents();
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partHidden(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partOpened(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partVisible(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose() {
		this.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
}
