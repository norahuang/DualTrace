package ca.uvic.chisel.atlantis.datacache;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatFileBackend;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.search.binaryformat.BinaryFormatTextSearchResultCollector;
import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;
import ca.uvic.chisel.bfv.datacache.BigFileReader.FileWrapper;
import ca.uvic.chisel.gibraltar.Gibraltar;


// This class follows a Singleton pattern
public class BinaryFormatFileModelDataLayer extends AtlantisFileModelDataLayer {
	
	/**
	 * Only exists for the new binary formats, not for the old text format.
	 */
	private BinaryFormatFileBackend binaryFileBackend;
	
	public BinaryFormatFileModelDataLayer(File blankFile) throws Exception {
		super(blankFile);
	}
	
	
	@Override
	public FileWrapper getWrappedFile() throws IOException {
		return new BinaryFileWrapper(binaryFileSet, this.gibraltar);
	}
	
	public static class BinaryFileWrapper implements FileWrapper {
		private AtlantisBinaryFormat binaryFileSet;
		private BinaryFormatParser byteProcessor;
		private long numLinesProcessed;
		private int numberOfLinesForMonitorIncrement;
		private long numRecords;
		
		Gibraltar gibraltar;
		
		public BinaryFileWrapper(AtlantisBinaryFormat file, Gibraltar gibraltar) throws IOException{
			binaryFileSet = file;
			this.byteProcessor = new BinaryFormatParser(binaryFileSet);
			// If we have more than max int lines, we don't increment the monitor on every line...
			this.numRecords = byteProcessor.getNumberOfTraceLines();
			this.numberOfLinesForMonitorIncrement = (int)Math.ceil(Math.max(1.0, numRecords/Integer.MAX_VALUE));
			this.numLinesProcessed = 0;
			this.gibraltar = gibraltar;
		}
		
		public boolean validate(){
			return binaryFileSet.isCompleteBinaryFileSystem();
		}
		
		public int getFileSize(){
			// Receiver needs int in my case, so I don't need to complicate with alternate methods with long.
			return (int)this.numRecords;
		}
		
		public long getFileSizeOnDisk(){
			return this.binaryFileSet.getBinaryFileSystemSize();
		}
		
		@Override
		public long getFileOutputSizeOnDisk() {
			return this.gibraltar.getDbFileSizeOnDisk();
		}
		
		@Override
		public String getFilePath() {
			return this.binaryFileSet.binaryFolder.getAbsolutePath();
		}
		
		public int getLinesWorked(){
			// Easier than counting bytes, which would require knowing total bytes in full binary trace.
			if(numLinesProcessed % numberOfLinesForMonitorIncrement == 0){
				return 1;
			} else {
				return 0;
			}
		}
		
		public long getLinesCompleted(){
			return numLinesProcessed;
		}
		
		public TraceLine getLine() throws IOException{
			TraceLine line = byteProcessor.parseNextLineToString();
			numLinesProcessed++;
			return line;
		}
		
		public void dispose() throws IOException{
			byteProcessor.close();
		}

		
	}
	
	@Override
	public String getFinalizingText(){
		return "Finalizing memory and function reconstruction...";
	}
	
	@Override
	public String getFileBuildText(){
		return "Reconstructing complete memory state, function calls...";
	}
	
	private AtlantisBinaryFormat binaryFileSet; // Don't initialize to null. Must stay default.
	
	protected void createBackend() throws Exception {
		this.binaryFileSet = gibraltar.binaryFileSet;
		this.binaryFileBackend = gibraltar.binaryFileBackend;
		this.fileBackend = null;
	}
	
	@Override
	public void removeIndexForFile(IFile file){
		binaryFileBackend.abortAndDeleteIndex();
	}

	/**
	 * Expects {@link BinaryFormatTextSearchResultCollector}
	 */
	@Override
	public void getSearchResultsForTraceLines(String regexSearch, IFile iFileCurrentDoc, boolean useRegexSearch, TextSearchRequestor searchResults, CharSequence searchInput, SearchFileModelJob monitorUpdateJob)  throws CoreException{
		this.binaryFileBackend.getRegexMatchResults(regexSearch, iFileCurrentDoc, useRegexSearch, (BinaryFormatTextSearchResultCollector)searchResults, searchInput, monitorUpdateJob);
		// TODO Bad casting above, for sure. Depends on overall program logic to ensure runtime validity.
	}
	
	//-----------------------------------------------------------------------\\
	//------------------  Overrides Due To Binary Format  -------------------\\
	//-----------------------------------------------------------------------\\
	
	@Deprecated
	@Override
	public int getCharLengthBetween(long fileStartLine, long fileEndLine){
		// Could do caching of this here, but the motivation to do so was the binary file format.
		// Getting char lengths from that is an expensive operation. I let it cache its own
		// expensive data. Could also have been pushed up into the interval pager that
		// was making too much of said expensive calls.
		return this.binaryFileBackend.getCharLengthBetween(fileStartLine, fileEndLine);
	}
	
	@Override
	public Map<Long, Pair<Long, Long>> getAllLineCharLengthsBetween(long fileStartLine, long fileEndLine){
		return this.binaryFileBackend.getAllLineCharLengthBetween(fileStartLine, fileEndLine);
	}
	
	@Override
	public String getFileLines(int startLine, int endLine) {
		List<String> lines = binaryFileBackend.getLineRange(startLine, endLine);
		
		StringBuffer buffer = new StringBuffer();
		
		for(int i=0; i<lines.size(); i++) {
			String line = lines.get(i);
			buffer.append(line);
		}
		
		return buffer.toString();
	}
	
	public String getMemoryChangesForLine(int lineNumber) {
		return binaryFileBackend.getMemoryChangesForLine(lineNumber);
	}
	
	@Override
	public long getNumberOfLines() {
		return binaryFileBackend.getNumberOfLines();
	}
	
	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean regExSearch, boolean caseSensitiveSearch, FileTextSearchScope scope){
		return binaryFileBackend.createSearchQuery(searchText, regExSearch, caseSensitiveSearch, scope);
	}
	
	@Override
	public void cancelCurrentlyRunningSearchStatement(){
		binaryFileBackend.cancelCurrentlyRunningSearchStatement();
	}
	
	
	@Override
	public String getFileDelimiter() {
		return binaryFileBackend.getFileDelimiter();
	}
	
	
	public void performFunctionSearch(FileSearchResult searchResults, InstructionId instructionId, IFile currentFile, AtlantisTraceEditor activeEditor){
		this.gibraltar.fileDbBackend.performFunctionSearch(searchResults, instructionId, currentFile, activeEditor);
	}

	public void performFunctionCallerSearch(FileSearchResult searchResults, InstructionId instructionId, IFile currentFile, AtlantisTraceEditor activeEditor){
		this.gibraltar.fileDbBackend.performFunctionCallerSearch(searchResults, instructionId, currentFile, activeEditor);
	}
	
	public Match getTraceLine(int lineNumber, IFile currentFile, AtlantisTraceEditor activeEditor){
		return this.gibraltar.fileDbBackend.getTraceLine(lineNumber, currentFile, activeEditor);
	}

}