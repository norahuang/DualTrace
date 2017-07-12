package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

public abstract class DbBackendAbstract {

	protected static final String DB_ERROR_MESSSAGE_PREFIX = "Check Windows->Preferences->Trace Analysis->Database Connection. Error connecting to trace database:\n";
	protected static final int MAX_TABLE_NAME_SIZE = 64;
	public static final String DEFAULT_PASSWORD = "root";
	public static final String DEFAULT_USERNAME = "root";
	
	protected static boolean jdbcRegistered = false;
	
	// XXX TODO the max_allowed_packet is 1G, this might actually be allowed to be way bigger.
	public static final int MAX_QUERY_LENGTH = 1000000;
			
	abstract String getDEFAULT_DATABASE(File originalFile);
	
	abstract String getDEFAULT_URL();
	
	abstract String getDATABASE_CONNECTION_PARAMS();
	
	/**
	 * Method used to determine whether or not a table with a specific name exists in the database
	 */
	public abstract boolean tableExists(String database, String tableName, Connection connection) throws SQLException;
		
	
	static void registerJDBCDrivers(String jdbcClassName) {
		if(DbBackendAbstract.jdbcRegistered) {
			return;
		}
		
		try {
			Class.forName(jdbcClassName);
			DbBackendAbstract.jdbcRegistered = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	// TODO move this to constructor and non static, yar?
	public static void ensureMetaDataTablesExist(Connection connection) throws SQLException {
		// Queries have IF NOT EXISTS, no need to check like we used to.
		try(
		PreparedStatement  createFileMetaTableStmt = AtlantisQueries.mk(connection, AtlantisQueries.createMetaDataTable);
		){
			int result = createFileMetaTableStmt.executeUpdate();
			connection.commit();
		}
		
		try(
			PreparedStatement  createMetaStatement = AtlantisQueries.mk(connection, AtlantisQueries.createFileStatusTable);
		){
			int result = createMetaStatement.executeUpdate();
			connection.commit();
		}
		
		connection.commit();
	}

}