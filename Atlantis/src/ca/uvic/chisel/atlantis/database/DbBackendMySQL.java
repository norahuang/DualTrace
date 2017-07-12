package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ca.uvic.chisel.atlantis.database.DbConnectionManager.TraceState;

public class DbBackendMySQL extends DbBackendAbstract {

	public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/";
	public static final String DEFAULT_DATABASE = "atlantis";
	protected static final String DATABASE_CONNECTION_PARAMS = "?rewriteBatchedStatements=true";
	
	final static String jdbcClassName = "com.mysql.jdbc.Driver";
	
	@Override
	String getDEFAULT_DATABASE(File originalFile){
		return DEFAULT_DATABASE;
	}
	
	@Override
	String getDEFAULT_URL(){
		return DEFAULT_URL;
	}
	
	@Override
	String getDATABASE_CONNECTION_PARAMS() {
		return DATABASE_CONNECTION_PARAMS;
	}
	
	public DbBackendMySQL() {
		System.out.println("USING MySQL: "+DEFAULT_URL+" "+jdbcClassName);
		registerJDBCDrivers(jdbcClassName);
	}
	
	/**
	 * Method used to determine whether or not a table with a specific name exists in the database
	 */
	public boolean tableExists(String database, String tableName, Connection connection) throws SQLException {	
		int count = 0;
		try(
		DoesTableExist tableExistsCheckStatement = AtlantisQueries.mk(connection, new DoesTableExist());
		){
			tableExistsCheckStatement.setParam(tableExistsCheckStatement.table_schema, database);
			// Even when table names have back ticks, they are named without them in the
			// system tables.
			tableExistsCheckStatement.setParam(tableExistsCheckStatement.table_name, tableName.replaceAll("`", ""));
			
			tableExistsCheckStatement.execute();
			try(
			TypedResultSet results = tableExistsCheckStatement.getResultSet();
			){
				results.next();
				count = results.resultSet.getInt(1);
			}
		}
		return count > 0;
	}
	
	/**
	 * Static implementation for querying to find out if a file is indexed prior to opening
	 * it in an editor. Can pass a null connection when calling from static context.
	 * 
	 * @param traceFilePath
	 * @param connection
	 * @return
	 */
	protected static boolean isTraceFileInMetaTable(File traceFile, Connection connection, boolean mustBeComplete) throws SQLException {
		boolean exists;
		
		if(null != connection){
			exists = DbBackendMySQL.isTraceFileInMetaTableImp(traceFile, connection, mustBeComplete);
		} else {
			try(Connection tempConnect = refreshStaticConnection(connection);){
				exists = DbBackendMySQL.isTraceFileInMetaTableImp(traceFile, tempConnect, mustBeComplete);
			}
			
		}
		
		return exists;
	}
	
	private static boolean isTraceFileInMetaTableImp(File traceFile, Connection connection, boolean mustBeComplete) throws SQLException {
		ensureMetaDataTablesExist(connection);
		boolean exists = false;
		try(
		SelectTraceFileState checkFileExistsStmt = AtlantisQueries.mk(connection, new SelectTraceFileState());
		){
			checkFileExistsStmt.execute();
			try(
			TypedResultSet results = checkFileExistsStmt.getResultSet();
			){
				exists = results.next();
				
				if(mustBeComplete){
					exists = exists && TraceState.getTraceState(results.get(checkFileExistsStmt.traceState)) == TraceState.COMPLETED;
				}
			}
			return exists;
		}
	}
	
	private static Connection refreshStaticConnection(Connection connection) {
		try {
			if(null == connection || connection.isClosed()){
				System.out.println(DbConnectionManager.prefurl + DbConnectionManager.prefdatabase + DATABASE_CONNECTION_PARAMS);
				connection = DriverManager.getConnection (DbConnectionManager.prefurl + DbConnectionManager.prefdatabase + DATABASE_CONNECTION_PARAMS, DbConnectionManager.prefusername, DbConnectionManager.prefpassword);
				connection.setAutoCommit(false);
//				// Create regexp() function to make the REGEXP operator available
//                Function.create(connection, "REGEXP", new Function() {
//                    @Override
//                    protected void xFunc() throws SQLException {
//                        String expression = value_text(0);
//                        String value = value_text(1);
//                        if (value == null)
//                            value = "";
//
//                        Pattern pattern=Pattern.compile(expression);
//                        result(pattern.matcher(value).find() ? 1 : 0);
//                    }
//                });
			}
			
			return connection;
		} catch(SQLException e) {
			// TODO Can look at the SQL code if we want to make articulated messages.
			// showDatabaseErrorMessage(e);
			e.printStackTrace();
		}
		
		return null;
	}
	
}
