package ca.uvic.chisel.atlantis.views.search.db;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.atlantis.views.search.db.DbTextSearchVisitor.ReusableMatchAccess;

public final class DbTextSearchResultCollector extends TextSearchRequestor {

	private final AbstractTextSearchResult fResult;
	private final boolean fIsFileSearchOnly;
	private final boolean fSearchInBinaries;
	private ArrayList fCachedMatches;

	public DbTextSearchResultCollector(AbstractTextSearchResult result, boolean isFileSearchOnly, boolean searchInBinaries) {
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
		// Seems an awful lot like the binary format one now...
		ReusableMatchAccess matchRequestor = (ReusableMatchAccess)matchReq;
		
		int matchOffset= matchRequestor.getMatchOffset();

		LineElement lineElement= new LineElement(matchRequestor.getFile(), matchRequestor.getLineNumber(), matchRequestor.getMatchOffset(), matchRequestor.getFileContent());

		FileMatch fileMatch= new FileMatch(matchRequestor.getFile(), matchOffset, matchRequestor.getMatchLength(), lineElement);
		fCachedMatches.add(fileMatch);

		return true;
	}

	private LineElement getLineElement(int offset, TextSearchMatchAccess matchRequestor) {
		int lineNumber= 1;
		int lineStart= 0;
		if (!fCachedMatches.isEmpty()) {
			// match on same line as last?
			FileMatch last= (FileMatch) fCachedMatches.get(fCachedMatches.size() - 1);
			LineElement lineElement= last.getLineElement();
			if (lineElement.contains(offset)) {
				return lineElement;
			}
			// start with the offset and line information from the last match
			lineStart= lineElement.getOffset() + lineElement.getLength();
			lineNumber= lineElement.getLine() + 1;
		}
		if (offset < lineStart) {
			return null; // offset before the last line
		}

		int i= lineStart;
		int contentLength= matchRequestor.getFileContentLength();
		while (i < contentLength) {
			char ch= matchRequestor.getFileContentChar(i++);
			if (ch == '\n' || ch == '\r') {
				if (ch == '\r' && i < contentLength && matchRequestor.getFileContentChar(i) == '\n') {
					i++;
				}
				if (offset < i) {
					String lineContent= getContents(matchRequestor, lineStart, i); // include line delimiter
					return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
				}
				lineNumber++;
				lineStart= i;
			}
		}
		if (offset < i) {
			String lineContent= getContents(matchRequestor, lineStart, i); // until end of file
			return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
		}
		return null; // offset outside of range
	}

	private static String getContents(TextSearchMatchAccess matchRequestor, int start, int end) {
		StringBuffer buf= new StringBuffer();
		for (int i= start; i < end; i++) {
			char ch= matchRequestor.getFileContentChar(i);
			if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
				buf.append(' ');
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
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