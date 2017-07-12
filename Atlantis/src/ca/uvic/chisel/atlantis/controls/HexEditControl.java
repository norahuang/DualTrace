package ca.uvic.chisel.atlantis.controls;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.HexVisualization;
import ca.uvic.chisel.atlantis.views.MemoryVisualization;

public class HexEditControl extends Composite {
	
	// the number of bits to display (8,16,32,64...)
	private static final int DISPLAY_BITS = 64;
		
	// text controls
	private StyledText addressTextControl;
	private StyledText hexTextControl;
	private StyledText asciiTextControl;
	
	SortedMap<Integer, String> memoryEntryFromHexTextMap;
	
	private Action watchAddressesAction;
	private Action jumpToChangeAction;
	
	// the font to use for displaying numbers
	private final Font numberFont;
	
	// colors
	private Color location;
	private Color faded;
	private Color foreground;
	private Color highlight; 
	private Color pink;

	private HexVisualization parentHexVisualization;
	
	//address need to be highlighted
	private BigInteger highlightAddress;

	
	public HexEditControl(HexVisualization hexVisualization, Composite parent, int style) {
		super(parent, style);
		
		parentHexVisualization = hexVisualization;
		
		// layout
		GridLayout layout = new GridLayout(3, false);
		setLayout(layout);
		
		// setup the font
		FontData fd = JFaceResources.getFont(JFaceResources.TEXT_FONT).getFontData()[0];
		fd.setHeight(10);
		numberFont = new Font(getDisplay(), fd);
		
		// colors
		location = new Color(getDisplay(), 0x1F, 0x45, 0xFC);
		faded = new Color(getDisplay(), 0xCC, 0xCC, 0xCC);
		highlight = new Color(getDisplay(), 0xDC, 0x38, 0x1F);
        pink = new Color(getDisplay(), 0xFF, 0x9B, 0xB4);
		
		
		// address text
		addressTextControl = new StyledText(this, 0);
		addressTextControl.setFont(numberFont);
		addressTextControl.setLineSpacing(0);
		addressTextControl.setEditable(false);
		addressTextControl.setCaret(null);
		GridData addressGridData = new GridData();
		addressGridData.verticalAlignment = GridData.FILL;
		addressGridData.horizontalAlignment = GridData.FILL;
		addressGridData.grabExcessVerticalSpace = true;
		addressTextControl.setLayoutData(addressGridData);
		foreground = addressTextControl.getForeground();
		addressTextControl.setForeground(location);
		addressTextControl.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) { }
			@Override
			public void mouseDown(MouseEvent arg0) {
				hexTextControl.setSelection(0, 0);
				asciiTextControl.setSelection(0, 0);
				
				// Watch whole row context menu
//				mgr.add(watchAddressesAction);
				
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		addressTextControl.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.character == 3 && (e.stateMask & SWT.CTRL) != 0) {
					String text = addressTextControl.getSelectionText();
					text = text.replaceAll(" ", "");
					text = text.replaceAll("\n", "");
					Transfer transfer = TextTransfer.getInstance();
					new Clipboard(getDisplay()).setContents(
							new Object[] { text },
				            new Transfer[] { transfer });
					e.doit = false;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) { }
		});
		
		// hex text
		hexTextControl = new StyledText(this, 0);
		hexTextControl.setFont(numberFont);
		hexTextControl.setLineSpacing(0);
		hexTextControl.setEditable(false);
		hexTextControl.setCaret(null);
		GridData hexGridData = new GridData();
		hexGridData.verticalAlignment = GridData.FILL;
		hexGridData.horizontalAlignment = GridData.FILL;
		hexGridData.grabExcessVerticalSpace = true;
		hexTextControl.setLayoutData(hexGridData);
		hexTextControl.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) { }
			@Override
			public void mouseDown(MouseEvent arg0) {
				addressTextControl.setSelection(0, 0);
				asciiTextControl.setSelection(0, 0);
				
				// Watch highlighted context menu
				if(arg0.button == 3){
					// Problem: I need the Watch view to work with this hex view. I set up a context
					// menu, but when the right click occurs away from the (invisible) caret position,
					// the code will not have that information available. The right click also
					// fails to deselect any selected region. This is the very firm behavior of StyledTextEditor.
					// Right clicking in a StyledText widget does not update the selection point.
					// We cannot extend the class. We cannot fire a programmatic event to change the selection.
					// We cannot compute the selection on the basis of what data we have available.
					// It is distasteful to take the code from that class and copy it into another class,
					// largely because there will be default scoping (package) on all sorts of related
					// classes that the code would use, and we would have to copy all of those classes, and so on...
					// I refuse to copy an entire package to have a right click behave this way.
					// Without further ado, hacks....
					
					// Create a mouse event to trigger (possibly temporary) moving of the StyledText
					// selection. Not that mouse events and selections are not the same thing...but we
					// cannot externally translate between them. We trigger the selection change by using
					// this mouse event.
					// MouseEvent{StyledText {} time=-1767508159 data=null button=3 stateMask=0x0 x=10 y=58 count=1}
					Event leftClickEvent = new Event();
					leftClickEvent.button = 1; // Don't clone this one!
					leftClickEvent.x = arg0.x;
					leftClickEvent.y = arg0.y;
					leftClickEvent.data = arg0.data;
					leftClickEvent.stateMask = arg0.stateMask;
					leftClickEvent.count = arg0.count;
					leftClickEvent.time=arg0.time;
					leftClickEvent.widget = arg0.widget;
					
					/* 
					 * Warning! This is very hackish and messes witht he selection. If there are any selection
					 * listeners that do anything "important", they will be triggered.
					 * 1. Get selection
					 * 2. Trigger mouse event for the right click
					 * 3. Re-get new selection
					 * 4. See if it is a subset of the original. If so...give menu old selection
					 *        if it isn't, give menu the new selection (point)
					 * 5. But...I don't want to deselect the old selection, do I? Perhaps I can
					 *    jam it right back into the styledText as is...
					 * 
					 */
					Point originalSelection = hexTextControl.getSelection();
					
					hexTextControl.notifyListeners(SWT.MouseDown, leftClickEvent);
					hexTextControl.notifyListeners(SWT.MouseUp, leftClickEvent);
					
					Point newRightClickSelection = hexTextControl.getSelection();

					// If the new selection is within the old one, then put the old selection back to preserve
					// the range of it. This only matters for when x and y are unequal, and the text is highlighted.
					// otherwise, the right click was outside the highlighted text, so we leave the new selection as is.
					if(newRightClickSelection.x > originalSelection.x && newRightClickSelection.x < originalSelection.y){
						hexTextControl.setSelection(originalSelection.x, originalSelection.y);
					}
					
					hexTextControl.getMenu().setVisible(true);
				}
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		hexTextControl.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.character == 3 && (e.stateMask & SWT.CTRL) != 0) {
					String text = hexTextControl.getSelectionText();
					text = text.replaceAll(" ", "");
					text = text.replaceAll("\n", "");
					Transfer transfer = TextTransfer.getInstance();
					new Clipboard(getDisplay()).setContents(
							new Object[] { text },
				            new Transfer[] { transfer });
					e.doit = false;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) { }
		});
		
		// ascii text
		asciiTextControl = new StyledText(this, 0);
		asciiTextControl.setFont(numberFont);
		asciiTextControl.setLineSpacing(0);
		asciiTextControl.setEditable(false);
		asciiTextControl.setCaret(null);
		GridData asciiGridData = new GridData();
		asciiGridData.verticalAlignment = GridData.FILL;
		asciiGridData.horizontalAlignment = GridData.FILL;
		asciiGridData.grabExcessVerticalSpace = true;
		asciiGridData.grabExcessHorizontalSpace = true;
		asciiTextControl.setLayoutData(asciiGridData);
		asciiTextControl.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) { }
			@Override
			public void mouseDown(MouseEvent arg0) {
				addressTextControl.setSelection(0, 0);
				hexTextControl.setSelection(0, 0);
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		asciiTextControl.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.character == 3 && (e.stateMask & SWT.CTRL) != 0) {
					String text = asciiTextControl.getSelectionText();
					text = text.replaceAll("\n", "");
					Transfer transfer = TextTransfer.getInstance();
					new Clipboard(getDisplay()).setContents(
							new Object[] { text },
				            new Transfer[] { transfer });
					e.doit = false;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) { }
		});
		
		addDisposeListener(new DisposeListener() {
	         public void widgetDisposed(DisposeEvent e) {
	        	 location.dispose();
	        	 faded.dispose();
	        	 
	        	 numberFont.dispose();
	        	 
	        	 addressTextControl.dispose();
	        	 hexTextControl.dispose();
	        	 asciiTextControl.dispose();
	         }
	    });
		
		createContextMenu();
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
		Menu menu = menuMgr.createContextMenu(hexTextControl);
		hexTextControl.setMenu(menu);
	}

	static private String JUMP_TO_ADDRESS_MENU_ITEM_TEXT = "Jump to Line of Most Recent Change";
	static private String WATCH_ADDRESS_MENU_ITEM_TEXT = "Add to Watch View";
	private void fillContextMenu(IMenuManager mgr) {
		Collection<String> addressList = getMouseSelectedAddressList();
		Iterator<String> iter = addressList.iterator();
		MemoryReference memoryEntry;
		String addressText;
		String lineNumberText;
		if(iter.hasNext()){
			addressText = iter.next();
			memoryEntry = ModelProvider.INSTANCE.getMemory(addressText);
			jumpToChangeAction.setEnabled(null != memoryEntry);
			lineNumberText = "line "+((null == memoryEntry) ? "not set yet" : memoryEntry.getLineNumber()+1);
		} else {
			addressText = "N/A";
			jumpToChangeAction.setEnabled(false);
			lineNumberText = "N/A";
			addressText = "N/A";
		}
		
		addressText = StringUtils.leftPad(addressText, 16, "0");
		String lastAddressOfRange = null;
		while(iter.hasNext()){
			lastAddressOfRange = iter.next();
		}
		if(null != lastAddressOfRange){
			addressText += " to "+StringUtils.leftPad(lastAddressOfRange, 16, "0");
		}
		jumpToChangeAction.setText(JUMP_TO_ADDRESS_MENU_ITEM_TEXT+" ("+lineNumberText+")");
		watchAddressesAction.setText(WATCH_ADDRESS_MENU_ITEM_TEXT+" ["+addressText+"]");
		
		mgr.add(jumpToChangeAction);
		mgr.add(watchAddressesAction);
	}
	
	public void createActions() {
		jumpToChangeAction = new Action(JUMP_TO_ADDRESS_MENU_ITEM_TEXT) {
			@Override
			public void run() {
				Collection<String> addressList = getMouseSelectedAddressList();
				if(null == addressList){
					return;
				}
				try {
					Iterator<String> iter = addressList.iterator();
					if(!iter.hasNext()){
						return;
					}
					String addressTarget = iter.next();
					
					MemoryReference memoryRef = ModelProvider.INSTANCE.getMemory(addressTarget);
					int lineNumber  = 0;
					if(memoryRef != null){
						lineNumber = memoryRef.getLineNumber();
					} else {
						lineNumber = 0;
					}

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
		};
		
		watchAddressesAction = new Action(WATCH_ADDRESS_MENU_ITEM_TEXT) {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewPart view = page.findView(MemoryVisualization.ID);
				MemoryVisualization memVis = (MemoryVisualization)view;
				
				Collection<String> addressList = getMouseSelectedAddressList();
				if(null == addressList){
					return;
				}
				for (String address : addressList) {
					memVis.getWatchedLocations().add(address);
				}
				
				memVis.updateWatchedLocations();
			}
		};
	
	}
	
	private Collection<String> getMouseSelectedAddressList(){
		// I have to convert the selected block to memory addresses, and set the menu
		// option to say specifically which memory locations (within the range) will be watched.
		Point selectionPoint = hexTextControl.getSelection();
		
		Collection<String> addressList = convertMouseSelectionRangeToMemoryLocationRange(selectionPoint);
		return addressList;
	}
	
	/**
	 * Given the text preparation we did on the refresh(), figure out what models are associated with the text selection provided
	 * 
	 * @param selectionPoint
	 * @param selectionRange
	 * @return
	 */
	private Collection<String> convertMouseSelectionRangeToMemoryLocationRange(Point selectionPoint){
		// We get coordinates for the first four address words: 37, 46, 55.
		// The first column is all multiples of 38 after subtracting 37 first.
		// Each column is some addition of 9 (8 hex plus a space).
		// Then add an additional 1 to the y coordinate in order to get the inclusive subMap.
		
		if(selectionPoint != null){
			// If only a point, grab the word it is in
			int startLineCount = 1 * ((selectionPoint.x - 37) / 38);
			int endLineCount = 1 * ((selectionPoint.y - 37) / 38);
			int start = selectionPoint.x - ((selectionPoint.x - 37 - startLineCount*38) % 9); // mod 9 because of each column being 9 wide
			int end = selectionPoint.y - ((selectionPoint.y - 37 - endLineCount*38) % 9) + 1; // + 1 for inclusive idnex later.
			if(end - start > 1 && (end - 2) % 9 == 0){
				// Check the end minus two (minus 1 for the inclusive modification, minus 1 for the 1 offset we have throughout.
				// If this is true, we are fenceposted at the onset of the next address, because the user has highlighted up
				// to but not including the next address up. In this case, let's pop the end down one to prevent
				// inclusion of the next non-highlighted address.
				// Could assign x+1 instead of subtracting 2...but no, we can't if several words are highlighted.
				end -= 2;
			}
			//	System.out.println(selectionPoint);
			//	System.out.println(start+", "+end);
			//	System.out.println("+"+startLineCount+", "+"+"+endLineCount);
			return memoryEntryFromHexTextMap.subMap(start, end).values();
		} else {
			return null;
		}
	}
	
	public int getCurrentWidthInBytes() {
		return 16;
	}
	
	public int getCurrentByteCapacity() {
		int height = (getBounds().height - 14) / hexTextControl.getLineHeight() - 1;
		
		return getCurrentWidthInBytes() * height;
	}
	
	public void setAddress(BigInteger address) {
		
		BigInteger tempAddress = address;
		BigInteger lineLength = BigInteger.valueOf(getCurrentWidthInBytes());
		int heightInLines = getCurrentByteCapacity() / getCurrentWidthInBytes();
		String addressText = "                 ";
		
		for(int i = 0; i < heightInLines; i++) {
			addressText += "  \n";
			
			String hexAddress = tempAddress.toString(16).toUpperCase();
			int newNibbles = (DISPLAY_BITS/4) - hexAddress.length();
			for(int j = 0; j < newNibbles; j++) {
				hexAddress = "0" + hexAddress;
			}
			
			addressText += hexAddress;
			
			tempAddress = tempAddress.add(lineLength);
		}
		
		addressTextControl.setText(addressText);
	}

	public void setHexData(String hexData, BigInteger firstMemoryAddress) {
		
		memoryEntryFromHexTextMap = new TreeMap<Integer, String>();
		
		// clear styles
		hexTextControl.replaceStyleRanges(0, hexTextControl.getText().length(), new StyleRange[0]);
		
		// hex
		StringBuilder hexText = new StringBuilder();
		List<StyleRange> hexStyles = new ArrayList<StyleRange>();
		int start = 0;
		
		// Column headers (address suffix or offset)
		for(int i = 0; i < getCurrentWidthInBytes(); i++) {
			if(i % 4 == 0 && i != 0) {
				hexText.append(" ");
				start++;
			}
			hexText.append(String.format("%01x", i));
			hexText.append(" ");
			start += 2;
		}
		
		hexStyles.add(new StyleRange(0, start, location, hexTextControl.getBackground()));
		
		BigInteger currentAddressColumn = firstMemoryAddress;
		BigInteger four = BigInteger.valueOf(4);
		BigInteger eight = BigInteger.valueOf(8);
		
		// Just added the -2. How did this ever work before?
		for(int i = 0; i < hexData.length() - 2; i += 2) {
			if(i % 8 == 0 && i != 0) {
				// Append space after the value
				hexText.append(" ");
				start++;
			}
			
			if(i % (getCurrentWidthInBytes() * 2) == 0) {
				hexText.append(" \n");
				start += 2;
			}
			
			String hexByte = hexData.substring(i, i + 2);
			Color byteColor = null;
			
			if(hexByte.equals("??")) {
				byteColor = faded;
			} else {
				byteColor = pink;
			}
			// Make a registry for each address indexed by the first char position. Need to fetch objects
			// for mouse and menu interactions.
			if(i % 8 == 0){
				memoryEntryFromHexTextMap.put(hexText.length(), currentAddressColumn.toString(16).toUpperCase());
				currentAddressColumn = currentAddressColumn.add(four);
			}
			
			
			if(this.highlightAddress != null && (this.highlightAddress.equals(currentAddressColumn.subtract(four)) || this.highlightAddress.equals(currentAddressColumn.subtract(eight)))
			   ) 
			{
				byteColor = highlight;
			}
			
			hexStyles.add(new StyleRange(start, 2, byteColor, hexTextControl.getBackground()));
			

			
			start += 2;
			hexText.append(hexByte);
		}
		
		hexTextControl.setText(hexText.toString());
		
		if(hexData.length() > 0) {
			StyleRange[] ranges = hexStyles.toArray(new StyleRange[0]);
			hexTextControl.setStyleRanges(ranges);
		}
		
		// ascii
		StringBuilder asciiText = new StringBuilder();
		List<StyleRange> asciiStyles = new ArrayList<StyleRange>();
		start = 0;
		
		for(int i = 0; i < hexData.length(); i += 2) {
			if(i % (getCurrentWidthInBytes() * 2) == 0) {
				asciiText.append("\n");
				start++;
			}
			
			String byteString = hexData.substring(i, i + 2);
			String character = null;
			Color charColor = foreground;
			
			if(byteString.equals("??")) {
				charColor = faded;
				character = ".";
			} else {
				int byteValue = Integer.parseInt(byteString, 16);
				
				if(byteValue < 32 || byteValue > 126) {
					charColor = faded;
					character = ".";
				} else {
					character = MemoryVisualization.convertHexToString(byteString);
				}
			}
			
			asciiStyles.add(new StyleRange(start, 1, charColor, asciiTextControl.getBackground()));
			asciiText.append(character);
			start++;
		}
		
		asciiTextControl.setText(asciiText.toString());
		
		if(hexData.length() > 0) {
			StyleRange[] ranges = asciiStyles.toArray(new StyleRange[0]);
			asciiTextControl.setStyleRanges(ranges);
		}
	}
	
	public void setHighlightAddress(BigInteger highlightAddress) {
		this.highlightAddress = highlightAddress;
	}
}
