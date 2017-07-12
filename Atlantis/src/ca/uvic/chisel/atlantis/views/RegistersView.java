package ca.uvic.chisel.atlantis.views;

import static ca.uvic.chisel.atlantis.views.MemoryVisualization.createMemoryReferenceList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.controls.FlagsControl;
import ca.uvic.chisel.atlantis.controls.RegisterControl;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.handlers.Force64BitRegistersHandler;
import ca.uvic.chisel.atlantis.handlers.ShowAdditionalRegistersHandler;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class RegistersView extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.atlantis.views.RegistersView";
	
	private TableViewer viewer;
	
	private Action addItemAction;
	private Action jumpAction;
	
	private Map<String, RegisterControl> registerControls;
	private Map<String, RegisterControl> additionalRegisterControls;
	private FlagsControl flagsControl;
	
	private long currentRegistersLine = -1;
	private boolean is64BitExecution = false;
	
	private ScrolledComposite scrollParent;

	private Composite registersContainer;
	
	private AtlantisTraceEditor activeTraceDisplayer;

	private AtlantisFileModelDataLayer fileModel;
	
	public RegistersView() {
		registerControls = new HashMap<String, RegisterControl>();
		additionalRegisterControls = new HashMap<String, RegisterControl>();
	}

	@Override
	public void createPartControl(Composite parent) {
		FillLayout layout = new FillLayout();
		parent.setLayout(layout);
		
		createControls(parent);
		createContextMenu();
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	@Override
	public void setFocus() {
		//viewer.getControl().setFocus();
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
		//Menu menu = menuMgr.createContextMenu(viewer.getControl());
		//viewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager mgr) {
		mgr.add(addItemAction);
		mgr.add(jumpAction);
	}

	public void createActions() {
		addItemAction = new Action("Copy") {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(RegistersView.ID);
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

		jumpAction = new Action("Jump To") {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(RegistersView.ID);
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
						if (activeTraceDisplayer != null) {
							try {
								activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(lineNumber - 1, 0);
							} catch (BadLocationException e) {
								BigFileApplication
										.showErrorDialog(
												"Error synchronizing execution sequence view",
												"Unable to navigate to selected line",
												e);
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
	
	
	
	public void createControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		scrollParent = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
		scrollParent.setLayout(new FillLayout());
		scrollParent.setExpandHorizontal(true);
		scrollParent.setExpandVertical(true);
		scrollParent.addListener(SWT.Activate, new Listener() {
		    public void handleEvent(Event e) {
		    	scrollParent.setFocus();
		    }
		});
		
		registersContainer = new Composite(scrollParent, SWT.NONE);
		GridLayout layout = new GridLayout();
		registersContainer.setLayout(layout);
		
		scrollParent.setContent(registersContainer);
		scrollParent.addControlListener(new ControlAdapter() {
		    public void controlResized(ControlEvent e) {
		        Rectangle r = scrollParent.getClientArea();
		        scrollParent.setMinSize(registersContainer
		                .computeSize(r.width, SWT.DEFAULT));
		    }
		});

		flagsControl = new FlagsControl(registersContainer, 0);
		
		ArrayList<RegisterConfig> registerPopulation = RegisterViewContentConfig.getRegisterPopulation();
		for(RegisterConfig reg: registerPopulation){
			registerControls.put(reg.label, new RegisterControl(registersContainer, 0, reg));
			if(reg.placeUnderHideToggle){
				additionalRegisterControls.put(reg.label, registerControls.get(reg.label));
			}
		}
		
		// Call refresh. Actually, we'll be calling it twice...
		refresh();
		// very hacky, calling a second time because things didn't render properly on program load otherwise
		// if you feel up to it, see if you can fix this.
		refresh();
	}

	public void setCurrentLine64BitExecutionMode(boolean is64BitExecution){
		this.is64BitExecution = is64BitExecution;
	}
	
	public void refresh() {
		// For the memory view, the refresh that iterates over memory values is fast. So a similar process should work here.
		// I have premature optimization concerns regarding cases like when the only time a register is touched is
		// millions of lines before...but it should all work out.
		long startTime = System.currentTimeMillis();
		int smallestLine = Integer.MAX_VALUE;
		
		boolean force64bitRender = Force64BitRegistersHandler.isToggledToShow();
		
		// Deal with whether they are visible or not first, then process updates to data
		for(RegisterControl control : registerControls.values()) {
			boolean toggledInvisible = additionalRegisterControls.containsValue(control) && !ShowAdditionalRegistersHandler.isToggledToShow();
			if(toggledInvisible || (!force64bitRender && control.registerWidthIsZero())){
				control.setVisible(false);
			} else {
				control.setVisible(true);
			}
		}
				
		HashMap<String, Integer> regInUI = new HashMap<>();
		for(MemoryReference reg: ModelProvider.INSTANCE.getMemoryQueryResults().getUnfilteredRegisterAndFlagEntries()){
			if(reg.getLineNumber() < smallestLine){
				smallestLine = reg.getLineNumber();
			}
			if(reg.getLineNumber() <= ModelProvider.INSTANCE.lastUpdatedTraceLine
					&& (!regInUI.containsKey(reg.getRegName()) || regInUI.get(reg.getRegName()) < reg.getLineNumber())){
				if(reg.getRegName().equalsIgnoreCase("RFLAGS")){
					flagsControl.setValue(reg.getMemoryContent().getMemoryValue());
					flagsControl.redraw();
				} else {
					RegisterControl control = registerControls.get(reg.getRegName());
					if(null == control){
						// Do nothing
						// Some (pseudo)register values like AGEN are not actually allocated a UI element
						// EFLAGS is uninteresting, RFLAGS is used instead.
						// Print them out here if you want to see what all is skipped.
					} else {
						control.setLineNumber(reg.getLineNumber(), ModelProvider.INSTANCE.lastUpdatedTraceLine);
						control.setValue(reg.getMemoryContent().getMemoryValue());
						control.setDisplay64BitLongMode(is64BitExecution || force64bitRender);
						control.redraw();
					}
				}
				regInUI.put(reg.getRegName(), reg.getLineNumber());
			}
		}
		
		for(RegisterControl control : registerControls.values()) {
			if(!regInUI.containsKey(control.getCanonicalLabel())){
				control.setValueToEmpty();
				control.setLineNumber(0, 0);
				control.setDisplay64BitLongMode(is64BitExecution || force64bitRender);
				control.redraw();
			}
		}
		
		// Corrects container contents, has extra vertical white/scroll space if not performed, and the right side will be cut off.
		registersContainer.layout(true);
		scrollParent.setMinSize(registersContainer
                .computeSize(scrollParent.getClientArea().width, SWT.DEFAULT));
		
		long endTime = System.currentTimeMillis();
		// System.out.println("Register update duration for line "+ModelProvider.INSTANCE.lastUpdatedTraceLine+": " + (endTime-startTime)/1000.0);
	}

	public void clearContents() {
		// For GUI purposes, need to zero things out first...
		flagsControl.setValueToEmpty();
		flagsControl.redraw();
		for(RegisterControl control : registerControls.values()) {
			control.setValueToEmpty();
			control.setLineNumber(0, 0);
			control.redraw();
		}
		
		currentRegistersLine = -1;
		
		ModelProvider.INSTANCE.clearMemoryAndRegisterData();
		this.refresh();
	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				if (part != activeTraceDisplayer) { // don't need to redraw the visualization if the active Trace Displayer hasn't changed
					activeTraceDisplayer = (AtlantisTraceEditor) part;
					fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
					refresh();
				}
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO  think this is the best way to handle partClosed() events. This could be used in the other view classes. 
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(null == activeWorkbenchWindow){
			// Closing entire program if this happens
			clearContents();
			return;
		}
		activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
		if(null == activeTraceDisplayer){
			clearContents();
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference arg0) {}

	@Override
	public void partHidden(IWorkbenchPartReference arg0) {}

	@Override
	public void partInputChanged(IWorkbenchPartReference arg0) {}

	@Override
	public void partOpened(IWorkbenchPartReference arg0) {}

	@Override
	public void partVisible(IWorkbenchPartReference arg0) {}
	
	@Override
	public void dispose() {
		this.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}

	
}
