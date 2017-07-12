package ca.uvic.chisel.atlantis.views;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.functionparsing.FunctionTreeContentProvider;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.views.CombinedFileSearchView;

public class MemorySearchView extends ViewPart {

	public static final String ID = "ca.uvic.chisel.atlantis.views.MemorySearchView";
	
	private Text asciiTextControl;
	private Text hexTextControl;
	private Table resultTable;
	private TableViewer resultTableViewer;
	private Button unicodeCheck;
	
	private Action gotoAddress;
	private Action gotoLine;
	
	public MemorySearchView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);
		
		createControls(parent);
		createContextMenu();
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
		Menu menu = menuMgr.createContextMenu(resultTableViewer.getControl());
		resultTableViewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, resultTableViewer);
	}

	private void fillContextMenu(IMenuManager mgr) {
		if(resultTable.getSelectionIndex() == -1) {
			return;
		}
		
		mgr.add(gotoAddress);
		mgr.add(gotoLine);
	}

	public void createActions() {
		gotoAddress = new Action("Display Address in Hex View") {
			@Override
			public void run() {
				MemorySearchView.this.gotoAddressOfSelected();
			}
		};
		gotoLine = new Action("Go To Line of Change in Trace View") {
			@Override
			public void run() {
				if(resultTable.getSelectionIndex() == -1) {
					return;
				}
				
				@SuppressWarnings("unchecked")
				List<MemoryReference> references = (List<MemoryReference>)resultTableViewer.getInput();
				MemoryReference ref = references.get(resultTable.getSelectionIndex());
				
				try {
					AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().getActiveEditor();
					if (activeTraceDisplayer != null) {
						
						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(ref.getLineNumber(), 0);
					}
				} catch (Exception ex) {
					System.err.println("Error jumping to line");
					ex.printStackTrace();
				}
			}
		};
	}
	
	public void createControls(Composite parent) {
		Label asciiLabel = new Label(parent, 0);
		asciiLabel.setText("Ascii:");
		
		asciiTextControl = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.FILL);
		GridData asciiTextControlData = new GridData();
		asciiTextControlData.grabExcessHorizontalSpace = true;
		asciiTextControlData.horizontalAlignment = SWT.FILL;
		asciiTextControl.setLayoutData(asciiTextControlData);
		asciiTextControl.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				MemorySearchView.this.asciiTextKeyReleased(e);
			}
			
			@Override
			public void keyPressed(KeyEvent e) { 
				if(e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					MemorySearchView.this.searchButtonMouseUp(null);
				}
			}
		});
		
		unicodeCheck = new Button(parent, SWT.CHECK);
		unicodeCheck.setText("Null separated");
		unicodeCheck.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MemorySearchView.this.unicodeCheckSelected(e);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) { }
		});
		
		Label hexLabel = new Label(parent, 0);
		hexLabel.setText("Hex:");
		
		hexTextControl = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.FILL);
		GridData hexTextControlData = new GridData();
		hexTextControlData.grabExcessHorizontalSpace = true;
		hexTextControlData.horizontalAlignment = SWT.FILL;
		hexTextControl.setLayoutData(hexTextControlData);
		hexTextControl.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				MemorySearchView.this.hexTextKeyReleased(e);
			}
			
			@Override
			public void keyPressed(KeyEvent e) { 
				if(e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					MemorySearchView.this.searchButtonMouseUp(null);
				}
			}
		});
		hexTextControl.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				MemorySearchView.this.verifyHexText(e);
			}
		});
		
		Button searchButton = new Button(parent, SWT.PUSH);
		searchButton.setText("Search");
		searchButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				MemorySearchView.this.searchButtonMouseUp(e);
			}
			@Override
			public void mouseDown(MouseEvent arg0) { }
			@Override
			public void mouseDoubleClick(MouseEvent arg0) { }
		});
		GridData searchButtonData = new GridData();
		searchButtonData.horizontalAlignment = SWT.FILL;
		searchButton.setLayoutData(searchButtonData);
		
		Composite tableComposite = new Composite(parent, 0);
		GridData tableCompositeData = new GridData();
		tableCompositeData.horizontalSpan = 3;
		tableCompositeData.horizontalAlignment = SWT.FILL;
		tableCompositeData.grabExcessHorizontalSpace = true;
		tableCompositeData.verticalAlignment = SWT.FILL;
		tableCompositeData.grabExcessVerticalSpace = true;
		tableComposite.setLayoutData(tableCompositeData);
		
		resultTable = new Table(tableComposite, SWT.NO_SCROLL | SWT.V_SCROLL 
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
		resultTable.setItemCount(0);
		resultTableViewer = new TableViewer(resultTable);
		
		createColumns(tableComposite, resultTableViewer);
		resultTable.setHeaderVisible(true);
		
		resultTableViewer.setContentProvider(new ArrayContentProvider());
		
		resultTableViewer.setInput(new ArrayList<MemoryReference>());
		
		resultTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent e) {
				MemorySearchView.this.doubleClick(e);
			}
		});
	}
	
	private void createColumns(final Composite parent, final TableViewer viewer) {
			
		URL imageURL = getClass().getResource("/icons/found_memory.gif");
		final Image foundMemoryIcon = ImageDescriptor.createFromURL(imageURL).createImage();
		
		TableViewerColumn col = new TableViewerColumn(resultTableViewer, SWT.NONE);
		col.getColumn().setText("Results");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				return foundMemoryIcon;
			}
			
			@Override
			public String getText(Object element) {
				MemoryReference ref = (MemoryReference)element;
				return "0x" + ref.getAddressAsHexString();
			}
		});
		
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(col.getColumn(), new ColumnWeightData(100));
		parent.setLayout(tableLayout);
	}
	
	private void gotoAddressOfSelected() {
		try {
			String selectedAddress = ((MemoryReference)(resultTable.getSelection()[0].getData())).getAddressAsHexString(); //getText(0).substring(2);
			
			HexVisualization hexView = (HexVisualization) 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(HexVisualization.ID);
			if(hexView != null) {
				hexView.setAddress(new BigInteger(selectedAddress, 16));
			}
		} catch (PartInitException e) {
			// Failure? It's ok.
		}
	}
	
	
	private void doubleClick(DoubleClickEvent e) {
		if(!e.getSelection().isEmpty()) {
			gotoAddressOfSelected();
		}
	}
	
	private String asciiToHex(String ascii, boolean nullSeparated){
        StringBuilder hexBuilder = new StringBuilder();
        
        for (int i = 0; i < ascii.length(); i++) {
            if(nullSeparated) {
            	hexBuilder.append("00");
            }
        	
        	String byteStr = Integer.toHexString(ascii.charAt(i));
        	hexBuilder.append(byteStr);
        	
        	if(byteStr.length() == 1) {
        		hexBuilder.append("0");
        	}
        }
        
        return hexBuilder.toString();
    } 
	
	private String hexToPossibleAscii(String hex, boolean assumeOddCharactersNull) {
		StringBuilder asciiBuilder = new StringBuilder();
		
		for(int i = 0; i < hex.length(); i += (assumeOddCharactersNull ? 4 : 2)) {
			if(i >= hex.length()) {
				break;
			}
			
			int end = i + 2 > hex.length() ? hex.length() : i + 2;
			String byteStr = hex.substring(i, end);
			int byteInt = Integer.parseInt(byteStr, 16);
			
			// is byteStr a displayable character?
			if(byteInt >= 32 && byteInt <= 126) {
				asciiBuilder.append(Character.toChars(byteInt)[0]);
			} else {
				asciiBuilder.append(Character.toChars(0x25a0)[0]); // opaque square
			}
		}
		
		return asciiBuilder.toString();
	}
	
	private List<MemoryReference> doSearch(String hexSearch) {

		 Collection<MemoryReference> memoryValues = ModelProvider.INSTANCE.getMemoryValues();
		
		// sort the memory references by address
		ArrayList<MemoryReference> sortedMemoryReferences = new ArrayList<MemoryReference>(memoryValues.size());
		for(MemoryReference ref : memoryValues) {
			sortedMemoryReferences.add(ref);
		}
		Collections.sort(sortedMemoryReferences, new Comparator<MemoryReference>() {
			@Override
			public int compare(MemoryReference o1, MemoryReference o2) {
				return BigInteger.valueOf(o1.getAddress()).compareTo(BigInteger.valueOf(o2.getAddress()));
			}
		});
		
		hexSearch = hexSearch.toUpperCase(Locale.US);

		ArrayList<MemoryReference> results = new ArrayList<MemoryReference>();
		Deque<MemoryReference> matchSet = new LinkedList<MemoryReference>();
		String matchString = "";
		
		for(MemoryReference ref : sortedMemoryReferences) {
			
			// if the nodes are non-contiguous, then clear the match set
			if(!matchSet.isEmpty() && BigInteger.valueOf(matchSet.getLast().getAddress())
				.add(BigInteger.valueOf(matchSet.getLast().getMemoryContent().hexStringLength() / 2))
				.compareTo(BigInteger.valueOf(ref.getAddress())) < 0) {
				matchSet.clear();
				matchString = "";
			}
			
			// if the new match set will be longer than the search string even without the
			// first element in it, then there is no way the first element can be involved
			// in a match, so remove it
			if(!matchSet.isEmpty()){
				int strLen = (int)(long)(matchSet.getFirst().getMemoryContent().hexStringLength()); // unfortunate
				if(!matchSet.isEmpty() && matchString.length() + ref.getMemoryContent().hexStringLength() - 
						strLen > hexSearch.length()) {
					if(strLen > matchString.length()) {
						matchString = "";
					} else {
						matchString = matchString.substring(strLen);
					}
					matchSet.removeFirst();
				}
			}
			
			// add the string to the match
			matchSet.add(ref);
			matchString += ref.getMemoryContent().getMemoryValue().toUpperCase(Locale.US);
			
			// find a match
			int matchOffset = matchString.indexOf(hexSearch);
			
			// if we have a match
			if(matchOffset != -1 && matchOffset % 2 == 0) {
				
				// find the last memory reference that starts before the found offset
				MemoryReference found = matchSet.getFirst();
				int i = 0;
				for(MemoryReference foundRef : matchSet) {
					if(i <= matchOffset) {
						found = foundRef;
					}
					i += foundRef.getMemoryContent().hexStringLength();
				}
				results.add(found);
				
				// the character index off the end of the match
				int endIndex = matchOffset + hexSearch.length();
				
				// loop through included memory references, and remove them
				// if they end before endIndex
				LinkedList<MemoryReference> toRemove = new LinkedList<MemoryReference>();
				for(MemoryReference incRef : matchSet) {
					if(matchString.indexOf(incRef.getMemoryContent().getMemoryValue().toUpperCase(Locale.US)) + incRef.getMemoryContent().hexStringLength() <= endIndex) {
						toRemove.add(incRef);
					}
				}
				matchSet.removeAll(toRemove);
				
				// chop the matchString to start at endIndex
				matchString = matchString.substring(endIndex);
			}
		}
		
		return results;		
	}
	
	private void searchButtonMouseUp(MouseEvent e) {
		if(hexTextControl.getText().length() == 0) {
			return;
		}
		
		String hex = hexTextControl.getText();
		if(hex.length() % 2 != 0) {
			hex = hex + "0";
		}
		
		resultTableViewer.setInput(doSearch(hex));
		
		int currentLineNumber = -1;
		try {
			AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor();
			if (activeTraceDisplayer != null) {
				currentLineNumber = activeTraceDisplayer.getProjectionViewer().getCurrentLineNumber();
			}
		} catch (Exception ex) {
			System.err.println("Error getting line number");
			ex.printStackTrace();
		}
		
		resultTable.getColumns()[0].setText("Results for Current Memory State (for Line " + (currentLineNumber + 1) + ")");
	}
	
	private void unicodeCheckSelected(SelectionEvent e) {
		hexTextControl.setText(asciiToHex(asciiTextControl.getText(), unicodeCheck.getSelection()));
	}
	
	private void verifyHexText(VerifyEvent e) {
		for(int i = 0; i < e.text.length(); i++) {
			if(!((e.text.charAt(i) >= 48 && e.text.charAt(i) <= 57) || // 0-9
				(e.text.charAt(i) >= 65 && e.text.charAt(i) <= 70) || // A-F
				(e.text.charAt(i) >= 97 && e.text.charAt(i) <= 102))) { // a-f
				e.doit = false;
				return;
			}
		}
	}
	
	private void asciiTextKeyReleased(KeyEvent e) {
		hexTextControl.setText(asciiToHex(asciiTextControl.getText(), unicodeCheck.getSelection()));
	}
	
	private void hexTextKeyReleased(KeyEvent e) {
		asciiTextControl.setText(hexToPossibleAscii(hexTextControl.getText(), unicodeCheck.getSelection()));
	}
	
	private void doSearch() {
		
	}

	public void refresh() {
		doSearch();
	}

	public void clearContents() {
		
		this.refresh();
	}

	
}
