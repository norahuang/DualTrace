package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

import ca.uvic.chisel.atlantis.database.DbConnectionManager.TraceState;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;

public class DbBackendSQLite extends DbBackendAbstract {

	public static final String DEFAULT_DATABASE_FILE_NAME = "atlantis_trace.sqlite.db";
	
	public static final String DEFAULT_URL = "jdbc:sqlite:file:"; //"jdbc:sqlite://localhost:3306/";
//	public static final String DEFAULT_DATABASE = "::memory:"; //":atlantis.db";
//	public static final String DEFAULT_DATABASE = ":file:atlantis.db";
	public static final String DEFAULT_DATABASE_FILENAME = "memdb1";
	// https://bitbucket.org/xerial/sqlite-jdbc/issues/42/using-shared-in-memory-with-uri-filename
	// Shared cache significantly saves IO and memory use when I make multiple connections to the DB
//	protected static final String DATABASE_CONNECTION_PARAMS = "?mode=memory&cache=shared"; //&rewriteBatchedStatements=true";
	protected static final String DATABASE_CONNECTION_PARAMS = "?cache=shared"; //&rewriteBatchedStatements=true";
	// TODO Why did I disable rewrite Batch?

	final static String jdbcClassName = "org.sqlite.JDBC";

	public DbConnectionManager connMan;
	
	@Override
	String getDEFAULT_DATABASE(File originalFile){
		// using replace there is cheesey, but I need some very broad refactoring to fix that need.
		return originalFile.getPath().replace(AtlantisFileUtils.BINARY_FORMAT_TMP_FILE_EXTENSION, "")+"/" + DEFAULT_DATABASE_FILE_NAME;
	}
	
	@Override
	String getDEFAULT_URL(){
		return DEFAULT_URL;
	}
	
	@Override
	String getDATABASE_CONNECTION_PARAMS() {
		return DATABASE_CONNECTION_PARAMS;
	}
	
	DbBackendSQLite(){
		System.out.println("USING SQLite: "+DEFAULT_URL+" "+jdbcClassName);
		registerJDBCDrivers(jdbcClassName);
	}
	
	@Deprecated
	void loadFromDiskIfExists(Connection conn){
		// Only needed if we are using in-memory database. Otherwise the connection will
		// either open or create the database.
		Statement stat;
		try {
			stat = conn.createStatement();
			stat.executeUpdate("restore from "+DEFAULT_DATABASE_FILENAME);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void saveToDiskIfExists(Connection conn){
		Statement stat;
		try {
			stat = conn.createStatement();
			stat.executeUpdate("backup to "+DEFAULT_DATABASE_FILENAME);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method used to determine whether or not a table with a specific name exists in the database
	 * 
	 * If we do need to read this, we need to do transaction read only for them. System tables lock on select otherwise.
	 */
	@Deprecated
	public boolean tableExists(String database, String tableName, Connection connection) throws SQLException {
		int count = 0;
		if(null == connection){
			System.out.println("Null connection received");
		}
		/*
		 CREATE TABLE sqlite_master (
			  type TEXT,
			  name TEXT,
			  tbl_name TEXT,
			  rootpage INTEGER,
			  sql TEXT
			);
		*/
		try(
			PreparedStatement tableExistsCheckStatement = AtlantisQueries.mk(connection, AtlantisQueries.checkTableExists);
		){
					System.out.println("CheckTablesExist");
			// Even when table names have back ticks, they are named without them in the
			// system tables.
			tableExistsCheckStatement.setString(1, tableName.replaceAll("`", ""));
			tableExistsCheckStatement.execute();
			try(
			ResultSet results = tableExistsCheckStatement.getResultSet();
			){
				results.next();
				count = results.getInt(1);
			}
			connection.commit();
			return count > 0;
		}
	}

	/**
	 * Static implementation for querying to find out if a file is indexed prior to opening
	 * it in an editor. Can pass a null connection when calling from static context.
	 * 
	 * @param traceFileFolder
	 * @param connection
	 * @return
	 */
	protected static boolean isTraceFileInDatabase(File traceFileFolder, Connection connection, boolean mustBeComplete) throws SQLException {
		// Checking the actual database contents validates the DB...but takes longer. Skip it?
		if(traceFileFolder == null){
			return false;
		}
		// Find the sqlite database file in the binary format folder. If it's not, clearly the trace has not been processed.
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		    	return name.equals(DEFAULT_DATABASE_FILE_NAME);
		    }
	    };
	    File[] fileResults = traceFileFolder.listFiles(filter);
		File dbFile = (fileResults != null && fileResults.length != 0) ? fileResults[0] : null;
		if(null == dbFile || !dbFile.exists()){
			return false;
		} else {
			boolean closeAtEnd = false;
			
			if(null == connection){
				closeAtEnd = true;
				
				String dbConnect = DEFAULT_URL + traceFileFolder +"/" +DEFAULT_DATABASE_FILE_NAME + DATABASE_CONNECTION_PARAMS;
				
								connection = DriverManager.getConnection(dbConnect, "", "");
				// Atlantis was using MySQL with autocommit false for a long time. I looked into it, and it is because
				// InnoDB does a flush on every insert if autocommit is true.
				// http://dba.stackexchange.com/questions/42704/is-it-better-to-use-autocommit-0
				connection.setAutoCommit(false);
			}
			
			ensureMetaDataTablesExist(connection);
			
			NamedParameterPreparedStatement checkFileExistsStmt = AtlantisQueries.mk(connection, AtlantisQueries.checkFileExistsinDb);
			checkFileExistsStmt.execute();
			ResultSet results = checkFileExistsStmt.getResultSet();
			
			boolean exists = results.next();
			
			if(mustBeComplete){
				exists = exists && TraceState.getTraceState(results.getString(1)) == TraceState.COMPLETED;
			}
			results.close();
			checkFileExistsStmt.close();
			
			if(closeAtEnd) {
				connection.close();
			}
			
			return exists;
		}
	}
	
}
