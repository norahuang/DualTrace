package ca.uvic.chisel.atlantis.views;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import org.apache.commons.lang3.StringUtils;
import ca.uvic.chisel.atlantis.controls.HexEditControl;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class HexVisualization extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.atlantis.views.HexVisualization";
	
	private Slider sliderScrollBar;
	private HexEditControl hexDisplay;
	private Text searchText;
	
	// the address of the last byte of memory
	private BigInteger memoryExtent;
	
	// the number of lines required for all lines
	private BigInteger numLines;
	
	// the current line
	private BigInteger currenHexLine;
	
	// is scroll change deactivated for goto address?
	private boolean disableScroll;
	
	// goto box colors
	private final static Color GREEN = new Color(null, 180, 255, 155);
	private final static Color PINK = new Color(null, 255, 155, 180);
	private final static Color WHITE = new Color(null, 255, 255, 255);
	
	// changed memory controls
	private List<BigInteger> memoryChanges;
	private int currentChanged;
	private Button lastChangedButton;
	private Button nextChangedButton;
	private Button thisChangedButton;

	private AtlantisTraceEditor activeTraceDisplayer;

	private AtlantisFileModelDataLayer fileModel;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		parent.setLayout(layout);
		
		disableScroll = false;
		
		Composite searchComposite = new Composite(parent, 0);
		searchComposite.setLayout(new GridLayout(5, false));
		GridData searchCompositeData = new GridData();
		searchCompositeData.horizontalSpan = 2;
		searchCompositeData.grabExcessHorizontalSpace = true;
		searchCompositeData.horizontalAlignment = SWT.FILL;
		searchComposite.setLayoutData(searchCompositeData);
		
		Label gotoLabel = new Label(searchComposite, 0);
		gotoLabel.setText("Go To Address: ");
		
		searchText = new Text(searchComposite, SWT.BORDER | SWT.SEARCH | SWT.FILL);
		GridData searchTextData = new GridData();
		searchTextData.grabExcessHorizontalSpace = true;
		searchTextData.horizontalAlignment = SWT.FILL;
		searchText.setLayoutData(searchTextData);
		
		searchText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) { }
			@Override
			public void keyReleased(KeyEvent e) {
				HexVisualization.this.searchTextKeyReleased(e);
			}
		});
		
		lastChangedButton = new Button(searchComposite, 0);
		lastChangedButton.setText("<");
		lastChangedButton.setToolTipText("Go to the previous change on this line.");
		lastChangedButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				HexVisualization.this.lastChangedClicked();
			}
			@Override
			public void mouseDown(MouseEvent arg0) { }
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		
		thisChangedButton = new Button(searchComposite, 0);
		thisChangedButton.setText("No Changes");
		thisChangedButton.setToolTipText("No changes on this line.");
		thisChangedButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				HexVisualization.this.thisChangedClicked();
			}
			@Override
			public void mouseDown(MouseEvent arg0) { }
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		
		nextChangedButton = new Button(searchComposite, 0);
		nextChangedButton.setText(">");
		nextChangedButton.setToolTipText("Go to the next change on this line.");
		nextChangedButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				HexVisualization.this.nextChangedClicked();
			}
			@Override
			public void mouseDown(MouseEvent arg0) { }
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		
		nextChangedButton.setEnabled(false);
		lastChangedButton.setEnabled(false);
		thisChangedButton.setEnabled(false);
		memoryChanges = new ArrayList<BigInteger>();
		currentChanged = 0;
		
		createViewer(parent);
		
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	private void createViewer(Composite parent) {
		hexDisplay = new HexEditControl(this, parent, SWT.BORDER);
		
		sliderScrollBar = new Slider(parent, SWT.VERTICAL);
		
		GridData hexGridData = new GridData();
		hexGridData.horizontalAlignment = GridData.FILL;
		hexGridData.verticalAlignment = GridData.FILL;
		hexGridData.grabExcessHorizontalSpace = true;
		hexGridData.grabExcessVerticalSpace = true;
		hexDisplay.setLayoutData(hexGridData);
		
		hexDisplay.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseScrolled(MouseEvent e) {
				currenHexLine = currenHexLine.subtract(BigInteger.valueOf(e.count));
				if(currenHexLine.compareTo(BigInteger.ZERO) < 0) {
					currenHexLine = BigInteger.ZERO;
				}
				if(currenHexLine.compareTo(numLines) > 0) {
					currenHexLine = numLines;
				}
				refresh();
			}
		});
		
		hexDisplay.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent arg0) {
				refresh();
			}
			
			@Override
			public void controlMoved(ControlEvent arg0) { }
		});
		
		GridData scrollGridData = new GridData();
		scrollGridData.horizontalAlignment = GridData.END;
		scrollGridData.verticalAlignment = GridData.FILL;
		scrollGridData.grabExcessHorizontalSpace = false;
		scrollGridData.grabExcessVerticalSpace = true;
		sliderScrollBar.setLayoutData(scrollGridData);
		
		sliderScrollBar.setMinimum(0);
		sliderScrollBar.setMaximum(Integer.MAX_VALUE);
		sliderScrollBar.setIncrement(1);
		sliderScrollBar.setSelection(0);
		
		sliderScrollBar.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				HexVisualization.this.onSelection(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// do noting
			}
		});
		
		currenHexLine = BigInteger.ZERO;
		
		rebuildMemoryInfo();
	}
	
	private void searchTextKeyReleased(KeyEvent e) {
		if(searchText.getText().isEmpty()){
			searchText.setBackground(WHITE);
			return;
		}
		
		BigInteger address = null;
		
		try {
			address = new BigInteger(searchText.getText(), 16);
		} catch(NumberFormatException ex) {
			// incorrect format
			searchText.setBackground(PINK);
			return;
		}
		
		if(address.compareTo(BigInteger.ZERO) < 0 ||
				address.compareTo(memoryExtent) > 0) {
			// out of bounds
			searchText.setBackground(PINK);
			return;
		}
		
		gotoLineOfAddress(address);
		
		searchText.setBackground(GREEN);
		
		if (e != null && (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)) {
			hexDisplay.forceFocus();
		}
	}
	
	private void nextChangedClicked() {
		currentChanged++;
		
		if(currentChanged == memoryChanges.size() - 1) {
			nextChangedButton.setEnabled(false);
		}
		if(currentChanged > 0) {
			lastChangedButton.setEnabled(true);
		}
		
		thisChangedButton.setToolTipText("Go to memory change " + (currentChanged + 1) + " on this line.");
		
		BigInteger address = memoryChanges.get(currentChanged);
		
		thisChangedButton.setText((currentChanged + 1) + "/" + memoryChanges.size() + " Changes");
		gotoLineOfAddress(address);
		hexDisplay.forceFocus();
	}
	
	private void lastChangedClicked() {
		currentChanged--;
		
		if(currentChanged == 0) {
			lastChangedButton.setEnabled(false);
		}
		if(currentChanged < memoryChanges.size() - 1) {
			nextChangedButton.setEnabled(true);
		}
		
		thisChangedButton.setToolTipText("Go to memory change " + (currentChanged + 1) + " on this line.");
		
		BigInteger address = memoryChanges.get(currentChanged);
		
		thisChangedButton.setText((currentChanged + 1) + "/" + memoryChanges.size() + " Changes");
		gotoLineOfAddress(address);
		hexDisplay.forceFocus();
	}
	
	private void thisChangedClicked() {
		gotoLineOfAddress(memoryChanges.get(currentChanged));
		hexDisplay.forceFocus();
	}
	
	public void setAddress(BigInteger address) {
		gotoLineOfAddress(address);
	}
	
	private void gotoLineOfAddress(BigInteger address) {
		currenHexLine = address.divide(BigInteger.valueOf(hexDisplay.getCurrentWidthInBytes()));
		hexDisplay.setHighlightAddress(address);
		
		// turn off the scroll handler while forcing the scroll bar
		disableScroll = true;
		double linesPerIncrement = numLines.doubleValue() / (double)(sliderScrollBar.getMaximum() - sliderScrollBar.getThumb());
		sliderScrollBar.setSelection((int)(currenHexLine.doubleValue() / linesPerIncrement));
		disableScroll = false;
		
		refresh();
		hexDisplay.setHighlightAddress(null);
	}
	
	private void countMemoryChanges() {
		// Might be null in particular when closing all traces.
		if(activeTraceDisplayer == null) {
			thisChangedButton.setText("No Changes");
			thisChangedButton.setToolTipText("No changes on this line.");
			return;
		}
				
		int lineNumber = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber();
		
		String line = fileModel.getMemoryChangesForLine(lineNumber);
		
		// clear old changes
		memoryChanges = new ArrayList<BigInteger>();
		currentChanged = 0;
		
		// Not all lines have possible associated memory change.
		if(null != line){
			Matcher memoryMatcher = MemoryDeltaTree.matchMemoryChangesForline(line);
			
			while(memoryMatcher.find()) {
				BigInteger address = new BigInteger(memoryMatcher.group(1), 16);
				
				memoryChanges.add(address);
			}
		}
		
		// selectively enable relevant buttons
		if(memoryChanges.size() == 0) {
			nextChangedButton.setEnabled(false);
			lastChangedButton.setEnabled(false);
			thisChangedButton.setEnabled(false);
			thisChangedButton.setText("No Change");
			thisChangedButton.setToolTipText("No change on this line.");
		} else if(memoryChanges.size() == 1) {
			nextChangedButton.setEnabled(false);
			lastChangedButton.setEnabled(false);
			thisChangedButton.setEnabled(true);
			thisChangedButton.setText("1/1 Change");
			thisChangedButton.setToolTipText("Go to the memory changed on this line.");
		} else {
			nextChangedButton.setEnabled(true);
			lastChangedButton.setEnabled(false);
			thisChangedButton.setEnabled(true);
			thisChangedButton.setText("1/" + memoryChanges.size() + " Changes");
			thisChangedButton.setToolTipText("Go to memory change 1 on this line.");
		}
	}
	
	/**
	 * This way of refreshing the memory view deals with data in a more raw and unfiltered form, compared
	 * to before. Previously, the memory for the current line was fully determined when the line was changed,
	 * whereas now, we do not perform that work. By avoiding that work, we can do collation of memory only
	 * here, where it goes to the UI. This prevents us from having to do address-overlap and time-overlap
	 * comparisons. These used to involve costly subRange queries into a TreeMap, which took 500 seconds to
	 * resolve when used near the end of an 82million line file.
	 */
	public void refresh() {
		long startTime = System.currentTimeMillis();
		// what address did we scroll to
		BigInteger targetMemoryAddress = BigInteger.valueOf(hexDisplay.getCurrentWidthInBytes()).multiply(currenHexLine);
		
		// what data should be visible from that address
		MemoryQueryResults newMemoryReferences = ModelProvider.INSTANCE.getMemoryQueryResults();
		TreeSet<MemoryReference> memEntries = newMemoryReferences.getUnfilteredMemoryEntries();
		
		// These are the visible memory locations, and the capacity is usually under 300 chars when the view is 1/3 the window height.
		char[] data = StringUtils.repeat("??", hexDisplay.getCurrentByteCapacity()).toCharArray();
		int[] dataLineAttribution = new int[Math.max(0, hexDisplay.getCurrentByteCapacity()*2)];
		
		// This looks funny. It's because we need to step over each memory address/value/width/line
		// element, each of which is (now) only a byte (not a complete multi-byte memory write as seen in the binary format).
		// Each *byte* will be written to the UI, on the basis of whether it fits in the UI span displayed, and whether it
		// is newer than whatever is written in the UI String currently. This needs to be done as an alternative to the
		// variable-sized reference approach used before, that used MemoryReference objects for the complete memory write,
		// or for the write as fragmented by partial overwrites. That approach also used a TreeMap to do subRange() queries,
		// which determined if something was newer than data it partially overlapped with. Now, we ignore the overlap,
		// and take care of it implicitly, in that every byte overlaps some other byte (probably), but we track the
		// line number (age) of each byte that we use for the UI. Counter-intuitively, things are a lot faster with these
		// byte-atomic age comparisons and overwrites, even though very many memory values will be 4-bytes, give or take,
		// or even gigantic writes following from syscalls.
		// Note that we need to write nibbles (each hex letter) in groups of two (two nibbles to the byte, two hex chars to the byte).
		if(null != newMemoryReferences && newMemoryReferences.getMemoryEntries().size() > 0){
			
		BigInteger endTargetAddress = targetMemoryAddress.add(BigInteger.valueOf(hexDisplay.getCurrentByteCapacity()));
		
		// Use only those that are in the range we are showing currently
		MemoryReference dummyTargetAddress = MemoryReference.createDummyMemRef(targetMemoryAddress.longValue(), 1);
		MemoryReference dummyEndAddress = MemoryReference.createDummyMemRef(endTargetAddress.longValue(), 1);
		SortedSet<MemoryReference> subSet = memEntries.subSet(dummyTargetAddress, dummyEndAddress);
		
		// replace question marks with data as needed
		for(MemoryReference memRef: subSet){
			BigInteger currentAddress = BigInteger.valueOf(memRef.getAddress());
			int memRefLine = memRef.getLineNumber();
			BigInteger byteWidth = BigInteger.valueOf(memRef.getMemoryContent().getByteWidth());
			long charWidth = memRef.getMemoryContent().getByteWidth() * 2; // *2 here because 1 hex string represents a nibble, half a byte.
			
			// This could be negative, when the entry has a tail overlapping into the region we are writing to the UI
			int baseIndexOfEntryInDataArray = currentAddress.subtract(targetMemoryAddress).intValue() * 2; // need nibble difference, not byte difference
			for(int newCharIndex = 0; newCharIndex < charWidth; newCharIndex = newCharIndex+2){
				int offsetIndexForNewHexByte = baseIndexOfEntryInDataArray + newCharIndex; // -1 to get 0 indexed into data array
				if(offsetIndexForNewHexByte < 0){
					// Don't start writing until we are within 
					continue;
				}
				if(offsetIndexForNewHexByte > data.length - 1){
					break;
				}
				String memVal = memRef.getMemoryContent().getMemoryValue();
				// And if there is not already newer data present (in terms of line the change occurred on)... ModelProvider.INSTANCE.getCurrentLine()
				if(memRefLine <= activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber() && memRefLine > dataLineAttribution[offsetIndexForNewHexByte]){
					// Write two nibbles, two chars, one byte; to the current index and to the one following it.
					
					data[offsetIndexForNewHexByte] = memVal.charAt(newCharIndex);
					data[offsetIndexForNewHexByte+1] = memVal.charAt(newCharIndex+1);
					
					dataLineAttribution[offsetIndexForNewHexByte] = memRefLine;
					dataLineAttribution[offsetIndexForNewHexByte+1] = memRefLine;
					// TODO dataLineAttribution could be made sparser to save some memory, by changing it to a map. Consider doing so.
					// That would remove any allocations of integers for all '?' values, at the cost of slower lookup of the byte ages.
				}
			}
		}
		}
		
		String strData = new String(data);//data.toString();
		hexDisplay.setAddress(targetMemoryAddress);
		hexDisplay.setHexData(strData, targetMemoryAddress);
		
		long endTime = System.currentTimeMillis();
		if(null != activeTraceDisplayer){
			System.out.println("Hex view update duration "+activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber()+": " + (endTime-startTime)/1000.0);
		}
	}
	
	public void rebuildMemoryInfo() {
		  Collection<MemoryReference> memoryValues = ModelProvider.INSTANCE.getMemoryValues();
		
		if(memoryValues.size() == 0) {
			// no data, start at zero
			memoryExtent = BigInteger.valueOf(hexDisplay.getCurrentByteCapacity());
			numLines = BigInteger.ZERO;
		} else {
			// find the largest memory address used
			MemoryReference largest = null;
			for(MemoryReference ref : memoryValues) {
				if(null == largest
						|| BigInteger.valueOf(ref.getAddress()).compareTo(BigInteger.valueOf(largest.getAddress())) > 0) {
					largest = ref;
				}
			}
			
			// and set the size accordingly
			memoryExtent = BigInteger.valueOf(largest.getAddress())
							.add(BigInteger.valueOf(largest.getMemoryContent().hexStringLength() / 2));
			numLines = memoryExtent.divide(BigInteger.valueOf(hexDisplay.getCurrentWidthInBytes()));
			
			// if we don't end on a line break, add a final line
			if(memoryExtent.mod(BigInteger.valueOf(hexDisplay.getCurrentWidthInBytes())).compareTo(BigInteger.ZERO) != 0) {
				numLines = numLines.add(BigInteger.ONE);
			}
		}
		
		// count the changes
		countMemoryChanges();
	}
	
	// Duplicated from deprecated tabular memory view
	public void clearContents() {
		// clear whats in the memorymap
//		ModelProvider.INSTANCE.clearMemoryEntries();
		// TODO Not clear whether register view is supposed to be
		// fully dependent on the MemoryVisualization or not...
		// Call clear here??
//		ModelProvider.INSTANCE.clearRegistersEntries();
		ModelProvider.INSTANCE.clearMemoryAndRegisterData();
	}
	
	private void onSelection(SelectionEvent e) {
		if(disableScroll) {
			return;
		}
		
		switch(e.detail) {
			case SWT.ARROW_UP: {
				currenHexLine = currenHexLine.subtract(BigInteger.ONE);
				if(currenHexLine.compareTo(BigInteger.ZERO) < 0) {
					currenHexLine = BigInteger.ZERO;
				}
			} break;
			case SWT.ARROW_DOWN: {
				currenHexLine = currenHexLine.add(BigInteger.ONE);
				if(currenHexLine.compareTo(numLines) > 0) {
					currenHexLine = numLines;
				}
			} break;
			default:
			{
				double linesPerIncrement = numLines.doubleValue() / (double)(sliderScrollBar.getMaximum() - sliderScrollBar.getThumb());
				double lineToScrollTo = linesPerIncrement * sliderScrollBar.getSelection();
				currenHexLine = BigInteger.valueOf((long)lineToScrollTo);
			} break;
		}
		refresh();
	}
	
	@Override
	public void setFocus() { 
		hexDisplay.forceFocus();
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		refresh(); // Needed to get the view to size correctly when no trace file is open at program launch
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
		IWorkbenchPart part = partRef.getPart(false);
		if (activeTraceDisplayer != null && activeTraceDisplayer == part) {
			activeTraceDisplayer = null;
			clearContents();
			rebuildMemoryInfo();
			refresh();
			nextChangedButton.setEnabled(false);
			lastChangedButton.setEnabled(false);
			thisChangedButton.setEnabled(false);
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
