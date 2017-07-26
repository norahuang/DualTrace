package ca.uvic.chisel.atlantis.views;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.datacache.AsyncResult;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.BinaryFormatFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.ModelProvider;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.dualtrace.BfvFileChannelCreateMatch;
import ca.uvic.chisel.bfv.dualtrace.BfvFileMessageMatch;
import ca.uvic.chisel.bfv.dualtrace.DualBfvFileMessageMatch;
import ca.uvic.chisel.bfv.dualtrace.DuplicateMessageOccurrenceException;
import ca.uvic.chisel.bfv.dualtrace.MessageFunction;
import ca.uvic.chisel.bfv.dualtrace.MessageOccurrenceSR;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

public class MessageTypesView extends ViewPart implements IPartListener2, MenuListener {

	public static final String ID = "ca.uvic.chisel.atlantis.views.MessageTypesView";
	private BigFileEditor activeEditor;
	private CheckboxTreeViewer treeViewer;
	private Menu menu;
	private MenuItem deleteItem;
	private MenuItem editItem;
	private MenuItem searchOccurrenceItem;
	private IEditorReference[] editors;
	private BigFileEditor sendEditor;
	private BigFileEditor recvEditor;
	private FileSearchResult searchResults = new FileSearchResult((FileSearchQuery) null);
	// Variables for navigating through tag occurrences
	private MessageType selectedType = null;
	private int currentSelectedTypeOccurrence = 0;

	private Table resultTable;
	private TableViewer resultTableViewer;
	private Action gotoSender;
	private Action gotoReceiver;
	// private BfvFileMessageMatch newMatchObj = null;
	private Map<String, String> sendChannelMap = new HashMap<String, String>();
	private Map<String, String> recvChannelMap = new HashMap<String, String>();
	private BfvFileChannelCreateMatch newChannelMatchObj = null;
	private BfvFileMessageMatch newMessageMatchObj = null;

	public MessageTypesView() {
		this.searchResults = new FileSearchResult((FileSearchQuery) null);
	}

	private class MessageTypesContentProvider implements ITreeContentProvider {
		private Collection<MessageType> messageTypes;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof Collection<?>) {
				messageTypes = new ArrayList<MessageType>(); // clear out the
																// old data
				Collection<?> input = (Collection<?>) newInput;
				for (Object o : input) { // add in the new data
					messageTypes.add((MessageType) o);
				}
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MessageType) {
				MessageType type = (MessageType) parentElement;
				List<Object> functions = new ArrayList<Object>();
				if (type.getReceive() != null) {
					functions.add(type.getReceive());
				}
				if (type.getSend() != null) {
					functions.add(type.getSend());
				}
				if (type.getSendChannelCreate() != null) {
					functions.add(type.getSendChannelCreate());
				}
				if (type.getReceiveChannelCreate() != null) {
					functions.add(type.getReceiveChannelCreate());
				}
				return functions.toArray();

			} else {
				return null;
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return messageTypes.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof MessageType) {
				MessageType type = (MessageType) element;
				return type.getReceive() != null || type.getSend() != null || type.getSendChannelCreate() != null
						|| type.getReceiveChannelCreate() != null;
			} else {
				return false;
			}
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof MessageFunction) {
				MessageFunction func = (MessageFunction) element;
				return func.getType();
			} else {
				return null;
			}
		}

	}

	/**
	 * Update this view with the current comments data from the File Editor
	 */
	public void updateView() {

		if (activeEditor == null) {
			treeViewer.setInput(null);
			return;
		}

		Collection<MessageType> types = activeEditor.getProjectionViewer().getFileModel().getMessageTypes(true);
		treeViewer.setInput(types);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) {
		// TODO Auto-generated method stub
		System.out.println("partBroughtToTop:");

	}

	@Override
	public void partClosed(IWorkbenchPartReference arg0) {
		IEditorReference[] opendeditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getEditorReferences();
		if (opendeditors.length == 0) {
			treeViewer.setInput(null);
			resultTableViewer.setInput(new ArrayList<MemoryReference>());
			refresh();
			return;
		}
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
		treeViewer.refresh();
	}

	public void clearContents() {

	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {

		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor instanceof BigFileEditor) {
			BigFileEditor newEditor = (BigFileEditor) editor;

			if (newEditor == activeEditor) {
				return;
			}

			activeEditor = newEditor;
			treeViewer.setInput(activeEditor.getProjectionViewer().getFileModel().getMessageTypes(true));
			refresh();

		}
	}

	@Override
	public void setFocus() {
		treeViewer.getTree().setFocus();

	}

	@Override
	public void createPartControl(Composite parent) {

		createTypeControl(parent);
		createSearchResultControl(parent);
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
		createContextMenu();
	}

	private void createTypeControl(Composite parent) {
		treeViewer = new CheckboxTreeViewer(parent, SWT.V_SCROLL);
		treeViewer.setLabelProvider(new LabelProvider());
		treeViewer.setContentProvider(new MessageTypesContentProvider());
		final Tree tree = treeViewer.getTree();

		// Add a listener to handle items in the tree being checked or unchecked
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				try {
					if (event.getElement() instanceof MessageType) {
						MessageType type = (MessageType) event.getElement();
						/*
						 * activeEditor.getProjectionViewer().
						 * showOrHideStickyTooltip(type, event.getChecked(),
						 * true); // Also apply the same checked value to every
						 * occurrence of the tag for (TagOccurrence occurrence :
						 * type.getOccurrences()) {
						 * treeViewer.setChecked(occurrence,
						 * event.getChecked()); }
						 */
					} else if (event.getElement() instanceof TagOccurrence) {
						TagOccurrence occurrence = (TagOccurrence) event.getElement();
						activeEditor.getProjectionViewer().showOrHideStickyTooltip(occurrence, event.getChecked());
						Tag tag = occurrence.getTag();
						treeViewer.setChecked(tag, tag.getShowStickyTooltip()); // make
																				// sure
																				// the
																				// underlying
																				// tag's
																				// check
																				// is
																				// kept
																				// up-to-date
					}
				} catch (JAXBException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding tag tooltips",
							"Could not update file's tags file", e);
				} catch (CoreException e) {
					BigFileApplication.showErrorDialog("Error showing/hiding tag tooltips",
							"Problem refreshing file's tags file", e);
				}
			}
		});

		SelectionListener messageTypeSelectedListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// updateSelectedMessageType();
			}
		};
		Listener goToLineListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				// gotoCurrentSelectedMessageOccurrence();
			}
		};
		KeyListener enterKeyGoToMessageListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					// gotoCurrentSelectedMessageOccurrence();
				}

			}
		};
		tree.addSelectionListener(messageTypeSelectedListener);
		tree.addListener(SWT.MouseDoubleClick, goToLineListener);
		tree.addKeyListener(enterKeyGoToMessageListener);
		// Tried adding SWT.CR and SWT.LF listeners, but those were leading to
		// single-click
		// events firing onto them (likely due to single-click support in
		// Eclipse).
		// If we can prevent that, we can add them back in...

		menu = new Menu(tree);
		tree.setMenu(menu);
		menu.addMenuListener(this);

		editItem = new MenuItem(menu, SWT.CASCADE);
		editItem.setText("Rename This Communication Type");
		editItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (selection.getFirstElement() instanceof MessageType) {
					MessageType selected = (MessageType) selection.getFirstElement();
					RenameMessageTypeDialog renameDialog = new RenameMessageTypeDialog(parent.getShell(),
							selected.getName());
					renameDialog.create();
					if (renameDialog.open() == Window.OK) {
						try {
							IFileModelDataLayer fileModel = activeEditor.getProjectionViewer().getFileModel();
							String newTypeName = renameDialog.getName();
							fileModel.renameMessageType(selected, newTypeName);
							updateView();
						} catch (JAXBException | CoreException | DuplicateMessageOccurrenceException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});

		// Menu item for deleting tags or tag occurrences
		deleteItem = new MenuItem(menu, SWT.CASCADE);
		deleteItem.setText("Remove This Communication Type");
		deleteItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (selected instanceof MessageType) {
					MessageType type = (MessageType) selected;
					boolean delete = MessageDialog.openQuestion(shell, "Delete message type",
							"This will delete all occurrences of Communication type '" + type.getName()
									+ "'. Are you sure you want to do this?");
					if (delete) {
						try {
							activeEditor.getProjectionViewer().deleteMessageType(type);
						} catch (JAXBException e) {
							BigFileApplication.showErrorDialog("Error deleting message type",
									"Could not update file's Communication types file", e);
						} catch (CoreException e) {
							BigFileApplication.showErrorDialog("Error deleting message type",
									"Problem refreshing file's Communication types file", e);
						}
						updateView();
						selectedType = null;
						currentSelectedTypeOccurrence = -1;
					}
				}
			}
		});

		searchOccurrenceItem = new MenuItem(menu, SWT.CASCADE);
		searchOccurrenceItem.setText("Search Communication Occurrences of This Communication Type");
		searchOccurrenceItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				Object selected = selection.getFirstElement();
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (selected instanceof MessageType) {
					MessageType type = (MessageType) selected;
					searchMatchSendRecv(type);
				}
			}
		});
	}

	private void createSearchResultControl(Composite parent) {
		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		Composite tableComposite = new Composite(parent, 0);
		GridData tableCompositeData = new GridData();
		tableCompositeData.horizontalSpan = 3;
		tableCompositeData.horizontalAlignment = SWT.FILL;
		tableCompositeData.grabExcessHorizontalSpace = true;
		tableCompositeData.verticalAlignment = SWT.FILL;
		tableCompositeData.grabExcessVerticalSpace = true;
		tableComposite.setLayoutData(tableCompositeData);

		resultTable = new Table(tableComposite,
				SWT.NO_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
		resultTable.setItemCount(0);
		resultTableViewer = new TableViewer(resultTable);

		createColumns(tableComposite, resultTableViewer);
		resultTable.setHeaderVisible(true);

		resultTableViewer.setContentProvider(new ArrayContentProvider());

		resultTableViewer.setInput(new ArrayList<MemoryReference>());
	};

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
				DualBfvFileMessageMatch mac = (DualBfvFileMessageMatch) element;
				return mac.toString();
			}
		});

		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(col.getColumn(), new ColumnWeightData(100));
		parent.setLayout(tableLayout);
	}

	@Override
	public void menuHidden(MenuEvent arg0) {
		// TODO Auto-generated method stub

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
		if (resultTable.getSelectionIndex() == -1) {
			return;
		}

		mgr.add(gotoSender);
		mgr.add(gotoReceiver);
	}

	@Override
	public void menuShown(MenuEvent arg0) {
		// TODO Auto-generated method stub

	}

	private void searchMatchSendRecv(MessageType type) {
		IFileUtils fileUtil = RegistryUtils.getFileUtils();
		File f = fileUtil.convertBlankFileToActualFile(activeEditor.getCurrentBlankFile());
		sendEditor = null;
		recvEditor = null;

		editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (IEditorReference e : editors) {
			try {
				if (e.getEditorInput().getName().contains(type.getSend().getAssociatedFileName())) {
					sendEditor = (BigFileEditor) e.getEditor(true);
				}
				if (e.getEditorInput().getName().contains(type.getReceive().getAssociatedFileName())) {
					recvEditor = (BigFileEditor) e.getEditor(true);
				}

			} catch (PartInitException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

		if (sendEditor == null) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			String fullpathsend = f.getParent() + "\\" + type.getSend().getAssociatedFileName() + "\\"
					+ "address_space.itable";
			File fSend = new File(fullpathsend);

			fSend = fileUtil.convertFileToBlankFile(fSend);
			IFile convertedSendFile = BfvFileUtils.convertFileIFile(fSend);
			if (!convertedSendFile.exists()) {
				fileUtil.createEmptyFile(convertedSendFile);
			}

			// NB file is converted above
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry()
					.getDefaultEditor(convertedSendFile.getName());
			try {
				sendEditor = (BigFileEditor) page.openEditor(new FileEditorInput(convertedSendFile), desc.getId());
			} catch (PartInitException e1) {
				System.out.println("Can not open receiver file");
			}
		}

		if (recvEditor == null) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			String fullpathRecv = f.getParent() + "\\" + type.getReceive().getAssociatedFileName() + "\\"
					+ "address_space.itable";
			File fRecv = new File(fullpathRecv);

			fRecv = fileUtil.convertFileToBlankFile(fRecv);
			IFile convertedRecvFile = BfvFileUtils.convertFileIFile(fRecv);

			if (!convertedRecvFile.exists()) {
				fileUtil.createEmptyFile(convertedRecvFile);
			}

			// NB file is converted above
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry()
					.getDefaultEditor(convertedRecvFile.getName());
			try {
				recvEditor = (BigFileEditor) page.openEditor(new FileEditorInput(convertedRecvFile), desc.getId());
			} catch (PartInitException e1) {
				System.out.println("Can not open receiver file");
			}
		}

		String fullpathRecv = f.getParent() + "\\" + type.getReceive().getAssociatedFileName();
		File fSend = new File(fullpathRecv);
		String fullpathsend = f.getParent() + "\\" + type.getSend().getAssociatedFileName();
		File fRecv = new File(fullpathsend);

		IFile currentFileSend = BfvFileUtils.convertFileIFile(fSend);
		List<BfvFileChannelCreateMatch> sendChannelList = searchChannels(
				new Instruction(type.getSendChannelCreate().getFirst()).getIdGlobalUnique(), currentFileSend,
				type.getSendChannelCreate().getChannelNameAddress(), type.getSendChannelCreate().getChannelIdReg(),
				type.getSendChannelCreate(), sendEditor);

		List<BfvFileMessageMatch> sendList = searchSendFunctions(
				new Instruction(type.getSend().getFirst()).getIdGlobalUnique(), currentFileSend,
				type.getSend().getMessageLengthAddress(), type.getSend().getMessageAddress(),
				type.getSend().getChannelIdReg(), sendChannelList);

		List<BfvFileChannelCreateMatch> recvChannelList = searchChannels(
				new Instruction(type.getReceiveChannelCreate().getFirst()).getIdGlobalUnique(), currentFileSend,
				type.getReceiveChannelCreate().getChannelNameAddress(),
				type.getReceiveChannelCreate().getChannelIdReg(), type.getReceiveChannelCreate(), recvEditor);

		IFile currentFileRecv = BfvFileUtils.convertFileIFile(fRecv);
		List<BfvFileMessageMatch> recvList = searchRecvFunctions(
				new Instruction(type.getReceive().getFirst()).getIdGlobalUnique(), currentFileRecv,
				type.getReceive().getMessageLengthAddress(), type.getReceive().getMessageAddress(),
				type.getReceive().getChannelIdReg(), type.getReceive(), recvChannelList);

		List<Match> matches = new ArrayList<Match>();
		for (BfvFileMessageMatch send : sendList) {
			for (BfvFileMessageMatch recv : recvList) {
				if (recv.getMessage().startsWith(send.getMessage())
						&& recv.getChannelName().equals(send.getChannelName())) {
					DualBfvFileMessageMatch newMatch = new DualBfvFileMessageMatch(send, recv);
					matches.add(newMatch);
				}
			}
		}

		resultTableViewer.setInput(matches);

	}

	private List<BfvFileMessageMatch> searchSendFunctions(final InstructionId targetInstructionId, IFile currentFile,
			String messageLenghtReg, String messageAddReg, String channelIdReg,
			List<BfvFileChannelCreateMatch> sendChannelList) {
		List<MessageOccurrenceSR> result = new ArrayList<MessageOccurrenceSR>();
		System.out.println("Searching database function table backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting database function table search at time " + dateFormat.format(date));
		ISearchQuery dummyQuery;
		RegexSearchInput dummyInput = new RegexSearchInput(targetInstructionId.toString(), false, false, false,
				currentFile);
		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils
				.getFileModelDataLayerFromRegistry(sendEditor.getCurrentBlankFile());
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

		((BinaryFormatFileModelDataLayer) fileModel).performFunctionSearch(this.searchResults, targetInstructionId,
				currentFile, (AtlantisTraceEditor) sendEditor);
		IFile sendEmpty = BfvFileUtils.convertFileIFile(sendEditor.getEmptyFile());
		Match[] originalMatches = this.searchResults.getMatches(sendEmpty);
		ArrayList<BfvFileMessageMatch> matches = new ArrayList<BfvFileMessageMatch>();
		for (int i = 0; i < originalMatches.length; i++) {
			FileMatch match = (FileMatch) originalMatches[i];
			LineElement lineElement = match.getLineElement();

			int intraLineOffset = match.getOffset() - lineElement.getOffset();

			Job memoryUpdateJob = new Job("Memory Update Job") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {

					// These will cancel themselves and return null if cancelled
					final AsyncResult<MemoryQueryResults> newMemoryEventsAsync = fileModel
							.getMemoryEventsAsync(lineElement.getLine() + 1, monitor);

					if (newMemoryEventsAsync.isCancelled()) {
						return Status.CANCEL_STATUS;
					}

					MemoryQueryResults newMemoryReferencesResult = newMemoryEventsAsync.getResult();
					ModelProvider.INSTANCE.setMemoryQueryResults(newMemoryReferencesResult, lineElement.getLine());
					newMemoryReferencesResult.collateMemoryAndRegisterResults(lineElement.getLine());
					SortedMap<String, MemoryReference> map = newMemoryReferencesResult.getRegisterList();
					short messagelenght = (short) Long.parseLong(
							(map.get(messageLenghtReg.toUpperCase()).getMemoryContent().getMemoryValue()), 16);
					Long add = Long.parseLong(map.get(messageAddReg.toUpperCase()).getMemoryContent().getMemoryValue(),
							16);
					BigInteger messageAddress = BigInteger.valueOf(add);

					String channelId = String.valueOf((short) Long
							.parseLong(map.get(channelIdReg.toUpperCase()).getMemoryContent().getMemoryValue(), 16));
					String channelName = "";
					int channelCreateLine = 0;
					for (BfvFileChannelCreateMatch channel : sendChannelList) {
						if (channelId.equals(channel.getId())
								&& channel.getLineElement().getLine() < lineElement.getLine()
								&& channelCreateLine < channel.getLineElement().getLine()) {
							channelCreateLine = channel.getLineElement().getLine();
							channelName = channel.getChannelName();
						}
					}

					String message = "";
					newMessageMatchObj = null;
					for (MemoryReference memRef : newMemoryReferencesResult.getMemoryList().values()) {
						if (add <= memRef.getAddress() && add + messagelenght > memRef.getEndAddress()) {
							String hex = memRef.getMemoryContent().getMemoryValue();
							for (int i = 0; i < hex.length(); i += 2) {
								String str = hex.substring(i, i + 2);
								message += (char) Integer.parseInt(str, 16);
							}
						}
					}
					if (!message.equals("") && !channelName.equals("")) {
						newMessageMatchObj = new BfvFileMessageMatch(sendEmpty, match.getOriginalOffset(),
								match.getLength(),
								new LineElement(currentFile, lineElement.getLine(), lineElement.getOffset(),
										lineElement.getContents()),
								intraLineOffset, messageAddress, message, channelName);
					}

					System.out.println("messageeeeee:" + message);
					System.out.println(messagelenght);
					System.out.println(
							"messageadd:" + map.get(messageAddReg.toUpperCase()).getMemoryContent().getMemoryValue());
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
			if (newMessageMatchObj != null) {
				matches.add(newMessageMatchObj);
			}
			// Get old FileMatch object out, put new BfvFileMatch in its place.

		}
		return matches;
	}

	private List<BfvFileMessageMatch> searchRecvFunctions(final InstructionId targetInstructionId, IFile currentFile,
			String messageLenghtReg, String messageAddReg, String channelIdReg, MessageFunction function,
			List<BfvFileChannelCreateMatch> recvChannelList) {
		List<MessageOccurrenceSR> result = new ArrayList<MessageOccurrenceSR>();
		System.out.println("Searching database function table backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting database function table search at time " + dateFormat.format(date));
		ISearchQuery dummyQuery;
		RegexSearchInput dummyInput = new RegexSearchInput(targetInstructionId.toString(), false, false, false,
				currentFile);
		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils
				.getFileModelDataLayerFromRegistry(recvEditor.getCurrentBlankFile());
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

		int moduleId = function.getFirst().getModuleId();

		((BinaryFormatFileModelDataLayer) fileModel).performFunctionSearch(this.searchResults, targetInstructionId,
				currentFile, (AtlantisTraceEditor) recvEditor);
		IFile recvEmpty = BfvFileUtils.convertFileIFile(recvEditor.getEmptyFile());
		Match[] originalMatches = this.searchResults.getMatches(recvEmpty);
		ArrayList<BfvFileMessageMatch> matches = new ArrayList<BfvFileMessageMatch>();
		for (int i = 0; i < originalMatches.length; i++) {
			FileMatch match = (FileMatch) originalMatches[i];
			LineElement lineElement = match.getLineElement();

			Job memoryUpdateJob = new Job("Memory Update Job") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
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
					short messagelenght = (short) Long.parseLong(
							(map.get(messageLenghtReg.toUpperCase()).getMemoryContent().getMemoryValue()), 16);
					String channelId = String.valueOf((short) Long
							.parseLong(map.get(channelIdReg.toUpperCase()).getMemoryContent().getMemoryValue(), 16));
					Long add = Long.parseLong(map.get(messageAddReg.toUpperCase()).getMemoryContent().getMemoryValue(),
							16);
					BigInteger messageAddress = BigInteger.valueOf(add);

					String channelName = "";
					int channelCreateLine = 0;
					for (BfvFileChannelCreateMatch channel : recvChannelList) {
						if (channelId.equals(channel.getId())
								&& channel.getLineElement().getLine() < lineElement.getLine()
								&& channelCreateLine < channel.getLineElement().getLine()) {
							channelCreateLine = channel.getLineElement().getLine();
							channelName = channel.getChannelName();
						}
					}

					int retLineNumber = (int) fileModel.getFunctionRetLine(moduleId, lineElement.getLine() - 1);
					System.out.println("ret line:" + retLineNumber);

					FileMatch retMatch = (FileMatch) ((BinaryFormatFileModelDataLayer) fileModel)
							.getTraceLine(retLineNumber, currentFile, (AtlantisTraceEditor) recvEditor);
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

					newMessageMatchObj = null;
					String message = "";
					for (MemoryReference memRef : functionEndMemoryReferencesResult.getMemoryList().values()) {
						if (add <= memRef.getAddress() && add + messagelenght > memRef.getEndAddress()) {
							String hex = memRef.getMemoryContent().getMemoryValue();
							for (int i = 0; i < hex.length(); i += 2) {
								String str = hex.substring(i, i + 2);
								message += (char) Integer.parseInt(str, 16);
							}
						}
					}
					if (!message.equals("")) {
						newMessageMatchObj = new BfvFileMessageMatch(recvEmpty, retMatch.getOriginalOffset(),
								retMatch.getLength(),
								new LineElement(currentFile, retLineNumber, retLineElement.getOffset(),
										retLineElement.getContents()),
								intraLineOffset, messageAddress, message, channelName);
					}
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
			if (newMessageMatchObj != null) {
				matches.add(newMessageMatchObj);
			}
			// Get old FileMatch object out, put new BfvFileMatch in its place.

		}

		// if (matches != null && matches.size() > 0) {
		// resultTableViewer.setInput(matches);
		// }
		return matches;
	}

	private List<BfvFileChannelCreateMatch> searchChannels(final InstructionId targetInstructionId, IFile currentFile,
			String channelNameAddReg, String channelIdReg, MessageFunction function, BigFileEditor editor) {
		List<MessageOccurrenceSR> result = new ArrayList<MessageOccurrenceSR>();
		System.out.println("Searching database function table backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting database function table search at time " + dateFormat.format(date));
		ISearchQuery dummyQuery;
		RegexSearchInput dummyInput = new RegexSearchInput(targetInstructionId.toString(), false, false, false,
				currentFile);
		AtlantisFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils
				.getFileModelDataLayerFromRegistry(editor.getCurrentBlankFile());
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

		int moduleId = function.getFirst().getModuleId();

		((BinaryFormatFileModelDataLayer) fileModel).performFunctionSearch(this.searchResults, targetInstructionId,
				currentFile, (AtlantisTraceEditor) editor);
		IFile empty = BfvFileUtils.convertFileIFile(editor.getEmptyFile());
		Match[] originalMatches = this.searchResults.getMatches(empty);
		List<BfvFileChannelCreateMatch> matches = new ArrayList<BfvFileChannelCreateMatch>();
		for (int i = 0; i < originalMatches.length; i++) {
			FileMatch match = (FileMatch) originalMatches[i];
			LineElement lineElement = match.getLineElement();

			Job memoryUpdateJob = new Job("Memory Update Job") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
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

					Long add = Long.parseLong(
							map.get(channelNameAddReg.toUpperCase()).getMemoryContent().getMemoryValue(), 16);
					BigInteger channelNameAddress = BigInteger.valueOf(add);

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

					String channelName = "";
					String channelId = "";
					newChannelMatchObj = null;
					for (MemoryReference memRef : functionEndMemoryReferencesResult.getMemoryList().values()) {
						if (add <= memRef.getAddress() && add + 256 > memRef.getEndAddress()) {
							String hex = memRef.getMemoryContent().getMemoryValue();
							for (int i = 0; i < hex.length(); i += 2) {
								String str = hex.substring(i, i + 2);
								channelName += (char) Integer.parseInt(str, 16);
							}
						}
					}

					SortedMap<String, MemoryReference> endmap = functionEndMemoryReferencesResult.getRegisterList();

					channelId = String.valueOf((short) Long
							.parseLong(endmap.get(channelIdReg.toUpperCase()).getMemoryContent().getMemoryValue(), 16));

					channelName = channelName.split("\n")[0];
					channelName = channelName.trim();
					if (!channelId.equals("")) {
						newChannelMatchObj = new BfvFileChannelCreateMatch(empty, retMatch.getOriginalOffset(),
								retMatch.getLength(),
								new LineElement(currentFile, retLineNumber, retLineElement.getOffset(),
										retLineElement.getContents()),
								intraLineOffset, channelNameAddress, channelId, channelName);
					}
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
			
			if (newChannelMatchObj != null) {
				matches.add(newChannelMatchObj);
			}
			// Get old FileMatch object out, put new BfvFileMatch in its place.

		}

		// if (matches != null && matches.size() > 0) {
		// resultTableViewer.setInput(matches);
		// }
		return matches;
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
		gotoSender = new Action("Go To Line of Message Sender") {
			@Override
			public void run() {
				if (resultTable.getSelectionIndex() == -1) {
					return;
				}

				try {
					AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) sendEditor;
					if (activeTraceDisplayer != null) {
						List<DualBfvFileMessageMatch> refs = (List<DualBfvFileMessageMatch>) resultTableViewer
								.getInput();
						BfvFileMessageMatch match = refs.get(resultTable.getSelectionIndex()).getSendMatch();
						gotoAddressOfMessage(match.getTargetMemoryAddress());
						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(match.getLineElement().getLine(),
								0);
						activeTraceDisplayer.triggerCursorPositionChanged();
					}

				} catch (Exception ex) {
					System.err.println("Error jumping to line");
					ex.printStackTrace();
				}
			}
		};

		gotoReceiver = new Action("Go To Line of Message Receiver") {
			@Override
			public void run() {
				if (resultTable.getSelectionIndex() == -1) {
					return;
				}

				try {
					AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) recvEditor;
					if (activeTraceDisplayer != null) {

						List<DualBfvFileMessageMatch> refs = (List<DualBfvFileMessageMatch>) resultTableViewer
								.getInput();
						BfvFileMessageMatch match = refs.get(resultTable.getSelectionIndex()).getRecvMatch();
						gotoAddressOfMessage(match.getTargetMemoryAddress());
						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset(match.getLineElement().getLine(),
								0);
						activeTraceDisplayer.triggerCursorPositionChanged();

					}
				} catch (Exception ex) {
					System.err.println("Error jumping to line");
					ex.printStackTrace();
				}
			}
		};
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

}