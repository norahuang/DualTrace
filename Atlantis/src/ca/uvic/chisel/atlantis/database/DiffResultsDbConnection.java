package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;

import ca.uvic.chisel.atlantis.compare.DiffRegion;

public class DiffResultsDbConnection extends DbConnectionManager {

	protected static final String DIFF_RESULTS_TABLE_NAME = "diff_results";
	
	
	private PreparedStatement insertDiffRegionStatement;
	private PreparedStatement getMatchingRegionsStatement;
	private PreparedStatement getDifferentRegionsStatement;
	private PreparedStatement getAllRegionsStatement;
	private PreparedStatement getRegionNumberStatement;
	
	// TODO Adding the IPath stuff, but the diff is not currently working anyhow.
	public DiffResultsDbConnection(String referenceFileIdentifier, IPath referenceFilePath, File originalFile, String challengerFileIdentifier, IPath challengerFilePath) throws Exception{
		// TODO I feel like any entry should have the challenger and reference file attached?
		super(DIFF_RESULTS_TABLE_NAME, originalFile);
	}
	
	// Want name parameters? Check: Spring JDBC, or check http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html?page=2
	@Override
	public void initializePreparedStatements() throws SQLException {	
		insertDiffRegionStatement = AtlantisQueries.mk(connection, AtlantisQueries.insertDiffRegion);
		getMatchingRegionsStatement = AtlantisQueries.mk(connection, AtlantisQueries.getMatchingRegion);
		getDifferentRegionsStatement = AtlantisQueries.mk(connection, AtlantisQueries.getDifferentRegions);
		getAllRegionsStatement = AtlantisQueries.mk(connection, AtlantisQueries.fetchAllRegions);
		getRegionNumberStatement = AtlantisQueries.mk(connection, AtlantisQueries.fetchRegionByNumber);
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
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection,  AtlantisQueries.createDiffTable);
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
	
	public boolean saveDiffResults(ArrayList<DiffRegion> allRegions) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			int i;
			int x = 1;
			for(Iterator<DiffRegion> iterator = allRegions.listIterator();iterator.hasNext();){
				i = 1;
				DiffRegion region = iterator.next();
				insertDiffRegionStatement.setInt(i++, x);
				insertDiffRegionStatement.setInt(i++, region.getLeftStartLine());
				insertDiffRegionStatement.setInt(i++, region.getLeftEndLine());
				insertDiffRegionStatement.setLong(i++, region.getLeftOffset());
				insertDiffRegionStatement.setInt(i++, region.getLeftLength());
				insertDiffRegionStatement.setInt(i++, region.getRightStartLine());
				insertDiffRegionStatement.setInt(i++, region.getRightEndLine());
				insertDiffRegionStatement.setLong(i++, region.getRightOffset());
				insertDiffRegionStatement.setInt(i++, region.getRightLength());
				insertDiffRegionStatement.setBoolean(i++, region.getMatchState());
				insertDiffRegionStatement.addBatch();
				x++;
			}

			this.executeInsertDiffRegionBatch();
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public ArrayList<DiffRegion> getMatchingRegions(){
		ArrayList<DiffRegion> resList = new ArrayList<DiffRegion>();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			boolean success = getMatchingRegionsStatement.execute();
			
			if(success){
				try(
				ResultSet results = getMatchingRegionsStatement.getResultSet();
				){
					DiffRegion region;
					while(results.next()){
						region = new DiffRegion();
						region.setLeftStartLine(results.getInt(2));
						region.setLeftEndLine(results.getInt(3));
						region.setLeftOffset(results.getInt(4));
						region.setLeftLength(results.getInt(5));
						region.setRightStartLine(results.getInt(6));
						region.setRightEndLine(results.getInt(7));
						region.setRightOffset(results.getInt(8));
						region.setRightLength(results.getInt(9));
						region.setMatchState(results.getBoolean(10));
						resList.add(region);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return resList;
	}
	
	public ArrayList<DiffRegion> getDifferentRegions(){
		ArrayList<DiffRegion> resList = new ArrayList<DiffRegion>();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			boolean success = getDifferentRegionsStatement.execute();
			
			if(success){
				try(
				ResultSet results = getDifferentRegionsStatement.getResultSet();
				){
					DiffRegion region;
					while(results.next()){
						region = new DiffRegion();
						region.setLeftStartLine(results.getInt(2));
						region.setLeftEndLine(results.getInt(3));
						region.setLeftOffset(results.getInt(4));
						region.setLeftLength(results.getInt(5));
						region.setRightStartLine(results.getInt(6));
						region.setRightEndLine(results.getInt(7));
						region.setRightOffset(results.getInt(8));
						region.setRightLength(results.getInt(9));
						region.setMatchState(results.getBoolean(10));
						resList.add(region);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return resList;
	}
	
	public ArrayList<DiffRegion> getAllRegions(){
		ArrayList<DiffRegion> resList = new ArrayList<DiffRegion>();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			boolean success = getAllRegionsStatement.execute();
			
			if(success){
				try(
				ResultSet results = getAllRegionsStatement.getResultSet();
				){
					DiffRegion region;
					while(results.next()){
						region = new DiffRegion();
						region.setLeftStartLine(results.getInt(2));
						region.setLeftEndLine(results.getInt(3));
						region.setLeftOffset(results.getInt(4));
						region.setLeftLength(results.getInt(5));
						region.setRightStartLine(results.getInt(6));
						region.setRightEndLine(results.getInt(7));
						region.setRightOffset(results.getInt(8));
						region.setRightLength(results.getInt(9));
						region.setMatchState(results.getBoolean(10));
						resList.add(region);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return resList;
	}
	
	public DiffRegion getRegionNumber(int regionNumber) {
		DiffRegion region = new DiffRegion();
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			int i = 1;
			getRegionNumberStatement.setInt(i++, regionNumber);
			boolean success = getRegionNumberStatement.execute();
			
			if(success){
				try(
				ResultSet results = getRegionNumberStatement.getResultSet();
				){
					while(results.next()){
						region.setLeftStartLine(results.getInt(2));
						region.setLeftEndLine(results.getInt(3));
						region.setLeftOffset(results.getInt(4));
						region.setLeftLength(results.getInt(5));
						region.setRightStartLine(results.getInt(6));
						region.setRightEndLine(results.getInt(7));
						region.setRightOffset(results.getInt(8));
						region.setRightLength(results.getInt(9));
						region.setMatchState(results.getBoolean(10));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return region;
	}
	
	public void executeInsertDiffRegionBatch(){
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertDiffRegionStatement.executeBatch();
			connection.commit();
		} catch(BatchUpdateException e){
			printBatchFailure(insertDiffRegionStatement, e);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected void createIndices() throws SQLException{
		try(
		PreparedStatement installIndexStatement = AtlantisQueries.createDiffResultIndex(connection, tableName);
		){
			refreshConnection();
			installIndexStatement.executeUpdate();
			connection.commit();
		}
	}
	
	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertDiffRegionStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
