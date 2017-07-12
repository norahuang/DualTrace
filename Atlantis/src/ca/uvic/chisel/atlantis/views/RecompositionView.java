package ca.uvic.chisel.atlantis.views;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.functionparsing.BasicBlock;
import ca.uvic.chisel.atlantis.functionparsing.Function;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.recomposition.BasicBlockElement;
import ca.uvic.chisel.atlantis.recomposition.JumpElement;
import ca.uvic.chisel.atlantis.recomposition.OverlapRemover;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class RecompositionView extends ViewPart implements IPartListener2 {
	public static final String ID = "ca.uvic.chisel.atlantis.views.RecompositionView";
	
	private AtlantisFileModelDataLayer fileModel = null;
	private AtlantisTraceEditor activeTraceDisplayer = null;
	
	private Graph graph = null;
	private Composite parent = null;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		createControls(parent);
		createContextMenu();
		
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	private void createContextMenu() {

		createActions();

//		// Create menu manager.
//		MenuManager menuMgr = new MenuManager();
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			@Override
//			public void menuAboutToShow(IMenuManager mgr) {
//				fillContextMenu(mgr);
//			}
//		});
//
//		// Create menu.
//		Menu menu = menuMgr.createContextMenu(resultTableViewer.getControl());
//		resultTableViewer.getControl().setMenu(menu);
//
//		// Register menu for extension.
//		getSite().registerContextMenu(menuMgr, resultTableViewer);
	}
	
	private void fillContextMenu(IMenuManager mgr) {
//		if(resultTable.getSelectionIndex() == -1) {
//			return;
//		}
//		
//		mgr.add(gotoAddress);
//		mgr.add(gotoLine);
	}
	
	public void createActions() {
//		gotoAddress = new Action("Go To Address") {
//			@Override
//			public void run() {
//				MemorySearchView.this.gotoAddressOfSelected();
//			}
//		};
//		gotoLine = new Action("Go To Line of Change") {
//			@Override
//			public void run() {
//				if(resultTable.getSelectionIndex() == -1) {
//					return;
//				}
//				
//				@SuppressWarnings("unchecked")
//				List<MemoryReference> references = (List<MemoryReference>)resultTableViewer.getInput();
//				MemoryReference ref = references.get(resultTable.getSelectionIndex());
//				
//				try {
//					AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
//							.getActivePage().getActiveEditor();
//					if (activeTraceDisplayer != null) {
//						
//						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(ref.getLineNumber(), 0);
//					}
//				} catch (Exception ex) {
//					System.err.println("Error jumping to line");
//					ex.printStackTrace();
//				}
//			}
//		};
	}
	
	private void createGraphControl() {
		graph = new Graph(parent, SWT.NONE);
		graph.setBackground(new Color(graph.getDisplay(), 0xF0, 0xF0, 0xF0));
		
		GridData graphDisplayData = new GridData();
		graphDisplayData.grabExcessHorizontalSpace = true;
		graphDisplayData.grabExcessVerticalSpace = true;
		graphDisplayData.horizontalAlignment = SWT.FILL;
		graphDisplayData.verticalAlignment = SWT.FILL;
		graph.setLayoutData(graphDisplayData);
	}
	
	public void createControls(Composite parent) {
		this.parent = parent;
		createGraphControl();
	}
	
	public void clearGraph(){
		if(graph != null) {
			graph.dispose();
			graph = null;
		}
	}
	
	public void recomposite(Function function) {
		reacquireFileModel();
		
		NavigableSet<BasicBlockElement> elements = new TreeSet<BasicBlockElement>();
		BasicBlockElement firstBlock = null;
		Set<JumpElement> invalidlyConnectedJumps = new TreeSet<JumpElement>();
		Deque<BasicBlock> edge = new LinkedList<BasicBlock>();
		
		edge.addLast(fileModel.getBasicBlockDb().
				getDisconnectedBasicBlockByStartInstruction(function.getFirst(), fileModel.getInstructionDb()));
		
		int i = 0;
		while(!edge.isEmpty()) {
			BasicBlock front = edge.removeFirst();
			
			String startId = front.getStart().getIdStringGlobalUnique();
			String endId = front.getEnd().getIdStringGlobalUnique();
			
			BasicBlockElement blockElement = new BasicBlockElement(startId, endId);
			blockElement.setStartInstruction(front.getStart().getInstruction());
			blockElement.setEndInstruction(front.getEnd().getInstruction());
			blockElement.setModule(front.getStart().getModule());
			blockElement.setStartModuleOffset(front.getStart().getModuleOffset());
			blockElement.setEndModuleOffset(front.getEnd().getModuleOffset());
			
			if(elements.isEmpty()) {
				firstBlock = blockElement;
			}
			
			if(elements.add(blockElement)) {
				i++;
				
				Instruction end = fileModel.getInstructionDb().getInstruction(front.getEnd().getIdStringGlobalUnique());
				if(!end.getInstruction().equalsIgnoreCase("ret")) {
					for(BasicBlock successor : fileModel.getBasicBlockDb().getDisconnectedSuccessors(front, fileModel.getJumpDb(), fileModel.getInstructionDb())) {

						edge.add(successor);
						JumpElement connection = new JumpElement(
								new BasicBlockElement(successor.getStart().getIdStringGlobalUnique(), successor.getEnd().getIdStringGlobalUnique()), 
								blockElement, front.getEnd().isBranch(), successor.isLoadedAsBranchTaken());
						invalidlyConnectedJumps.add(connection);
					}
				}
			}
			
			// TODO: find a better way to ensure functions without returns at the end of all branches don't lead to infinite loops
			if(i > 200) {
				System.err.println("The block limit for a single function has been reached. The graph of this function may be invalid.");
				break;
			}
		}
		
		// fix-up the jumps to point to the correct BasicBlockElements
		for(JumpElement invalidJump : invalidlyConnectedJumps) {
			if(elements.contains(invalidJump.getFrom()) && elements.contains(invalidJump.getTo())) {
				BasicBlockElement source = elements.floor(invalidJump.getFrom());
				BasicBlockElement target = elements.floor(invalidJump.getTo());
				
				invalidJump.setFrom(source);
				invalidJump.setTo(target);
				
				source.addSourceConnection(invalidJump);
				target.addTargetConnection(invalidJump);
			}
		}
		
		// fill in the graph
		clearGraph();
		
		createGraphControl();
		
		parent.layout();
		
		Map<BasicBlockElement, GraphNode> nodeMap = new HashMap<BasicBlockElement, GraphNode>();
		for(BasicBlockElement block : elements) {
			
			String asmString = null;
			int linesOfAsm = 0;
			String hoverText = null;
			
			for(Instruction inst : fileModel.getInstructionDb()
					.getInstructionsInRangeInclusive(block.getModule(), block.getStartModuleOffset(), block.getEndModuleOffset())) {
				if(asmString != null) {
					asmString += "\n";
				} else {
					asmString = "";
					hoverText = inst.getModule() + "+" + Long.toHexString(inst.getModuleOffset());
				}
				asmString += inst.getFullText();
				linesOfAsm++;
			}
			
			GraphNode node = new GraphNode(graph, SWT.NONE, asmString);
			node.setBackgroundColor(new Color(graph.getDisplay(), 0xFF, 0xFF, 0xFF));
			node.setForegroundColor(new Color(graph.getDisplay(), 0x00, 0x00, 0x00));
			node.setHighlightColor(new Color(graph.getDisplay(), 0xFF, 0xFF, 0xFF));
			node.setSize(node.getSize().width, linesOfAsm * 15 + 5);
			
			IFigure tooltip = new Label(hoverText);
			node.setTooltip(tooltip);
			
			if(block == firstBlock) {
				node.setBackgroundColor(new Color(graph.getDisplay(), 0xFF, 0xFA, 0xD4));
			} else if(block.getEndInstruction().equalsIgnoreCase("ret")) {
				node.setBackgroundColor(new Color(graph.getDisplay(), 0xD4, 0xFF, 0xEF));
			}
			
			nodeMap.put(block, node);
		}
		
		for(BasicBlockElement block : elements) {
			for(JumpElement jump : block.getSourceConnections()) {
				GraphConnection connection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, nodeMap.get(jump.getFrom()), nodeMap.get(jump.getTo()));
				connection.setLineWidth(2);
				
				if(jump.isBranch()) {
					if(jump.isBranchTaken()) {
						connection.setLineColor(new Color(graph.getDisplay(), 0x00, 0xD4, 0x43));
					} else {
						connection.setLineColor(new Color(graph.getDisplay(), 0xFF, 0x4D, 0x4D));
					}
				} else {
					connection.setLineColor(new Color(graph.getDisplay(), 0x4A, 0x63, 0xF0));
				}
			}
		}
		
		graph.setLayoutAlgorithm(new 
				CompositeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING, new 
						LayoutAlgorithm[] { 
							new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), // apply tree layout first
							new OverlapRemover(LayoutStyles.NO_LAYOUT_NODE_RESIZING, 20) // then our custom layout
						}), true);
	}

	@Override
	public void setFocus() {
		if(graph != null) {
			graph.forceFocus();
		}
	}
	
	private boolean reacquireFileModel() {
		try {
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
			fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
			
		} catch (Exception e) {
			// TODO we need better error handling
			return false;
		}
		return true;
	}
	
	/**
	 * DO NOT call this method outside of the UI classes of the recomposition view.
	 * Acquires the file model used by the current instance of RecompositionView.
	 * @return The file model.
	 */
	public static AtlantisFileModelDataLayer getFileModel() {
		RecompositionView compView = (RecompositionView) 
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RecompositionView.ID);
		if(compView != null) {
			if(compView.fileModel == null) {
				compView.reacquireFileModel();
			}
			
			return compView.fileModel;
		}
		
		return null;
	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		// if(!reacquireFileModel()){
		// 	return;
		// }
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				if (part != activeTraceDisplayer) { // don't need to redraw the visualization if the active Trace Displayer hasn't changed
					activeTraceDisplayer = (AtlantisTraceEditor) part;
					fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
					clearGraph();
				}
			}
		}
	}
	
	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) { }
	@Override
	public void partClosed(IWorkbenchPartReference arg0) { }
	@Override
	public void partDeactivated(IWorkbenchPartReference arg0) { }
	@Override
	public void partHidden(IWorkbenchPartReference arg0) { }
	@Override
	public void partInputChanged(IWorkbenchPartReference arg0) { }
	@Override
	public void partOpened(IWorkbenchPartReference arg0) { }
	@Override
	public void partVisible(IWorkbenchPartReference arg0) { }
}
