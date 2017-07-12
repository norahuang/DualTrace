package ca.uvic.chisel.atlantis.views.search.binaryformat;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.SearchResultUpdater;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;


import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatFileBackend;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

public class BinaryFormatFileSearchQuery extends FileSearchQuery {
//implements ISearchQuery {

	private final FileTextSearchScope fScope;
	private final String fSearchText;
	private final boolean fIsRegEx;
	private final boolean fIsCaseSensitive;

	private FileSearchResult fResult;
	
	private BinaryFormatFileBackend binaryFormatBackend;
	private IFileModelDataLayer fileModel;

	public BinaryFormatFileSearchQuery(BinaryFormatFileBackend binaryFormatFileBackend, IFileModelDataLayer fileModel, String searchText, boolean isRegEx, boolean isCaseSensitive, FileTextSearchScope scope) {
		super(searchText, isRegEx, isCaseSensitive, scope);
		this.binaryFormatBackend= binaryFormatFileBackend;
		this.fileModel = fileModel;
		fSearchText= searchText;
		fIsRegEx= isRegEx;
		fIsCaseSensitive= isCaseSensitive;
		fScope= scope;
	}

	@Override
	public FileTextSearchScope getSearchScope() {
		return fScope;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public IStatus run(final IProgressMonitor monitor) {
		AbstractTextSearchResult textResult= (AbstractTextSearchResult) getSearchResult();
		textResult.removeAll();

		Pattern searchPattern= getSearchPattern();
		boolean searchInBinaries= !isScopeAllFileTypes();

		BinaryFormatTextSearchResultCollector collector= new BinaryFormatTextSearchResultCollector(textResult, isFileNameSearch(), searchInBinaries);
		return TextSearchEngine.create().search(fScope, collector, searchPattern, monitor);
	}

	private boolean isScopeAllFileTypes() {
		String[] fileNamePatterns= fScope.getFileNamePatterns();
		if (fileNamePatterns == null)
			return true;
		for (int i= 0; i < fileNamePatterns.length; i++) {
			if ("*".equals(fileNamePatterns[i])) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}


	@Override
	public String getLabel() {
		return SearchMessages.FileSearchQuery_label;
	}

	@Override
	public String getSearchString() {
		return fSearchText;
	}

	@Override
	public String getResultLabel(int nMatches) {
		String searchString= getSearchString();
		if (searchString.length() > 0) {
			// text search
			if (isScopeAllFileTypes()) {
				// search all file extensions
				if (nMatches == 1) {
					Object[] args= { searchString, fScope.getDescription() };
					return Messages.format(SearchMessages.FileSearchQuery_singularLabel, args);
				}
				Object[] args= { searchString, new Integer(nMatches), fScope.getDescription() };
				return Messages.format(SearchMessages.FileSearchQuery_pluralPattern, args);
			}
			// search selected file extensions
			if (nMatches == 1) {
				Object[] args= { searchString, fScope.getDescription(), fScope.getFilterDescription() };
				return Messages.format(SearchMessages.FileSearchQuery_singularPatternWithFileExt, args);
			}
			Object[] args= { searchString, new Integer(nMatches), fScope.getDescription(), fScope.getFilterDescription() };
			return Messages.format(SearchMessages.FileSearchQuery_pluralPatternWithFileExt, args);
		}
		// file search
		if (nMatches == 1) {
			Object[] args= { fScope.getFilterDescription(), fScope.getDescription() };
			return Messages.format(SearchMessages.FileSearchQuery_singularLabel_fileNameSearch, args);
		}
		Object[] args= { fScope.getFilterDescription(), new Integer(nMatches), fScope.getDescription() };
		return Messages.format(SearchMessages.FileSearchQuery_pluralPattern_fileNameSearch, args);
	}

	/**
	 * @param result all result are added to this search result
	 * @param parentRunMonitor the progress monitor to use
	 * @param file the file to search in
	 * @param fileModel 
	 * @return returns the status of the operation
	 */
	public IStatus searchInFile(final AbstractTextSearchResult result, final IProgressMonitor parentRunMonitor, IFile file) {
		FileTextSearchScope scope= FileTextSearchScope.newSearchScope(new IResource[] { file }, new String[] { "*" }, true); //$NON-NLS-1$

		Pattern searchPattern= getSearchPattern();
		BinaryFormatTextSearchResultCollector collector= new BinaryFormatTextSearchResultCollector(result, isFileNameSearch(), true);

		TextSearchEngine searchEngine = createDefault(this.fileModel, file, this.isRegexSearch());
		IFile[] fileAsArray = {file};
		return searchEngine.search(fileAsArray, collector, searchPattern, parentRunMonitor);
	}
	
	/**
	 * Creates the default, built-in, text search engine that implements a brute-force search, not using
	 * any search index.
	 * Note that clients should always use the search engine provided by {@link #create()}.
	 * @param fileModel 
	 * @return an instance of the default text search engine {@link TextSearchEngine}.
	 */
	private TextSearchEngine createDefault(final IFileModelDataLayer fileModel, IFile file, final boolean useRegexSearch) {
		// This is ultimately what was called when constructing search stuff. Call directly.
		return new TextSearchEngine() {
			@Override
			public IStatus search(TextSearchScope scope, TextSearchRequestor requestor, Pattern searchPattern, IProgressMonitor parentRunMonitor) {
				// Don't want to call this one, because scope doesn't work so well for our situation!
				return new BinaryFormatFileTextSearchVisitor(requestor, searchPattern, useRegexSearch).search(scope, parentRunMonitor, fileModel);
			}

			@Override
			public IStatus search(IFile[] scope, TextSearchRequestor requestor, Pattern searchPattern, IProgressMonitor parentRunMonitor) {
				 return new BinaryFormatFileTextSearchVisitor(requestor, searchPattern, useRegexSearch).search(scope, parentRunMonitor, fileModel);
			}
		};
	}

	@Override
	protected Pattern getSearchPattern() {
		return PatternConstructor.createPattern(fSearchText, fIsCaseSensitive, fIsRegEx);
	}

	@Override
	public boolean isFileNameSearch() {
		return fSearchText.length() == 0;
	}

	@Override
	public boolean isRegexSearch() {
		return fIsRegEx;
	}

	@Override
	public boolean isCaseSensitive() {
		return fIsCaseSensitive;
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (fResult == null) {
			fResult= new FileSearchResult(this);
			new SearchResultUpdater(fResult);
		}
		return fResult;
	}
}