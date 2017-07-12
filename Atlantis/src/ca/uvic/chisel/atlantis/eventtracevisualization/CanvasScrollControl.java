package ca.uvic.chisel.atlantis.eventtracevisualization;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

public class CanvasScrollControl {

	private static final int DEFAULT_SCROLL_INCREMENT = 50;
	private Point currentScrollOffset = new Point(0,0);
	private Canvas canvas;
	private boolean verticalScroll;
	private boolean horizontalScroll;
	private int totalWidth = 0;
	private int totalHeight = 0;
	
	public CanvasScrollControl(Canvas canvas) throws Exception {
		this(canvas, true, true);
	}
	
	public CanvasScrollControl(Canvas canvas, boolean verticalScroll, boolean horizontalScroll) throws Exception {
		
		this.canvas = canvas;
		this.verticalScroll = verticalScroll;
		this.horizontalScroll = horizontalScroll;
		
		initializeScrollBars();
	}

	private void initializeScrollBars() throws Exception {
		if(verticalScroll) {
			intializeVerticalScroll();
		}
		
		if(horizontalScroll) {
			initializeHorizontalScroll();
		}
	}
	
	public Point getCurrentScrollOffset() {
		return currentScrollOffset;
	}
	
	public void resetScrollbars() {
		ScrollBar hBar = canvas.getHorizontalBar();
		ScrollBar vBar = canvas.getVerticalBar();
		
		if(horizontalScroll) {
			resetBar(hBar);
		}
		
		if(verticalScroll) {
			resetBar(vBar);
		}
	}

	private void resetBar(ScrollBar hBar) {
		hBar.setMaximum(1);
		hBar.setVisible(false);
	}
	
	/**
	 * 
	 * @param totalWidth Must be a value greater than 0, represents the total number of horizontal pixels of the whole visualization
	 * @param totalHeight Must be a value greater than 0, represents the total number of vertical pixels of the whole visualization
	 * @param clientWidth The width of the current viewport
	 * @param clientHeight The height of the current viewport
	 */
	public void initializeScrollSizes(int totalWidth, int totalHeight, Rectangle clientArea) {
		this.totalWidth = totalWidth;
		this.totalHeight = totalHeight;
		
		handleResize(clientArea);
	}
	
	public void setLocation(Point virtualLocation, int clientWidth, int clientHeight) {
		if(horizontalScroll) {
			canvas.getHorizontalBar().setSelection(virtualLocation.x);
			this.currentScrollOffset.x = -1 * (int) Math.min(totalWidth - clientWidth, virtualLocation.x);
		}
		
		if(verticalScroll) {
			canvas.getVerticalBar().setSelection(virtualLocation.y);
			
			if(totalHeight - clientHeight <= 0) {
				this.currentScrollOffset.y = 0;
			} else {
				this.currentScrollOffset.y = -1 * (int) Math.min(totalHeight - clientHeight, virtualLocation.y);
			}
			
		}
	}
	
	public void handleResize(Rectangle clientArea)  {
		int clientWidth = clientArea.width;
		int clientHeight = clientArea.height;

		//The scrollBars are not initialized yet
		if(totalWidth == 0 || totalHeight == 0) {
			return;
		}
		
		if(verticalScroll) {
			setScrollBarSize(canvas.getVerticalBar(), totalHeight - clientHeight);
		}
		
		if(horizontalScroll) {
			setScrollBarSize(canvas.getHorizontalBar(), totalWidth - clientWidth);
		};

		// make sure that we aren't wasting vertical space.
		int bottomYPos = (-1 * currentScrollOffset.y) + clientHeight;
		if(bottomYPos > totalHeight) {
			
			int yToMove = bottomYPos - totalHeight;

			int newX = -1 * currentScrollOffset.x;
			int newY = -1 * currentScrollOffset.y - yToMove;
			
			setLocation(new Point(newX, newY), clientWidth, clientHeight);
		}
	}

	private void setScrollBarSize(ScrollBar verticalBar, int size) {
		verticalBar.setMaximum(Math.max(0, size));
		verticalBar.setVisible(size > 0 ? true : false);
	}
	

	private void intializeVerticalScroll() throws Exception {
		final ScrollBar verticalBar = canvas.getVerticalBar();
		
		if(verticalBar == null) {
			throw new Exception("Cannot create vertical scroll control on a canvas with no vertical Scroll Bar");
		}
		
		verticalBar.setVisible(false);
		verticalBar.setIncrement(DEFAULT_SCROLL_INCREMENT);
		
		// The below event handlers allow for horizontal scrolling functionality
		verticalBar.addListener(SWT.Selection, new Listener() {
	        @Override
			public void handleEvent(Event e) {
	            int vSelection = verticalBar.getSelection();
	            int destY = -vSelection - currentScrollOffset.y;
	            
	            //scrolling here helps with the smoothness of the repaint
	            canvas.scroll(0, destY, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
	            
	            currentScrollOffset.y = -vSelection;
	            
	            Rectangle portal = canvas.getClientArea();
	            
	            // Only redraw the visible part of the canvas, this speeds up the redraw significantly
	            canvas.redraw(portal.x, portal.y, portal.width, portal.height, false);
	        }
	    });
	}

	/**
	 * Initializes the horizontal scroll bar on the canvas.  Sets the scroll bar to be invisible.
	 * @param canvas The canvas which owns the horizontal bar.
	 * @throws Exception
	 */
	private void initializeHorizontalScroll() throws Exception {
		
		final ScrollBar horizontalBar = canvas.getHorizontalBar();
		
		if(horizontalBar == null) {
			throw new Exception("Cannot create horizontal scroll control on a canvas with no horizontal Scroll Bar");
		}
		
		horizontalBar.setVisible(false);
		horizontalBar.setIncrement(DEFAULT_SCROLL_INCREMENT);
		
		
		// The below event handlers allow for horizontal scrolling functionality
		horizontalBar.addListener(SWT.Selection, new Listener() {
	        @Override
			public void handleEvent(Event e) {
	            int hSelection = horizontalBar.getSelection();
	            int destX = -hSelection - currentScrollOffset.x;
	            
	            //scrolling here helps with the smoothness of the repaint
	            canvas.scroll(destX, 0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
	            
	            currentScrollOffset.x = -hSelection;
	            
	            Rectangle portal = canvas.getClientArea();
	            
	            // Only redraw the visible part of the canvas, this speeds up the redraw significantly
	            canvas.redraw(portal.x, portal.y, portal.width, portal.height, false);
	        }
	    });
	}
	
}
