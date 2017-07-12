package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ca.uvic.chisel.atlantis.models.ThreadEventType;
import ca.uvic.chisel.atlantis.models.TraceThreadEvent;

@Deprecated
public class ThreadEventDbConnection extends DbConnectionManager {
	
	private PreparedStatement insertThreadEventStatement;
	private PreparedStatement findThreadEventRangeByLineStatement;
	private PreparedStatement findThreadMarkerContainingLineStatement;
	private PreparedStatement findNumberOfThreadEventsStatement;
	private PreparedStatement findThreadMarkerByPixelStatement;
	private PreparedStatement findDistinctThreadNamesStatement;
	private PreparedStatement findFinalThreadEventEndPointStatement;
	
	protected static final String THREAD_EVENT_TABLE_NAME = "thread_change_events";
	
	public ThreadEventDbConnection(File originalFile) throws Exception{
		super(THREAD_EVENT_TABLE_NAME, originalFile);
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {	
		insertThreadEventStatement = AtlantisQueries.mk(connection, AtlantisQueries.insertThread);
		findNumberOfThreadEventsStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectNumberOfThreadEvents);
		findThreadEventRangeByLineStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectThreadEventRangeByLine);
		findThreadMarkerContainingLineStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectThreadMarkerContainingLine);
		findThreadMarkerByPixelStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectThreadMarkerByPixel);
		findDistinctThreadNamesStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectDistinctThreadNames);
		findFinalThreadEventEndPointStatement = AtlantisQueries.mk(connection, AtlantisQueries.selectFinalThreadEndPoint);
	}
	
	/**
	 * XXX Note tid's are null in the case of Thread End Events, should this remain that way?
	 * 
	 * Prepare tables and indices as necessary.
	 * 
	 * @return	Success of initialization; true means we have fresh empty tables; false return means tables already existed with data in them.
	 */
	@Override
	public boolean createTables(){
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createThreadEventTable);
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
	protected void createIndices() throws SQLException{
		createIndex(tableName, "lineNumber", "LineNumberIndex");
		createIndex(tableName, "viewStartXPixel", "PixelStartIndex");
		createIndex(tableName, "viewEndXPixel", "PixelEndIndex");
	}
	
	int insertCount = 0;

	public boolean saveThreadEventMarker(String tid, int lineNumber, int numLines, ThreadEventType eventType, int startPixelCoordinate, int endPixelCoordinate) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
				
			if(null == tid || tid.contains("null")){
				// Final thread end does not get assigned a thread ID in our system.
				insertThreadEventStatement.setNull(i++, java.sql.Types.INTEGER);
			} else {
				Integer parsedIdToInt = tid.equals("0") ? 0 : Integer.parseInt(StringUtils.stripStart(tid, "0"));
				insertThreadEventStatement.setInt(i++, parsedIdToInt);
			}
			insertThreadEventStatement.setInt(i++, lineNumber);
			insertThreadEventStatement.setInt(i++, numLines);
			insertThreadEventStatement.setInt(i++, eventType.getId());
			insertThreadEventStatement.setInt(i++, startPixelCoordinate);
			insertThreadEventStatement.setInt(i++, endPixelCoordinate);
			
			insertThreadEventStatement.addBatch();
			
			insertCount++;
			if((insertCount % 1000) == 0){
				this.executeInsertLineNumberBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean saveThreadEventMarker(TraceThreadEvent event) {
		return saveThreadEventMarker(event.getTid(), event.getLineNumber(), event.getNumLines(), event.getEventType(), event.getPixelStart(), event.getPixelEnd());
	}
	
	public void executeInsertLineNumberBatch(){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertThreadEventStatement.executeBatch();
			connection.commit();
		} catch(BatchUpdateException e){
			printBatchFailure(insertThreadEventStatement, e);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	protected ResultSet getThreadMarkerRange(int startRangelineNumber, int endRangelineNumber) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
			findThreadEventRangeByLineStatement.setInt(i++, startRangelineNumber);
			findThreadEventRangeByLineStatement.setInt(i++, endRangelineNumber);
			boolean success = findThreadEventRangeByLineStatement.execute();
			
			if(success){
				// Closed by caller, not great
				ResultSet results = findThreadEventRangeByLineStatement.getResultSet();
				return results;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	protected ResultSet getThreadMarkerPixelRange(int startPixelCoordinate, int endPixelCoordinate) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
			findThreadMarkerByPixelStatement.setInt(i++, endPixelCoordinate);
			findThreadMarkerByPixelStatement.setInt(i++, startPixelCoordinate);
			boolean success = findThreadMarkerByPixelStatement.execute();
			
			if(success){
				// Closed by caller, not great
				ResultSet results = findThreadMarkerByPixelStatement.getResultSet();
				return results;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	

	public int getTotalNumberOfThreadEvents() {
		int numberOfLines = 0;
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = findNumberOfThreadEventsStatement.execute();
			
			if(success){
				try(
				ResultSet results = findNumberOfThreadEventsStatement.getResultSet();
				){
					results.next();
					numberOfLines = results.getInt(1);
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return numberOfLines;
	}
	
	public List<String> getDistinctThreadNames() {
		List<String> threadLabels = new ArrayList<String>();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = this.findDistinctThreadNamesStatement.execute();
			
			if(success){
				try(
				ResultSet results = findDistinctThreadNamesStatement.getResultSet();
				){
					while(results.next()){
						String id = results.getString(1);
						if(null != id){
							threadLabels.add(id);
						}
					}
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return threadLabels;
	}
	

	public int getFinalThreadEventEndPoint() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = this.findFinalThreadEventEndPointStatement.execute();
			
			if(success){
				try(
				ResultSet results = findFinalThreadEventEndPointStatement.getResultSet();
				){
					if(results.next()){
						 int finalThreadEventPoint = results.getInt(1);
						 results.close();
						return finalThreadEventPoint;
					}
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return -1;
	}

	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertThreadEventStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public List<TraceThreadEvent> getThreadEvents(int startLine, int endLine) {
		ResultSet batchResults = getThreadMarkerRange(startLine, endLine);
		return processQueryResults(batchResults);
	}
	
	public List<TraceThreadEvent> getThreadEventsInPixelRange(int startPixelCoordinate, int endPixelCoordinate){
		ResultSet batchResults = getThreadMarkerPixelRange(startPixelCoordinate, endPixelCoordinate);
		return processQueryResults(batchResults);
	}
	
	public List<TraceThreadEvent> getThreadEventForLine(int lineNumber) {
		ResultSet batchResults = getThreadMarkerForLine(lineNumber);
		return processQueryResults(batchResults);
	}
	
	public ResultSet getThreadMarkerForLine(int lineNumber) {
		
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i = 1;
			findThreadMarkerContainingLineStatement.setInt(i++, lineNumber);
			findThreadMarkerContainingLineStatement.setInt(i++, lineNumber); // limit
			boolean success = findThreadMarkerContainingLineStatement.execute();
			
			if(success){
				// Closed by caller, not great
				ResultSet results = findThreadMarkerContainingLineStatement.getResultSet();
				return results;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	/**
	 * Note: this method is responsible for closing the ResultSet when it is done with it
	 */
	protected List<TraceThreadEvent> processQueryResults(ResultSet batchResults) {
		List<TraceThreadEvent> results = new LinkedList<>();
		try{
			while(batchResults.next()){
				Integer tid = batchResults.getInt("tid");
				int eventLineNumber = batchResults.getInt("lineNumber");
				int typeId = batchResults.getInt("typeId");
				int numLines = batchResults.getInt("numberOfLines");
				int startPixel = batchResults.getInt("viewStartXPixel");
				// int endPixel = batchResults.getInt("viewEndXPixel");
				TraceThreadEvent newEvent = new TraceThreadEvent(tid.toString(), eventLineNumber, ThreadEventType.getFromId(typeId), startPixel);
				newEvent.setNumLines(numLines);
				results.add(newEvent);
			}
			batchResults.close();
		} catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		
		return results;
	}
}
