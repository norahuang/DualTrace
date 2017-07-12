package ca.uvic.chisel.atlantis.bytecodeparsing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ModuleRec;
import ca.uvic.chisel.atlantis.database.RemoveTablesDbConnection;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.search.binaryformat.BinaryFormatFileSearchQuery;
import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;
import ca.uvic.chisel.atlantis.views.search.binaryformat.BinaryFormatTextSearchResultCollector;
import ca.uvic.chisel.atlantis.views.search.binaryformat.BinaryFormatFileTextSearchVisitor.ReusableMatchAccess;
import ca.uvic.chisel.bfv.datacache.AbstractLine;
import ca.uvic.chisel.bfv.datacache.FileLine;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.IFileLineBackend;
import ca.uvic.chisel.bfv.editor.RegistryUtils;

/**
 * 
 * TODO
 * 
 * This backend uses random access on the binary format files, to allow access to the
 * disasembly (line contents)
 * 
 * It uses a faster access method that bypasses most of the parsing done when first opening
 * a binary format file system. This is in contrast with the defunct line DB system, that stored
 * lines explicitly in the DB, and the text format system, which uses random access reads from disk.
 * In this case, we still need to jump to the locations where disassembly is stored, and possibly
 * parse other structures for vital information.
 * 
 * We might have to store the rehydrated lines explicitly somewhere for full text search.
 * 
 * {@link BinaryFormatFileBackend} is an immutable backend. It will not respond to attempts
 * to save lines (which surely came from its backing files). Use it in read situations, not 
 * during initial parsing.
 *
 */
public class BinaryFormatFileBackend implements IFileLineBackend<AbstractLine> {

	private static final char NEW_LINE_ENDING = '\n';
	
	private final AtlantisBinaryFormat binaryFileSet;
	
	private final BinaryFormatParser binaryParser;

	public BinaryFormatFileBackend(AtlantisBinaryFormat binaryFileSet) {
		this.binaryFileSet = binaryFileSet;
		if(!binaryFileSet.isCompleteBinaryFileSystem()){
			System.err.println("Incomplete binary file set for "+binaryFileSet.providedFile);
		}
		binaryParser = new BinaryFormatParser(this.binaryFileSet);
	}

	/**
	 * {@link BinaryFormatFileBackend} is an immutable backend. It will not respond to attempts
	 * to save lines (which surely came from its backing files).
	 * 
	 * Use it in read situations, not during initial parsing.
	 */
	@Override
	public boolean saveFileLine(long lineNumber, long blockOffset, String lineContents, AbstractLine lineData) throws Exception {
		return false;
	}
	
	@Override
	public List<FileLine> getFileLineRange(int startRangelineNumber, int endRangelineNumber) {
		// TODO Used for trace comparison only. Package lines into the FileLine objects.
		return null;
	}

	@Override
	public String getFileDelimiter() {
		// Usign just LF to reduce the number of chars per blank line.
		return "" + NEW_LINE_ENDING;
	}

	@Override
	public List<String> getLineRange(int startRangelineNumber, int endRangelineNumber) {
		List<String> lines = new ArrayList<String>();
		for(long lineNumber = startRangelineNumber; lineNumber <= endRangelineNumber; lineNumber++){
			lines.add(this.binaryParser.fetchLineForDisplay(lineNumber).getStringRepresentation());
		}
		return lines;
	}
	
	public boolean isExecutionMode64Bit(int currentLine){
		return this.binaryParser.isExecutionMode64Bit(currentLine);
	}
	
	public String getMemoryChangesForLine(int lineNumber) {
		return this.binaryParser.getMemoryChangesForLine(lineNumber);
	}
	
	public ArrayList<ModuleRec> getAllModules(){
		return this.binaryParser.getAllModules();
	}
	
	@Deprecated
	public List<Integer> getLineRangeLineSizes(long fileStartLine, long fileEndLine) {
		List<Integer> pairs = new ArrayList<Integer>();
		for(long lineNumber = fileStartLine; lineNumber <= fileEndLine; lineNumber++){
			Integer size = this.binaryParser.getLengthOfLineDissassembly(lineNumber);
			pairs.add(size);
		}
		return pairs;
	}
	
	public Map<Long, Pair<Long, Long>> getAllLineCharLengthBetween(long fileStartLine, long fileEndLine){
		// There was an issue with how often this could be called, but it was solved by caching in the
		// caller (the page in class). Other callers might produce new problems.
		Map<Long, Pair<Long, Long>> results = new HashMap<>();
		long sumFromStart = 0;
		for(long lineNumber = fileStartLine; lineNumber <= fileEndLine; lineNumber++){
			Integer size = this.binaryParser.getLengthOfLineDissassembly(lineNumber);
			sumFromStart += size;
			results.put(lineNumber, Pair.of((long)size, sumFromStart));
		}
		return results;
	}
	
	@Deprecated
	public int getCharLengthBetween(long fileStartLine, long fileEndLine){		
		List<Integer> sizes = this.getLineRangeLineSizes(fileStartLine, fileEndLine);
		int totalSize = 0;
		for(Integer size: sizes){
			totalSize += size;
		}
		return totalSize;
	}

	@Override
	public int getNumberOfLines() {
		return (int)this.binaryParser.maxLineNumber;
	}

	@Override
	public void initialize() {
		// Constructor sets up binary parser. Should it be here instead?
		// Otherwise, nothing; no DB work needed, which is largely initialization
		// of other IFileLineBackend classes.
	}

	@Override
	public boolean isFreshlyInitialized() {
		// If we know the number of lines, it is initialized.
		return binaryParser.maxLineNumber > 0;
	}

	@Override
	public void finish() {
		// Nada
	}

	@Override
	public void abortAndDeleteIndex() {
		File baseFile = this.binaryFileSet.binaryFolder;
		File blankFile = RegistryUtils.getFileUtils().convertFileToBlankFile(baseFile);
		IFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(blankFile);
		fileModel.dispose();
		
		try{
			RemoveTablesDbConnection clearTraceFromDatabase = new RemoveTablesDbConnection(baseFile);
			clearTraceFromDatabase.removeTables();
			clearTraceFromDatabase.closeConnection();
			
			RegistryUtils.clearFileModelDataLayerFromRegistry(fileModel);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		// I considered closing the file set, but I don't know if the caller
		// wants that untouched. Also...it doesn't appear to have any open
		// handles, just IFile references.
		this.binaryParser.close();
		
	}
	
	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean isRegEx,
			boolean isCaseSensitive, FileTextSearchScope scope) {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
		IFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
		
		return new BinaryFormatFileSearchQuery(this, fileModel, searchText, isRegEx, isCaseSensitive, scope);
	}

	public void getRegexMatchResults(String searchString, IFile fileReference, boolean useRegexSearch, BinaryFormatTextSearchResultCollector searchResults, CharSequence searchInput, SearchFileModelJob monitorUpdateJob)  throws CoreException{
		int linesSearched = 0;
		int sumLineLengthScanned = 0;
		int numLines = this.getNumberOfLines();
		
		Pattern searchPattern;
		if(useRegexSearch){
			searchPattern = Pattern.compile(searchString);
		} else {
			searchPattern = Pattern.compile(".*"+searchString+".*");
		}
		 

		while(linesSearched < numLines){
			
			if(monitorUpdateJob.getState() == Job.NONE){
				return;
			}
			
			String line = this.binaryParser.fetchLineForDisplay(linesSearched).getStringRepresentation();
			
			Matcher searchMatcher = searchPattern.matcher(line);
			boolean success = searchMatcher.find(); // use searchMatcher.group(1) to grab match
			
			// This will pop us above the actual number of lines whenever
			// it is not an even interval of chunkSize, but it don't matter none.
			monitorUpdateJob.incrementLinesScanned(1);
			
			linesSearched++;
			sumLineLengthScanned += line.length();
			
			if(!success){
				// If it was a success, we'll go ahead and process what we have.
				continue; // return;
			} else if(success){
				ReusableMatchAccess fMatchAccess = new ReusableMatchAccess();
				String results = searchMatcher.group(0);
				// Gather result in a form more useful to the outside world
				int startOfMatch = searchMatcher.start();
				int lengthOfMatch = searchMatcher.end() - startOfMatch;

				// Do not subtract from the linesSearched; it works as is (pre-incremented).
				fMatchAccess.initialize(fileReference, sumLineLengthScanned, lengthOfMatch, line, linesSearched);
				boolean res= searchResults.acceptPatternMatch(fMatchAccess);
				if (!res) {
					break; // no further reporting requested
				}
			}
		}
	}
	
	@Override
	public void cancelCurrentlyRunningSearchStatement(){
		// Nothing in particular needed for this implementation
	}
}
