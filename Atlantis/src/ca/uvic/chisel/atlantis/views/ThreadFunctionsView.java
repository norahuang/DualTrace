package ca.uvic.chisel.atlantis.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.print.attribute.standard.PageRanges;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.functionparsing.FunctionNameRegistry;
import ca.uvic.chisel.atlantis.functionparsing.InstructionFileLineConsumer;
import ca.uvic.chisel.atlantis.functionparsing.LightweightThreadFunctionBlock;
import ca.uvic.chisel.atlantis.functionparsing.LightweightThreadFunctionBlockLoader;
import ca.uvic.chisel.atlantis.functionparsing.ModuleColorProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class ThreadFunctionsView extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.atlantis.views.ThreadFunctionsView";
	
	private Canvas blockCanvas;
	private Slider horizontalScrollBar;
	private Slider verticalScrollBar;
	private Combo threadsCombo;
	private Button goToCurrentLineButton;

	// the thread to display blocks for
	private int currentThread = -1;
	
	// used to avoid reloading data when we don't switch files
	private String currentFile = "";
	
	// maximum horizontal length, in pixels/instructions of the current thread
	private long currentThreadLength = -1;
	
	// the width of the canvas devoted to displaying the thread function blocks
	private long blockCanvasDisplayWidth = -1;
	
	// the ratio to scroll the horizontal scroll bar
	private double horizontalScrollRatio = -1.0;
	
	// the current distance from the leftmost visible edge of the blocks to the beginning
	// of the entire block display. From 0, increases as we scroll to the right.
	private long currentXOffset = 0;
	
	// the distance from the left of the canvas to the start of the block display
	private int blockCanvasBlockLeftStart = 200;
	
	// the height of a function block
	private int blockHeight = 15;
	
	// the height of a row of blocks
	private int rowHeight = 20;
	
	// the distance from the top of the canvas to the start of the rows of blocks
	private int blockStartHeight = 35;
	
	// the distance from the left side of the canvas to the left side of the module names
	private int functionModuleLeftStart = 5;
	
	// the distance from the left side of the canvas to the left side of the function names
	private int functionNameLeftStart = 100;
	
	// all the blocks in the current thread
	private List<LightweightThreadFunctionBlock> threadBlocks = null;
	
	// the indices into the thread block list
	private Map<Integer, Integer> threadBlockIndices = null;
	
	// this size of indexed pages
	private int threadBlockIndexPageSize = 1000;
	
	// the blocks within the current display
	private List<LightweightThreadFunctionBlock> blocksInCurrentDisplay = null;
	
	// the block currently being hovered
	private LightweightThreadFunctionBlock highlightBlock = null;

	// flag to disable updating while internally adjusting the scroll bar
	private boolean horizontalScrollInternalAdjustment = false;
	
	// rectangles for which to display a tooltip
	private List<Pair<Rectangle, String>> toolTipRegions = null;
	
	// the currently active tooltip
	private ToolTip currentToolTip = null;
	
	// the x offset for the currently loaded blocks
	private long currentLoadedBlocksXOffset = -1;
	
	// the thread for the currently loaded blocks
	private int currentLoadedBlocksThread = -1;
	
	// the width of the set of currently loaded blocks
	private long currentLoadedBlocksWidth = -1;
	
	// are we in a loading screen
	private boolean inLoadingScreen = true;
	
	// loading bar dimensions
	private int loadingBarHeight = 15;
	private int loadingBarWidth = 100;
	
	// the block loader
	private LightweightThreadFunctionBlockLoader blockLoader = null;
	
	// cursors
	private Cursor defaultCursor;
	private Cursor hoverCursor;
	
	private ModuleColorProvider colorProvider = null;
	
	private AtlantisFileModelDataLayer fileModel = null;

	private AtlantisTraceEditor activeTraceDisplayer;
	
	public ThreadFunctionsView() {
	}
	
	private boolean reacquireFileModel() {
		// This is done differently in other views, by responding in parActivated() (e.g. in AbstractTraceVisualizationView)
		// The best way is likely to *not* have a member variable for the model or displayer.
		// This might cause problems with detecting a *change* in trace though...
		// Which way is better?
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

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.marginBottom = layout.marginRight = layout.marginTop = layout.marginLeft = -5;
		parent.setLayout(layout);
		
		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)); 
		
		createControls(parent);
		
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
		
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				try{
					colorProvider.disposeAllColors();
				} catch (NullPointerException npe){
					// nada
				}
			}
		});
	}

	@Override
	public void setFocus() {
		blockCanvas.forceFocus();
	}
	
	public void createControls(Composite parent) {
		
		blockCanvas = new Canvas(parent, SWT.NO_BACKGROUND);
		blockCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				ThreadFunctionsView.this.paintControl(e);
			}
		});
		blockCanvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				ThreadFunctionsView.this.mouseMove(e);
			}
		});
		blockCanvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				ThreadFunctionsView.this.mouseUp(e);
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) { }
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		blockCanvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				ThreadFunctionsView.this.mouseScrolled(e);
			}
		});
		// this is required so the mouse scroll activates,
		// even if we weren't handling the event anyway
		blockCanvas.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) { }
			@Override
			public void keyPressed(KeyEvent e) { 
				ThreadFunctionsView.this.keyPressed(e);
			}
		});
		blockCanvas.addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) { 
				ThreadFunctionsView.this.mouseHover(e);
			}	
			@Override
			public void mouseExit(MouseEvent arg0) { }
			
			@Override
			public void mouseEnter(MouseEvent arg0) {
				blockCanvas.forceFocus();
			}
		});
		blockCanvas.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				ThreadFunctionsView.this.controlResized(e);
			}
			@Override
			public void controlMoved(ControlEvent arg0) { }
		});
		
		defaultCursor = blockCanvas.getCursor();
		hoverCursor = new Cursor(blockCanvas.getDisplay(), SWT.CURSOR_HAND);
		
		threadsCombo = new Combo(blockCanvas, SWT.READ_ONLY);
		threadsCombo.setEnabled(false);
		threadsCombo.setBounds(5, 5, 60, threadsCombo.getSize().y);
		threadsCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ThreadFunctionsView.this.threadsComboSelectionChanged(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});
		
		goToCurrentLineButton = new Button(blockCanvas, SWT.PUSH);
		goToCurrentLineButton.setEnabled(false);
		goToCurrentLineButton.setBounds(70, 5, 110, threadsCombo.getSize().y);
		goToCurrentLineButton.setText("Show Current Line");
		goToCurrentLineButton.setToolTipText("Show the thread-function for the line currently selected in the trace view.");
		goToCurrentLineButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				showCurrentTraceLineInView();
			}
		});
		
		GridData blockCanvasData = new GridData();
		blockCanvasData.horizontalAlignment = GridData.FILL;
		blockCanvasData.verticalAlignment = GridData.FILL;
		blockCanvasData.grabExcessHorizontalSpace = true;
		blockCanvasData.grabExcessVerticalSpace = true;
		blockCanvas.setLayoutData(blockCanvasData);
		
		verticalScrollBar = new Slider(parent, SWT.VERTICAL);
		
		GridData vertScrollBarData = new GridData();
		vertScrollBarData.horizontalAlignment = GridData.END;
		vertScrollBarData.verticalAlignment = GridData.FILL;
		vertScrollBarData.grabExcessHorizontalSpace = false;
		vertScrollBarData.grabExcessVerticalSpace = true;
		verticalScrollBar.setLayoutData(vertScrollBarData);
		
		verticalScrollBar.setMinimum(0);
		verticalScrollBar.setMaximum(Integer.MAX_VALUE);
		verticalScrollBar.setIncrement(1);
		verticalScrollBar.setSelection(0);
		
		verticalScrollBar.addSelectionListener(new SelectionListener() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				ThreadFunctionsView.this.verticalScrollSelectionChanged(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) { }
		});
		
		horizontalScrollBar = new Slider(parent, SWT.HORIZONTAL);
		
		GridData hozScrollBarData = new GridData();
		hozScrollBarData.horizontalAlignment = GridData.FILL;
		hozScrollBarData.verticalAlignment = GridData.END;
		hozScrollBarData.grabExcessHorizontalSpace = true;
		hozScrollBarData.grabExcessVerticalSpace = false;
		horizontalScrollBar.setLayoutData(hozScrollBarData);
		
		horizontalScrollBar.setMinimum(0);
		horizontalScrollBar.setMaximum(Integer.MAX_VALUE);
		horizontalScrollBar.setIncrement(1);
		horizontalScrollBar.setSelection(0);
		
		horizontalScrollBar.addSelectionListener(new SelectionListener() {		  
			@Override
			public void widgetSelected(SelectionEvent e) {
				ThreadFunctionsView.this.horizontalScrollSelectionChanged(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) { }
		});
	}
	
	private void paintControl(PaintEvent e) {
		
		// If the view isn't visible, we cannot try to paint it.
		if(null == fileModel || null == activeTraceDisplayer){
			return;
		}
		
		// double buffer
		Image bufferImage = new Image(blockCanvas.getDisplay(), blockCanvas.getBounds().width, blockCanvas.getBounds().height); 
		// In other views, we have used the PaintEvent e.gc. Why not the same here?
		GC g = new GC(bufferImage); 
		
		Color background = g.getBackground();
		Color foreground = g.getForeground();

		Color grey = new Color(blockCanvas.getDisplay(), 0xCC, 0xCC, 0xCC);
		
		// draw the loading screen
		if(blocksInCurrentDisplay == null || inLoadingScreen) {
			// don't draw anything if there are no blocks
			// draw the top line
			g.setForeground(grey);
			g.drawLine(0, blockStartHeight - 2, blockCanvas.getBounds().width, blockStartHeight - 2);
			
			if(inLoadingScreen && blocksInCurrentDisplay != null){
				// determine the position of the loading text
				int x = blockCanvas.getBounds().width / 2 - loadingBarWidth / 2;
				int y = (blockCanvas.getBounds().height - blockStartHeight - 2) / 2 + (blockStartHeight - 2) - loadingBarHeight / 2;
				g.drawText("Loading...", x, y);
			}
			
			g.setForeground(foreground);
			g.setBackground(background);
			
		} else {
		
			// determine block row positions
			Map<LightweightThreadFunctionBlock, Integer> rowOrdering = new TreeMap<LightweightThreadFunctionBlock, Integer>();
			Map<InstructionId, Integer> rowAssignedFunctionStarts = new TreeMap<InstructionId, Integer>();
			
			fillFunctionBlockRowOrder(rowOrdering, rowAssignedFunctionStarts);
			
			// draw block guidelines
			Color guidelineColor = new Color(blockCanvas.getDisplay(), 0xEE, 0xEE, 0xEE);
			g.setForeground(guidelineColor);
			g.setLineStyle(SWT.LINE_DOT);
			for(LightweightThreadFunctionBlock block : blocksInCurrentDisplay) {
				int y = rowOrdering.get(block) * rowHeight + blockStartHeight - verticalScrollBar.getSelection();
				
				if(y < blockStartHeight - rowHeight) {
					continue;
				}
				
				g.drawLine(blockCanvasBlockLeftStart, y + blockHeight + 2, blockCanvasBlockLeftStart + (int)blockCanvasDisplayWidth, y + blockHeight + 2);
			}
			g.setLineStyle(SWT.LINE_SOLID);
			g.setForeground(foreground);
			guidelineColor.dispose();
			
			// draw the blocks themselves
			for(LightweightThreadFunctionBlock block : blocksInCurrentDisplay) {
				
				int x = (int)(block.getXOffset() - currentXOffset) + blockCanvasBlockLeftStart;
				int width = (int)block.getWidth();
				int y = rowOrdering.get(block) * rowHeight + blockStartHeight - verticalScrollBar.getSelection();
				int height = blockHeight;
				
				if(x < blockCanvasBlockLeftStart) {
					width -= blockCanvasBlockLeftStart - x;
					x = blockCanvasBlockLeftStart;
				}
				
				if(y < blockStartHeight - rowHeight) {
					continue;
				}
				
				if(block.getBlockModule() != null) {
					g.setForeground(colorProvider.getModuleLighterColor(block.getBlockModule()));
					g.setBackground(colorProvider.getModuleDarkerColor(block.getBlockModule()));
				} else {
					g.setForeground(grey);
					g.setBackground(grey);
				}
				
				g.fillGradientRectangle(x, y, width, height, true);
				
				if(block == highlightBlock) {
					g.setForeground(foreground);
					g.drawRectangle(x - 1, y - 1, width + 1, blockHeight + 1);
				}
				
				// draw leading dot, helps for debugging only. Shows when rows are not properly assigned.
				// g.setForeground(grey);
				// g.drawRectangle(x, y+blockHeight/2, 1, 1);
				
			}
			
			
			// draw name strings
			g.setForeground(foreground);
			g.setBackground(background);
			
			toolTipRegions = new LinkedList<Pair<Rectangle, String>>();
			
			for(Map.Entry<LightweightThreadFunctionBlock, Integer> row : rowOrdering.entrySet())
			{
				LightweightThreadFunctionBlock tfb = row.getKey();
				String functionModule = "UNKNOWN";
				String functionName = "UNKNOWN";
				
				if(tfb.getFunctionStartInstruction() != null) {
					functionModule = tfb.getFunctionModule();
					
					functionName = FunctionNameRegistry.getFunction(tfb.getFunctionModule(), tfb.getFunctionStartAddress());
					if(functionName == null) {
						functionName = Long.toHexString(tfb.getFunctionStartAddress());
					}
				} else {
					functionModule = tfb.getBlockModule();
				}
				
				int y = row.getValue() * rowHeight + blockStartHeight - verticalScrollBar.getSelection();
				if(y < blockStartHeight - rowHeight) {
					continue;
				}
				
				String truncatedFunctionModule = truncateTextToVisibleWidth(functionNameLeftStart - functionModuleLeftStart - 5, functionModule, g);
				String truncatedFunctionName = truncateTextToVisibleWidth(blockCanvasBlockLeftStart - functionNameLeftStart - 5, functionName, g);
				
				g.drawString(truncatedFunctionModule, functionModuleLeftStart, y);
				g.drawString(truncatedFunctionName, functionNameLeftStart, y);
				
				if(y < blockStartHeight) {
					y = blockStartHeight;
				}
				
				if(truncatedFunctionModule.endsWith("...")) {
					Point stringBounds = g.textExtent(truncatedFunctionModule);
					toolTipRegions.add(new ImmutablePair<Rectangle, String>(
							new Rectangle(functionModuleLeftStart, y, stringBounds.x, stringBounds.y), 
							functionModule));
				}
				
				if(truncatedFunctionName.endsWith("...")) {
					Point stringBounds = g.textExtent(truncatedFunctionName);
					toolTipRegions.add(new ImmutablePair<Rectangle, String>(
							new Rectangle(functionNameLeftStart, y, stringBounds.x, stringBounds.y), 
							functionName));
				}
			}
			
			// draw the obstructing rectangle to make the top of the scroll smooth
			g.fillRectangle(0, blockStartHeight - rowHeight - 1, blockCanvas.getBounds().width, rowHeight);
			
			// draw the module color line
			for(LightweightThreadFunctionBlock block : blocksInCurrentDisplay) {
				int x = (int)(block.getXOffset() - currentXOffset) + blockCanvasBlockLeftStart;
				int width = (int)block.getWidth();
				
				if(x < blockCanvasBlockLeftStart) {
					width -= blockCanvasBlockLeftStart - x;
					x = blockCanvasBlockLeftStart;
				}
				
				if(block.getBlockModule() != null) {
					g.setForeground(colorProvider.getModuleLighterColor(block.getBlockModule()));
					g.setBackground(colorProvider.getModuleDarkerColor(block.getBlockModule()));

				} else {
					g.setForeground(grey);
					g.setBackground(grey);
				}
				
				g.fillRectangle(x, blockStartHeight - 15, width, 5);
			}
			
			// draw the left and top edges of blocks lines		
			g.setForeground(grey);
			g.drawLine(blockCanvasBlockLeftStart - 1, 0, blockCanvasBlockLeftStart - 1, blockCanvas.getBounds().height);
			g.drawLine(0, blockStartHeight - 2, blockCanvas.getBounds().width, blockStartHeight - 2);
			g.setForeground(foreground);
			
			// draw current selected line position (if it's in the view)
			long linePos = getCurrentLineInstructionAbsoluteXPosition(blocksInCurrentDisplay);
			linePos -= currentXOffset;
			// the -2 check allows the line to appear when at the very beginning of the file.
			if(linePos >= -2) {
				Color currentPosColor = new Color(blockCanvas.getDisplay(), 0xB4, 0x04, 0x04);
				g.setForeground(currentPosColor);
				// The +1 allows the line to be placed directly on the block
				g.drawLine((int)linePos + blockCanvasBlockLeftStart + 1, 0, (int)linePos + blockCanvasBlockLeftStart + 1, blockCanvas.getBounds().height);
				currentPosColor.dispose();
			}
		}
		
		grey.dispose();
		
		// flip double buffer
		e.gc.drawImage(bufferImage, 0, 0);
		bufferImage.dispose();
	}

	private String truncateTextToVisibleWidth(int width, String text, GC g) {
		
		if(g.textExtent(text).x <= width) {
			return text;
		}
		
		String result = text + "...";
		while(g.textExtent(result).x > width && result.length() > 3) {
			result = result.substring(0, result.length() - 4) + "...";
		}
		
		return result;
	}
	
	private void keyPressed(KeyEvent e) { 
		switch(e.keyCode) {
			case SWT.ARROW_LEFT: {
				Event baseEvt = new Event();
				baseEvt.widget = blockCanvas;
				SelectionEvent evt = new SelectionEvent(baseEvt);
				evt.detail = SWT.ARROW_UP;
				horizontalScrollSelectionChanged(evt);
			} break;
			case SWT.ARROW_RIGHT: {
				Event baseEvt = new Event();
				baseEvt.widget = blockCanvas;
				SelectionEvent evt = new SelectionEvent(baseEvt);
				evt.detail = SWT.ARROW_DOWN;
				horizontalScrollSelectionChanged(evt);
			} break;
			case SWT.ARROW_DOWN: {
				verticalScrollBar.setSelection(verticalScrollBar.getSelection() + 10);
				verticalScrollSelectionChanged(null);
			} break;
			case SWT.ARROW_UP: {
				verticalScrollBar.setSelection(verticalScrollBar.getSelection() - 10);
				verticalScrollSelectionChanged(null);
			} break;
		}
	}
	
	private void controlResized(ControlEvent e) {
		blockCanvasDisplayWidth = blockCanvas.getBounds().width - blockCanvasBlockLeftStart;
		
		if(inLoadingScreen) {
			blockCanvas.redraw();
			return;
		}
		
		// if the thread is shorter than the view, disable the horizontal scroll
		if(currentThreadLength < blockCanvasDisplayWidth) {
			horizontalScrollInternalAdjustment = true;
			horizontalScrollBar.setSelection(0);
			horizontalScrollBar.setEnabled(false);
			horizontalScrollInternalAdjustment = false;
			
			currentXOffset = 0;
			
		} else {
			// otherwise ensure it is still enabled
			horizontalScrollBar.setEnabled(true);
		}
		
		moveViewHorizontally();
	}
	
	private void mouseHover(MouseEvent e) {
		// check tool tips
		if(toolTipRegions != null) {
			blockCanvas.getDisplay().getActiveShell().setToolTipText(null);
			for(Pair<Rectangle, String> toolTipEntry : toolTipRegions) {
				if(toolTipEntry.getLeft().contains(e.x, e.y)) {
					currentToolTip = new ToolTip(blockCanvas.getDisplay().getActiveShell(), SWT.TOOL);
					currentToolTip.setMessage(toolTipEntry.getRight());
					currentToolTip.setAutoHide(false);
					currentToolTip.getDisplay().timerExec(1500, new Runnable() {	
						@Override
						public void run() {
							if(!currentToolTip.isDisposed()) {
								currentToolTip.setVisible(false);
							}
						}
					});
					currentToolTip.setVisible(true);
					return;
				}
			}
		}
	}
	
	private void mouseMove(MouseEvent e) {
		if(inLoadingScreen || blocksInCurrentDisplay == null) {
			return;
		}
		
		if(currentToolTip != null && !currentToolTip.isDisposed()) {
			currentToolTip.setVisible(false);
		}
		
		Map<LightweightThreadFunctionBlock, Integer> rowOrdering = new TreeMap<LightweightThreadFunctionBlock, Integer>();
		Map<InstructionId, Integer> rowAssignedFunctionStarts = new TreeMap<InstructionId, Integer>();
		
		fillFunctionBlockRowOrder(rowOrdering, rowAssignedFunctionStarts);
		
		if(e.y < blockStartHeight) {
			highlightBlock = null;
			blockCanvas.setCursor(defaultCursor);
			blockCanvas.redraw();
			return;
		}
		
		for(LightweightThreadFunctionBlock block : blocksInCurrentDisplay) {
			int x = (int)(block.getXOffset() - currentXOffset) + blockCanvasBlockLeftStart;
			int width = (int)block.getWidth();
			int y = rowOrdering.get(block) * rowHeight + blockStartHeight - verticalScrollBar.getSelection();
			
			if(x < blockCanvasBlockLeftStart) {
				width -= blockCanvasBlockLeftStart - x;
				x = blockCanvasBlockLeftStart;
			}
			
			if(new Rectangle(x, y, width, blockHeight).contains(e.x, e.y)) {
				highlightBlock = block;
				blockCanvas.setCursor(hoverCursor);
				blockCanvas.redraw();
				return;
			}
		}
		
		if(highlightBlock != null) {
			highlightBlock = null;
			blockCanvas.setCursor(defaultCursor);
			blockCanvas.redraw();
		}
	}
	
	private void mouseUp(MouseEvent e) {
		if(blockCanvas.getCursor() == hoverCursor && highlightBlock != null) {
			// go to the block that was clicked
			try {
				if (activeTraceDisplayer != null) {
					activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset((int)highlightBlock.getStartLineNumber(), 0);
					blockCanvas.forceFocus();
				}
			} catch (Exception ex) {
				System.err.println("Error jumping to block");
				ex.printStackTrace();
			}
		}
	}
	
	private void mouseScrolled(MouseEvent e) {
		if(inLoadingScreen) {
			return;
		}
		
		// if the shift key is down, scroll left-right, otherwise scroll up-down
		if((e.stateMask & SWT.SHIFT) > 0) {
			currentXOffset -= e.count * 10;
			if(currentXOffset < 0) {
				currentXOffset = 0;
			}
			if(currentXOffset > currentThreadLength - blockCanvasDisplayWidth) {
				currentXOffset = currentThreadLength - blockCanvasDisplayWidth;
			}
			
			moveViewHorizontally();
		} else {
			verticalScrollBar.setSelection(verticalScrollBar.getSelection() - e.count);
			verticalScrollSelectionChanged(null);
		}
	}
	
	/**
	 * Besides the usual function calls and returns to deal with, we also have to represent functions that
	 * were parachuted into, that is, functions underway when tracing began. These can of course be several
	 * deep in the stack. These are all processed in {@link InstructionFileLineConsumer#insertThreadFunctionBlock}.
	 * 
	 * @param rowOrdering
	 * @param rowAssignedFunctionStarts
	 */
	private void fillFunctionBlockRowOrder(Map<LightweightThreadFunctionBlock, Integer> rowOrdering, 
			Map<InstructionId, Integer> rowAssignedFunctionStarts) {
		
		if(blocksInCurrentDisplay == null || blocksInCurrentDisplay.size() == 0) {
			return;
		}
		
		int currentRow = 0;
		
		LightweightThreadFunctionBlock last = null;
		Set<Integer> nullTakenFunctionRows = new TreeSet<Integer>();
		for(LightweightThreadFunctionBlock block : blocksInCurrentDisplay) {
			if(last != null && last.endsWithCall()) {
				// the last instruction was a call, which means we are entering
				// a function, check to see if this function is already on a row
				if(block.getFunctionStartInstruction() != null && rowAssignedFunctionStarts.containsKey(block.getFunctionStartInstruction())) {
					currentRow = rowAssignedFunctionStarts.get(block.getFunctionStartInstruction());
				} else {
					// it was not already on a row, so search for the next free row
					while(rowAssignedFunctionStarts.containsValue(currentRow) || nullTakenFunctionRows.contains(currentRow)) {
						currentRow++;
					}
					
					if(block.getFunctionStartInstruction() == null) {
						nullTakenFunctionRows.add(currentRow);
					} else {
						rowAssignedFunctionStarts.put(block.getFunctionStartInstruction(), currentRow);
					}
				}
			} else {
				// the last instruction was a return, so we are leaving a function
				// check if we are returning to a function with a row already
				if(block.getFunctionStartInstruction() != null && rowAssignedFunctionStarts.containsKey(block.getFunctionStartInstruction())) {
					currentRow = rowAssignedFunctionStarts.get(block.getFunctionStartInstruction());
				} else {
					// no row for this function, search up for the next row
					// it was not already on a row, so search for the next free row
					while(rowAssignedFunctionStarts.containsValue(currentRow) || nullTakenFunctionRows.contains(currentRow)) {
						currentRow--;
					}
					
					if(block.getFunctionStartInstruction() == null) {
						nullTakenFunctionRows.add(currentRow);
					} else {
						rowAssignedFunctionStarts.put(block.getFunctionStartInstruction(), currentRow);
					}
				}
			}
			
			rowOrdering.put(block, currentRow);
			
			if(last == null) {
				if(block.getFunctionStartInstruction() == null) {
					nullTakenFunctionRows.add(currentRow);
				} else {
					rowAssignedFunctionStarts.put(block.getFunctionStartInstruction(), currentRow);
				}
			}
			
			last = block;
		}
		
		// find the highest row (lowest index)
		int highestRow = 0;
		for(Integer row : rowOrdering.values()) {
			if(row < highestRow) {
				highestRow = row;
			}
		}
		
		// make the rows start at zero
		for(Map.Entry<LightweightThreadFunctionBlock, Integer> tfbRow : rowOrdering.entrySet()) {
			rowOrdering.put(tfbRow.getKey(), tfbRow.getValue() + Math.abs(highestRow));
		}
	}
	
	private long threadPositionFromScrollPosition() {
		int scrollPosition = horizontalScrollBar.getSelection();
		
		return (long)(horizontalScrollRatio * (double)scrollPosition);
	}
	
	private void scrollToThreadPositionForCurrentTraceLine(){
		long currentLineXPosition;
		
		int lineNumber = this.getCurrentLineNumber();
		LightweightThreadFunctionBlock singleBlock = fileModel.getThreadFunctionBlockDb().getLightweightThreadFunctionBlockForLine(lineNumber);
		
		// Need the block for which the line corresponds, only, really.
		 if(null == singleBlock){
			 currentLineXPosition = -1;
		 } else {
			 ArrayList<LightweightThreadFunctionBlock> singleBlockInList = new ArrayList<LightweightThreadFunctionBlock>();
			 singleBlockInList.add(singleBlock);
			 currentLineXPosition = getCurrentLineInstructionAbsoluteXPosition(singleBlockInList);
			 // Red line is relative to window, but is like:  = (int)linePos + blockCanvasBlockLeftStart;
		 }
		
		// Some race conditions when loading, so if you debug here, expect the thread func view to not load...
		horizontalScrollBar.setEnabled(true);
		
		currentXOffset = threadPositionFromScrollPosition();
		
		if(currentXOffset < 0) {
			currentXOffset = 0;
		}
		else {
			currentXOffset = currentLineXPosition;
		}
		
		horizontalScrollInternalAdjustment = true;
		horizontalScrollBar.setSelection((int)((double)currentXOffset / horizontalScrollRatio));
		horizontalScrollInternalAdjustment = false;
		
		if(null != singleBlock){
			moveViewHorizontally();
		}
	}
	
	private void enterLoadingScreen() {
		verticalScrollBar.setEnabled(false);
		threadsCombo.setEnabled(false);
		goToCurrentLineButton.setEnabled(false);
		inLoadingScreen = true;
		blockCanvas.redraw();
	}
	
	private void exitLoadingScreen() {
		if(threadsCombo.isDisposed()) {
			return;
		}
		
		threadsCombo.setEnabled(true);
		goToCurrentLineButton.setEnabled(true);
		
		loadBlocks();
		
		verticalScrollBar.setEnabled(true);
		updateVerticalScrollForXOffsetChange();
		
		inLoadingScreen = false;
		blockCanvas.redraw();
	}
	
	private void showCurrentTraceLineInView(){
		// Get line number, figure out the thread it's in, change to that thread, scroll to correct position
		if(activeTraceDisplayer == null) {
			return;
		}
			
		int lineNumber = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber();
		
		
		LightweightThreadFunctionBlock tfb;
		try {
			tfb = fileModel.getThreadBlockContainingOrFollowingLine(lineNumber);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return;
		}
		
		String targetTid = tfb.getThreadId()+"";
		int index = 0;
		for(String tid: threadsCombo.getItems()){
			if(targetTid.equals(tid)){
				threadsCombo.select(index);
				scrollToThreadPositionForCurrentTraceLine();
				// Rely on threadsComboSelectionChanged() to move to current line when we swap?
				// No, have it call the function I make...
				break;
			}
			index++;
		}
	}
	
	private void moveViewHorizontally() {
		if(blockLoader.areBlocksInRangeAvailable(currentXOffset, currentXOffset + blockCanvasDisplayWidth)) {
			refresh();
		} else {
			if(!inLoadingScreen) {
				enterLoadingScreen();
			}
			
			final int requestedThread = currentThread;
			blockLoader.putRangeOnTopOfQueue(currentXOffset, currentXOffset + blockCanvasDisplayWidth);
			blockLoader.listenForBlocksLoaded(currentXOffset, currentXOffset + blockCanvasDisplayWidth, new Runnable() {
				@Override
				public void run() {
					// end of life safety
					if(blockLoader == null) {
						return;
					}
					
					// if we have the required blocks for the view now
					if(blockLoader.areBlocksInRangeAvailable(currentXOffset, currentXOffset + blockCanvasDisplayWidth) 
							&& inLoadingScreen && requestedThread == currentThread) {
						exitLoadingScreen();
					}
				}
			});
		}
	}
	
	private void threadsComboSelectionChanged(SelectionEvent e) {
		if(threadsCombo.getSelectionIndex() == -1) {
			return;
		}
		
		int selectedThread = Integer.parseInt(threadsCombo.getItems()[threadsCombo.getSelectionIndex()]);
		
		if(selectedThread == currentThread) {
			return;
		}
		
		currentThread = selectedThread;
		
		currentThreadLength = fileModel.getThreadLengthDbConnection().getThreadLength(currentThread);
		
		if(!inLoadingScreen) {
			enterLoadingScreen();
		}
		
		if(blockLoader != null) {
			blockLoader.kill();
			blockLoader = null;
		}
		
		blockLoader = new LightweightThreadFunctionBlockLoader(fileModel, currentThread, currentThreadLength);
		blockLoader.beginAsyncPageLoading();
		
		blockCanvasDisplayWidth = blockCanvas.getBounds().width - blockCanvasBlockLeftStart;
		horizontalScrollRatio = (double)(currentThreadLength - blockCanvasDisplayWidth) / (double)horizontalScrollBar.getMaximum();
		
		// if the thread is shorter than the view, disable the horizontal scroll
		if(currentThreadLength < blockCanvasDisplayWidth) {
			horizontalScrollInternalAdjustment = true;
			horizontalScrollBar.setSelection(0);
			horizontalScrollBar.setEnabled(false);
			horizontalScrollInternalAdjustment = false;
			
			currentXOffset = 0;
			
		} else {
			// otherwise scroll the corresponding percentage along
			horizontalScrollBar.setEnabled(true);
			
			currentXOffset = threadPositionFromScrollPosition();
			
			if(currentXOffset < 0) {
				currentXOffset = 0;
			}
			else if(currentXOffset > currentThreadLength - blockCanvasDisplayWidth) {
				currentXOffset = currentThreadLength - blockCanvasDisplayWidth;
			}
			
			horizontalScrollInternalAdjustment = true;
			horizontalScrollBar.setSelection((int)((double)currentXOffset / horizontalScrollRatio));
			horizontalScrollInternalAdjustment = false;
		}
		
		moveViewHorizontally();
	}
	
	private void recomputeVerticalScrollSize() {
		Map<LightweightThreadFunctionBlock, Integer> rowOrdering = new TreeMap<LightweightThreadFunctionBlock, Integer>();
		Map<InstructionId, Integer> rowAssignedFunctionStarts = new TreeMap<InstructionId, Integer>();
		
		fillFunctionBlockRowOrder(rowOrdering, rowAssignedFunctionStarts);
		
		Set<Integer> uniqueRows = new TreeSet<Integer>();
		
		for(Integer i : rowOrdering.values()) {
			uniqueRows.add(i);
		}
		
		int numRows = uniqueRows.size();
		int maxHeight = numRows * rowHeight;
		
		verticalScrollBar.setMaximum(maxHeight);
	}
	
	private void updateVerticalScrollForXOffsetChange() {
		double verticalScrollFraction = (double)verticalScrollBar.getSelection() / (double)verticalScrollBar.getMaximum();
		recomputeVerticalScrollSize();
		int newVericalScrollPosition = (int)(verticalScrollFraction * (double)verticalScrollBar.getMaximum());
		verticalScrollBar.setSelection(newVericalScrollPosition);
		
		refresh();
	}
	
	private void horizontalScrollSelectionChanged(SelectionEvent e) {		
		if(horizontalScrollInternalAdjustment) {
			return;
		}
		
		switch(e.detail) {
			case SWT.ARROW_UP: 
			case SWT.PAGE_UP: {
				currentXOffset -= 10;
				if(currentXOffset < 0) {
					currentXOffset = 0;
				}
			} break;
			case SWT.ARROW_DOWN: 
			case SWT.PAGE_DOWN: {
				currentXOffset += 10;
				if(currentXOffset > currentThreadLength - blockCanvasDisplayWidth) {
					currentXOffset = currentThreadLength - blockCanvasDisplayWidth;
				}
			} break;
			default:
			{
				long oldXOffset = currentXOffset;
				
				currentXOffset = (long)(horizontalScrollRatio * (double)horizontalScrollBar.getSelection());
				if(currentXOffset < 0) {
					currentXOffset = 0;
				}
				else if(currentXOffset > currentThreadLength - blockCanvasDisplayWidth) {
					currentXOffset = currentThreadLength - blockCanvasDisplayWidth;
				}
				
				// floating point rounding error fix
				if(currentXOffset == oldXOffset - 1) {
					currentXOffset = oldXOffset;
					return;
				}
				
			} break;
		}
		
		horizontalScrollInternalAdjustment = true;
		horizontalScrollBar.setSelection((int)((double)currentXOffset / horizontalScrollRatio));
		horizontalScrollInternalAdjustment = false;
		
		moveViewHorizontally();
	}
	
	private void verticalScrollSelectionChanged(SelectionEvent e) {	
		blockCanvas.redraw();
	}
	
	private int getCurrentLineNumber(){
		int line = -1;
		
		try {
			if (activeTraceDisplayer != null) {
				 // Line is 1 indexed, as per file
				line = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber() - 1;
			} else {
				return -1;
			}
		} catch (Exception ex) {
			System.err.println("Error getting line number.");
			ex.printStackTrace();
		}
		
		return line;
	}
	
	/**
	 * Given a set of blocks (e.g. visible blocks only, or all blocks in thread), compute the graphical
	 * horizontal position corresponding to the current line active in the trace viewer.
	 * 
	 * This needs to be computed with reference to blocks since threads are arbitrary slices in the
	 * space of absolute trace line numbers; the line numbers can only by converted to the pixel space
	 * by first mapping through blocks.
	 * 
	 * @param blockList
	 * @return
	 */
	private long getCurrentLineInstructionAbsoluteXPosition(List<LightweightThreadFunctionBlock> blockList) {
		int line = this.getCurrentLineNumber();
		
		if(null == blockList){
			return -1;
		}
		
		// search the blocks on screen
		LightweightThreadFunctionBlock inBlock = null;
		for(LightweightThreadFunctionBlock tfb : blockList) {
			if(line >= tfb.getStartLineNumber() && line <= tfb.getEndLineNumber()) {
				inBlock = tfb;
			}
		}
		
		// are we not in a block
		if(inBlock == null) {
			return -1;
		}
		
		return line - inBlock.getStartLineNumber() + inBlock.getXOffset(); // Used to have: - currentXOffset;
	}
	
	private void loadBlocks() {
		if(currentXOffset == currentLoadedBlocksXOffset && currentThread == currentLoadedBlocksThread 
				&& blockCanvasDisplayWidth == currentLoadedBlocksWidth && blocksInCurrentDisplay != null) {
			return;
		}
		if(null == blockLoader){
			return;
		}
		blocksInCurrentDisplay = blockLoader.getBlocksInRange(currentXOffset, currentXOffset + blockCanvasDisplayWidth);
		
		// queue up pages on both sides
		blockLoader.putRangeOnTopOfQueue(currentXOffset - blockLoader.getPageSize(), currentXOffset);
		blockLoader.putRangeOnTopOfQueue(currentXOffset + blockCanvasDisplayWidth, currentXOffset + blockCanvasDisplayWidth + blockLoader.getPageSize());
		
		currentLoadedBlocksXOffset = currentXOffset;
		currentLoadedBlocksWidth = blockCanvasDisplayWidth;
		currentLoadedBlocksThread = currentThread;
	}
	
	public void refresh() {
		if(!inLoadingScreen) {
			loadBlocks();
		}
		blockCanvas.redraw();
	}
	
	

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		if(!reacquireFileModel()){
			// TODO A quick fix to prevent exceptions when loading Atlantis when no trace file is
			// already loaded. Is this acceptable?
			return;
		}
		boolean scrollAfter = false;
		if(!fileModel.getOriginalFile().getName().equals(currentFile)) {
			scrollAfter = true;
			inLoadingScreen = true;
			
			currentFile = fileModel.getOriginalFile().getName();
			colorProvider = new ModuleColorProvider(blockCanvas.getDisplay());
			for(String module : fileModel.getInstructionDb().getModules()) {
				colorProvider.addModuleAndGenerateColorsIfNotPresent(module);
			}
			colorProvider.addModuleAndGenerateColorsIfNotPresent("NO_MODULE");
			
			List<Integer> intThreads = fileModel.getThreadFunctionBlockDb().getThreads();
			
			String[] threads = new String[intThreads.size()];
			int i = 0;
			for(int thread : intThreads) {
				threads[i++] = Integer.toString(thread);
			}
		
			goToCurrentLineButton.setEnabled(true);
			threadsCombo.setEnabled(true);
			threadsCombo.setItems(threads);
			
			if(threads.length > 0) {
				threadsCombo.select(0);
			}
		}
		
		threadsComboSelectionChanged(null);
		
		if(scrollAfter) {
			showCurrentTraceLineInView();
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) { }

	@Override
	public void partClosed(IWorkbenchPartReference partRef) { 
		IWorkbenchPart part = partRef.getPart(false);
		if (activeTraceDisplayer != null && activeTraceDisplayer == part) {
			if(blockLoader != null) {
				blockLoader.kill();
				blockLoader = null;
			}
			if(threadsCombo.isDisposed()){
				// When closing program, this check saves exceptions here
				return;
			}
			threadsCombo.setItems(new String[]{""});
			threadsCombo.setEnabled(false);
			goToCurrentLineButton.setEnabled(false);
			
			colorProvider.disposeAllColors();
			colorProvider = null;
			
			currentFile = null;
			fileModel = null;
			
			inLoadingScreen = false;
			
			blocksInCurrentDisplay = null;
			
			inLoadingScreen = false;
			
			currentThread = -1;
			currentThreadLength = -1;
			
			threadsComboSelectionChanged(null);
			
			blockCanvas.redraw();
		}
	}

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
