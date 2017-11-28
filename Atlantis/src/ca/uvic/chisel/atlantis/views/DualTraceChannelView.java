package ca.uvic.chisel.atlantis.views;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.datacache.AsyncResult;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.BinaryFormatFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.functionparsing.Register;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.dualtracechannel.BfvFileWithAddrMatch;
import ca.uvic.chisel.bfv.dualtracechannel.Channel;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelEvent;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelEvent.CommunicationStage;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelGroup;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelGroup.ChennelType;
import ca.uvic.chisel.bfv.dualtracechannel.FullFunctionMatch;
import ca.uvic.chisel.bfv.dualtracechannel.Trace;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

public class DualTraceChannelView extends ViewPart implements IPartListener2, MenuListener {

	public static final String ID = "ca.uvic.chisel.atlantis.views.DualTraceChannelView";
	private FileSearchResult searchResults = new FileSearchResult((FileSearchQuery) null);
	// private Table resultTable;
	private TreeViewer resultTableViewer;
	private Action gotoFunctionEnd;
	FullFunctionMatch fullFunctionMatch = null;
	private AtlantisTraceEditor traceEditor1;
	private AtlantisTraceEditor traceEditor2;

	public DualTraceChannelView() {
		this.searchResults = new FileSearchResult((FileSearchQuery) null);
	}

	private class ChannelContentProvider implements ITreeContentProvider {
		private Collection<ChannelGroup> channelGroups;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				channelGroups = new ArrayList<ChannelGroup>(); // clear out the
																// old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					channelGroups.add((ChannelGroup) o);
				}
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ChannelGroup) {
				ChannelGroup group = (ChannelGroup) parentElement;
				Trace[] traces = { group.getTrace1(), group.getTrace2() };
				return traces;
			} else if (parentElement instanceof Trace) {
				Trace trace = (Trace) parentElement;
				List<Channel> channels = trace.getChannels();
				Collections.sort(channels);
				return channels.toArray();
			} else if (parentElement instanceof Channel) {
				Channel channel = (Channel) parentElement;
				List<ChannelEvent> events = channel.getEvents();
				Collections.sort(events);
				return events.toArray();
			} else {
				return null;
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return channelGroups.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ChannelGroup) {
				return true;
			} else if (element instanceof Trace) {
				return true;
			} else if (element instanceof Channel) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Trace) {
				return ((Trace) element).getChannelGroup();
			} else if (element instanceof Channel) {
				return ((Channel) element).getTrace();
			} else if (element instanceof ChannelEvent) {
				return ((ChannelEvent) element).getChannel();
			} else {
				return null;
			}
		}

	}

	public ChannelGroup getNamedPipeChannels(boolean setInput, IEditorPart editorPart1, IEditorPart editorPart2) {
		Trace trace1 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		Trace trace2 = new Trace(((AtlantisTraceEditor) editorPart2).getTitle());
		ChannelGroup namedPipeChannelGroup = new ChannelGroup(ChennelType.NamedPipeChannels, trace1, trace2);
		trace1.setChannelGroup(namedPipeChannelGroup);
		trace2.setChannelGroup(namedPipeChannelGroup);

		IFile editorFile1 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart1).getEmptyFile());
		searchNamedPipeChannels(editorFile1, (AtlantisTraceEditor) editorPart1, trace1);
		IFile editorFile2 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart2).getEmptyFile());
		searchNamedPipeChannels(editorFile2, (AtlantisTraceEditor) editorPart2, trace2);

		if (setInput) {
			Collection<ChannelGroup> channelGroups = new ArrayList<ChannelGroup>();
			channelGroups.add(namedPipeChannelGroup);
			resultTableViewer.setInput(channelGroups);
			traceEditor1 = (AtlantisTraceEditor) editorPart1;
			traceEditor2 = (AtlantisTraceEditor) editorPart2;
		}
		return namedPipeChannelGroup;
	}

	public ChannelGroup getTcpChannels(boolean setInput, IEditorPart editorPart1, IEditorPart editorPart2) {
		Trace trace1 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		Trace trace2 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		ChannelGroup tcpChannelGroup = new ChannelGroup(ChennelType.TCPChannels, trace1, trace2);

		if (setInput) {
			resultTableViewer.setInput(tcpChannelGroup);
			traceEditor1 = (AtlantisTraceEditor) editorPart1;
			traceEditor2 = (AtlantisTraceEditor) editorPart2;
		}
		return tcpChannelGroup;
	}

	public ChannelGroup getUDPChannels(boolean setInput, IEditorPart editorPart1, IEditorPart editorPart2) {
		Trace trace1 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		Trace trace2 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		ChannelGroup udpChannelGroup = new ChannelGroup(ChennelType.UDPChannels, trace1, trace2);
		if (setInput) {
			resultTableViewer.setInput(udpChannelGroup);
			traceEditor1 = (AtlantisTraceEditor) editorPart1;
			traceEditor2 = (AtlantisTraceEditor) editorPart2;
		}
		return udpChannelGroup;
	}

	public void searchAllChannels(IEditorPart editorPart1, IEditorPart editorPart2) {
		Collection<ChannelGroup> channelGroups = new ArrayList<ChannelGroup>();
		ChannelGroup namedPipeChannelGroup = getNamedPipeChannels(false, editorPart1, editorPart2);
		ChannelGroup tcpChannelGroup = getTcpChannels(false, editorPart1, editorPart2);
		ChannelGroup udpChannelGroup = getUDPChannels(false, editorPart1, editorPart2);
		channelGroups.add(namedPipeChannelGroup);
		channelGroups.add(tcpChannelGroup);
		channelGroups.add(udpChannelGroup);
		resultTableViewer.setInput(udpChannelGroup);
		traceEditor1 = (AtlantisTraceEditor) editorPart1;
		traceEditor2 = (AtlantisTraceEditor) editorPart2;
	}

	/**
	 * Update this view with the current comments data from the File Editor
	 */
	public void updateView() {
		// resultTableViewer.setInput(new ArrayList<ChannelGroup>());
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		System.out.println("partBroughtToTop:");

	}

	@Override
	public void partClosed(IWorkbenchPartReference arg0) {
		resultTableViewer.setInput(new ArrayList<ChannelGroup>());
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference arg0) {
		System.out.println("deativate:");
	}

	@Override
	public void partHidden(IWorkbenchPartReference arg0) {
		System.out.println("hide:");
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

	public void refresh() {
		resultTableViewer.refresh();
	}

	public void clearContents() {

	}

	@Override
	public void createPartControl(Composite parent) {
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		resultTableViewer = new TreeViewer(parent, SWT.V_SCROLL);
		resultTableViewer.setContentProvider(new ChannelContentProvider());
		resultTableViewer.setInput(new ArrayList<ChannelGroup>());
		createContextMenu();
		resultTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent e) {
				this.doubleClick(e);
			}
		});
	}
	
	private void doubleClick(DoubleClickEvent e) {
		if(!e.getSelection().isEmpty()) {

			ITreeSelection selection = (ITreeSelection) e.getSelection();
			Object selected = selection.getFirstElement();

			AtlantisTraceEditor activeTraceDisplayer = null;

			if (selected instanceof ChannelEvent) {
				ChannelEvent channelEvent = (ChannelEvent) selected;
				String traceName = channelEvent.getChannel().getTrace().getTraceName();
				if (traceEditor1.getTitle().equals(traceName)) {
					activeTraceDisplayer = (AtlantisTraceEditor) traceEditor1;
				} else if (traceEditor2.getTitle().equals(traceName)) {
					activeTraceDisplayer = (AtlantisTraceEditor) traceEditor2;
				} else {
					return;
				}
				try {
					if (activeTraceDisplayer != null) {
						BfvFileWithAddrMatch match = channelEvent.getFullFunctionMatch().getEventStart();
						BigInteger targetMemoryAdd = match.getTargetMemoryAddress();
						if (targetMemoryAdd != null){
							gotoAddressOfMessage(targetMemoryAdd);
						}
						
						activeTraceDisplayer.getProjectionViewer()
								.gotoLineAtOffset(match.getLineElement().getLine(), 0);
						activeTraceDisplayer.triggerCursorPositionChanged();
						
						RegistersView registersView = getRegistersView();
						if(registersView != null) {
							registersView.refresh(); // suspicious, this does not occur when the new line's memory data is available
						}
					}

				} catch (Exception ex) {
					System.err.println("Error jumping to line");
					ex.printStackTrace();
				}
			} else {
				return;
			}

		
		}
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
		ITreeSelection selection = (ITreeSelection) resultTableViewer.getSelection();
		Object selected = selection.getFirstElement();

		if (!(selected instanceof ChannelEvent)) {
			return;
		}

		mgr.add(gotoFunctionEnd);
	}

	@Override
	public void menuHidden(MenuEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void menuShown(MenuEvent arg0) {
		// TODO Auto-generated method stub

	}

	private void searchNamedPipeChannels(IFile currentFile, AtlantisTraceEditor editor, Trace trace) {
		System.out.println("Searching database function table backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting database function table search at time " + dateFormat.format(date));

		// search for createNamePipe function calls
		List<FullFunctionMatch> createNamePipeAFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getCreateNamedPipeA(),
				editor.getNamedPipeFunctions().getCreateNamedPipeFileNameReg(),
				editor.getNamedPipeFunctions().getCreateNamedPipeFileNameReg(), null);

		List<FullFunctionMatch> createFileAFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getCreateFileA(),
				editor.getNamedPipeFunctions().getCreateFileFileNameReg(),
				editor.getNamedPipeFunctions().getCreateFileFileNameReg(), null);

		List<FullFunctionMatch> createNamePipeWFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getCreateNamedPipeW(),
				editor.getNamedPipeFunctions().getCreateNamedPipeFileNameReg(),
				editor.getNamedPipeFunctions().getCreateNamedPipeFileNameReg(), null);

		List<FullFunctionMatch> createFileWFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getCreateFileW(),
				editor.getNamedPipeFunctions().getCreateFileFileNameReg(),
				editor.getNamedPipeFunctions().getCreateFileFileNameReg(), null);

		List<FullFunctionMatch> writeFileFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getWriteFile(),
				editor.getNamedPipeFunctions().getWriteFileFileHandleReg(),
				editor.getNamedPipeFunctions().getWriteFileDataAddrReg(), null);

		List<FullFunctionMatch> readFileFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getReadFile(), editor.getNamedPipeFunctions().getReadFileFileHandleReg(),
				null, editor.getNamedPipeFunctions().getReadFileDataAddrReg());

		List<FullFunctionMatch> closeHandleFuncs = searchFullFunction(currentFile, editor, trace,
				editor.getNamedPipeFunctions().getCloseHandle(),
				editor.getNamedPipeFunctions().getCloseHandleFileHandleReg(), null, null);

		for (FullFunctionMatch createPipe : createNamePipeAFuncs) {
			Channel c = new Channel(trace, createPipe.getEventStart().getLineElement().getLine(),
					createPipe.getRetVal(), createPipe.getInputVal());
			ChannelEvent e = new ChannelEvent(editor.getNamedPipeFunctions().getCreateNamedPipeAFuncName(),
					CommunicationStage.OPENING, createPipe, c);
			c.addEvent(e);
			trace.addChannel(c);

		}

		for (FullFunctionMatch createFile : createFileAFuncs) {
			Channel c = new Channel(trace, createFile.getEventStart().getLineElement().getLine(),
					createFile.getRetVal(), createFile.getInputVal());
			ChannelEvent e = new ChannelEvent(editor.getNamedPipeFunctions().getCreateFileAFuncName(),
					CommunicationStage.OPENING, createFile, c);
			c.addEvent(e);
			trace.addChannel(c);
		}

		for (FullFunctionMatch createPipe : createNamePipeWFuncs) {
			Channel c = new Channel(trace, createPipe.getEventStart().getLineElement().getLine(),
					createPipe.getRetVal(), createPipe.getInputVal());
			ChannelEvent e = new ChannelEvent(editor.getNamedPipeFunctions().getCreateNamedPipeWFuncName(),
					CommunicationStage.OPENING, createPipe, c);
			c.addEvent(e);
			trace.addChannel(c);

		}

		for (FullFunctionMatch createFile : createFileWFuncs) {
			Channel c = new Channel(trace, createFile.getEventStart().getLineElement().getLine(),
					createFile.getRetVal(), createFile.getInputVal());
			ChannelEvent e = new ChannelEvent(editor.getNamedPipeFunctions().getCreateFileWFuncName(),
					CommunicationStage.OPENING, createFile, c);
			c.addEvent(e);
			trace.addChannel(c);
		}

		for (FullFunctionMatch closeHandle : closeHandleFuncs) {
			searchChannelForClose(closeHandle.getInputVal(), closeHandle, trace.getChannels(), editor);
		}

		for (FullFunctionMatch writeFile : writeFileFuncs) {
			searchChannelForDataTrans(writeFile.getInputVal(), writeFile, trace.getChannels(), editor, editor.getNamedPipeFunctions().getWriteFileFuncName());
		}

		for (FullFunctionMatch readFile : readFileFuncs) {
			searchChannelForDataTrans(readFile.getInputVal(), readFile, trace.getChannels(), editor, editor.getNamedPipeFunctions().getReadFileFuncName());
		}

	}

	private void searchChannelForClose(String channelHandle, FullFunctionMatch closeHandle, List<Channel> channels,
			AtlantisTraceEditor editor) {
		Channel resultChannel = null;
		int lineNum = closeHandle.getEventStart().getLineElement().getLine();
		for (Channel c : channels) {
			if (c.getChannelHandle().equals(channelHandle) && c.getChannelStartLineNum() < lineNum
					&& (resultChannel == null || resultChannel.getChannelStartLineNum() < c.getChannelStartLineNum())) {
				resultChannel = c;
			}
		}

		if (resultChannel != null) {
			resultChannel.setChannelEndLineNum(lineNum);
			ChannelEvent e = new ChannelEvent(editor.getNamedPipeFunctions().getCloseHandleFuncName(),
					CommunicationStage.CLOSING, closeHandle, resultChannel);
			resultChannel.addEvent(e);
		}

	}

	private void searchChannelForDataTrans(String channelHandle, FullFunctionMatch writeFile, List<Channel> channels,
			AtlantisTraceEditor editor, String functionName) {
		Channel resultChannel = null;
		int lineNum = writeFile.getEventStart().getLineElement().getLine();
		for (Channel c : channels) {
			if (c.getChannelHandle().equals(channelHandle) && c.getChannelStartLineNum() < lineNum
					&& c.getChannelEndLineNum() > lineNum) {
				resultChannel = c;
			}
		}

		if (resultChannel != null) {
			resultChannel.setChannelEndLineNum(lineNum);
			ChannelEvent e = new ChannelEvent(functionName, CommunicationStage.DATATRANS, writeFile, resultChannel);
			resultChannel.addEvent(e);
		}
	}

	private List<FullFunctionMatch> searchFullFunction(IFile currentFile, AtlantisTraceEditor editor, Trace trace,
			Instruction instruction, Register inputValReg, Register inputAddReg, Register outputAddReg) {
		if (instruction == null) {
			return new ArrayList<FullFunctionMatch>();
		}

		InstructionId instructionId = instruction.getIdGlobalUnique();

		ISearchQuery dummyQuery;
		RegexSearchInput dummyInput = new RegexSearchInput(instructionId.toString(), false, false, false, currentFile);
		if (dummyInput.getScope() != null) {
			try {
				dummyQuery = TextSearchQueryProvider.getPreferred().createQuery(dummyInput);
				dummyQuery.run(null);
				this.searchResults.removeAll();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils
				.getFileModelDataLayerFromRegistry(editor.getCurrentBlankFile());
		((BinaryFormatFileModelDataLayer) fileModel).performFunctionSearch(this.searchResults, instructionId,
				currentFile, (AtlantisTraceEditor) editor);
		IFile empty = BfvFileUtils.convertFileIFile(editor.getEmptyFile());
		Match[] originalMatches = this.searchResults.getMatches(empty);
		List<FullFunctionMatch> fullFunctionMatchs = new ArrayList<FullFunctionMatch>();
		for (int i = 0; i < originalMatches.length; i++) {
			FileMatch match = (FileMatch) originalMatches[i];
			LineElement lineElement = match.getLineElement();
			int intraLineOffset = match.getOffset() - lineElement.getOffset();
			IFile activeEmpty = BfvFileUtils.convertFileIFile(editor.getEmptyFile());

			Job memoryUpdateJob = new Job("Memory Update Job") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					fullFunctionMatch = null;
					final AsyncResult<MemoryQueryResults> funcStartMemoryEventsAsync = fileModel
							.getMemoryEventsAsync(lineElement.getLine() + 1, monitor);

					if (funcStartMemoryEventsAsync.isCancelled()) {
						return Status.CANCEL_STATUS;
					}
					MemoryQueryResults functionStartMemoryReferencesResult = funcStartMemoryEventsAsync.getResult();
					ModelProvider.INSTANCE.setMemoryQueryResults(functionStartMemoryReferencesResult,
							lineElement.getLine());
					functionStartMemoryReferencesResult.collateMemoryAndRegisterResults(lineElement.getLine());
					SortedMap<String, MemoryReference> map = functionStartMemoryReferencesResult.getRegisterList();

					BigInteger inputAddress = null;
					if (inputAddReg != null) {
						MemoryReference mfInputA = map.get(inputAddReg.getName().toUpperCase());
						if (mfInputA != null) {
							Long add = Long.parseLong(mfInputA.getMemoryContent().getMemoryValue(), 16);
							inputAddress = BigInteger.valueOf(add);
						}
					}

					BigInteger outputAddress = null;
					if (outputAddReg != null) {
						MemoryReference mfOutput = map.get(outputAddReg.getName().toUpperCase());
						if (mfOutput != null) {
							Long add = Long.parseLong(mfOutput.getMemoryContent().getMemoryValue(), 16);
							outputAddress = BigInteger.valueOf(add);
						}
					}
					int moduleId = instruction.getModuleId();
					int retLineNumber = (int) fileModel.getFunctionRetLine(moduleId, lineElement.getLine() - 1);
					System.out.println("ret line:" + retLineNumber);

					FileMatch retMatch = (FileMatch) ((BinaryFormatFileModelDataLayer) fileModel)
							.getTraceLine(retLineNumber, currentFile, (AtlantisTraceEditor) editor);
					LineElement retLineElement = retMatch.getLineElement();
					int intraLineOffset = retMatch.getOffset() - retLineElement.getOffset();

					// These will cancel themselves and return null if cancelled
					final AsyncResult<MemoryQueryResults> functionEndMemoryEventsAsync = fileModel
							.getMemoryEventsAsync(retLineElement.getLine() + 1, monitor);

					if (functionEndMemoryEventsAsync.isCancelled()) {
						return Status.CANCEL_STATUS;
					}

					MemoryQueryResults functionEndMemoryReferencesResult = functionEndMemoryEventsAsync.getResult();
					ModelProvider.INSTANCE.setMemoryQueryResults(functionEndMemoryReferencesResult, retLineNumber);
					functionEndMemoryReferencesResult.collateMemoryAndRegisterResults(retLineNumber);
					SortedMap<String, MemoryReference> retmap = functionEndMemoryReferencesResult.getRegisterList();
					String returnReg = editor.getNamedPipeFunctions().getRetrunValReg().getName();
					String retVal = "";
					MemoryReference mfRet = retmap.get(returnReg.toUpperCase());
					if (mfRet != null) {
						retVal = mfRet.getMemoryContent().getMemoryValue();
					}

					String inputVal = "";
					if (inputValReg != null) {
						MemoryReference mfInput = map.get(inputValReg.getName().toUpperCase());
						if (mfInput != null) {
							if (inputValReg.isValue()) {
								inputVal = mfInput.getMemoryContent().getMemoryValue();
							} else {
								Long add = Long.parseLong(mfInput.getMemoryContent().getMemoryValue(), 16);
								for (MemoryReference memRef : functionEndMemoryReferencesResult.getMemoryList()
										.values()) {
									if (add <= memRef.getAddress() && add + 256 > memRef.getEndAddress()) {
										String hex = memRef.getMemoryContent().getMemoryValue();
										for (int i = 0; i < hex.length(); i += 2) {
											String str = hex.substring(i, i + 2);
											inputVal += (char) Integer.parseInt(str, 16);
										}
									}
								}
								inputVal = inputVal.split("\n")[0];
								inputVal = inputVal.trim();
							}
						}
					}

					BfvFileWithAddrMatch eventEnd = new BfvFileWithAddrMatch(activeEmpty, retMatch.getOriginalOffset(),
							retMatch.getLength(), new LineElement(currentFile, retLineNumber,
									retLineElement.getOffset(), retLineElement.getContents()),
							intraLineOffset, outputAddress);

					BfvFileWithAddrMatch eventStart = new BfvFileWithAddrMatch(activeEmpty,
							match.getOriginalOffset(), match.getLength(), new LineElement(currentFile,
									lineElement.getLine(), lineElement.getOffset(), lineElement.getContents()),
							intraLineOffset, inputAddress);

					fullFunctionMatch = new FullFunctionMatch(eventStart, eventEnd, inputVal, retVal);
					return Status.OK_STATUS;
				}
			};

			memoryUpdateJob.schedule();
			try {
				memoryUpdateJob.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (fullFunctionMatch != null) {
				fullFunctionMatchs.add(fullFunctionMatch);
			}

		}
		return fullFunctionMatchs;
	}

	protected class RegexSearchInput extends TextSearchInput {

		private String searchText;
		private boolean caseSensitive;
		private boolean useRegexSearch;
		private boolean useRealScope;
		private IFile currentFile;

		public RegexSearchInput(String searchText, boolean caseSensitive, boolean useRegexSearch, boolean useRealScope,
				IFile currentFile) {
			this.searchText = searchText;
			this.caseSensitive = caseSensitive;
			this.useRegexSearch = useRegexSearch;
			this.useRealScope = useRealScope;
			this.currentFile = currentFile;
		}

		@Override
		public String getSearchText() {
			return searchText;
		}

		@Override
		public boolean isCaseSensitiveSearch() {
			return caseSensitive;
		}

		@Override
		public boolean isRegExSearch() {
			return this.useRegexSearch;
		}

		@Override
		public FileTextSearchScope getScope() {
			String[] fileNamePatterns = null; // have to do it this way to avoid
												// an ambiguous method call
			if (!useRealScope) {
				return FileTextSearchScope.newSearchScope(new IResource[] {}, fileNamePatterns, true);
			} else {
				try {
					return FileTextSearchScope.newSearchScope(new IResource[] { currentFile }, fileNamePatterns, true);
				} catch (NullPointerException e) {
					// No files are currently open, so there's no file to search
					return null;
				}
			}
		}
	}

	public void createActions() {
		gotoFunctionEnd = new Action("Go To Line of Function End") {
			@Override
			public void run() {

				ITreeSelection selection = (ITreeSelection) resultTableViewer.getSelection();
				Object selected = selection.getFirstElement();

				AtlantisTraceEditor activeTraceDisplayer = null;

				if (selected instanceof ChannelEvent) {
					ChannelEvent e = (ChannelEvent) selected;
					String traceName = e.getChannel().getTrace().getTraceName();
					if (traceEditor1.getTitle().equals(traceName)) {
						activeTraceDisplayer = (AtlantisTraceEditor) traceEditor1;
					} else if (traceEditor2.getTitle().equals(traceName)) {
						activeTraceDisplayer = (AtlantisTraceEditor) traceEditor2;
					} else {
						return;
					}
					try {
						if (activeTraceDisplayer != null) {
							BfvFileWithAddrMatch match = e.getFullFunctionMatch().getEventEnd();
							BigInteger targetMemoryAdd = match.getTargetMemoryAddress();
							if (targetMemoryAdd != null){
								gotoAddressOfMessage(targetMemoryAdd);
							}
							
							activeTraceDisplayer.getProjectionViewer()
									.gotoLineAtOffset(match.getLineElement().getLine(), 0);
							activeTraceDisplayer.triggerCursorPositionChanged();
							
							RegistersView registersView = getRegistersView();
							if(registersView != null) {
								registersView.refresh(); // suspicious, this does not occur when the new line's memory data is available
							}
						}

					} catch (Exception ex) {
						System.err.println("Error jumping to line");
						ex.printStackTrace();
					}
				} else {
					return;
				}

			}
		};

	}
	
	private RegistersView getRegistersView() {
		if(null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()){
			return null;
		}
		return (RegistersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RegistersView.ID);
	}

	private void gotoAddressOfMessage(BigInteger messageAddress) {
		try {
			HexVisualization hexView = (HexVisualization) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(HexVisualization.ID);
			if (hexView != null) {
				hexView.setAddress(messageAddress);
			}
		} catch (PartInitException e) {
			// Failure? It's ok.
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}