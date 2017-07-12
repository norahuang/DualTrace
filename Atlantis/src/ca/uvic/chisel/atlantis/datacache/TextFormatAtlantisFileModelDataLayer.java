package ca.uvic.chisel.atlantis.datacache;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IFile;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatFileBackend;

import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;
import ca.uvic.chisel.atlantis.views.search.db.DbTextSearchResultCollector;

import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.bfv.datacache.FileLineFileBackendConsumer;
import ca.uvic.chisel.bfv.filebackend.FileLineFileBackend;


// This class follows a Singleton pattern
/**
 * The old approach was for the text trace files. This is still useful if we need to generate test
 * traces with the PinTool.
 * 
 * Known extension of this class is {@link AtlantisFileModelDataLayer}, which critically overrides
 * the things to work off of the {@link BinaryFormatFileBackend} and {@link AtlantisBinaryFormat}.
 *
 */
public class TextFormatAtlantisFileModelDataLayer extends AtlantisFileModelDataLayer {
	
	public TextFormatAtlantisFileModelDataLayer(File blankFile) throws Exception {
		super(blankFile);
	}
	
	@Override
	protected void createBackend() throws Exception {
		// New binary format doesn't need file line backend. That would require we rehydrate the binary into
		// both the database (strictly required), as well as a text file, increasing time to process
		// and disk requirements both.
		this.fileBackend = new FileLineFileBackend(originalFile);
	}
	
	/**
	 * This is an early initialization method.
	 * 
	 * This method allows you define the different line consumers that will be used when reading the file.  This method
	 * is intended to be extended by anyone extending the framework.
	 */
	@Override
	protected ArrayList<IFileContentConsumer> createFileLineConsumers() throws Exception {
		ArrayList<IFileContentConsumer> consumers = super.createFileLineConsumers();
		
		IFileContentConsumer fileLineConsumer = new FileLineFileBackendConsumer((FileLineFileBackend)fileBackend);
		consumers.add(fileLineConsumer);
		
		return consumers;
	}
	
	@Override
	public void removeIndexForFile(IFile file){
		fileBackend.abortAndDeleteIndex();
	}
	
	/**
	 * Expects {@link DbTextSearchResultCollector}
	 */
	@Override
	public void getSearchResultsForTraceLines(String regexSearch, IFile iFileCurrentDoc, boolean useRegexSearch, TextSearchRequestor searchResults, CharSequence searchInput, SearchFileModelJob monitorUpdateJob)  throws CoreException{
		this.gibraltar.fileDbBackend.getRegexMatchResults(regexSearch, iFileCurrentDoc, useRegexSearch, (DbTextSearchResultCollector)searchResults, searchInput, monitorUpdateJob);
		// TODO Bad casting above, for sure. Depends on overall program logic to ensure runtime validity.
	}
	
	//-----------------------------------------------------------------------\\
	//------------------  Overrides Differing From Binary Format ------------\\
	//-----------------------------------------------------------------------\\
	
	@Deprecated
	@Override
	public int getCharLengthBetween(long fileStartLine, long fileEndLine){
		// Could do caching of this here, but the motivation to do so was the binary file format.
		// Getting char lengths from that is an expensive operation. I let it cache its own
		// expensive data. Could also have been pushed up into the interval pager that
		// was making too much of said expensive calls.
		return super.getCharLengthBetween(fileStartLine, fileEndLine);
	}
	
	@Override
	public Map<Long, Pair<Long, Long>> getAllLineCharLengthsBetween(long fileStartLine, long fileEndLine){
		return super.getAllLineCharLengthsBetween(fileStartLine, fileEndLine);
	}
	
	@Override
	public String getFileLines(int startLine, int endLine) {
		return super.getFileLines(startLine, endLine);
	}
	
	public String getMemoryChangesForLine(int lineNumber) {
		// If I make this object oriented, probably best to compose
		// the string into memory change objects here.
		return this.getFileLines(lineNumber, lineNumber);
	}
	
	@Override
	public long getNumberOfLines() {
		return super.getNumberOfLines();
	}
	
	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean regExSearch, boolean caseSensitiveSearch, FileTextSearchScope scope){
		return super.createSearchQuery(searchText, regExSearch, caseSensitiveSearch, scope);
	}
	
	@Override
	public void cancelCurrentlyRunningSearchStatement(){
		super.cancelCurrentlyRunningSearchStatement();
	}
	
	@Override
	public String getFileDelimiter() {
		return super.getFileDelimiter();
	}

}