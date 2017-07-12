package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.TraceLine;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;
import ca.uvic.chisel.atlantis.views.search.db.DbFileSearchQuery;
import ca.uvic.chisel.atlantis.views.search.db.DbTextSearchResultCollector;
import ca.uvic.chisel.atlantis.views.search.db.DbTextSearchVisitor.ReusableMatchAccess;
import ca.uvic.chisel.bfv.datacache.FileLine;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.IFileLineBackend;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.views.BfvFileMatch;

public class TraceFileLineDbConnection extends DbConnectionManager implements IFileLineBackend <TraceLine> {

	private InsertTraceLine insertTraceLineStatement;
	private SelectLineRange findLineRangeStatement;
	private CountNumberOfLines findNumberOfLinesStatement;
	private RegexSearchLineContents regexSearchLineContents;
	private RegexSearchLineContents regularSearchLineContents;
	private FindAllInstructionInstances findAllInstructionInstances;
	private FindAllCallersOfFunction findAllCallersOfFunction;
	private GetTraceLine getTraceLine;
	
	private int lastLineNumber = -1;
	
	/**
	 * Only non-null when search is running. Used because we toggle
	 * regex and not regex.
	 */
	private RegexSearchLineContents currentlyRunningSearchStatement = null;
	
	protected static final String FILE_LINE_TABLE_NAME = "trace_file_text_lines";
	
	public TraceFileLineDbConnection(File originalFile) throws Exception{
		super(FILE_LINE_TABLE_NAME, originalFile);
	}
	
	// Want name parameters? Check: Spring JDBC, or check http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html?page=2
	@Override
	public void initializePreparedStatements() throws SQLException {		
		insertTraceLineStatement = AtlantisQueries.mk(connection, new InsertTraceLine());
		findLineRangeStatement = AtlantisQueries.mk(connection, new SelectLineRange());
		findNumberOfLinesStatement = AtlantisQueries.mk(connection, new CountNumberOfLines());
		regexSearchLineContents = AtlantisQueries.mk(connection, new RegexSearchLineContents(false));
		regularSearchLineContents = AtlantisQueries.mk(connection, new RegexSearchLineContents(true));
		findAllInstructionInstances = AtlantisQueries.mk(connection, new FindAllInstructionInstances());
		findAllCallersOfFunction = AtlantisQueries.mk(connection, new FindAllCallersOfFunction());
		getTraceLine = AtlantisQueries.mk(connection, new GetTraceLine());
	}

	@Override
	public void initialize(){
		createTables();
	}
	
	@Override
	public boolean isFreshlyInitialized(){
		// Trinary...compared to binary possible values for the file implementation.
		return TableState.CREATED == this.getTableState();
	}
	
	static public boolean checkIfFileHasIndexThatIsComplete(File file, File folder){
		try {
			return DbConnectionManager.isTraceFileDatabase(file, folder, null, true);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Prepare tables and indices as necessary.
	 * 
	 * @return	Success of initialization; true means we have fresh empty tables; false return means tables already existed with data in them.
	 */
	@Override
	public boolean createTables(){
		try{	
			TableState state = this.getTableState();
			
			// TODO Rename blockOffset to be fileCharOffset. Need a schema change control system...
			// something to assist in supporting changes. For now this is ok...if I include a script for developers.
			if(state == TableState.MISSING){
				try(
				PreparedStatement createTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createTraceFileLineTable);
				){
					refreshConnection();
					createTableStatement.executeUpdate();
					connection.commit();
					this.saveTableStateToMetaDataTable(TableState.CREATED);
					return true;
				}
			}
			
			return false;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	@Override
	protected void createIndices() throws SQLException{
		createIndex(tableName, "instructionId", "TraceLinesInsId");
	}
	
	private int insertCount = 0;

	private String currentQuery = null;
	
	@Override
	public boolean saveFileLine(long lineNumber, long lineOffset, String lineContents, TraceLine lineData) throws Exception {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
			boolean bigFileLine = false;
			if(lineContents.length() > 2000){
				bigFileLine = true;
				System.out.println("Only the first characters are printed for the next extremely long line.");
				System.out.println("["+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))+"]");
				System.out.println("Extremely long line of length "+lineContents.length()+" for line "+lineNumber+":: "+lineContents.substring(0, 120)+" et cetera...");
				System.out.println("Size is: "+(lineContents.getBytes().length/1000000)+"MiB");
				lineContents = lineContents.substring(0, 120)+"...";
			}
			InsertTraceLine q = insertTraceLineStatement;
			
			q.setParam(q.traceLineNumber, (int)lineNumber);
			if(lineData != null && lineData.getInstructionId() != null) {
				q.setParam(q.instructionId, lineData.getInstructionId());
			} else {
				q.setParam(new NullParameter(q.instructionId.parameterName), Types.VARCHAR);
			}
			q.setParam(q.blockOffset, lineOffset);
			q.setParam(q.lineContents, lineContents);
			
			q.addBatch();
			
			insertCount++;
			if((insertCount % 1000) == 0 || bigFileLine){
				this.executeInsertLineNumberBatch();
			}
			
			return true;
		} catch(Exception e) {
			throw e;
		}
	}

	
	public void executeInsertLineNumberBatch() throws BatchUpdateException {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertTraceLineStatement.executeBatch();
			connection.commit();
		} catch(BatchUpdateException e){
			printBatchFailure(insertTraceLineStatement, e);
			throw e;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public List<String> getLineRange(int startRangelineNumber, int endRangelineNumber) {
		List<String> lineResults = new ArrayList<String>();
		StringTraceGatherer gatherer = new StringTraceGatherer(startRangelineNumber, lineResults);
		
		return getTraceLines(startRangelineNumber,endRangelineNumber, gatherer);
	}

	@Override
	public List<FileLine> getFileLineRange(int startRangelineNumber, int endRangelineNumber) {
		List<FileLine> lineResults = new ArrayList<FileLine>();
		FileLineTraceGatherer gatherer = new FileLineTraceGatherer(startRangelineNumber, lineResults);
		
		return getTraceLines(startRangelineNumber, endRangelineNumber, gatherer);
	}
	
	protected <T> List<T> getTraceLines(int startRangelineNumber, int endRangelineNumber, AbstractTraceGatherer<T> gatherer){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
			findLineRangeStatement.setParam(findLineRangeStatement.traceLineLeftBound, startRangelineNumber);
			findLineRangeStatement.setParam(findLineRangeStatement.traceLineRightBound, endRangelineNumber);
			boolean success = findLineRangeStatement.execute();
			connection.commit();
			
			if(success){
				try(
				TypedResultSet results = findLineRangeStatement.getResultSet();
				){
//					// +1 here from Brent...he needed for adjustment I presume?
//					// Not used elsewhere, but this leads to deceptive values for the line number.
//					int x = startRangelineNumber + 1;
					FileLine line;
					while(results.next()){
//						line = new FileLine(x, results.getString("lineContents"));
//						line.setLineOffset(results.getLong("blockOffset"));
						gatherer.addLine(results.resultSet);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return gatherer.resultsList;
	}
	
	/**
	 * This is used to gather trace lines into the specialized container that is provided.
	 *
	 */
	private abstract class AbstractTraceGatherer<T> {
		protected int startLine;
		protected final List<T> resultsList;
		
		public AbstractTraceGatherer(int startLine, List<T> resultsList){
			this.startLine = startLine;
			this.resultsList = resultsList;
		}
		
		public void addLine(ResultSet line){
			resultsList.add(transformLine(line));
		}
		
		abstract public T transformLine(ResultSet line);
		
	}
	
	private class StringTraceGatherer extends AbstractTraceGatherer<String> {
		public StringTraceGatherer(int startLine, List<String> resultsList) {
			super(startLine, resultsList);
		}

		@Override
		public String transformLine(ResultSet line) {
			try {
				return line.getString("lineContents");
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	/**
	 * Specialized gatherer for diff system. Has a +1 offset to starting line, as
	 * expected of that system.
	 *
	 */
	private class FileLineTraceGatherer extends AbstractTraceGatherer<FileLine> {
		public FileLineTraceGatherer(int startLine, List<FileLine> resultsList) {
			super(startLine + 1, resultsList);
		}
		
		@Override
		public FileLine transformLine(ResultSet line) {
			try{
				int lineNumber = startLine + resultsList.size();
				FileLine fileLine = new FileLine(lineNumber, line.getString("lineContents"));
				// Start line needs to be +1 for external users of FileLine, but we need
				// the original line number to get the offset.
				fileLine.setLineOffset(line.getLong("blockOffset"));
				return fileLine;
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	@Override
	public int getNumberOfLines() {
		if(-1 == lastLineNumber){
			lastLineNumber = this.queryForNumberOfLines();
		}
		return lastLineNumber;
	}
	
	private int queryForNumberOfLines() {
		int numberOfLines = 0;
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = findNumberOfLinesStatement.execute();
			
			if(success){
				try(
				TypedResultSet results = findNumberOfLinesStatement.getResultSet();
				){
					results.next();
					numberOfLines = results.resultSet.getInt(1);
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return numberOfLines;
	}
	
	public void getRegexMatchResults(String searchString, IFile fileReference, boolean useRegexSearch, DbTextSearchResultCollector searchResults, CharSequence searchInput, SearchFileModelJob monitorUpdateJob)  throws CoreException{
		try {
			// We will get the results in chunks, so that the progress meter can be honest.
			// Job cancellation occurs via a listener, so that responsivity does not rely on chunk size.
			// Chunk size can be modified if we find that certain queries take too long.
			// For now, let's do 20 queries, which means we will want to see how many lines there are.
			int linesSearched = 0;
			int numLines = this.getNumberOfLines();
			long sumLineLengthScanned = 0;
			int numQueriesDesired = 100;
			int chunkSize = numLines/numQueriesDesired;
			int chunkCounter = 0;
			while(linesSearched < numLines){
				refreshConnection(); // Can close via timeout, this safely opens only if closed already.
				final RegexSearchLineContents searchStatement;
				
				int startChunkiechunkie = chunkSize * chunkCounter;
				chunkCounter++;
				
				if(useRegexSearch){
					regexSearchLineContents.setParam(regexSearchLineContents.contentsMatchTarget, searchString);
					searchStatement = regexSearchLineContents;
					// Set bookends for BETWEEN
					regexSearchLineContents.setParam(regexSearchLineContents.traceLineLeftBound, startChunkiechunkie);
					regexSearchLineContents.setParam(regexSearchLineContents.traceLineRightBound, startChunkiechunkie + chunkSize);
				} else {
					regularSearchLineContents.setParam(regularSearchLineContents.contentsMatchTarget, "%"+searchString+"%");
					searchStatement = regularSearchLineContents;
					// Set bookends for BETWEEN
					regularSearchLineContents.setParam(regularSearchLineContents.traceLineLeftBound, startChunkiechunkie);
					regularSearchLineContents.setParam(regularSearchLineContents.traceLineRightBound, startChunkiechunkie + chunkSize);
				}
				
				if(monitorUpdateJob.getState() == Job.NONE){
					return;
				}
				
				// This is where we will loop to get progress meter output, and allow for users to cancel.
				CancelSearchQueryJobChangeListener cancelListener = new CancelSearchQueryJobChangeListener(searchStatement);
				monitorUpdateJob.addJobChangeListener(cancelListener);
				
				currentlyRunningSearchStatement = searchStatement;
				
				// System.out.println("Query "+chunkCounter+" of "+numQueriesDesired);
				
				boolean success = false;
				try{
					success = searchStatement.execute();
				} catch (SQLException e) {
					// User may have hit cancel. Let this pass on by.
					success = false;
				}
				
				currentlyRunningSearchStatement = null;
				
				// This will pop us above the actual number of lines whenever
				// it is not an even interval of chunkSize, but it don't matter none.
				monitorUpdateJob.incrementLinesScanned(chunkSize);
				linesSearched += chunkSize;
				
				// No need for listener; we got past execution one way or another.
				monitorUpdateJob.removeJobChangeListener(cancelListener);
				
				// DO I need to check the statement cancel state rather than the monitor (and this monitor does nothing when cancelled, it seems)
				if(!success){ // && monitorUpdateJob.getState() == Job.NONE){
					// If it was a success, we'll go ahead and process what we have.
					return;
				} else if(success){
					ReusableMatchAccess fMatchAccess = new ReusableMatchAccess();
					try(
					TypedResultSet results = searchStatement.getResultSet();
					){
					// Gather result in a form more useful to the outside world
					while(results.next()){
						long startOfMatch = results.get(searchStatement.blockOffset); // blockOffset...except that's for the block, not the line! Try it...
						// int lengthOfMatch = results.getString(2).length();
						int lengthOfMatch = 0;
						
						// TODO If we want actual highlighting of the match in the results,
						// we need to *re-search* the line here in *Java* to get the offset of the match,
						// rather than suing the offset of the line. Get it? Is it worth it?
						
						// start and length are normally determined from the fMatcher [Matcher] start() and end() methods
						// These are likely block offsets, verifying...
						String line = results.get(searchStatement.lineContents);
						Integer lineNumber = results.get(searchStatement.traceLineNumber);
						sumLineLengthScanned += line.length();
						// Line num + 1 for 0 vs 1 indexing
						// This int cast is lame, but the recipient requires it, Java/Eclipse framework limitations.
						fMatchAccess.initialize(fileReference, (int)sumLineLengthScanned, lengthOfMatch, line, lineNumber+1);
						boolean res= searchResults.acceptPatternMatch(fMatchAccess);
						if (!res) {
							break; // no further reporting requested
						}
					}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	
	@Override
	public void cancelCurrentlyRunningSearchStatement(){
		if(null != currentlyRunningSearchStatement){
			try{
				currentlyRunningSearchStatement.cancel();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class CancelSearchQueryJobChangeListener extends JobChangeAdapter{
		private RegexSearchLineContents statement;
		public CancelSearchQueryJobChangeListener(RegexSearchLineContents statement){
			this.statement = statement;
			
		}
		
		@Override
		public void done(IJobChangeEvent arg0) {
			try {
				// TODO It seems that I only get here after the *job* finishes, and that cancellation isn't leading here.
				// Is it because the job is not responding to cancel requests properly?
				// System.out.println("Cancelling query using argument from constructor");
				// This is the place that cancellation *actually* occurs. There is another place it could...but it is confusing.
				statement.cancel();
				cancelCurrentlyRunningSearchStatement();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void performFunctionSearch(FileSearchResult searchResults, InstructionId instructionId, IFile currentFile, AtlantisTraceEditor activeEditor){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			FindAllInstructionInstances q = findAllInstructionInstances;
			
			q.setParam(q.instructionIdParam, instructionId);
			
			TypedResultSet rs = q.executeQuery();
			
			IFile activeEmpty = BfvFileUtils.convertFileIFile(activeEditor.getEmptyFile());
			
			while(rs.next()){
				String lineContents = rs.get(q.lineContents);
				// +1 for the UI, DB using 0 indexing for legacy reasons, textual trace support likely needs it.
				Integer lineNumber = rs.get(q.traceLineNumber) + 1;
				
				BfvFileMatch newMatchObj = new BfvFileMatch(activeEmpty, 0, 10,
						new LineElement(currentFile, lineNumber, 0, lineContents), 0);

				searchResults.addMatch(newMatchObj);
			}
			
			return;
			
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	
	public void performFunctionCallerSearch(FileSearchResult searchResults, InstructionId instructionId, IFile currentFile, AtlantisTraceEditor activeEditor){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			FindAllCallersOfFunction q = findAllCallersOfFunction;
			
			q.setParam(q.instructionIdParam, instructionId);
			
			TypedResultSet rs = q.executeQuery();
			
			IFile activeEmpty = BfvFileUtils.convertFileIFile(activeEditor.getEmptyFile());
			
			while(rs.next()){
				String lineContents = rs.get(q.lineContents);
				// +1 for the UI, DB using 0 indexing for legacy reasons, textual trace support likely needs it.
				Integer lineNumber = rs.get(q.traceLineNumber) + 1;
				
				BfvFileMatch newMatchObj = new BfvFileMatch(activeEmpty, 0, 10,
						new LineElement(currentFile, lineNumber, 0, lineContents), 0);

				searchResults.addMatch(newMatchObj);
			}
			
			return;
			
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertTraceLineStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finish() {
		// Nothing to do here, at the moment. If we add a meta-data table,
		// we can finish adding to it here.
	}

	@Override
	public void abortAndDeleteIndex() {
		this.abortCommitAndDropTable();
	}

	@Override
	public void close() {
		// Was going to call abort() here, but that's a super dangerous method!
		try {
			insertTraceLineStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			findLineRangeStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			findNumberOfLinesStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			regexSearchLineContents.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			regularSearchLineContents.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive, FileTextSearchScope scope) {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
		IFileModelDataLayer fileModel = (AtlantisFileModelDataLayer) RegistryUtils.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
				
		return new DbFileSearchQuery(fileModel, searchText, isRegEx, isCaseSensitive, scope);
	}

	// This method makes the assumption that all valid files end in either \r\n or in \n
	@Override
	public String getFileDelimiter() {
		
		String line = getLineRange(0, 0).get(0);
		
		if(line.endsWith("\r\n")) {
			return "\r\n";
		} else {
			return "\n";
		}
	}
	
	
	public BfvFileMatch getTraceLine(int traceLineNumber, IFile currentFile, AtlantisTraceEditor activeEditor){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			GetTraceLine q = getTraceLine;
			
			q.setParam(q.traceLineNumberParam, traceLineNumber);
			
			TypedResultSet rs = q.executeQuery();
			
			IFile activeEmpty = BfvFileUtils.convertFileIFile(activeEditor.getEmptyFile());
			
			if(rs.next()){
				String lineContents = rs.get(q.lineContents);
				// +1 for the UI, DB using 0 indexing for legacy reasons, textual trace support likely needs it.
				Integer lineNumber = rs.get(q.traceLineNumber) + 1;
				
				BfvFileMatch matchObj = new BfvFileMatch(activeEmpty, 0, 10,
						new LineElement(currentFile, lineNumber, 0, lineContents), 0);
                return matchObj;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null; 
	}
}
