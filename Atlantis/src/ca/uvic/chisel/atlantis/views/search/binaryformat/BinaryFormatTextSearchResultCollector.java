package ca.uvic.chisel.atlantis.views.search.binaryformat;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.atlantis.views.search.binaryformat.BinaryFormatFileTextSearchVisitor.ReusableMatchAccess;

public final class BinaryFormatTextSearchResultCollector extends TextSearchRequestor {

	private final AbstractTextSearchResult fResult;
	private final boolean fIsFileSearchOnly;
	private final boolean fSearchInBinaries;
	private ArrayList fCachedMatches;

	public BinaryFormatTextSearchResultCollector(AbstractTextSearchResult result, boolean isFileSearchOnly, boolean searchInBinaries) {
		fResult= result;
		fIsFileSearchOnly= isFileSearchOnly;
		fSearchInBinaries= searchInBinaries;

	}

	@Override
	public boolean acceptFile(IFile file) throws CoreException {
		if (fIsFileSearchOnly) {
			fResult.addMatch(new FileMatch(file));
		}
		flushMatches();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.core.text.TextSearchRequestor#reportBinaryFile(org.eclipse.core.resources.IFile)
	 */
	@Override
	public boolean reportBinaryFile(IFile file) {
		return fSearchInBinaries;
	}

	@Override
	public boolean acceptPatternMatch(TextSearchMatchAccess matchReq) throws CoreException {

		ReusableMatchAccess matchRequestor = (ReusableMatchAccess)matchReq;
		
		int matchOffset= matchRequestor.getMatchOffset();

		LineElement lineElement= new LineElement(matchRequestor.getFile(), matchRequestor.getLineNumber(), matchRequestor.getMatchOffset(), matchRequestor.getFileContent());

		FileMatch fileMatch= new FileMatch(matchRequestor.getFile(), matchOffset, matchRequestor.getMatchLength(), lineElement);
		fCachedMatches.add(fileMatch);

		return true;
	}

	@Override
	public void beginReporting() {
		fCachedMatches= new ArrayList();
	}

	@Override
	public void endReporting() {
		flushMatches();
		fCachedMatches= null;
	}

	private void flushMatches() {
		if (!fCachedMatches.isEmpty()) {
			fResult.addMatches((Match[]) fCachedMatches.toArray(new Match[fCachedMatches.size()]));
			fCachedMatches.clear();
		}
	}
}