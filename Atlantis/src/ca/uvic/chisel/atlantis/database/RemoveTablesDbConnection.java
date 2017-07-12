package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated // Not needed for SQLite really. Right?
public class RemoveTablesDbConnection extends DbConnectionManager {

	private PreparedStatement countAndTagTables;
	private PreparedStatement collectTables;
	private PreparedStatement dropTables;
	private PreparedStatement updateStatusTable;
	private PreparedStatement updateMetaTable;
	
	
	public RemoveTablesDbConnection(File originalFile) throws Exception {
		super(null, originalFile);
	}
	
	// Want name parameters? Check: Spring JDBC, or check http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html?page=2
	@Override
	public void initializePreparedStatements() throws SQLException {		
		countAndTagTables = AtlantisQueries.mk(connection, AtlantisQueries.countAndLabelTablesForDeletion);
		collectTables = AtlantisQueries.mk(connection, AtlantisQueries.selectTablesForTrace);
	}
	
	/**
	 * If we move to SQLite, this method is no longer sensible. We'd abort the write, and delete the DB file.
	 * 
	 * @throws SQLException
	 */
	@Deprecated
	public void removeTables() throws SQLException {
		int tablesSuccessfullyDropped = 0;
		try{
			refreshConnection();
			
			String dbName = connection.getCatalog();
			
			countAndTagTables.setString(1, this.traceFileName);
			countAndTagTables.executeUpdate();
			connection.commit();
			int tablesToDrop = countAndTagTables.getUpdateCount();
			
			collectTables.setString(1, this.traceFileName);
			collectTables.execute();
			try(
			ResultSet tables = collectTables.getResultSet();
			){
			
				tablesSuccessfullyDropped = 0;
				while(tables.next()){
					String tableName = tables.getString(1);
					try{
						dropTables = AtlantisQueries.dropTable(connection, tableName);
						dropTables.execute();
						connection.commit();
						
						tablesSuccessfullyDropped++;
						updateStatusTable = AtlantisQueries.updateTableStatus(connection, dbName);
						updateStatusTable.setString(1, tableName);
						updateStatusTable.executeUpdate();
						connection.commit();
						System.out.println("Removed "+tableName);
						System.out.println(updateStatusTable.toString());
					} catch(SQLException e){
						System.err.println("Table failed to drop from database: "+tableName);
					}
					updateStatusTable.close();
					dropTables.close();
				}
			
			}
			
			try{
				updateMetaTable = AtlantisQueries.updateMetaTableStatus(connection, dbName);
				updateMetaTable.executeUpdate();
				connection.commit();
				System.out.println(updateMetaTable.toString());
			} catch(SQLException e){
					System.err.println("Failed to remove meta information for file: "+this.traceFileName);
			}
			updateMetaTable.close();
			
			
			System.out.println("Count of tables successfully dropped: "+tablesSuccessfullyDropped+"/"+tablesToDrop);
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Prepare tables and indices as necessary.
	 * 
	 * @return	Success of initialization; true means we have fresh empty tables; false return means tables already existed with data in them.
	 */
	@Override
	public boolean createTables(){
		return true;
	}

	@Override
	protected void preAbortCommitAndDropTable() {
		// Nada
	}

	@Override
	protected void createIndices() throws SQLException {
		// Nada
	}
	
}
