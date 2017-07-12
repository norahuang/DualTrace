package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import ca.uvic.chisel.atlantis.models.AssemblyChangedEvent;
import ca.uvic.chisel.atlantis.models.AssemblyEventType;

public class AssemblyEventDbConnection extends DbConnectionManager {

	private InsertAssemblyEventMarker insertAssemblyEventMarkerStatement;
	private FindNumberfMarkers findNumberOfMarkersStatement;
	private FindAssemblyMarkerByLine findAssemblyMarkerByLineStatement;
	private FindAssemblyMarkerContainingLine findAssemblyMarkerContainingLineStatement;
	private FindAssemblyMarkerByPixel findAssemblyMarkerByPixelStatement;
	private FindDistinctAssemblyNames findDistinctAssemblyNamesStatement;
	private FindAssemblyEventEndpoint findFinalAssemblyEventEndPointStatement;
	
	protected static final String ASSEMBLY_EVENT_TABLE_NAME = "assembly_events";
	
	public AssemblyEventDbConnection(File originalFile) throws Exception {
		super(ASSEMBLY_EVENT_TABLE_NAME, originalFile);
	}
	
	// Want name parameters? Check: Spring JDBC, or check http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html?page=2
	@Override
	public void initializePreparedStatements() throws SQLException {		
		insertAssemblyEventMarkerStatement = AtlantisQueries.mk(connection, new InsertAssemblyEventMarker());
		findNumberOfMarkersStatement = AtlantisQueries.mk(connection, new FindNumberfMarkers());
		findAssemblyMarkerByLineStatement = AtlantisQueries.mk(connection, new FindAssemblyMarkerByLine());
		findAssemblyMarkerContainingLineStatement = AtlantisQueries.mk(connection, new FindAssemblyMarkerContainingLine());
		findAssemblyMarkerByPixelStatement = AtlantisQueries.mk(connection, new FindAssemblyMarkerByPixel());
		findDistinctAssemblyNamesStatement = AtlantisQueries.mk(connection, new FindDistinctAssemblyNames());
		findFinalAssemblyEventEndPointStatement = AtlantisQueries.mk(connection, new FindAssemblyEventEndpoint());
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
			
			if(state == TableState.MISSING){
				doCreateTable();
				return true;
			}
			
			return false;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
		
	}

	private void doCreateTable() throws SQLException {
		try(
		PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createAssemblyTable);
		){
		
			refreshConnection();
			installTableStatement.executeUpdate();
			connection.commit();
			this.saveTableStateToMetaDataTable(TableState.CREATED);
		}
		
	}
	
	@Override
	protected void createIndices() throws SQLException{
		// Creating all of the indexes all in one go is much faster (around 2x as fast);
		createIndex(tableName, "startLineNum", "LineNumberIndex");
		createIndex(tableName, "viewStartXPixel", "PixelStartIndex");
		createIndex(tableName, "viewEndXPixel", "PixelEndIndex");
	}
	
	private int insertCount = 0;
	
	public boolean saveAssemblyEventMarker(String id, int startLineNum, int numLines, AssemblyEventType assemblyType, int startPixelCoordinate, int endPixelCoordinate) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			InsertAssemblyEventMarker q = insertAssemblyEventMarkerStatement;
			q.setParam(q.startLineNum, startLineNum);
			q.setParam(q.numLines, numLines);
			q.setParam(q.id, id);
			q.setParam(q.assemblyTypeId, assemblyType.getId()); // always AssemblyType.SWITCH?
			q.setParam(q.viewStartXPixel, startPixelCoordinate);
			q.setParam(q.viewEndXPixel, endPixelCoordinate);
			
			q.addBatch();
			
			insertCount++;
			if((insertCount % 1000) == 0){
				this.executeInsertAssemblyEventMarkerBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean saveAssemblyEventMarker(AssemblyChangedEvent event) {
		return saveAssemblyEventMarker(event.getIdentifier(), event.getLineNumber(), event.getNumLines(), event.getEventType(), event.getPixelStart(), event.getPixelEnd());
	}
	
	public void executeInsertAssemblyEventMarkerBatch(){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertAssemblyEventMarkerStatement.executeBatch();
			connection.commit();
		} catch(BatchUpdateException e){
			printBatchFailure(insertAssemblyEventMarkerStatement, e);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public TypedResultSet getAssemblyMarkerRange(int lineNumber, int maxLineNum) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			FindAssemblyMarkerByLine q = findAssemblyMarkerByLineStatement;
			q.setParam(q.leftBoundary, lineNumber);
			q.setParam(q.rightBoundary, maxLineNum); // limit
			boolean success = q.execute();
			
			if(success){
				// Closed by caller, not great
				TypedResultSet results = q.getResultSet();
				return results;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	public TypedResultSet getAssemblyMarkerPixelRange(int startPixelCoordinate, int endPixelCoordinate) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			FindAssemblyMarkerByPixel q = findAssemblyMarkerByPixelStatement;
			q.setParam(q.leftBoundary, endPixelCoordinate);
			q.setParam(q.rightBoundary, startPixelCoordinate);
			boolean success = q.execute();
			
			if(success){
				// Closed by caller, not great
				return q.getResultSet();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	public TypedResultSet getAssemblyMarkerForLine(int lineNumber) {
		
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			FindAssemblyMarkerContainingLine q = findAssemblyMarkerContainingLineStatement;
			q.setParam(q.leftInnerBoundary, lineNumber);
			q.setParam(q.rightInnerBoundary, lineNumber); // limit
			boolean success = q.execute();
			
			if(success){
				// Closed by caller, not great
				TypedResultSet results = findAssemblyMarkerContainingLineStatement.getResultSet();
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
	public List<AssemblyChangedEvent> processQueryResults(TypedResultSet batchResults) {
		List<AssemblyChangedEvent> results = new LinkedList<>();
		// Not querying with this, just using for typed result fields
		AssemblyEventAllSelectFields q = new AssemblyEventAllSelectFields();
		try{
			while(batchResults.next()){
				String id = batchResults.get(q.id);
				int startLineNum = batchResults.get(q.startLineNum);
				int numLines = batchResults.get(q.numLines);
				int startPixel = batchResults.get(q.viewStartXPixel);
				// int typeId = batchResults.getInt("assemblyTypeId");
				// AssemblyEventType assemblyType = AssemblyEventType.getFromId(typeId);
				results.add(new AssemblyChangedEvent(id, startLineNum, numLines, startPixel));
			}
			
			batchResults.close();
		} catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return results;
	}
	
	@Deprecated
	public int getNumberOfAssemblyMarkers() {
		int numberOfMarkers = 0;
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = findNumberOfMarkersStatement.execute();
			
			if(success){
				try(
				TypedResultSet results = findNumberOfMarkersStatement.getResultSet();
				){
					results.next();
					numberOfMarkers = results.resultSet.getInt(1);
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return numberOfMarkers;
	}
	
	public List<String> getDistinctAssemblyNames() {
		List<String> assemblyLabels = new ArrayList<String>();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = this.findDistinctAssemblyNamesStatement.execute();
			
			if(success){
				try(
				TypedResultSet results = findDistinctAssemblyNamesStatement.getResultSet();
				){
					while(results.next()){
						String id = results.resultSet.getString(1);
						if(null != id)
							assemblyLabels.add(id);
					}
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return assemblyLabels;
	}
	
	public int getFinalAssemblyEventEndPoint() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			boolean success = this.findFinalAssemblyEventEndPointStatement.execute();
			
			if(success){
				try(
				TypedResultSet results = findFinalAssemblyEventEndPointStatement.getResultSet();
				){
					if(results.next()){
						return results.resultSet.getInt(1);
					}
				}
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return -1;
	}
	
	public List<AssemblyChangedEvent> getAssemblyEventsInRange(int startLine, int endLine) {
		TypedResultSet batchResults = getAssemblyMarkerRange(startLine, endLine);
		return processQueryResults(batchResults);
	}
	
	public List<AssemblyChangedEvent> getAssemblyEventsInPixelRange(int startPixelCoordinate, int endPixelCoordinate){
		TypedResultSet batchResults = getAssemblyMarkerPixelRange(startPixelCoordinate, endPixelCoordinate);
		return processQueryResults(batchResults);
	}
	
	public List<AssemblyChangedEvent> getAssemblyEventForLine(int lineNumber) {
		TypedResultSet batchResults = getAssemblyMarkerForLine(lineNumber);
		return processQueryResults(batchResults);
	}

	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertAssemblyEventMarkerStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
