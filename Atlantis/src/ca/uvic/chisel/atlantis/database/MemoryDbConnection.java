package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import ca.uvic.chisel.atlantis.datacache.AsyncResult;
import ca.uvic.chisel.atlantis.datacache.IdGenerator;
import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.deltatree.DeltaConverter;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree.MemoryDeltaTreeNode;
import ca.uvic.chisel.atlantis.models.MemoryReference;

public class MemoryDbConnection extends DbConnectionManager {
	/*
	 * Probably need to enlarge packet size on server:
	 * http://dev.mysql.com/doc/refman/5.0/en/packet-too-large.html
	 * But to what size? Here's the command...
	 * mysqld --max_allowed_packet=1G
	 * 
	 * Is MEDIUMBLOB going to be enough? That's 2^24 or 16MB
	 * LARGEBLOB gives you storage for blobs up to 4GB.
	 * That's overkill, given that MySQL limits packet size to 1GB.
	 * 
 	 * Also, is there any benefit to separating the blob into a second table, and doing joins?
	 */

	private InsertMemoryReference insertMemRefStatement;
	private SelectMemRefByLine findLineMemRefStatement;
	private SelectMaxLine findMaxLineStatement;
	
	public static final String MEMORY_TABLE_NAME = "memory_snapshot_delta_tree";
	
	private int maxLine = -1;
	
	private int branchOutFactor;
	private IdGenerator idGenerator;
	
	public MemoryDbConnection(File originalFile, int branchOutFactor) throws Exception{
		super(MEMORY_TABLE_NAME, originalFile);
		this.branchOutFactor = branchOutFactor;
		this.idGenerator = new IdGenerator(branchOutFactor, MemoryDeltaTree.TREE_HEIGHT);
	}
	
	// Want name parameters? Check: Spring JDBC, or check http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html?page=2
	@Override
	public void initializePreparedStatements() throws SQLException {		
		insertMemRefStatement = AtlantisQueries.mk(connection, new InsertMemoryReference());
		findLineMemRefStatement = AtlantisQueries.mk(connection, new SelectMemRefByLine());
		findMaxLineStatement = AtlantisQueries.mk(connection, new SelectMaxLine());
	}
	
	/**
	 * Prepare tables and indices as necessary.
	 * 
	 * @return	Success of initialization; true means we have fresh empty tables; false return means tables already existed with data in them.
	 */
	@Override
	public boolean createTables(){
		// 4GB limit on LONGBLOB. Will we hit that soon?
		
		try{	
			TableState state = getTableState();
			
			if(state == TableState.MISSING){
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createMemoryTable);
				){
					refreshConnection();
					installTableStatement.executeUpdate();
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
	public void createIndices() throws SQLException{
		createIndex(tableName, "endLine", "EndLineIndex");
		createIndex(tableName, "parentId", "ParentIndex");
	}

	private int insertCount = 0;
	/**
	 * Line most recently used to generate a DB query
	 */
	private int mostRecentLineQueried = -1;
	/**
	 * Line most recently used in a request for memory data update
	 * Can differ from that of the {@link MemoryDbConnection#mostRecentLineQueried}
	 * because a query can be finished and a new request made that takes precedence,
	 * but uses the same query data.
	 */
	private int mostRecentRequestedLineNumberForCancellation = -1;
	private int mostRecentLineQueriedStartSpan = -1;
	private int mostRecentLineQueriedEndSpan = -1;
	/**
	 * Used to control the width of the query window, the number of leaf nodes fetched back for
	 * processing later. Smaller means less DB and memory usage, bigger gives larger leaps for user prior to
	 * requiring a DB query.
	 */
	private final int cachedLineMemoryRefSpread = 400; // Could be better to go to something in the ballpark of 3-4 screens
	
	MemoryQueryResults mostRecentMemoryQueryResults = null;
	
	public boolean saveMemRefNode(MemoryDeltaTreeNode node) {
		MemoryDeltaTreeNode parentNode = node.getParentNode();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			InsertMemoryReference q = insertMemRefStatement;
			
			q.setParam(q.id, node.getId());

			if(parentNode != null) {
				q.setParam(q.parentId, parentNode.getId());
			} else {
				q.setParam(new NullParameter(q.parentId.parameterName), Types.INTEGER); // q.setNull(q.parentId, Types.INTEGER);
			}
			
			q.setParam(q.startLine, node.getStart());
			q.setParam(q.endLine, node.getEnd());
			
			// TODO Check data size here to predict if we will fail to insert with truncation.
			String deltaData = DeltaConverter.getDBString(node.getDelta());
			q.setParam(q.deltaData, deltaData);
			
			q.addBatch();
			node.setCommitted();
			
			insertCount++;
			if((insertCount % 1000) == 0 || deltaData.length() > 1000){
				// Arbitrary check to get large strings committed to DB sooner, so they can get cleared from heap
				// This is still desirable for use with the JIT system, that doesn't fetch the values from the
				// binary format until it is about to be committed; the large strings still should be shunted out
				// of here and to the DB as soon as possible.
				// Doing it for when they are 1000 big is questionable hard-coding, but it's ok.
//				if(deltaData.length() > 1000){
//					System.out.println("Over 1000 in the saveMemRefNode().");
//				}
				this.executeInsertMemRefBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void executeInsertMemRefBatch(){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertMemRefStatement.executeBatch();
			connection.commit();
		} catch(BatchUpdateException e){
			printBatchFailure(insertMemRefStatement, e);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public MemoryQueryResults getMostRecentMemoryResults(){
		return this.mostRecentMemoryQueryResults;
	}

	/**
	 * Will provide memory results, either from the database or cached/paged.
	 */
	public AsyncResult<MemoryQueryResults> getMemoryReferencesAsync(int lineNumber, IProgressMonitor monitor) {
		// TODO There is an excess of asyncs in this line, that need to be cleaned up some time (but not now).
		refreshConnection(); // Can close via timeout, this safely opens only if closed already.
		
		// This allows us to cancel queries and processing of results separately.
		mostRecentRequestedLineNumberForCancellation = lineNumber;
		
		if(maxLine == -1) {
			maxLine = getMaxMemRefLine();
		}
		
		// Might cancel existing queries, might let those finish and use their cached data
		// The querying calls are synchronized, and we have to cancel from before entering, then.
		boolean canceledQuery = cancelQueryForMemoryReferencesAsync(lineNumber);
		
		try {
			return getMemRefsAsync(lineNumber, monitor);
		} catch (SQLException e) {
			// something went wrong, just return an empty result
			// MySQL and SQLite versions of the error.
			if(e.getMessage().startsWith("Query execution was interrupted") || e.getMessage().startsWith("ResultSet closed")){
				// This is also due to us cancelling it
				System.out.println("A query cancelled: "+lineNumber);
			} else {
				e.printStackTrace();
			}
			mostRecentRequestedLineNumberForCancellation = -1;
			return new AsyncResult<MemoryQueryResults>(new MemoryQueryResults(-1,-1,-1), Status.OK_STATUS);
		} 
	}

	/**
	 * Will trigger a DB query if needed, otherwise will allow caller to use existing cached/paged data.
	 * 
	 * @param lineNumber
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	protected synchronized AsyncResult<MemoryQueryResults> getMemRefsAsync(int lineNumber, IProgressMonitor monitor) throws SQLException {
		String queryOrCache;
		long start = System.currentTimeMillis();
		
		if(!(mostRecentLineQueriedStartSpan <= lineNumber && mostRecentLineQueriedEndSpan >= lineNumber)){
			boolean success = this.queryForMemoryReferences(lineNumber, monitor);
			queryOrCache = "queried from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
			if(monitor.isCanceled()) {
				mostRecentLineQueriedStartSpan = -1;
				mostRecentLineQueriedEndSpan = -1;
				mostRecentLineQueried = -1;
				return AsyncResult.cancelled();
			}
		} else {
			// Leave for a while, likely want to remove, but it will be good to watch this behavior and query time
			// even after this is committed.
			// System.out.println("Request in span, no requery:"+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for "+lineNumber);
			queryOrCache = "cache from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
		}
		
		// This resList may have previous queries that suffice for the current line (results span it),
		// or it might contain newly fetched data.
		
		long end = System.currentTimeMillis();
		System.out.println("Memory retrieval took "+(end - start)/1000.0+" seconds ("+queryOrCache+")");
		
		return new AsyncResult<MemoryQueryResults>(this.mostRecentMemoryQueryResults, Status.OK_STATUS);
	}
	
	private synchronized boolean queryForMemoryReferences(int lineNumber, IProgressMonitor monitor) throws SQLException {
		mostRecentLineQueried = lineNumber;
		
		// Originally, queries were only up to and including the line
		// requested, but instead we will ask for lines forward and back from the clicked line.
		// Grabbing 400 forward and back.
		// We grab the memory data up to the earlier line (400 back), then grab all leaf
		// nodes from that point until the later line (400 forward). We only re-fetch when
		// the cached line data does not satisfy a request for memory data.
		// This means that we occasionally bypass a useful parent node, but it simplifies
		// querying dramatically, and reduces the number of database calls. In terms of
		// storage, it is not much worse than worst case (when the line just before a branching
		// factor is requested, requiring 999 leaf nodes).
		int startSpan = Math.max(0, lineNumber - cachedLineMemoryRefSpread);
		int endSpan = Math.min(maxLine, lineNumber + cachedLineMemoryRefSpread); // Could take Math.min() with max line number, but maybe don't need to
		
		mostRecentLineQueriedStartSpan = startSpan;
		mostRecentLineQueriedEndSpan = endSpan;
		
		// We need to leaf nodes from the beginning of the series until the end, and we need the parent nodes for up to the
		// beginning of the set. So, we can use the original mechanism for computing required nodes, then fetch the remaining
		// leaf nodes directly (their ids are equal to their startLine/endLine).
		List<Integer> positionDigits = getPositionDigits(lineNumber);
		
		int lastLevelPos = 0;
		
		// Re-initialize for getting fresh results.
		this.mostRecentMemoryQueryResults = new MemoryQueryResults(startSpan, lineNumber, endSpan);
		
		for(int level = positionDigits.size() - 1; level >= 0; level--) {
			
			int levelDigit = positionDigits.get(level);
			
			int startPos = lastLevelPos * branchOutFactor;
			int startId = idGenerator.getId(level, startPos);
			int endId = startId + (levelDigit);

			// This is because endId is not inclusive, the line number passed in is not included in the results.
			if(startId == endId) {
				continue;
			}
			// System.out.println("startId: "+startId+" and endId: "+endId);
			getMemoryReferenceForIds(startId, endId, monitor, this.mostRecentMemoryQueryResults);
			lastLevelPos = startPos + levelDigit;
			
			if(monitor.isCanceled()) {
				mostRecentLineQueriedStartSpan = -1;
				mostRecentLineQueriedEndSpan = -1;
				mostRecentLineQueried = -1;
				return false;
			}
		}
		
		// We can grab the leaf nodes leading to the end span directly, and after the prior set. It
		// keeps all leaf nodes towards the end of the resList.
		// NB: startSpan to endSpan+1 because the lower loop will only go up-to-before startSpan,
		// and only up to the second argument, thus +1 to cover that.
		getMemoryReferenceForIds(startSpan, endSpan+1, monitor, this.mostRecentMemoryQueryResults);
		return true;
	}

	/**
	 * This method will get all memory references from nodes between startId and endId, where startId is inclusive and endId is exclusive.
	 * If the progress monitor (eg the query) was cancelled before we execute the query, this method will return an empty list.
	 */
	private void getMemoryReferenceForIds(int startId, int endId, IProgressMonitor monitor, MemoryQueryResults memRefObj) throws SQLException {
		boolean nullReceived = true;
		int tries = 0;
		if(monitor.isCanceled()) {
			return;
		}
		
		// min, max and count are for debugging only, usage commented out below
		int min = Integer.MAX_VALUE;
		int max = -1;
		int count = 0;
		
		while(nullReceived && tries < 2){
			tries++;
			
			refreshConnection();
			
			findLineMemRefStatement.setParam(findLineMemRefStatement.idLeftBound, startId);
			findLineMemRefStatement.setParam(findLineMemRefStatement.idRightBound, endId);
			// long start = System.currentTimeMillis();
			boolean success = findLineMemRefStatement.execute();
			// long end = System.currentTimeMillis();
			// System.out.println("Memory pure query execute "+(end - start)/1000.0+" seconds");
			// System.out.println();
			if(success){
				try(
				TypedResultSet results = findLineMemRefStatement.getResultSet();
				){
					while(results.next()){
						
						if(monitor.isCanceled()) {
							return;
						}
						
						String string = results.get(findLineMemRefStatement.deltaData);
						if(null == string){
							// System.out.println("Null result on #"+tries);
							nullReceived = true;
							break;
						} else {
							// if(tries > 1) System.out.println("No null result on #"+tries); // verify behavior
							nullReceived = false; // prevents a second try
						}
						
						DeltaConverter.convertDeltaQueryResults(string, memRefObj);
						
						// For debugging, verifying leaf node span
						// if(results.get(findLineMemRefStatementRevised.startLine) == results.get(findLineMemRefStatementRevised.endLine)){
							// min = Math.min(min, results.get(findLineMemRefStatementRevised.startLine));
							// max = Math.max(max, results.get(findLineMemRefStatementRevised.startLine));
						// }
						// count++;
						
					}
				} catch(SQLException e){
					// Likely canceled
					e.printStackTrace();
					System.out.println("Catching ok?");
					monitor.setCanceled(true);
					return;
				}
			}
		}
		// System.out.println("Fetched min and max: "+min+"-"+max);
		// System.out.println("Count: "+count);
		return;
	}
	
	protected List<Integer> getPositionDigits(int lineNumber) {
		List<Integer> positionDigits = new ArrayList<>();
		
		if(lineNumber == 0) {
			positionDigits.add(0);
			return positionDigits;
		}
		
		
		int maxLevel = (int) (Math.log(lineNumber) / Math.log(branchOutFactor));
		
		int curVal = lineNumber;
		
		for(int i=0; i <= maxLevel; i++) {
			int levelDigit = curVal % branchOutFactor;
			positionDigits.add(levelDigit);
			curVal /= branchOutFactor;
		}
		return positionDigits;
	}
	
	/**
	 * Allows caller to cancel a query for the given line number, if that query
	 * is the most recent and is still able to be cancelled.
	 * 
	 * @param lineNumber
	 * @return
	 */
	public boolean cancelQueryForMemoryReferencesAsync(int lineNumber) {
		// If it's same line or within span, do not cancel previous, but do not execute current.
		// Only re-query if we do not have a span of data covering the desired line
		// Dramatically reduces number of queries when the user is clicking around
		if(lineNumber == mostRecentLineQueried
				|| (mostRecentLineQueriedStartSpan < lineNumber && lineNumber < mostRecentLineQueriedEndSpan)){
			return false;
		} else {
			forceCancelForMemoryQuery();
			return true;
		}
	}
	
	private void forceCancelForMemoryQuery(){
		// Both may be fresh (no previous job),
		// but we can still try and fail to cancel.
		try{
			findLineMemRefStatement.cancel();
		} catch(SQLException e){
			 // Perhaps there isn't a valid one to cancel.
		} catch(NullPointerException e){
			// Gets set to null after long waits?
		}
	}

	/**
	 * will return the highest numbered line that has a memory or register reference on it, or -1 if an error occurred;
	 */
	private int getMaxMemRefLine() {
		try {
			boolean success = this.findMaxLineStatement.execute();
			
			if(success) {
				try(
				TypedResultSet results = findMaxLineStatement.getResultSet();
				){
					results.next();
					int maxMemRefLine = results.resultSet.getInt(1);
					results.close();
					return maxMemRefLine;
				}
			}
			
			return -1;
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertMemRefStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
