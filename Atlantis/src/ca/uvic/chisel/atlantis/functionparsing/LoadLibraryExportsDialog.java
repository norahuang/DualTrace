package ca.uvic.chisel.atlantis.functionparsing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ModuleRec;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.FunctionsView;
import ca.uvic.chisel.atlantis.views.ThreadFunctionsView;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

public class LoadLibraryExportsDialog extends TitleAreaDialog  {

	private StyledText allModulesList;
	private Text fileName;
	private Label numFunctionsLoaded;
	private StyledText dllVersioningInfo;
	private TableViewer functionsTableViewer;
	
	long timeDateStamp = 0;
	int majorOperatingSystemVersion = 0;
	int minorOperatingSystemVersion = 0;
	int majorImageBVersion = 0;
	int minorImageVersion = 0;
	int majorSubsystemVersion = 0;
	int minorSubsystemVersion = 0;
	
	public LoadLibraryExportsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Library Exports");
		setMessage("Load the exported symbols from a DLL file and attempt to match them to functions in the trace.", IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite)super.createDialogArea(parent);
		
		// this composite houses our controls
		Composite container = new Composite(area, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(layout);
		
		// dll names and version info
		String allModuleInfo = "Please compare these trace version details manually to version info found when you select a DLL to load.\r\r";
		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry();
		for(ModuleRec module : fileModel.getAllModules()) {
			if(0 == module.versionInfoUnion && "" == allModuleInfo){
				allModuleInfo =
						"***********************************************************************\r"+
						"**No module versioning info available, this is an old trace format.**\r"+ // Looks short, it's not.
						"***********************************************************************\r"+
						"\r";
			}
			allModuleInfo += module.winNameValue+"\r\t- version: "+module.versionInfoUnion +"\r\t- timestamp: "+Long.toString(module.lastFileWriteTimestamp)+"\r";
		}
		allModulesList = new StyledText(container, SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.BORDER);
		allModulesList.setText(allModuleInfo);
		GridData modulesPresentData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		modulesPresentData.horizontalSpan = 2;
		modulesPresentData.grabExcessHorizontalSpace = true;
		modulesPresentData.heightHint = 90;
		allModulesList.setLayoutData(modulesPresentData);
		
		// the file load
		fileName = new Text(container, SWT.SINGLE | SWT.BORDER);
		fileName.setEditable(false);
		GridData fileNameData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fileNameData.grabExcessHorizontalSpace = true;
		fileName.setLayoutData(fileNameData);
		
		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, new Listener() {	
			@Override
			public void handleEvent(Event arg0) {
				// user clicked load
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				fileDialog.setFilterExtensions(new String [] {"*.dll"});
				fileDialog.setFilterPath("C:\\Windows\\system32");
				String dllFileName = fileDialog.open();
				
				if(dllFileName != null) {
					fileName.setText(dllFileName);
					readLibraryFile(dllFileName);
				}
			}
		});
		
		// dll versioning info
		dllVersioningInfo = new StyledText(container, SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.BORDER);
		dllVersioningInfo.setText(generateDllVersionInfoString());
		GridData dllVersionLoadedData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		dllVersionLoadedData.horizontalSpan = 2;
		dllVersionLoadedData.grabExcessHorizontalSpace = true;
		dllVersionLoadedData.horizontalSpan = 2;
		dllVersionLoadedData.heightHint = 80;
		dllVersioningInfo.setLayoutData(dllVersionLoadedData);
		
		// functions loaded
		numFunctionsLoaded = new Label(container, SWT.NULL);
		numFunctionsLoaded.setText("Loaded Functions: 0                 ");
		GridData functionsLoadedData = new GridData();
		functionsLoadedData.horizontalSpan = 2;
		numFunctionsLoaded.setLayoutData(functionsLoadedData);
		
		// table of functions
		functionsTableViewer = new TableViewer(container, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns(container, functionsTableViewer);
		functionsTableViewer.getTable().setHeaderVisible(true);
		functionsTableViewer.getTable().setLinesVisible(true);
		functionsTableViewer.setContentProvider(new ArrayContentProvider());
		functionsTableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				Pair<Long, String> t1 = (Pair<Long, String>)e1;
				Pair<Long, String> t2 = (Pair<Long, String>)e2;
    			return t1.getRight().compareToIgnoreCase(t2.getRight());
			};
		});
		
		List<Pair<Long, String>> pairs = new ArrayList<Pair<Long, String>>();
		functionsTableViewer.setInput(pairs);
		
		GridData functionsTableData = new GridData();
		functionsTableData.grabExcessHorizontalSpace = true;
		functionsTableData.grabExcessVerticalSpace = true;
		functionsTableData.horizontalSpan = 2;
		functionsTableData.horizontalAlignment = GridData.FILL;
		functionsTableData.verticalAlignment = GridData.FILL;
		functionsTableViewer.getTable().setLayoutData(functionsTableData);
		
		return area;
	}
	
	private void createColumns(final Composite parent, final TableViewer viewer) {
		
		// offset
		TableViewerColumn offsetColumn = new TableViewerColumn(viewer, SWT.NONE);
		offsetColumn.getColumn().setText("Offset");
		offsetColumn.getColumn().setWidth(100);
		offsetColumn.getColumn().setResizable(true);
		offsetColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Pair<Long, String> pair = (Pair<Long, String>)element;
				return "0x" + Long.toHexString(pair.getLeft());
			}
		});
		
		// name
		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
		nameColumn.getColumn().setText("Name");
		nameColumn.getColumn().setWidth(310);
		nameColumn.getColumn().setResizable(true);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Pair<Long, String> pair = (Pair<Long, String>)element;
				return pair.getRight();
			}
		});
		
	}
	
	private int readUnsignedInt16(RandomAccessFile input) throws IOException {
		byte[] data = new byte[2];
		input.read(data);
		
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		short read = bb.getShort();
		
		int value = (int)read;
		value &= 0X0000FFFF; 
		
		return value;
	}
	
	private long readUnsignedInt32(RandomAccessFile input) throws IOException {
		byte[] data = new byte[4];
		input.read(data);
		
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int read = bb.getInt();
		
		long value = (long)read;
		value &= 0X00000000FFFFFFFFL; 
		
		return value;
	}
	
	private BigInteger readUnsignedInt64(RandomAccessFile input) throws IOException {
		byte[] data = new byte[8];
		input.read(data);
		
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		long read = bb.getLong();
		
		BigInteger value = BigInteger.valueOf(read & ~Long.MIN_VALUE);
		
		return value;
	}
	
	private String readNullTerminatedAsciiString(RandomAccessFile input) throws IOException {
		ArrayList<Byte> data =	new ArrayList<Byte>();
		byte latest = 0;
		
		do {
			latest = input.readByte();
			data.add(latest);
		} while(latest != 0);
		
		Byte[] byteData = data.toArray(new Byte[0]);
		byte[] copyBecauseJavaIsDumb = new byte[byteData.length];
		for(int i = 0; i < byteData.length; i++) {
			copyBecauseJavaIsDumb[i] = byteData[i];
		}
		
		return new String(copyBecauseJavaIsDumb, "ASCII");
	}
	
	private void readLibraryFile(final String fileName) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());

		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					RandomAccessFile inputFile = null;
					
					try {
						// open the file
						inputFile = new RandomAccessFile(fileName, "r");
						
						// skip to the offset to the signature (skip to the first element in the PE header)
						inputFile.skipBytes(0x3C);
						
						// read the signture offset (will be different than the signature found at the onset of the DOS header)
						long signatureOffset = readUnsignedInt32(inputFile); // (aka e_lfanew, end of DOS header)
						
						// jump to the signature
						inputFile.seek(signatureOffset);
						
						// ensure signature is correct
						byte[] signature = new byte[4];
						inputFile.read(signature);
						if(signature[0] != 'P' || signature[1] != 'E' || signature[2] != 0 || signature[3] != 0) {
							throw new Exception("This is not a valid Windows PE file.");
						}
						
						// skip the first 2 bytes of the COFF header (skip the Machine entry)
						inputFile.skipBytes(2);
						
						// read the number of sections
						int numberOfSections = readUnsignedInt16(inputFile);
						
						// Needed for comparison to binary format reported timeDateStamp
						// https://en.wikipedia.org/wiki/Portable_Executable#/media/File:Portable_Executable_32_bit_Structure_in_SVG.svg
						timeDateStamp = readUnsignedInt32(inputFile);
						
						// skip the next 8 bytes of the COFF header to get to SizeOfOptionalHeader
						inputFile.skipBytes(8);
						
						// read the size of the optional header (useless?)
						int optionalHeaderSize = readUnsignedInt16(inputFile);
						
						// skip the end of the COFF header, skipping Characteristics
						inputFile.skipBytes(2);
						
						// Assert we have found magic here
						assert (inputFile.getFilePointer() == 0x0018);
						// check if this is a PE32 or PE32+ file
						// Murray found PE32 Plus docs elsewhere, but I found this: http://stackoverflow.com/a/15608028/682536
						// Require this flag to distinguish between 32 and 64 bit assemblies.
						int magicNumber = readUnsignedInt16(inputFile);
						boolean PE32Plus = magicNumber == 0x020B;
						
						// After getting the version items that *were not* part of Murray's original parse,
						// we need to account for the actual position in the file prior to taking the
						// next blind skipBytes() call. Those calls, dependent on PE32Plus, assume that we *just* finished
						// reading magicNumber, which resides at 0x0018.
						long afterMagicFilePointer = inputFile.getFilePointer();
						
						// skip to the versions. We need four to correspond to the moduleRec versionInfoUnion,
						// but I am not sure which quartet makes that up:
						// 2 bytes MajorOperatingSystemVersion, MinorOperatingSystemVersion at 0x0040
						// 2 bytes MajorImageVersion, and MinorImageVersion at 0x0044
						// 2 bytes MajorSubsystemVersion, MinorSubsystemVersion 0x0048
						// 4 bytes Win32VersionValue (zeros filled) 0x0052
						// The Win32VersionValue cannot be the version of interest, since we need four 16 bit numbers.
						// I will try to confirm, but otherwise implement as the OS and Image versions.
						// We have to keep track of what we have read and skipped so far, since Murray decided not have access
						// to the random-access capable HugeBitBuffer at the time of this implementation.
						// NB This is all fragile code, tread lightly.
						
						// Found that until after the version number info, the byte positions for things in the
						// Windows Specific Header are the same between 32 and 64 bit versions, therefore between PE32 and PE32+.
						// https://code.google.com/archive/p/corkami/wikis/PE102.wiki
						// Added pdf from that to repo. Note that "18+60+ means offset by 0x18 bytes, length 0x60 bytes.
						// Offsets for the versions from the pdf called PE102[booklet|poster]V1.pdf are:
						// 28+2 MajorOperatingSystemVersion
						// 2a+2 MinorOperatingSystemVersion
						// 2c+2 MajorImageBVersion
						// 2e+2 MinorImageVersion
						// 30+2 MajorSubsystemVersion
						// 32+2 MinorSubsystemVersion
						// 34+4 Win32VersionValue (cannot be the version number of interest for the binary format, too big)
						// These do not correspond with the offsets in Portable_Executable_32_bit_Structure_in_SVG.svg, which
						// I found on wikipedia and added to the repo as well.
						// I will use the values from the more comprehensive pdf instead, but I will commit the svg as well.
						
						majorOperatingSystemVersion = readUnsignedInt16(inputFile);
						minorOperatingSystemVersion = readUnsignedInt16(inputFile);
						majorImageBVersion = readUnsignedInt16(inputFile);
						minorImageVersion = readUnsignedInt16(inputFile);
						majorSubsystemVersion = readUnsignedInt16(inputFile);
						minorSubsystemVersion = readUnsignedInt16(inputFile);
						
						// After getting the version items that *were not* part of Murray's original parse,
						// we need to account for the actual position in the file prior to taking the
						// next blind skipBytes() call. Those calls, dependent on PE32Plus, assume that we *just* finished
						// reading magicNumber, which resides at 0x0018.
						// The easiest would be to seek to the point Mu was arbitrarily jumping from, after Magic.
						inputFile.seek(afterMagicFilePointer);
						
						
						// skip the standard fields in the optional header
						inputFile.skipBytes(PE32Plus ? 22 : 26);

						// skip the all but the last window specific field in the optional header
						inputFile.skipBytes(PE32Plus ? 84 : 64);
						
						// read the number of data directory RVA and size pairs
						long numDataDirectories = readUnsignedInt32(inputFile);
						
						// if we have less than 1, then we have no exports section
						if(numberOfSections < 1) {
							throw new Exception("This PE file has no exports.");
						}
						
						// read the RVA of the export table
						long exportTableRVA = readUnsignedInt32(inputFile);
						
						// read the size of the export table
						long exportTableSize = readUnsignedInt32(inputFile);
						
						// skip the remaining optional header data directories
						inputFile.skipBytes((int)(8 * (numDataDirectories - 1)));
						
						// did we find the actual ".edata"?
						boolean foundEDataSectionHeader = false;
						long eDataSectionStart = 0;
						
						// the info on the previous section
						long largestPreviousSectionRVA = 0;
						long largestPreviousSectionPointer = 0;
						
						// we are now in the section headers, search for ".edata"
						for(int i = 0; i < numberOfSections; i++) {
							
							// read the 8 byte string
							byte[] sectionNameBytes = new byte[8];
							inputFile.read(sectionNameBytes);
							
							// convert to string
							String sectionName = new String(sectionNameBytes, "UTF-8");
							
							// did we find ".edata"? (unlikely, but possible)
							if(sectionName.equals(".edata")) {

								// we found it!
								foundEDataSectionHeader = true;
								
								// skip 12 bytes of section info
								inputFile.skipBytes(12);
								
								// read the file pointer of the section data
								long sectionDataAt = readUnsignedInt32(inputFile);
								
								// set the edata file pointer
								eDataSectionStart = sectionDataAt;
								
								// we found it, no need to loop over more sections
								break;
							
							} else {
								// otherwise, skip the virtual size
								inputFile.skipBytes(4);
								
								// read the virtual address
								long RVA = readUnsignedInt32(inputFile);
								
								// skip the raw data size
								inputFile.skipBytes(4);
								
								// read the pointer to the raw data
								// NOTE: the spec says this is from the Optional Header,
								// that is incorrect, this is from the start of the file
								long pointer = readUnsignedInt32(inputFile);
								
								// is this section after the export table?
								if(RVA > exportTableRVA) {
									// if so, the last section was it
									break;
								}
								
								// otherwise this is the largest section yet
								largestPreviousSectionRVA = RVA;
								largestPreviousSectionPointer = pointer;
								
								// and skip the last 16 bytes of the section header
								inputFile.skipBytes(16);
							}
						}
						
						// if we did not find ".edata"
						if(!foundEDataSectionHeader) {
							// compute the actual file offset
							long RVADifference = exportTableRVA - largestPreviousSectionRVA;
							
							// and add that to the previous section pointer
							// NOTE: (as above) gives an actually offset relative to the beginning of
							// the file, NOT the Optional Header
							long targetAddress = RVADifference + largestPreviousSectionPointer;
							
							// set the edata file pointer
							eDataSectionStart = targetAddress;
						}
						
						inputFile.seek(eDataSectionStart);
						
						// skip the beginning of the export directory table
						inputFile.skipBytes(16);
						
						// read the ordinal base
						long ordinalBase = readUnsignedInt32(inputFile);
						
						// read the number of address table entries
						long numAddresses = readUnsignedInt32(inputFile);
						
						// read the number of name pointers
						long numNamePointers = readUnsignedInt32(inputFile);
						
						// set the task
						monitor.beginTask("Reading functions...", (int)numNamePointers);
						
						// skip the rest of the export directory table
						inputFile.skipBytes(12);
						
						// skip the export address table
						inputFile.skipBytes((int)(numAddresses * 4));
						
						final List<Pair<Long, String>> functionsList = new ArrayList<Pair<Long, String>>();
						
						// for each name in the name pointer table
						for(int i = 0; i < numNamePointers; i++) {
							// read the string name RVA
							long nameRVA = readUnsignedInt32(inputFile);
							
							// compute the file pointer to the string
							long nameFilePointer = eDataSectionStart + (nameRVA - exportTableRVA);
							
							// save the current position
							long currentPosition = inputFile.getFilePointer();
							
							// go to the string in the file
							inputFile.seek(nameFilePointer);
							
							// read the string
							String name = readNullTerminatedAsciiString(inputFile).trim();
							
							// seek to this entry in the ordinal table
							inputFile.seek(eDataSectionStart + 40 + numAddresses * 4 + numNamePointers * 4 + i * 2);
							
							// read the ordinal
							int ordinal = (int)(readUnsignedInt16(inputFile) + ordinalBase);
							
							// seek to this entry in the export address table
							inputFile.seek(eDataSectionStart + 40 + i * 4);
							
							// read the RVA of the function
							long functionRVA = readUnsignedInt32(inputFile);
							
							// and seek back for the next function
							inputFile.seek(currentPosition);
							
							// update the work done
							monitor.worked(1);
							
							// if the function's RVA is inside the export section, is is a forwarded 
							// function, not an actual export, so skip it
							if(functionRVA > exportTableRVA && functionRVA < exportTableRVA + exportTableSize) {
								continue;
							}
							
							// otherwise add it to the list of exports
							functionsList.add(new ImmutablePair<Long, String>(functionRVA, name));
						}
						
						// job's done
						monitor.done();
						
						Display.getDefault().asyncExec(new Runnable() {
						      @Override
						      public void run() {
									numFunctionsLoaded.setText("Loaded Functions: " + functionsList.size());
									dllVersioningInfo.setText(generateDllVersionInfoString());
									functionsTableViewer.setInput(functionsList);
						      }
					    });
						
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						if(inputFile != null) {
							try {
								inputFile.close();
							} catch (IOException e) {
								System.err.println("Error closing library file.");
								// don't want to re-throw because we are already in a finally block
							}
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	String generateDllVersionInfoString(){
		if(0 == timeDateStamp){
			return "DLL versioning info will be loaded here after selecting a file.";
		} else {
		return "DLL Versioning Info:\r"
			+"TimeDateStamp: "+timeDateStamp+"\r"
			+"OperatingSystemVersion: "+majorOperatingSystemVersion+"."+minorOperatingSystemVersion+"\r"
			+"ImageVersion: "+majorImageBVersion+"."+minorImageVersion+"\r"
			+"SubsystemVersion: "+majorSubsystemVersion+"."+minorSubsystemVersion
			;
		}
	}

	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Load Library Exports");
		newShell.setImage(ImageDescriptor.createFromURL(getClass().getResource("/icons/load_library_exports.gif")).createImage());
	}

	@Override
  	protected Point getInitialSize() {
		return new Point(450, 800);
	}
	
	@Override
	protected void okPressed() {
		List<Pair<Long, String>> functionsList = (ArrayList<Pair<Long, String>>)functionsTableViewer.getInput();
		
		if(functionsList.size() > 0) {
			setNamedPipeFuncSet(functionsList);
			FunctionsView functionsView = (FunctionsView) 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(FunctionsView.ID);
			if(functionsView != null) {
				functionsView.refresh();
			}
			
			ThreadFunctionsView threadFunctionsView = (ThreadFunctionsView) 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ThreadFunctionsView.ID);
			if(threadFunctionsView != null) {
				threadFunctionsView.refresh();
			}
		}
		
		super.okPressed();
	}
	
	private void setNamedPipeFuncSet(List<Pair<Long, String>> functionsList){
		String dllFileName = fileName.getText().substring(fileName.getText().lastIndexOf('\\') + 1);
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils
				.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
		NamedPipeFunctions namedPipeFunctions = new NamedPipeFunctions();
		for(Pair<Long, String> function : functionsList) {
			FunctionNameRegistry.registerFunction(dllFileName, function.getLeft(), function.getRight());
			
			if (namedPipeFunctions.getCreateNamedPipeAFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setCreateNamedPipeA(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}
			if (namedPipeFunctions.getCreateFileAFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setCreateFileA(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
			}
			
			if (namedPipeFunctions.getCreateNamedPipeWFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setCreateNamedPipeW(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}
			if (namedPipeFunctions.getCreateFileWFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setCreateFileW(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
			}
			
			if (namedPipeFunctions.getWriteFileFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setWriteFile(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}
			if (namedPipeFunctions.getReadFileFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setReadFile(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}
			if (namedPipeFunctions.getGetOverlappedResultFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setGetOverlappedResult(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}
			if (namedPipeFunctions.getCloseHandleFuncName().equals(function.getRight())){
				Function func = fileModel.getFunctionDb().getSingleFunctionFromModule(
						dllFileName, function.getLeft(), fileModel.getInstructionDb());
				if(func != null){
					namedPipeFunctions.setCloseHandle(fileModel.getFunctionDb().getSingleFunctionFromModule(
							dllFileName, function.getLeft(), fileModel.getInstructionDb()).getFirst());
				}
				
			}

		}
		
		activeTraceDisplayer.setNamedPipeFunctions(namedPipeFunctions);
	}
}

     
