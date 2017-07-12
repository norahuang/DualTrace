package ca.uvic.chisel.bfv.views;


import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.FileSearchPage;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressConstants;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.bfv.utils.IObservable;

public class CombinedFileSearchView extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.bfv.views.CombinedFileSearchView";

	private static final String DEFAULT_SEARCH_TEXT = "Enter search text here...";

	protected FileSearchQuery currentQuery;

	protected ISearchQuery dummyQuery;
	private boolean caseSensitive;
	private boolean useRegexForSearch;
	private Text searchField;
	public BigFileEditor activeEditor;
	protected IFileModelDataLayer fileModel;
	private Path filePath;
	protected IFile currentFile;
	private org.eclipse.search2.internal.ui.SearchView view;
	private StructuredViewer viewer;
	protected FileSearchResult searchResults;
	private IObservable observable;
	private Match[] matches;
	private boolean showSearchResults;

	private Button searchButton;

	/**
	 * Constructs a new Search View.
	 */
	public CombinedFileSearchView() {
		this.currentQuery = null;
		this.caseSensitive = false;
		this.searchResults = new FileSearchResult((FileSearchQuery) this.currentQuery);
		this.observable = new IObservable();
	}

	@Override
	public void createPartControl(Composite parent) {

		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;
		parent.setLayout(layout);

		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Search the current file for: ");

		// Search field
		searchField = new Text(parent, SWT.BORDER | SWT.SINGLE);
		searchField.setText(DEFAULT_SEARCH_TEXT);
		searchField.setEditable(true);

		// If focus goes to this, blank it out. If it is empty when focus is lost, fill it in.
		searchField.addListener(SWT.FocusIn, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String searchText = searchField.getText();
				if (searchText.equals(DEFAULT_SEARCH_TEXT)) {
					searchField.setText("");
				}
			}
		});

		searchField.addListener(SWT.FocusOut, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String searchText = searchField.getText();
				if (searchText.equals("")) {
					searchField.setText(DEFAULT_SEARCH_TEXT);
				}
			}
		});

		searchField.addListener(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String searchText = searchField.getText();

				// Execute the search if the user presses enter
				if ((event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) && searchText != null && !searchText.isEmpty()) {
					search(searchText, caseSensitive, useRegexForSearch);
				}

				if (searchText == null || searchText.isEmpty()) {
					searchButton.setEnabled(false);
				} else {
					searchButton.setEnabled(true);
				}
			}
		});
		GridData searchFieldData = new GridData();
		searchFieldData.grabExcessHorizontalSpace = true;
		searchFieldData.horizontalAlignment = SWT.FILL;
		searchField.setLayoutData(searchFieldData);

		Composite checkboxHolder = new Composite(parent, SWT.NONE);
		GridLayout checkboxLayout = new GridLayout(2, true);
		checkboxHolder.setLayout(checkboxLayout);
		GridData checkboxHolderData = new GridData();
		checkboxHolderData.grabExcessHorizontalSpace = true;
		checkboxHolderData.horizontalAlignment = SWT.FILL;
		checkboxHolder.setLayoutData(checkboxHolderData);

		// Case sensitivity checkbox
		final Button caseSensitiveCheck = new Button(checkboxHolder, SWT.CHECK);
		caseSensitiveCheck.setText("Case sensitive");
		caseSensitiveCheck.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				caseSensitive = caseSensitiveCheck.getSelection();
			}
		});

		// Regex checkbox
		final Button useRegexCheck = new Button(checkboxHolder, SWT.CHECK);
		useRegexCheck.setText("Regex (longer search)");
		useRegexCheck.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				useRegexForSearch = useRegexCheck.getSelection();
			}
		});

		Composite buttonHolder = new Composite(parent, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(2, true);
		buttonHolder.setLayout(buttonLayout);
		GridData buttonHolderData = new GridData();
		buttonHolderData.grabExcessHorizontalSpace = true;
		buttonHolderData.horizontalAlignment = SWT.FILL;
		buttonHolder.setLayoutData(buttonHolderData);

		// Button for clearing search results
		Button clearButton = new Button(buttonHolder, SWT.PUSH);
		clearButton.setText("Clear");
		clearButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		clearButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				searchField.setText("");
				clearResults();
			}
		});

		searchButton = new Button(buttonHolder, SWT.PUSH);
		searchButton.setText("Search Assembly");
		searchButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				search(searchField.getText(), caseSensitive, useRegexForSearch);
			}
		});
	}

	@SuppressWarnings("restriction")
	protected void updateMatches(Match[] originalMatches, final String searchText) {
		matches = new Match[originalMatches.length];
		try {
			// We need to fill the dummyQuery with our actual search results.
			// The dummyQuery interacts with the UI, and never gets to perform a search.
			// Its results must have the data to link to editor lines, updated as the editor changes,
			// based on a search of the original file or DB, depending on the backend.
			FileSearchResult fileSearchResult = ((FileSearchResult) dummyQuery.getSearchResult());

			for (int i = 0; i < originalMatches.length; i++) {
				FileMatch match = (FileMatch) originalMatches[i];
				LineElement lineElement = match.getLineElement();

				int intraLineOffset = match.getOffset() - lineElement.getOffset();
				IFile activeEmpty = BfvFileUtils.convertFileIFile(activeEditor.getEmptyFile());
				BfvFileMatch newMatchObj = new BfvFileMatch(activeEmpty, match.getOriginalOffset(), match.getLength(), new LineElement(currentFile, lineElement.getLine(), lineElement.getOffset(), lineElement.getContents()), intraLineOffset);

				// Get old FileMatch object out, put new BfvFileMatch in its place.
				matches[i] = newMatchObj;
			}
			// Replace (empty) dummy results with useful matches. Vital!
			fileSearchResult.removeAll();
			fileSearchResult.addMatches(matches);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				showSearchResults = true;
				if (matches.length > 10000)
					showSearchResults = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), 
						"This will take a while.", "There are " + matches.length + " results for " + searchText + 
						", this could take a while to load. Are you sure you want to continue?");
			}
		});
		if (showSearchResults) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					view = (org.eclipse.search2.internal.ui.SearchView) NewSearchUI.activateSearchResultView();
					FileSearchResult fileSearchResult = (FileSearchResult) dummyQuery.getSearchResult();
					view.showSearchResult(fileSearchResult);
					viewer = ((FileSearchPage) view.getActivePage()).getViewer();
					view.updateLabel();
					viewer.addDoubleClickListener(searchDoubleClickListener);
					activeEditor.setFocus();
				}
			});
		} else {
			return;
		}
	}

	@SuppressWarnings("restriction")
	private IDoubleClickListener searchDoubleClickListener = new IDoubleClickListener() {
		@Override
		public void doubleClick(DoubleClickEvent event) {
			try {
				LineElement element = (LineElement) ((TreeSelection) event.getSelection()).getFirstElement();
				int lineNumber = element.getLine();

				// Make sure to move to the correct line before the loop below to ensure that the line offset
				// in the is correct for what the document will be after paging.
				activeEditor.getProjectionViewer().gotoLineAtOffset(lineNumber - 1, 0);

				// Dummy Query associated with view, therefore it needs its results updated to be valid for current editor contents
				Match[] matches = ((FileSearchResult) dummyQuery.getSearchResult()).getMatches(currentFile);
				for (Match match : matches) {
					if(((BfvFileMatch)match).getLineElement().equals(element)) {
						int offset = activeEditor.getDocument().getLineOffset(lineNumber - 1) + ((BfvFileMatch)match).getIntraLineOffset();
						match.setOffset(offset);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * @param b
	 * Searches for the specified text in the current file.  If the string is null or empty the search will be cancelled.
	 * @param searchText text to search for
	 * @param caseSensitive whether this search should be case sensitive
	 * @throws
	 */
	public void search(final String searchText, boolean caseSensitive, final boolean useRegexForSearch) {	
		System.out.println("Searching file backend");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Starting file search at time "+dateFormat.format(date));

		// Make sure that we do not search for nothing.
		if (searchText == null || searchText.isEmpty()) {
			return;
		}

		RegexSearchInput realInput = new RegexSearchInput(searchText, caseSensitive, useRegexForSearch, true);
		RegexSearchInput dummyInput = new RegexSearchInput(searchText, caseSensitive, useRegexForSearch, false);
		if (realInput.getScope() != null) { // Only perform the search if there is a file open for searching
			try {
				if (dummyInput.getScope() != null) {
					dummyQuery = TextSearchQueryProvider.getPreferred().createQuery(dummyInput);
				}
				dummyQuery.run(null);
				searchResults.removeAll();

				// Old text format back end will provide one query mechanism, binary format will provide another.
				// The db line backend does too, but it is not clear if that is deprecated now.
				currentQuery = fileModel.createSearchQuery(realInput.getSearchText(), realInput.isRegExSearch(), realInput.isCaseSensitiveSearch(), realInput.getScope());

				Job searchJob = new Job("Search Assembly") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						setProperty(IProgressConstants.PROPERTY_IN_DIALOG, Boolean.FALSE);
						setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);

						currentQuery.searchInFile(searchResults, monitor, currentFile);	

						return Status.OK_STATUS;
					}
				};
				searchJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						// XXX This should probably not be used here; fileModel use shouldn't be presumed
						// even though it is passed all the way down to DbTextSearchVisitor#locateMatches()
						// Instead, shall we make methods on FileDbSearchQuery to parallel the searchInFile() call hierarchy?
						// NB The cancel call should not affect us when we are done due to finishing, but allows us to cancel
						// when we are done due to the user clicking the Cancel button
						// NB It is fairly important to cancel the MySQL queries underlying this.
						// It seems like this listener should be passed into the deep places,
						// rather than being attached to a job at a level where no worked() call can conceivably be made on it,
						// but I copied Eclipse, and got it to function...
						fileModel.cancelCurrentlyRunningSearchStatement();
						updateMatches(searchResults.getMatches(currentFile), searchText);
						
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						System.out.println("Finishing file search at time "+dateFormat.format(date));
					}
				});
				searchJob.setUser(true);
				searchJob.schedule();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Clears any existing search results.
	 */
	private void clearResults() {
		if (currentQuery != null) {
			NewSearchUI.removeQuery(currentQuery);
		}
	}

	@Override
	public void setFocus() {
		searchField.setFocus();
	}

	/**
	 * Search input class for supporting the ability to specify whether the search should be case sensitive.
	 * The Search View does case insensitive searches by default.
	 * @author Laura Chan
	 */
	protected class RegexSearchInput extends TextSearchInput {

		private String searchText;
		private boolean caseSensitive;
		private boolean useRegexSearch;
		private boolean useRealScope;

		/**
		 * Creates a new search input instance for performing a search query with the given search text.
		 * @param searchText text to search for
		 * @param caseSensitive whether or not this search should be case sensitive
		 * @param useRealScope	Whether to use real file or blank editor placeholder file. 
		 */
		public RegexSearchInput(String searchText, boolean caseSensitive, boolean useRegexSearch, boolean useRealScope) {
			this.searchText = searchText;
			this.caseSensitive = caseSensitive;
			this.useRegexSearch = useRegexSearch;
			this.useRealScope = useRealScope;
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
			String[] fileNamePatterns = null; // have to do it this way to avoid an ambiguous method call
			if(!useRealScope){
				return FileTextSearchScope.newSearchScope(new IResource[] {}, fileNamePatterns, true);
			} else {
				try {
					if (activeEditor == null) {
						activeEditor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					}
	
					// Only search the currently active file
					IFileUtils fileUtil = RegistryUtils.getFileUtils();
					File f = fileUtil.convertBlankFileToActualFile(activeEditor.getCurrentBlankFile());
					currentFile = BfvFileUtils.convertFileIFile(f);
					
					return FileTextSearchScope.newSearchScope(new IResource[] {currentFile}, fileNamePatterns, true);
				} catch (NullPointerException e) {
					// No files are currently open, so there's no file to search
					return null;
				}
			}
		}
	}

	public IObservable getObservable() {
		return observable;
	}

	public Match[] getMatches() {
		return matches;
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (part instanceof IEditorPart) {
			if (part instanceof BigFileEditor) {
				if (part != activeEditor) { // don't need to redraw the visualization if the active File Editor hasn't changed
					activeEditor = (BigFileEditor) part;
					try {
						fileModel = RegistryUtils.getFileModelDataLayerFromRegistry();
					} catch (Exception e) {
						e.printStackTrace();
					}
					resetSearchResults();
				}
			} else {
				activeEditor = null;
				resetSearchResults();
			}
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (activeEditor != null && activeEditor == part) {
			activeEditor = null;
			fileModel = null;
			resetSearchResults();
		}
	}

	private void resetSearchResults() {

		if (currentQuery != null) {
			NewSearchUI.removeQuery(currentQuery);
		}

		if (dummyQuery != null) {
			NewSearchUI.removeQuery(dummyQuery);
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

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
}
