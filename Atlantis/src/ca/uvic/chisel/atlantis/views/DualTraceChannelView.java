package ca.uvic.chisel.atlantis.views;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.lang3.ArrayUtils;
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
import ca.uvic.chisel.atlantis.functionparsing.ChannelFunctionInstruction;
import ca.uvic.chisel.atlantis.functionparsing.ChannelTypeInstruction;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.functionparsing.Register;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.dualtracechannel.BfvFileWithAddrMatch;
import ca.uvic.chisel.bfv.dualtracechannel.Channel;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelEvent;
import ca.uvic.chisel.bfv.dualtracechannel.ChannelGroup;
import ca.uvic.chisel.bfv.dualtracechannel.CommunicationStage;
import ca.uvic.chisel.bfv.dualtracechannel.FullFunctionMatch;
import ca.uvic.chisel.bfv.dualtracechannel.FunctionType;
import ca.uvic.chisel.bfv.dualtracechannel.Trace;
import ca.uvic.chisel.bfv.dualtracechannelmatch.ChannelDataTransEvent;
import ca.uvic.chisel.bfv.dualtracechannelmatch.ChannelOpenCloseEvent;
import ca.uvic.chisel.bfv.dualtracechannelmatch.FullFunctionMatchOfTrace;
import ca.uvic.chisel.bfv.dualtracechannelmatch.MatchChannel;
import ca.uvic.chisel.bfv.dualtracechannelmatch.MatchChannelGroup;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

public class DualTraceChannelView extends ViewPart implements IPartListener2, MenuListener {

	public static final String ID = "ca.uvic.chisel.atlantis.views.DualTraceChannelView";
	private FileSearchResult searchResults = new FileSearchResult((FileSearchQuery) null);
	// private Table resultTable;
	private TreeViewer resultTableViewer;
	private TreeViewer matchResultTableViewer;
	private Action gotoFunctionEnd;
	private Action removeChannel;
	private Action gotoFunctionEndInMatch;
	private Action removeChannelInMatch;
	FullFunctionMatch fullFunctionMatch = null;
	private AtlantisTraceEditor traceEditor1;
	private AtlantisTraceEditor traceEditor2;

	public DualTraceChannelView() {
		this.searchResults = new FileSearchResult((FileSearchQuery) null);
	}

	private class MatchChannelContentPrivider implements ITreeContentProvider {

		private Collection<MatchChannelGroup> channelGroups;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				channelGroups = new ArrayList<MatchChannelGroup>(); // clear out
																	// the
				// old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					channelGroups.add((MatchChannelGroup) o);
				}
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MatchChannelGroup) {
				MatchChannelGroup group = (MatchChannelGroup) parentElement;
				return group.getChannels().toArray();
			} else if (parentElement instanceof MatchChannel) {
				MatchChannel channel = (MatchChannel) parentElement;
				Object[] openEventsInTrace1 = channel.getOpenEventsInTrace1().toArray();
				Object[] openEventsInTrace2 = ArrayUtils.addAll(openEventsInTrace1,
						channel.getOpenEventsInTrace2().toArray());
				Object[] closeEventsInTrace1 = ArrayUtils.addAll(openEventsInTrace2,
						channel.getCloseEventsInTrace1().toArray());
				Object[] closeEventsInTrace2 = ArrayUtils.addAll(closeEventsInTrace1,
						channel.getCloseEventsInTrace2().toArray());
				Object[] channelDatatransEvents = ArrayUtils.addAll(closeEventsInTrace2,
						channel.getChannelDatatransEvents().toArray());
				return channelDatatransEvents;
			} else if (parentElement instanceof ChannelDataTransEvent) {
				ChannelDataTransEvent event = (ChannelDataTransEvent) parentElement;
				Object[] sendrecv = new Object[] { event.getSend(), event.getRecv() };
				return sendrecv;
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
			if (element instanceof MatchChannelGroup) {
				return true;
			} else if (element instanceof MatchChannel) {
				return true;
			} else if (element instanceof ChannelDataTransEvent) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof ChannelDataTransEvent) {
				return ((ChannelDataTransEvent) element).getChannel();
			} else if (element instanceof ChannelOpenCloseEvent) {
				return ((ChannelOpenCloseEvent) element).getChannel();
			} else if (element instanceof MatchChannel) {
				return ((MatchChannel) element).getChannelGroup();
			} else {
				return null;
			}
		}

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

	public void getChannels(List<String> selectedChannels, IEditorPart editorPart1, IEditorPart editorPart2) {
		Collection<ChannelGroup> channelGroups = new ArrayList<ChannelGroup>();
		for (String channel : selectedChannels) {
			if (((AtlantisTraceEditor) editorPart1).getChannelTypeInc(channel) == null
					|| ((AtlantisTraceEditor) editorPart2).getChannelTypeInc(channel) == null) {
				Throwable throwable = new Throwable("No corresponding library exports has been loaded for " + channel);
				BigFileApplication.showErrorDialog("No corresponding library exports has been loaded for " + channel,
						"Please load library exports from \"Dual trace Tool\" Menu for both traces", throwable);
				return;
			}
			Trace trace1 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
			Trace trace2 = new Trace(((AtlantisTraceEditor) editorPart2).getTitle());
			ChannelGroup channelGroup = new ChannelGroup(channel, trace1, trace2);
			IFile editorFile1 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart1).getEmptyFile());
			searchChannels(editorFile1, (AtlantisTraceEditor) editorPart1, trace1, channel);
			IFile editorFile2 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart2).getEmptyFile());
			searchChannels(editorFile2, (AtlantisTraceEditor) editorPart2, trace2, channel);
			channelGroups.add(channelGroup);
		}

		resultTableViewer.setInput(channelGroups);
		traceEditor1 = (AtlantisTraceEditor) editorPart1;
		traceEditor2 = (AtlantisTraceEditor) editorPart2;
	}

	public void getMatchChannels(List<String> selectedChannels, IEditorPart editorPart1, IEditorPart editorPart2) {
		Collection<MatchChannelGroup> channelGroups = new ArrayList<MatchChannelGroup>();
		Trace trace1 = new Trace(((AtlantisTraceEditor) editorPart1).getTitle());
		Trace trace2 = new Trace(((AtlantisTraceEditor) editorPart2).getTitle());
		IFile editorFile1 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart1).getEmptyFile());
		IFile editorFile2 = BfvFileUtils.convertFileIFile(((AtlantisTraceEditor) editorPart2).getEmptyFile());
		traceEditor1 = (AtlantisTraceEditor) editorPart1;
		traceEditor2 = (AtlantisTraceEditor) editorPart2;
		for (String channelGroupName : selectedChannels) {
			if (((AtlantisTraceEditor) editorPart1).getChannelTypeInc(channelGroupName) == null
					|| ((AtlantisTraceEditor) editorPart2).getChannelTypeInc(channelGroupName) == null) {
				Throwable throwable = new Throwable(
						"No corresponding library exports has been loaded for " + channelGroupName);
				BigFileApplication.showErrorDialog(
						"No corresponding library exports has been loaded for " + channelGroupName,
						"Please load library exports from \"Dual trace Tool\" Menu for both traces", throwable);
				return;
			}

			searchChannels(editorFile1, (AtlantisTraceEditor) editorPart1, trace1, channelGroupName);
			searchChannels(editorFile2, (AtlantisTraceEditor) editorPart2, trace2, channelGroupName);
			MatchChannelGroup channelGroup = new MatchChannelGroup(channelGroupName,
					((AtlantisTraceEditor) editorPart1).getTitle(), ((AtlantisTraceEditor) editorPart2).getTitle());
			for (Channel c1 : trace1.getChannels()) {
				for (Channel c2 : trace2.getChannels()) {
					if (c1.getChannelID().equals(c2.getChannelID())) {
						MatchChannel channel = matchTwoChannels(c1, c2, channelGroup);
						channelGroup.addChannelToGroup(channel);
					}
				}
			}
			
			channelGroups.add(channelGroup);
		}

		matchResultTableViewer.setInput(channelGroups);
	}

	private MatchChannel matchTwoChannels(Channel c1, Channel c2, MatchChannelGroup channelGroup) {
		MatchChannel channel = new MatchChannel(c1.getChannelID(), channelGroup);
		for (ChannelEvent e1 : c1.getEvents()) {
			String retval1 = e1.getFullFunctionMatch().getRetVal().substring(e1.getFullFunctionMatch().getRetVal().length()-8, 16);
			if (e1.getStage() == CommunicationStage.OPENING) {
				ChannelOpenCloseEvent e = new ChannelOpenCloseEvent(e1.getFunctionName(), CommunicationStage.OPENING,
						new FullFunctionMatchOfTrace(e1.getFullFunctionMatch(), c1.getTrace().getTraceName(),e1.getFunctionName()), channel, c1.getTrace().getTraceName());
				channel.addEventToOpenList1(e);
			} else if (e1.getStage() == CommunicationStage.CLOSING) {
				ChannelOpenCloseEvent e = new ChannelOpenCloseEvent(e1.getFunctionName(), CommunicationStage.CLOSING,
						new FullFunctionMatchOfTrace(e1.getFullFunctionMatch(), c1.getTrace().getTraceName(),e1.getFunctionName()), channel, c1.getTrace().getTraceName());
				channel.addEventToCloseList1(e);
				
			} else if (e1.getStage() == CommunicationStage.DATATRANS && !retval1.equals("00000000")) {
				String messageSend1 = "";
				String messageRecv1 = "";
				if (e1.getFullFunctionMatch().getType() == FunctionType.send) {
					messageSend1 = e1.getFullFunctionMatch().getEventStart().getMessage();
				} else {
					messageRecv1 = e1.getFullFunctionMatch().getEventEnd().getMessage();
				}

				for (ChannelEvent e2 : c2.getEvents()) {
					String messageSend2= "";
					String messageRecv2 = "";
					String retval2 = e2.getFullFunctionMatch().getRetVal().substring(e2.getFullFunctionMatch().getRetVal().length()-8, 16);
					if(retval2.equals("00000000")){
						continue;
					}
					if (e1.getFullFunctionMatch().getType() == FunctionType.send
							&& e2.getFullFunctionMatch().getType() != FunctionType.send) {
						messageRecv2 = e2.getFullFunctionMatch().getEventEnd().getMessage();
					} else if (e1.getFullFunctionMatch().getType() != FunctionType.send
							&& e2.getFullFunctionMatch().getType() == FunctionType.send) {
						messageSend2 = e2.getFullFunctionMatch().getEventStart().getMessage();
					}
					
					
					if (!messageSend1.equals("") && !messageRecv2.equals("") && messageRecv2.startsWith(messageSend1)) {
						FullFunctionMatchOfTrace match1 = new FullFunctionMatchOfTrace(e1.getFullFunctionMatch(),
								e1.getChannel().getTrace().getTraceName(),e1.getFunctionName());
						FullFunctionMatchOfTrace match2 = new FullFunctionMatchOfTrace(e2.getFullFunctionMatch(),
								e2.getChannel().getTrace().getTraceName(),e2.getFunctionName());
						ChannelDataTransEvent dataTransEvent = new ChannelDataTransEvent(match1, match2, messageSend1,channel);
						channel.addEventToDataTransList(dataTransEvent);
					}else if (!messageSend2.equals("") && !messageRecv1.equals("") && messageRecv1.startsWith(messageSend2)) {
						FullFunctionMatchOfTrace match1 = new FullFunctionMatchOfTrace(e1.getFullFunctionMatch(),
								e1.getChannel().getTrace().getTraceName(),e1.getFunctionName());
						FullFunctionMatchOfTrace match2 = new FullFunctionMatchOfTrace(e2.getFullFunctionMatch(),
								e2.getChannel().getTrace().getTraceName(),e2.getFunctionName());
						ChannelDataTransEvent dataTransEvent = new ChannelDataTransEvent(match1, match2, messageSend2,channel);
						channel.addEventToDataTransList(dataTransEvent);
					}
				}
			}

		}
		for (ChannelEvent e2 : c2.getEvents()) {
			if (e2.getStage() == CommunicationStage.OPENING) {
				ChannelOpenCloseEvent e = new ChannelOpenCloseEvent(e2.getFunctionName(), CommunicationStage.OPENING,
						new FullFunctionMatchOfTrace(e2.getFullFunctionMatch(), c2.getTrace().getTraceName(),e2.getFunctionName()), channel, c2.getTrace().getTraceName());
				channel.addEventToOpenList1(e);
			} else if (e2.getStage() == CommunicationStage.CLOSING) {
				ChannelOpenCloseEvent e = new ChannelOpenCloseEvent(e2.getFunctionName(), CommunicationStage.CLOSING,
						new FullFunctionMatchOfTrace(e2.getFullFunctionMatch(), c2.getTrace().getTraceName(),e2.getFunctionName()), channel, c2.getTrace().getTraceName());
				channel.addEventToCloseList1(e);
			}
		}

		return channel;
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
		matchResultTableViewer.setInput(new ArrayList<ChannelGroup>());
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
		matchResultTableViewer.refresh();
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
				DualTraceChannelView.this.doubleClick(e);
			}
		});

		matchResultTableViewer = new TreeViewer(parent, SWT.V_SCROLL);
		matchResultTableViewer.setContentProvider(new MatchChannelContentPrivider());
		matchResultTableViewer.setInput(new ArrayList<ChannelGroup>());
		createContextMenuForMatch();
		matchResultTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent e) {
				DualTraceChannelView.this.doubleClickForMatch(e);
			}
		});
	}

	private void doubleClick(DoubleClickEvent e) {
		if (!e.getSelection().isEmpty()) {

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
						if (targetMemoryAdd != null) {
							gotoAddressOfMessage(targetMemoryAdd);
						}

						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(match.getLineElement().getLine(),
								0);
						activeTraceDisplayer.triggerCursorPositionChanged();

						RegistersView registersView = getRegistersView();
						if (registersView != null) {
							registersView.refresh(); // suspicious, this does
														// not occur when the
														// new line's memory
														// data is available
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
	
	private void doubleClickForMatch(DoubleClickEvent e) {
		if (!e.getSelection().isEmpty()) {

			ITreeSelection selection = (ITreeSelection) e.getSelection();
			Object selected = selection.getFirstElement();

			AtlantisTraceEditor activeTraceDisplayer = null;

			if (selected instanceof FullFunctionMatchOfTrace) {
				FullFunctionMatchOfTrace functionMatch = (FullFunctionMatchOfTrace) selected;
				String traceName = functionMatch.getTraceName();
				if (traceEditor1.getTitle().equals(traceName)) {
					activeTraceDisplayer = (AtlantisTraceEditor) traceEditor1;
				} else if (traceEditor2.getTitle().equals(traceName)) {
					activeTraceDisplayer = (AtlantisTraceEditor) traceEditor2;
				} else {
					return;
				}
				try {
					if (activeTraceDisplayer != null) {
						BfvFileWithAddrMatch match = functionMatch.getEventStart();
						BigInteger targetMemoryAdd = match.getTargetMemoryAddress();
						if (targetMemoryAdd != null) {
							gotoAddressOfMessage(targetMemoryAdd);
						}

						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(match.getLineElement().getLine(),
								0);
						activeTraceDisplayer.triggerCursorPositionChanged();

						RegistersView registersView = getRegistersView();
						if (registersView != null) {
							registersView.refresh(); // suspicious, this does
														// not occur when the
														// new line's memory
														// data is available
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
		
		
		//

	}
	
	private void createContextMenuForMatch() {
		// Create menu manager.
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenuForMatch(mgr);
			}
		});

		// Create menu.
		Menu menu = menuMgr.createContextMenu(matchResultTableViewer.getControl());
		matchResultTableViewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, matchResultTableViewer);
		
		
		//

	}

	private void fillContextMenu(IMenuManager mgr) {
		ITreeSelection selection = (ITreeSelection) resultTableViewer.getSelection();
		Object selected = selection.getFirstElement();

		if (selected instanceof ChannelEvent) {
			mgr.add(gotoFunctionEnd);
			return;
		}

		if (selected instanceof Channel) {
			mgr.add(removeChannel);
			return;
		}

	}
	
	private void fillContextMenuForMatch(IMenuManager mgr) {
		ITreeSelection mselection = (ITreeSelection) matchResultTableViewer.getSelection();
		Object mselected = mselection.getFirstElement();

		if (mselected instanceof ChannelOpenCloseEvent || mselected instanceof FullFunctionMatchOfTrace) {
			mgr.add(gotoFunctionEndInMatch);
			return;
		}

		if (mselected instanceof MatchChannel) {
			mgr.add(removeChannelInMatch);
			return;
		}

	}

	@Override
	public void menuHidden(MenuEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void menuShown(MenuEvent arg0) {
		// TODO Auto-generated method stub

	}

	private void searchChannels(IFile currentFile, AtlantisTraceEditor editor, Trace trace, String channel) {
		System.out.println("Searching database function table backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting database function table search at time " + dateFormat.format(date));

		Map<String, List<FullFunctionMatch>> channelOpenStagefuns = new HashMap<String, List<FullFunctionMatch>>();
		Map<String, List<FullFunctionMatch>> dataTransStagefuns = new HashMap<String, List<FullFunctionMatch>>();
		Map<String, List<FullFunctionMatch>> channelCloseStagefuns = new HashMap<String, List<FullFunctionMatch>>();

		ChannelTypeInstruction channelTypeInc = editor.getChannelTypeInc(channel);
		for (ChannelFunctionInstruction function : channelTypeInc.getChannelOpenStageList()) {
			List<FullFunctionMatch> functionList = searchFullFunction(currentFile, editor, trace, function);
			channelOpenStagefuns.put(function.getFunction().getFunctionName(), functionList);
		}

		for (ChannelFunctionInstruction function : channelTypeInc.getDataTransStageList()) {
			List<FullFunctionMatch> functionList = searchFullFunction(currentFile, editor, trace, function);
			dataTransStagefuns.put(function.getFunction().getFunctionName(), functionList);
		}

		for (ChannelFunctionInstruction function : channelTypeInc.getChannelCloseStageList()) {
			List<FullFunctionMatch> functionList = searchFullFunction(currentFile, editor, trace, function);
			channelCloseStagefuns.put(function.getFunction().getFunctionName(), functionList);
		}

		for (Entry<String, List<FullFunctionMatch>> entry : channelOpenStagefuns.entrySet()) {
			for (FullFunctionMatch match : entry.getValue()) {
				Channel c = new Channel(trace, match.getEventStart().getLineElement().getLine(), match.getRetVal(),
						match.getInputVal());
				ChannelEvent e = new ChannelEvent(entry.getKey(), CommunicationStage.OPENING, match, c);
				c.addEvent(e);
				trace.addChannel(c);
			}
		}

		for (Entry<String, List<FullFunctionMatch>> entry : channelCloseStagefuns.entrySet()) {
			for (FullFunctionMatch match : entry.getValue()) {
				searchChannelForClose(match.getInputVal(), match, trace.getChannels(), editor, entry.getKey());
			}
		}

		for (Entry<String, List<FullFunctionMatch>> entry : dataTransStagefuns.entrySet()) {
			for (FullFunctionMatch match : entry.getValue()) {
				searchChannelForDataTrans(match.getInputVal(), match, trace.getChannels(), editor, entry.getKey());
			}
		}

	}

	private void searchChannelForClose(String channelHandle, FullFunctionMatch closeHandle, List<Channel> channels,
			AtlantisTraceEditor editor, String functionName) {
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
			ChannelEvent e = new ChannelEvent(functionName, CommunicationStage.CLOSING, closeHandle, resultChannel);
			resultChannel.addEvent(e);
		}

	}

	private void searchChannelForDataTrans(String channelHandle, FullFunctionMatch match, List<Channel> channels,
			AtlantisTraceEditor editor, String functionName) {
		Channel resultChannel = null;
		int lineNum = match.getEventStart().getLineElement().getLine();
		for (Channel c : channels) {
			if (c.getChannelHandle().equals(channelHandle) && c.getChannelStartLineNum() < lineNum
					&& (c.getChannelEndLineNum() == 0 || c.getChannelEndLineNum() > lineNum)) {

				resultChannel = c;
			}
		}

		if (resultChannel != null) {
			ChannelEvent e = new ChannelEvent(functionName, CommunicationStage.DATATRANS, match, resultChannel);
			resultChannel.addEvent(e);
		}
	}

	private List<FullFunctionMatch> searchFullFunction(IFile currentFile, AtlantisTraceEditor editor, Trace trace,
			ChannelFunctionInstruction function) {
		Instruction instruction = function.getInstruction();
		if (instruction == null) {
			return new ArrayList<FullFunctionMatch>();
		}

		Register inputValReg = function.getFunction().getValueInputReg();
		Register inputAddReg = function.getFunction().getMemoryInputReg();
		Register outputAddReg = function.getFunction().getMemoryOutputReg();
		Register retValReg = function.getFunction().getRetrunValReg();
		Register inputLengthReg = function.getFunction().getMemoryInputLenReg();
		Register outputLengthReg = function.getFunction().getMemoryOutputBufLenReg();

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
					Long inputadd = 0l;
					if (inputAddReg != null) {
						MemoryReference mfInputA = map.get(inputAddReg.getName().toUpperCase());
						if (mfInputA != null) {
							inputadd = Long.parseLong(mfInputA.getMemoryContent().getMemoryValue(), 16);
							inputAddress = BigInteger.valueOf(inputadd);
						}
					}

					long inputLen = 0;
					if (inputLengthReg != null) {
						MemoryReference mfInputLenA = map.get(inputLengthReg.getName().toUpperCase());
						if (mfInputLenA != null) {
							String s = mfInputLenA.getMemoryContent().getMemoryValue();
							inputLen = Long.parseLong(s.substring(s.length()-8), 16);
						}
					}

					int moduleId = instruction.getModuleId();
					int retLineNumber = (int) fileModel.getFunctionRetLine(lineElement.getLine() - 1);
					System.out.println("ret line:" + retLineNumber);

					FileMatch retMatch = (FileMatch) ((BinaryFormatFileModelDataLayer) fileModel)
							.getTraceLine(retLineNumber, currentFile, (AtlantisTraceEditor) editor);
					LineElement retLineElement = retMatch.getLineElement();
					int intraLineOffset = retMatch.getOffset() - retLineElement.getOffset();

					// These will cancel themselves and return null if cancelled
					final AsyncResult<MemoryQueryResults> functionEndMemoryEventsAsync = fileModel
							.getMemoryEventsAsync(retLineElement.getLine()+1, monitor);

					if (functionEndMemoryEventsAsync.isCancelled()) {
						return Status.CANCEL_STATUS;
					}

					MemoryQueryResults functionEndMemoryReferencesResult = functionEndMemoryEventsAsync.getResult();
					ModelProvider.INSTANCE.setMemoryQueryResults(functionEndMemoryReferencesResult, retLineNumber);
					functionEndMemoryReferencesResult.collateMemoryAndRegisterResults(retLineNumber);
					SortedMap<String, MemoryReference> retmap = functionEndMemoryReferencesResult.getRegisterList();
					String retVal = "";
					if (retValReg != null) {
						MemoryReference mfRet = retmap.get(retValReg.getName().toUpperCase());
						if (mfRet != null) {
							retVal = mfRet.getMemoryContent().getMemoryValue();
						}
					}


					String inputVal = "";
					if (inputValReg != null) {
						MemoryReference mfInput = map.get(inputValReg.getName().toUpperCase());
						if (mfInput != null) {
							if (inputValReg.isValue()) {
								inputVal = mfInput.getMemoryContent().getMemoryValue();
							} else {
								Long add = Long.parseLong(mfInput.getMemoryContent().getMemoryValue(), 16);
								for (MemoryReference memRef : functionStartMemoryReferencesResult.getMemoryList().values()) {
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

								if (inputVal == "") {
									for (MemoryReference memRef : functionEndMemoryReferencesResult.getMemoryList().values()) {
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
					}

					long outputBufLen = 0;
					if (outputLengthReg != null) {
						MemoryReference mfOutputLen = map.get(outputLengthReg.getName().toUpperCase());
						if (mfOutputLen != null) {
							String s = mfOutputLen.getMemoryContent().getMemoryValue();
							outputBufLen = Long.parseLong(s.substring(s.length()-8), 16);
						}
					}
					
					BigInteger outputAddress = null;
					Long outputadd = 0l;
					if (outputAddReg != null) {
						MemoryReference mfOutput = map.get(outputAddReg.getName().toUpperCase());
						if (mfOutput != null) {
							outputadd = Long.parseLong(mfOutput.getMemoryContent().getMemoryValue(), 16);
							outputAddress = BigInteger.valueOf(outputadd);
						}
					}
					
					if (function.getFunction().getType() == FunctionType.recv
							&& function.getFunction().getOutputDataAddressIndex() != null) {
						editor.addToDataAddressMap(
								function.getFunction().getOutputDataAddressIndex() + inputVal, outputAddress);
						editor.addToDataBufLenMap(
								function.getFunction().getOutputDataAddressIndex() + inputVal, outputBufLen);

					}

					if (function.getFunction().getType() == FunctionType.check
							&& function.getFunction().getOutputDataAddressIndex() != null) {
						outputAddress = editor
								.getFromDataAddressMap(function.getFunction().getOutputDataAddressIndex() + inputVal);
						outputBufLen = editor.getFromDataBufLenMap(function.getFunction().getOutputDataAddressIndex() + inputVal);
					}

					String messageAtStart = "";
					String messageAtEnd = "";
					if (function.getFunction().getType() == FunctionType.send && inputAddress != null) {
						for (MemoryReference memRef : functionStartMemoryReferencesResult.getMemoryList().values()) {
							if (inputadd <= memRef.getAddress() && inputadd + inputLen + 10 > memRef.getEndAddress()) {
								String hex = memRef.getMemoryContent().getMemoryValue();
								for (int i = 0; i < hex.length(); i += 2) {
									String str = hex.substring(i, i + 2);
									messageAtStart += (char) Integer.parseInt(str, 16);
								}
							}
						}
						messageAtStart = messageAtStart.substring(0, (int)inputLen-1);
					} else if (function.getFunction().getType() == FunctionType.recv
							|| function.getFunction().getType() == FunctionType.check) {
						for (MemoryReference memRef : functionEndMemoryReferencesResult.getMemoryList().values()) {
							if (outputadd <= memRef.getAddress() && outputadd + outputBufLen > memRef.getEndAddress()) {
								String hex = memRef.getMemoryContent().getMemoryValue();
								for (int i = 0; i < hex.length(); i += 2) {
									String str = hex.substring(i, i + 2);
									messageAtEnd += (char) Integer.parseInt(str, 16);
								}
							}
						}
						messageAtEnd = messageAtEnd.split("\n")[0];
						messageAtEnd = messageAtEnd.trim();
					}
					


					BfvFileWithAddrMatch eventEnd = new BfvFileWithAddrMatch(activeEmpty, retMatch.getOriginalOffset(),
							retMatch.getLength(),
							new LineElement(currentFile, retLineNumber, retLineElement.getOffset(),
									retLineElement.getContents()),
							intraLineOffset, outputAddress, outputBufLen, messageAtEnd);

					BfvFileWithAddrMatch eventStart = new BfvFileWithAddrMatch(activeEmpty, match.getOriginalOffset(),
							match.getLength(), new LineElement(currentFile, lineElement.getLine(),
									lineElement.getOffset(), lineElement.getContents()),
							intraLineOffset, inputAddress, inputLen, messageAtStart);

					fullFunctionMatch = new FullFunctionMatch(eventStart, eventEnd, inputVal, retVal,
							function.getFunction().getType());
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
							if (targetMemoryAdd != null) {
								gotoAddressOfMessage(targetMemoryAdd);
							}

							activeTraceDisplayer.getProjectionViewer()
									.gotoLineAtOffset(match.getLineElement().getLine(), 0);
							activeTraceDisplayer.triggerCursorPositionChanged();

							RegistersView registersView = getRegistersView();
							if (registersView != null) {
								registersView.refresh(); // suspicious, this
															// does not occur
															// when the new
															// line's memory
															// data is available
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

		removeChannel = new Action("Remove this channel") {
			@Override
			public void run() {

				ITreeSelection selection = (ITreeSelection) resultTableViewer.getSelection();
				Object selected = selection.getFirstElement();

				if (selected instanceof Channel) {
					Channel c = (Channel) selected;
					Trace trace = c.getTrace();
					trace.removeChannel(c);
					DualTraceChannelView.this.refresh();

				} else {
					return;
				}

			}
		};
		
		gotoFunctionEndInMatch = new Action("Go To Line of Function End") {
			@Override
			public void run() {

				ITreeSelection selection = (ITreeSelection) matchResultTableViewer.getSelection();
				Object selected = selection.getFirstElement();

				AtlantisTraceEditor activeTraceDisplayer = null;

				if (selected instanceof FullFunctionMatchOfTrace) {
					FullFunctionMatchOfTrace e = (FullFunctionMatchOfTrace) selected;
					String traceName = e.getTraceName();
					if (traceEditor1.getTitle().equals(traceName)) {
						activeTraceDisplayer = (AtlantisTraceEditor) traceEditor1;
					} else if (traceEditor2.getTitle().equals(traceName)) {
						activeTraceDisplayer = (AtlantisTraceEditor) traceEditor2;
					} else {
						return;
					}
					try {
						if (activeTraceDisplayer != null) {
							BfvFileWithAddrMatch match = e.getEventEnd();
							BigInteger targetMemoryAdd = match.getTargetMemoryAddress();
							if (targetMemoryAdd != null) {
								gotoAddressOfMessage(targetMemoryAdd);
							}

							activeTraceDisplayer.getProjectionViewer()
									.gotoLineAtOffset(match.getLineElement().getLine(), 0);
							activeTraceDisplayer.triggerCursorPositionChanged();

							RegistersView registersView = getRegistersView();
							if (registersView != null) {
								registersView.refresh(); // suspicious, this
															// does not occur
															// when the new
															// line's memory
															// data is available
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

		removeChannelInMatch = new Action("Remove this matched channel") {
			@Override
			public void run() {

				ITreeSelection selection = (ITreeSelection) matchResultTableViewer.getSelection();
				Object selected = selection.getFirstElement();

				if (selected instanceof MatchChannel) {
					MatchChannel c = (MatchChannel) selected;
					MatchChannelGroup channelGroup = c.getChannelGroup();
					channelGroup.removeChannelFromGroup(c);
					DualTraceChannelView.this.refresh();

				} else {
					return;
				}

			}
		};

	}

	private RegistersView getRegistersView() {
		if (null == PlatformUI.getWorkbench().getActiveWorkbenchWindow()) {
			return null;
		}
		return (RegistersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView(RegistersView.ID);
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