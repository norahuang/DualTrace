package ca.uvic.chisel.atlantis.eventtracevisualization;

import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.DEFAULT_ROW_LABEL_LENGTH;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.MARKER_HEIGHT;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.MIN_MARKER_WIDTH;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.ROW_HEIGHT;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.SCALE_FACTOR;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.SCROLLBAR_RIGHT_PADDING;
import static ca.uvic.chisel.atlantis.eventtracevisualization.TraceVisualizationConstants.VERTICAL_SHIFT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayerFactory;
import ca.uvic.chisel.atlantis.models.IEventMarkerModel;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
// needed to resolve an ambiguity with org.eclipse.swt.widgets.List
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.AnnotationsChangedListener;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalElement;
import ca.uvic.chisel.bfv.intervaltree.IntervalTree;

/**
 * View for displaying a visualization of the trace being viewed in the active Trace Displayer.
 * @authors Laura Chan and Patrick Gorman
 */
public abstract class AbstractTraceVisualizationView extends ViewPart implements IPartListener2, AnnotationsChangedListener {
	
	/**
	 * Class for the markers in the trace visualization for thread events. Markers are implemented with a Composite that has a listener
	 * that makes it jump to the corresponding trace line when clicked.
	 * @author Laura Chan
	 */
	public class EventMarker  implements VisualElement {
		private Composite marker;
		private Color defaultColor;
		private Color selectedColour;
		private int lineNumber;
		private int numLines;
		private Point location;
		private int width;
		private boolean selected;
		
		/**
		 * Creates a new thread event marker of the specified type.
		 * @param defaultColor the color of the marker when it is not selected
		 * @param selectedColor the color of the marker when it is selected
		 * @param location location on the canvas at which to place the marker
		 * @param lineNumber the line number to go to if the marker is clicked
		 * @param numLines The width of the marker in lines
		 */
		public EventMarker(Color defaultColor, Color selectedColor, Point location, int lineNum, int numLines) {
			this.defaultColor = defaultColor;
			this.selectedColour = selectedColor;
			this.lineNumber = lineNum;
			this.location = location;
			this.numLines = numLines;
			this.width = Math.max(numLines / SCALE_FACTOR, MIN_MARKER_WIDTH);
			this.selected = false;
		}
		
		@Override
		public void draw(GC gc, Point currentOffset, int virtualStartX) {
			
			//dispose and redraw if the marker is already initialized
			if(marker != null && !marker.isDisposed()) {
				marker.dispose();
			}
			
			//determine the final location and width
			int offsetX = location.x + currentOffset.x;
			Point finalLocation = new Point(offsetX, location.y + currentOffset.y);
			int finalWidth = width;
			
			if(virtualStartX > location.x) {
				finalLocation.x = virtualStartX + currentOffset.x;
				finalWidth = width - (virtualStartX - location.x);
			}
			
			// Have to use a composite even though a button might be more intuitive. 
			// Reason #1: using a button must trigger a lot more paint events or something--the code behaves almost like an infinite loop 
			// and runs out of handles
			// Reason #2: we can't set the color of the click-able part of the button
			marker = new Composite(canvas, SWT.NONE);
			
			marker.setBackground(selected ? this.selectedColour : this.defaultColor);
			marker.setLocation(finalLocation);
			marker.setSize(finalWidth, MARKER_HEIGHT);
			
			// Add a listener for jumping to the corresponding line when the marker is clicked. Also change the marker's colour to mark it as selected.
			marker.addListener(SWT.MouseDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (activeTraceDisplayer != null) {
						try {							
							activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(lineNumber, 0);
							EventMarker.this.select();
						} catch (Exception e) {
							BigFileApplication.showErrorDialog("Error navigating to line", "Invalid line number: " + lineNumber, e);
						}
					}
				}				
			});
		}
		
		@Override
		public void moveTo(GC gc, Point currentOffset, int virtualStartX) {
			if(marker == null || marker.isDisposed()) {
				draw(gc, currentOffset, virtualStartX);
				return;
			}
			
			//determine the final location and width
			int offsetX = location.x + currentOffset.x;
			Point finalLocation = new Point(offsetX, location.y + currentOffset.y);
			int finalWidth = width;
			
			if(virtualStartX > location.x) {
				finalLocation.x = virtualStartX + currentOffset.x;
				finalWidth = width - (virtualStartX - location.x);
			}
			
			marker.setLocation(finalLocation);
			marker.setSize(finalWidth, MARKER_HEIGHT);
		}
		
		/**
		 * Makes this marker the selected marker and changes its colour accordingly.
		 */
		private void select() {
			if (selectedMarker != null) {
				selectedMarker.deselect();
			}
			
			selectedMarker = this;
			if(marker != null) {
				marker.setBackground(selectedColour);
			}
			selected = true;
		}
		
		/**
		 * Deselects this marker, resetting it back to its default colour.
		 */
		private void deselect() {
			if(marker != null) {
				marker.setBackground(defaultColor);
			}
			selectedMarker = null;
			selected = false;
		}
		
		/**
		 * Disposes this marker by disposing its underlying widget.
		 */
		@Override
		public void dispose() {
			if(marker != null && !marker.isDisposed()) {
				marker.dispose();
			}
			marker = null;
		}

		@Override
		public IntervalElement<VisualElement> asInterval() {
			return new IntervalElement<VisualElement>(new Interval(location.x, location.x + width), this);
		}
	}
	
	protected AtlantisTraceEditor activeTraceDisplayer;
	private Canvas canvas;
	private Map<Integer, EventMarker> eventMarkers = new HashMap<Integer, EventMarker>();
	private List<VisualElement> elementsOnCanvas = new ArrayList<VisualElement>();
	private EventMarker selectedMarker = null;
	protected List<? extends IEventMarkerModel> eventMarkerModels = new ArrayList<IEventMarkerModel>();
	private boolean clearCanvas = false;
	private CanvasScrollControl scrollControl;
	
	private IntervalTree<VisualElement> intervalTree;
	private List<VisualElement> labelElements = new ArrayList<VisualElement>();
	private List<String> rowIds;
	protected AtlantisFileModelDataLayer fileModelCache;
	private List<IntervalElement<VisualElement>> visualElements = new ArrayList<IntervalElement<VisualElement>>();
	private List<IntervalElement<VisualElement>> cachedAnnotationElements;
	
	
	@Override
	public void createPartControl(Composite parent) {
		canvas = new Canvas(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_REDRAW_RESIZE);
		
	    // Add a paint listener to the canvas so that the visualization gets drawn (or cleared) when needed
 		canvas.addPaintListener(new PaintListener() {
 			@Override
 			public void paintControl(PaintEvent e) {
 				if (clearCanvas) {
 					doClearCanvas(e.gc);
 				}
 				
				paintVisualization(e.gc);
 			}
 		});
 		
 		initializeScrollControl();
 		
 		canvas.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Rectangle clientArea = canvas.getClientArea();
				scrollControl.handleResize(clientArea);
				
				// Must repaint if we are resizing to make it bigger, because the parts
				// that were previously hidden must be redrawn.
				repaint();
			}
		});
	    
	    
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	private void initializeScrollControl() {
		try {
 			scrollControl = new CanvasScrollControl(canvas);
 		} catch(Exception e) {
 			//TODO something
 			e.printStackTrace();
 		}
	}
	
	/**
	 * Paints the visualization of the current execution trace.
	 * @param gc GC with which to paint the visualization (issued by the PaintEvent that caused the canvas' paint listener to call this method)
	 */
	private void paintVisualization(GC gc) {
		
		if(null == fileModelCache || null == activeTraceDisplayer){
			return;
		}
		
		// if visual elements haven't been created yet
		// if(this.intervalTree == null) {
		if(this.rowIds == null) {
			initializeAnnotationElements();
			initializeVisualElements(gc);
			
			// this is the first paint, repaint to handle cases where the view was hidden when initialized
			repaint();
		}
		
		int labelSectionWidth = getLabelAreaWidth(gc);
		
		Point currentScrollOffset = scrollControl.getCurrentScrollOffset();
		
		// calculate the start and end X positions in the virtual visualization space.
		int virtualStartX = -1 * currentScrollOffset.x;
		int virtualEndX = virtualStartX + canvas.getSize().x;
		
		for(VisualElement labelElement : this.labelElements) {
			labelElement.draw(gc, currentScrollOffset, virtualStartX);
		}
		
		updatePagedVisualElements(virtualStartX, virtualEndX);
		
		List<VisualElement> intersectingMarkers = this.intervalTree.get(new Interval(virtualStartX, virtualEndX));
		List<VisualElement> disposedElements = new ArrayList<VisualElement>();
		
		for(VisualElement element : elementsOnCanvas) {
			if(intersectingMarkers.contains(element)) {
				// These elements are in the view port already, just move them to their new location
				element.moveTo(gc, new Point(currentScrollOffset.x + labelSectionWidth, currentScrollOffset.y), virtualStartX);
			}
			else {
				// These elements have fallen out of the view port, dispose
				element.dispose();
				disposedElements.add(element);
			}
		}
		
		elementsOnCanvas.removeAll(disposedElements);
		
		//no need to handle movers twice
		intersectingMarkers.removeAll(elementsOnCanvas);
		
		// draw all of the new elements that have entered the viewport
		for(VisualElement element : intersectingMarkers) {
			element.draw(gc, new Point(currentScrollOffset.x + labelSectionWidth, currentScrollOffset.y), virtualStartX);
			elementsOnCanvas.add(element);
		}
	}

	private int getRowLabelLength() {
		int length = DEFAULT_ROW_LABEL_LENGTH;
		
		if(rowIds != null) {
			for(String id : rowIds) {
				if(id != null) {
					length = id.length() > length ? id.length() : length;
				}
			}
		}
		
		return length;
	}
	
	private int getLabelAreaWidth(GC gc) {
		return getRowLabelLength() * gc.getFontMetrics().getAverageCharWidth() + TraceVisualizationConstants.LABEL_SECTION_PADDING;
	}

	private void initializeVisualElements(GC gc) {
		
		// We need to deal with the row labels on load for convenience.
		this.rowIds = getDistinctRowStrings(); // list of thread IDs for each thread
		int currentRowNum = 0;
		for(String id: rowIds){
			if(id != null) {
				labelElements.add(createLabeledRow("" + id, currentRowNum));
				currentRowNum++;
			}
		}
		
		Rectangle clientArea = canvas.getClientArea();
		int rightMostElementEndPoint = getRightmostElementEndPoint();
		int visWidth = rightMostElementEndPoint + SCROLLBAR_RIGHT_PADDING + getLabelAreaWidth(gc);
		int visHeight = rowIds.size() * ROW_HEIGHT;
		scrollControl.initializeScrollSizes(visWidth, visHeight, clientArea);

	}
	
	/**
	 * Load markers on the basis of whether the paged in set covers the specified line number.
	 * 
	 * @param lineNumber
	 */
	private void updatePagedVisualElementsToIncludeLineNumber(int lineNumber){
		TraceEventPager<?> pager = this.getEventPager();
		// Keep in mind that this call will load an unknown quantity of elements from the DB.
		// That's handled internally; it won't grab any if it doesn't need them.
		
		List<? extends IEventMarkerModel> newEventMarkerModels = pager.getModelsToCoverLineNumber(lineNumber);
		updatePaged(newEventMarkerModels);
	}


	/**
	 * On demand loading of results of a windowed query based on element start and end points in pixel space.
	 * 
	 * @param virtualEndX 
	 * @param virtualStartX 
	 */
	private void updatePagedVisualElements(int virtualStartX, int virtualEndX) {				
		// First step at refactoring...use the pager!
		TraceEventPager<?> pager = this.getEventPager();
		// Keep in mind that this call will load an unknown quantity of elements from the DB.
		// That's handled internally; it won't grab any if it doesn't need them.
		
		List<? extends IEventMarkerModel> newEventMarkerModels = pager.getModelsForRange(virtualStartX, virtualEndX);
		updatePaged(newEventMarkerModels);
	}
	
	private void updatePaged(List<? extends IEventMarkerModel> newEventMarkerModels){
		if(newEventMarkerModels.equals(eventMarkerModels)){
			return;
		} else {
			
			eventMarkerModels = newEventMarkerModels;
			visualElements.removeAll(cachedAnnotationElements);
			for(IntervalElement<VisualElement> element : visualElements){
				// TODO Could optimize by changing structures so we can keep old visual elements
				// instead of disposing and recreating below.
				element.getData().dispose();
			}
		}
		
		// Start fresh after disposing everything. Could optimize by not disposing and throwing all of them out.
		visualElements.clear();
		
		int currentRowNum = 0;
		int prevRowNum = 0;
		for (IEventMarkerModel eventModel : eventMarkerModels) {
						
			int index = rowIds.indexOf(eventModel.getIdentifier());
			if(index >= 0){
				currentRowNum = index;
			}
			if(currentRowNum != prevRowNum) {
				MarkerLinkVisualElement link = new MarkerLinkVisualElement(eventModel.getPixelStart(), prevRowNum * ROW_HEIGHT, currentRowNum);
				visualElements.add(link.asInterval());
			}
			
			Point location = new Point(eventModel.getPixelStart(), currentRowNum * ROW_HEIGHT + VERTICAL_SHIFT);
			EventMarker marker = new EventMarker(eventModel.getDefaultColor(), eventModel.getSelectedColor(), location, eventModel.getLineNumber(), eventModel.getNumLines());
			eventMarkers.put(eventModel.getLineNumber(), marker);
			
			visualElements.add(marker.asInterval());
			prevRowNum = currentRowNum;
		}
		
		visualElements.addAll(cachedAnnotationElements); // will combine after paging?
		this.intervalTree = new IntervalTree<VisualElement>(visualElements);
		
	}

	/**
	 * This method will iterate through the comments in the activeTraceDisplay's Trace object and create visual
	 * elements on the screen for each one.
	 * 
	 * @param visualElements The List that the calculated annotation elements should be added to.
	 */
	private void initializeAnnotationElements() {
		// Paging of those contents based on database queries,
		// makes it useful to simply cache the annotation elements.
		// They don't get paged, and don't need to be recomputed.
		
		cachedAnnotationElements = new ArrayList<>();
		
		ColorRegistry colorRegistry = AtlantisActivator.getDefault().getColorRegistry();
		
		Collection<Tag> tags = activeTraceDisplayer.getProjectionViewer().getFileModel().getTags();
		
		for(Tag tag : tags) {
			Color color = colorRegistry.get(tag.getColour());
			
			for(TagOccurrence occurance : tag.getOccurrences()) {
				createAnnotationElement(occurance.getStartLine(), color, cachedAnnotationElements);
			}
		}
		
		// Add in all of the comments
		for(CommentGroup cg : this.activeTraceDisplayer.getProjectionViewer().getFileModel().getCommentGroups()) {
			Color color = colorRegistry.get(cg.getColour());
			
			for(Comment comment : cg.getComments()) {
				createAnnotationElement(comment.getLine(), color, cachedAnnotationElements);
			}
		}
	}

	private void createAnnotationElement(
			int line, Color color,
			List<IntervalElement<VisualElement>> visualElements) {
		
		IEventMarkerModel eventAtLineNum = this.getEventAtLineNum(line);
		
		if(eventAtLineNum == null) {
			return;
		}
		
		int markerStartLine = eventAtLineNum.getLineNumber();
		EventMarker marker = eventMarkers.get(markerStartLine);
		
		if(marker == null) {
			return;
		}
		
		try {
			Interval lineInterval = new Interval(marker.lineNumber, marker.lineNumber +  marker.numLines);
			Interval positionInterval = new Interval(marker.location.x, marker.location.x + marker.width);
			
			int xLocation = (int)Interval.project(lineInterval, line, positionInterval);
			int startY = marker.location.y;
			AnnotationVisualElement newElement = new AnnotationVisualElement(xLocation, startY, startY - 5, color);
			
			visualElements.add(newElement.asInterval());
		} catch(Exception e) {
			//TODO log an error, but just continue
		}
	}

	/**
	 * Draws a new row in the table with alternating colors.  This row is the background color used to
	 * display a single threads entries.
	 * 
	 * @param gc 
	 * 		The GC used to draw
	 * @param numThreads
	 * 		The number of threads that are currently in the system.  This also corresponds to the next row index.
	 */
	private RowLabelVisualElement createLabeledRow(String label, int rowNum) {
		// Alternate the background colors for the rows
		Color rowColour;
		if (rowNum % 2 == 1) {
			rowColour = AtlantisActivator.getDefault().getColorRegistry().get(AtlantisColourConstants.THREAD_ROW);
		} else {
			rowColour = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE);
		}
		
		RowLabelVisualElement row = new RowLabelVisualElement(rowColour, rowNum, canvas, label, Integer.MAX_VALUE);
		return row;
	}
	
	/**
	 * Sets the clearCanvas flag and sends a redraw request to the canvas so that the next paint event handled by the canvas' paint listener
	 * calls the doClearCanvas() method.
	 */
	private void clearCanvas() {
		resetViewData();
		clearCanvas = true;
		repaint();
	}
	
	/**
	 * Clears the canvas of any graphics drawn on it.
	 * @param gc GC to use to clear the canvas (issued by the PaintEvent that caused the canvas' paint listener to call this method)
	 */
	private void doClearCanvas(GC gc) {
		// Draw a rectangle to cover up everything on the canvas
		gc.setBackground(canvas.getBackground());
		gc.fillRectangle(canvas.getClientArea());
		
		clearCanvas = false;
	}

	private void resetViewData() {
		// Dispose any thread event markers
		if (eventMarkers != null) {
			for (EventMarker marker : eventMarkers.values()) {
				marker.dispose();
			}
			
			eventMarkers = new HashMap<Integer, EventMarker>();
		}
		
		intervalTree = null;
		
		if(eventMarkerModels != null) {
			eventMarkerModels = new ArrayList<IEventMarkerModel>();
		}
		
		if(elementsOnCanvas != null) {
			elementsOnCanvas.clear();
		}
		
		if(labelElements != null) {
			labelElements.clear();
			rowIds = null;
		}
		
		if(scrollControl != null) {
			scrollControl.resetScrollbars();
		}
		
		if(visualElements != null) {
			visualElements.clear();
		}
		
		if(cachedAnnotationElements != null) {
			cachedAnnotationElements.clear();
		}
	}
	
	/**
	 * Redraws and updates the canvas.
	 */
	private void repaint() {
		Rectangle portal = canvas.getClientArea();
		canvas.redraw(portal.x, portal.y, portal.width, portal.height, false);
		canvas.update();
	}

	/**
	 * Selects the appropriate thread event marker for the given line number. If the line is a thread event, then its marker is selected; 
	 * otherwise, the marker for the first thread event line before the specified line is selected. 
	 * @param lineNumber number of the line whose corresponding marker is to be selected
	 */
	public void selectMarker(int lineNumber) {
		if (activeTraceDisplayer != null && traceDisplayerContentApplicable()) {
			
			IEventMarkerModel eventModel = getEventAtLineNum(lineNumber);

			if(eventModel == null || !eventMarkers.containsKey(eventModel.getLineNumber())) {
				//Then there is no event marker that should be selected for this line
				return;
			}
			
			EventMarker marker = eventMarkers.get(eventModel.getLineNumber());
			
			if(marker.selected) {
				return;
			}
			
			marker.select();
			
			Rectangle viewWindowSize = canvas.getClientArea();
			Point markerLocation = marker.location;
			Point currentScrollbarLocation = this.scrollControl.getCurrentScrollOffset();
			
			int x1 = currentScrollbarLocation.x * -1;
			int x2 = currentScrollbarLocation.x * -1 + viewWindowSize.width;
			//Only move the scrollbar if the marker is out of view
			if(markerLocation.x < x1 || markerLocation.x > x2){	
				//center the marker, unless it is just starting
				if(currentScrollbarLocation.x < 0){
					Point originalPoint = marker.location;
					Point newPoint = new Point(originalPoint.x - (canvas.getClientArea().width/2) + marker.width, originalPoint.y);
					this.scrollControl.setLocation(newPoint, canvas.getClientArea().width, canvas.getClientArea().height);
				}
				else{
					this.scrollControl.setLocation(marker.location, canvas.getClientArea().width, canvas.getClientArea().height);
				}
			}
			this.repaint();
		}
	}
	
	/**
	 * 
	 * @param lineNum the line number that is in focus
	 * @return the TraceThreadEvent that is active at this line, or null if one cannot be found.
	 */
	private IEventMarkerModel getEventAtLineNum(int lineNum){
		
		if(this.eventMarkerModels == null) {
			// Occurs when first opening file
			return null;
		}
		
		// Ensure that the markers for the desired line number are paged in.
		updatePagedVisualElementsToIncludeLineNumber(lineNum);
		for(IEventMarkerModel eventModel : eventMarkerModels) {
			if(eventModel.getLineNumber() <= lineNum && eventModel.getLineNumber() + eventModel.getNumLines() > lineNum) {
				return eventModel;
			}
		}
		
		return null;
	}
	
	/**
	 * Selects the thread event marker for the active trace displayer's current line.
	 */
	private void selectMarkerForCurrentLine() {
		if (activeTraceDisplayer != null && traceDisplayerContentApplicable()) {
			int currentLine = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber();
			selectMarker(currentLine);
		}
	}
	
	@Override
	public void setFocus() {
		canvas.setFocus();
	}
	
	@Override
	public void dispose() {
		this.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	// TODO this is called twice, once from the CommonNavigator, and once from the trace displayer, we should probably make this work 
	// independently of the trace displayer?
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				if (part != activeTraceDisplayer) { // don't need to redraw the visualization if the active Trace Displayer hasn't changed
					activeTraceDisplayer = (AtlantisTraceEditor) part;
					IFileModelDataLayer fileModelFromReg = RegistryUtils.getFileModelDataLayerFromRegistry();
					
					if (traceDisplayerContentApplicable() && fileModelFromReg instanceof AtlantisFileModelDataLayer) {
						try {
							
							fileModelCache = (AtlantisFileModelDataLayer)fileModelFromReg;
							
							// First, clear the previous trace's visualization and get rid of any selected marker data
							eventMarkerModels = null;
							rowIds = null;
							selectedMarker = null;
							clearCanvas(); 
							resetEventPager();
							
							// Paint the new active trace's visualization
							fileModelCache.registerAnnotationChangedListener(this);
						}
						catch(Exception e) {
							// TODO log an error
							e.printStackTrace();
						}
						repaint();
						selectMarkerForCurrentLine();
					} else {
						eventMarkerModels = null;
						selectedMarker = null;
						rowIds = null;
						clearCanvas();
						resetEventPager();
					}
				}
			} else {
				activeTraceDisplayer = null;
				eventMarkerModels = null;
				selectedMarker = null;
				clearCanvas();
				resetEventPager();
			}
		} 
	}

	protected abstract boolean traceDisplayerContentApplicable();
	
	/**
	 * @return	Get the strings that will be used as row headers.
	 */
	protected abstract List<String> getDistinctRowStrings();
	
	/**
	 * @return	Get the rightmost end point that we need to render.
	 */
	protected abstract int getRightmostElementEndPoint();
	
	protected abstract TraceEventPager getEventPager();
	
	protected abstract void resetEventPager();

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (activeTraceDisplayer != null && activeTraceDisplayer == part) {
			activeTraceDisplayer = null;
			eventMarkerModels = null;
			selectedMarker = null;
			rowIds = null;
			clearCanvas();
			resetEventPager();
			
			// This is done to ensure that there is no memory leak, by having the static FileDAO maintain a reference to every view opened
			if(fileModelCache != null) {
				fileModelCache.deregisterAnnotationChangedListener(this);
			}
		} // Otherwise, some other editor was closed, so we can ignore it
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

	// TODO could choose to only reload annotation elements based on the eventType
	@Override
	public void handleAnnotationChanged(EventType type) {
		
		if(visualElements == null || visualElements.isEmpty()) {
			return;
		}
		
		// Remove annotation elements, reconstruct the set, and re-add it.
		visualElements.removeAll(cachedAnnotationElements);
		initializeAnnotationElements();
		visualElements.addAll(cachedAnnotationElements);
		
		this.intervalTree = new IntervalTree<>(visualElements);
		
		this.repaint();
	}

	
}
