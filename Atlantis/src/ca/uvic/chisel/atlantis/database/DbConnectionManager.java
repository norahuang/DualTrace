package ca.uvic.chisel.atlantis.database;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.sqlite.Function;

import ca.uvic.chisel.atlantis.preferences.DatabaseConnectionPreferencePage;
import ca.uvic.chisel.bfv.BigFileApplication;
import com.mysql.jdbc.Statement;
import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

abstract public class DbConnectionManager  implements
//IPropertyChangeListener,
Closeable {
	
	// Will likely remove this, when MySQL is permanently replaced with SQLite.
//	@Override
//	public void propertyChange(PropertyChangeEvent event) {
//		if(updateDbSettings(event)){
//			reInitializeConnection();
//		}
//		System.out.println("Deprecated, removing MySQL, so no need for DB parameters");
//	}
	

	protected static final String FILE_DATA_STATUS_TABLE = "filedatatablestatus";
	protected static final String FILE_META_DATA_TABLE = "filemetadata";
	
	protected static final int SQL_ERROR_CODE_INTERRUPTED = 1317;
	
	protected static final int MAX_FILE_PATH_BYTE_LENGTH = 512; // Windows max is 260 total, Unix max is 255 per path segment (aka dentries).
	
//	// XXX TODO the max_allowed_packet is 1G, this might actually be allowed to be way bigger.
//	public static final int MAX_QUERY_LENGTH = 1000000;
	
	protected static String prefurl; // = DEFAULT_URL;
	protected static String prefdatabase; // = DEFAULT_DATABASE;
	protected static String prefusername; // = DEFAULT_USERNAME;
	protected static String prefpassword; // = DEFAULT_PASSWORD;
//	private static String prefdbConnectionParams; // = DATABASE_CONNECTION_PARAMS
	
	private String url; // = DEFAULT_URL;
	public String database; // = DEFAULT_DATABASE;
	private String username; // = DEFAULT_USERNAME;
	private String password; // = DEFAULT_PASSWORD;
	private String dbConnectionParams; // = DATABASE_CONNECTION_PARAMS
	
	public static DbBackendAbstract backend;
	
	public Connection connection = null;
	
	private static long gaveDbErrorWindowTimeMillisec = Long.MIN_VALUE;
	
	private static final int MAX_SUFFIX_COUNT = 999;
	
	
	protected PreparedStatement tableExistsCheckStatement;
	private PreparedStatement insertFileDataTableStatement;
	private PreparedStatement removeMetaDataStatement;
	
	/**
	 * Used for table naming, but not for identifying the file.
	 */
	protected String traceFileName;
	
	protected File originalFile;
	
	protected String tableName;
	protected TableState tableState;
	
	/**
	 * Table State represents where in the process of creating the index we are at with this table.
	 * 
	 * Processing means that the table is created, but has not been fully populated correctly.
	 * Populated means that we have verified that the table is created and filled correctly.
	 */
	public enum TraceState {
		PROCESSING("processing"), COMPLETED("completed");
	
		public String dbEnumVal;
		
		TraceState(String dbEnumVal) {
			this.dbEnumVal = dbEnumVal;
		}
		
		public static TraceState getTraceState(String dbString) {
			if(dbString.equals(COMPLETED.dbEnumVal)) {
				return COMPLETED;
			} else if(dbString.equals(PROCESSING.dbEnumVal)) {
				return PROCESSING;
			}
			return null;
		}
	}
	
	/**
	 * Table State represents where in the process of creating the index we are at with this table.
	 * 
	 * Missing means that the table hasn't even been created yet.
	 * Created means that the table is created, but has not been fully populated correctly.
	 * Populated means that we have verified that the table is created and filled correctly.
	 */
	public enum TableState {
		MISSING("missing"), CREATED("created"), POPULATED("populated"), DELETING("deleting");
	
		public String dbEnumVal;
		
		TableState(String dbEnumVal) {
			this.dbEnumVal = dbEnumVal;
		}
		
		public static TableState getTableState(String dbString) {
			if(dbString.equals(MISSING.dbEnumVal)) {
				return MISSING;
			} else if(dbString.equals(CREATED.dbEnumVal)) {
				return CREATED;
			} else if(dbString.equals(POPULATED.dbEnumVal)) {
				return POPULATED;
			}else if(dbString.equals(DELETING.dbEnumVal)) {
				return DELETING;
			}
			return null;
		}
	}
	

	
	public String getDEFAULT_DATABASE(File originalFile){ return backend.getDEFAULT_DATABASE(originalFile); };
	public String getDEFAULT_URL(){ return backend.getDEFAULT_URL(); };
	public String getDATABASE_CONNECTION_PARAMS(){ return backend.getDATABASE_CONNECTION_PARAMS(); };
	public String getDEFAULT_PASSWORD(){ return DbBackendAbstract.DEFAULT_PASSWORD; };
	public String getDEFAULT_USERNAME(){ return DbBackendAbstract.DEFAULT_USERNAME; };
	
	static Class backendClassDeprecated = DbBackendSQLite.class;
	
	static {
		// Toggle these to toggle which backend is in use. Desire full SQLite, no MySQL.
//		backend = new DbBackendMySQL();
		backend = new DbBackendSQLite();
	}
	
	public DbConnectionManager(String tableName, File originalFile) throws Exception {
		url = getDEFAULT_URL();
		database = getDEFAULT_DATABASE(originalFile);
		username = getDEFAULT_USERNAME();
		password = getDEFAULT_PASSWORD();
		dbConnectionParams = getDATABASE_CONNECTION_PARAMS();
		
		refreshConnection();
		
		this.traceFileName = originalFile.getName();
		this.tableName = tableName;
		this.originalFile = originalFile;
		initializeDatabaseConfig(null);
		
		initializeBaseTable();
		createTables();
		
		// Must be called later, after all tables have been created.
		// initializePreparedStatements();
	}
	
	/**
	 * Very hackish. Having the extended class handle the meta data and related data was a poor choice. Revise when
	 * the time is right.
	 */
	public void initializeBaseTable() throws Exception {
		// Changed to make the binary trace folder the original file...
		intializeRequiredTables(originalFile);
		initializeBasicPreparedStatements();
	}

	/**
	 * This method will initialize all of the tables required in the database including the
	 * fileMetaData table, the fileDataTableStatus table, and the table itself.
	 * @param traceFileProjectRelativePath 
	 */
	private void intializeRequiredTables(File traceFilePath) throws SQLException {
		DbBackendAbstract.ensureMetaDataTablesExist(connection);
		if(!isTraceFileInDatabaseProcessing(null, traceFilePath, connection)){
			putTraceFileInFileTable(traceFilePath);
		}
		initTableNameAndState();
		connection.commit();
	}
	
	protected void removeTraceFileFromFileTable() throws SQLException {
		File folder = originalFile.getParentFile();
		if(isTraceFileInTable(originalFile, folder, false)) {
			doRemoveTraceFileFromFileTable();
		}
	}


	private void putTraceFileInFileTable(File traceFilePath) throws SQLException {
		doInsertTraceFileIntoFileTable(traceFilePath);
	}
	
	private void doInsertTraceFileIntoFileTable(File traceFilePath)
			throws SQLException {
		try(
		PreparedStatement insertFileStatement = AtlantisQueries.mk(connection, AtlantisQueries.registerTraceFile);
				
		){
			insertFileStatement.setString(1, TraceState.PROCESSING.dbEnumVal);
			
			insertFileStatement.executeUpdate();
			connection.commit();
		}
	}
	
	private void doRemoveTraceFileFromFileTable()
			throws SQLException {
		try(
		PreparedStatement insertFileStatement = AtlantisQueries.mk(connection, AtlantisQueries.removeTrace);
		){
			
			insertFileStatement.execute();
			connection.commit();
		}
	}
	
	// TODO Can I refactor the index creation to accept arrays, and thus combine them? It can be 2x as fast.
	protected void createIndex(String tableName, String columnName, String indexName) throws SQLException {
		boolean ifNotExists = false;
		if(backend.getClass() == DbBackendSQLite.class){
			// Invalid syntax for MySQL
			ifNotExists = true;
		}
		try(
			PreparedStatement createIndicesStatement = AtlantisQueries.createIndex(connection, ifNotExists, tableName, columnName, indexName);
		){
			refreshConnection();
			createIndicesStatement.executeUpdate();
			connection.commit();
		}
	}


	private boolean isTraceFileInTable(File traceFile, File folder, boolean mustBeComplete) throws SQLException {
		return isTraceFileDatabase(traceFile, folder, connection, mustBeComplete);
	}

	private static void showDatabaseErrorMessage(SQLException e) {
		String message = DbBackendAbstract.DB_ERROR_MESSSAGE_PREFIX+e.getMessage()+"\n";
		if(Long.MIN_VALUE == DbConnectionManager.gaveDbErrorWindowTimeMillisec || (System.currentTimeMillis() - DbConnectionManager.gaveDbErrorWindowTimeMillisec) > 1000*60){
			BigFileApplication.showInformationDialog("Invalid Database Connection", message);
			DbConnectionManager.gaveDbErrorWindowTimeMillisec = System.currentTimeMillis();
		}
	}
	
	/**
	 * Static implementation for querying to find out if a file is indexed prior to opening
	 * it in an editor. Can pass a null connection when calling from static context.
	 * 
	 * @param traceFilePath
	 * @param connection
	 * @return
	 */
	protected static boolean isTraceFileDatabase(File traceFile, File traceFolder, Connection connection, boolean mustBeComplete) throws SQLException {
		if(backendClassDeprecated == DbBackendMySQL.class){
			return DbBackendMySQL.isTraceFileInMetaTable(traceFile, connection, mustBeComplete);
		} else {
			return DbBackendSQLite.isTraceFileInDatabase(traceFolder, connection, mustBeComplete);
		}
	}
	
	protected static boolean isTraceFileInDatabaseProcessing(File traceFile, File traceFolder, Connection connection) throws SQLException {
		return DbConnectionManager.isTraceFileDatabase(traceFile, traceFolder, connection, false);
	}
	
	protected static boolean isTraceFileInDatabaseComplete(File traceFile, File traceFolder, Connection connection) throws SQLException {
		return DbConnectionManager.isTraceFileDatabase(traceFile, traceFolder, connection, true);
	}

	protected String convertTraceToTableName(String traceFileName, String tablePrefix) {
		return "`"+tablePrefix + traceFileName.replace("`", "_")+"`";
	}

	private void initTableNameAndState() throws SQLException {
		try(
		NamedParameterPreparedStatement getTraceDataTableInfoStmt = AtlantisQueries.mk(connection, AtlantisQueries.queryTableState);
		){
			getTraceDataTableInfoStmt.setString("tableName", tableName);
			getTraceDataTableInfoStmt.execute();
			try(
			ResultSet rs = getTraceDataTableInfoStmt.getResultSet();
		){
				// There should only be one
				if(rs.next()) {
					tableState = TableState.getTableState(rs.getString(1));
					// if the table is in the CREATED state now, it may have corrupted data in it, so we better wipe it.
					if(tableState == TableState.CREATED) {
						truncateTable();
					}
					
				} else {
					this.tableState = TableState.MISSING;
				}
			}
		}
	}
	
	/**
	 * This method is called when we received a corrupted state from the database.  For some reason, 
	 * a data table was created but was not flagged as fully populated, so we will remove all of the 
	 * data in it before indexing the file.
	 * 
	 * @throws SQLException 
	 */
	private void truncateTable() throws SQLException {
		try{
			try(
			PreparedStatement  truncateTableStmt = AtlantisQueries.truncateMySQL(connection, tableName);
			){
				truncateTableStmt.executeUpdate();
				connection.commit();
			}
		} catch(SQLException e){
			// MySQL has TRUCNATE, SQLite does not, so use:
			try(
			PreparedStatement  truncateTableStmt = AtlantisQueries.truncateSQLite(connection, tableName);
			){
				truncateTableStmt.executeUpdate();
				connection.commit();
			}
		}
	}


	protected void initializeDatabaseConfig(DbConnectionManager instance){
		// Will likely remove this, when MySQL is permanently replaced with SQLite.
//		Map<Thread, StackTraceElement[]> m = Thread.getAllStackTraces();
//		// Terrible hard code, but MySQL or SQLite will eventually be taken out,
//		// and when that happens, either the conditional or this method itself will be unneeded.
//		for(Thread t: m.keySet()){
//			if(m.get(t).length == 0){
//				continue;
//			}
//			int length = m.get(t).length - 1;
//			if(GibraltarMain.class.toString().endsWith(m.get(t)[length].getClassName())){
//				return;
//			}
//		}
//		if(null != instance){
//			AtlantisActivator.getDefault().getSecurePreferenceStore().addPropertyChangeListener(instance);
//		}
//		
//		String dbPreference = AtlantisActivator.getDefault().getSecurePreferenceStore().getString(DatabaseConnectionPreferencePage.PREF_DB_DATABASE);
//		if(dbPreference != null && !dbPreference.isEmpty()) {
//			database = dbPreference;
//		}
//		
//		String pwPreference = AtlantisActivator.getDefault().getSecurePreferenceStore().getString(DatabaseConnectionPreferencePage.PREF_DB_PASSWORD);
//		if(pwPreference != null && !pwPreference.isEmpty()) {
//			password = pwPreference;
//		}
//		
//		String urlPreference = AtlantisActivator.getDefault().getSecurePreferenceStore().getString(DatabaseConnectionPreferencePage.PREF_DB_URL);
//		if(urlPreference != null && !urlPreference.isEmpty()) {
//			url = urlPreference;
//		}
//		
//		String usernamePreference = AtlantisActivator.getDefault().getSecurePreferenceStore().getString(DatabaseConnectionPreferencePage.PREF_DB_USERNAME);
//		if(usernamePreference != null && !usernamePreference.isEmpty()) {
//			username = usernamePreference;
//		}
	}
	
	protected void saveTableStateToMetaDataTable(TableState tableState) throws SQLException {
		int i=1;
		insertFileDataTableStatement.setString(i++, tableName);
		insertFileDataTableStatement.setString(i++, originalFile.getPath());
		insertFileDataTableStatement.setString(i++, tableState.dbEnumVal);
		insertFileDataTableStatement.executeUpdate();
		connection.commit();
		
		this.tableState = tableState;
	}
	
	protected void removeTableFromMetaDataTable() throws SQLException {
		removeMetaDataStatement.setString(1, this.tableName);
		removeMetaDataStatement.executeUpdate();
		connection.commit();
		removeMetaDataStatement.close();
		
		this.tableState = TableState.MISSING;
	}
	
	static private boolean updateDbSettings(PropertyChangeEvent event){
		boolean reconnect = false;
		if (DatabaseConnectionPreferencePage.PREF_DB_DATABASE.equals(event.getProperty())){
			prefdatabase = (String)event.getNewValue();
			reconnect = true;
		} else if(DatabaseConnectionPreferencePage.PREF_DB_URL.equals(event.getProperty())){
			prefurl = (String)event.getNewValue();
			reconnect = true;
		} else if(DatabaseConnectionPreferencePage.PREF_DB_USERNAME.equals(event.getProperty())){
			prefusername = (String)event.getNewValue();
			reconnect = true;
		} else if(DatabaseConnectionPreferencePage.PREF_DB_PASSWORD.equals(event.getProperty())) {
			prefpassword = (String)event.getNewValue();
			reconnect = true;
		}
		
		return reconnect;
	}

	private void reInitializeConnection() {
		try {
			if(connection != null && !connection.isClosed()){
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initializeDatabaseConfig(this);
	}
	
	public boolean isConnected() {
		try {
			return (null != connection && !connection.isClosed());
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * This method will attempt to open a connection to the default database.  If a connection cannot be made for whatever reason,
	 * this method will return false.
	 */
	public boolean refreshConnection() {
		// Because the connection params are instance and not static, this works for SQLite, because
		// it will make a new connection to the existing database when it can.
		// For MySQL, this will continue to work, because the max connections is huge, so it doesn't
		// matter. We probably should have had a single connection per consumer anyway, since connection.commit()
		// is used...affecting all possible pending updates.
		try {
			if(!isConnected()){
				// System.out.println(url + database + dbConnectionParams);
				connection = DriverManager.getConnection(url + database + dbConnectionParams, username, password);
				// Atlantis was using MySQL with autocommit false for a long time. I looked into it, and it is because
				// InnoDB does a flush on every insert if autocommit is true.
				// http://dba.stackexchange.com/questions/42704/is-it-better-to-use-autocommit-0
				connection.setAutoCommit(false);
				connection.commit(); // SQLite req this it seems???
				// Give SQLite more memory. Give MySQL more memory. Benchmark
				if(DbBackendSQLite.class == backend.getClass()){
					Function.create(connection, "REGEXP", new Function() {
	                    @Override
	                    protected void xFunc() throws SQLException {
	                        String expression = value_text(0);
	                        String value = value_text(1);
	                        if (value == null)
	                            value = "";
	
	                        Pattern pattern=Pattern.compile(expression);
	                        result(pattern.matcher(value).find() ? 1 : 0);
	                    }
	                });
				// Need this if we do as in-memory DB, which we may or may not.
				// ((DbBackendSQLite)backend).loadFromDiskIfExists(connection);
				}
				if(null != this.tableName){
					// Only *re-initialize*.
					initializePreparedStatements();
				}
			}
			return true;
		} catch(SQLException e) {
			// TODO Can look at the SQL code if we want to make articulated messages.
			showDatabaseErrorMessage(e);

			// e.printStackTrace();
			connection = null;
		}
		
		return false;
	}
	
	public abstract void initializePreparedStatements() throws SQLException;
	
	public void close(){
		this.closePreparedStatements();
	}
	
	private void closePreparedStatements(){
		Field[] Fields = this.getClass().getDeclaredFields();
		for (Field f : Fields){
			if(f.getDeclaringClass() == PreparedStatement.class){
				try {
					((PreparedStatement)f.get(this)).close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}    
	}
	
	abstract public boolean createTables(); 
	
	/**
	 * Returns the aggregate state of all involved tables.
	 * If any table is missing, returns {@link TableState#MISSING}.
	 * If any table is not empty, returns {@link TableState#POPULATED}.
	 * If <b>all</b> tables are empty, returns {@link TableState#CREATED}.
	 */
	public TableState getTableState() {
		return tableState;
	}
	
	/**
	 * Destroys tables and removes awaiting data from database.
	 * Call only when fully appropriate (interrupted processing, user requires cleaning).
	 * 
	 */
	public void abortCommitAndDropTable(){
		preAbortCommitAndDropTable();
		try(
//			PreparedStatement dropTableStatement = connection.prepareStatement(
//					"DROP TABLE " + this.tableName);
			PreparedStatement dropTableStatement = AtlantisQueries.dropTable(connection, this.tableName);
		){
			dropTableStatement.setString(1, this.tableName);
			dropTableStatement.executeUpdate();
			connection.commit();
			removeTableFromMetaDataTable();
			removeTraceFileFromFileTable();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	abstract protected void preAbortCommitAndDropTable();
	
	protected void initializeBasicPreparedStatements() throws SQLException{
		insertFileDataTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.updateFileDataTable);
		removeMetaDataStatement = AtlantisQueries.mk(connection, AtlantisQueries.removeMetaData);
	}

	/**
	 * Determines whether there are any rows in the specified table.
	 * 
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	protected boolean isTableEmpty(String tableName) throws SQLException {
		try(
		PreparedStatement tableEmptyCheckStatement = AtlantisQueries.isTableEmpty(connection, tableName);
		){
			tableEmptyCheckStatement.execute();
			try(
			ResultSet tableEmptyResults = tableEmptyCheckStatement.getResultSet();
			){
				boolean tableIsEmpty = !tableEmptyResults.next();
				return tableIsEmpty;
			}
		}
	}

	public void closeConnection() {
		try
		{
			if(connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	abstract protected void createIndices() throws SQLException;
	
	public void doIntermediateCommit() {
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void doFinalCommit() {
		try {
			connection.commit();
			createIndices();
			saveTableStateToMetaDataTable(TableState.POPULATED);
			
			doFinalCommitTraceMetaData();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void doFinalCommitTraceMetaData(){
		try(
			PreparedStatement updateMetaDataStatement = AtlantisQueries.mk(connection, AtlantisQueries.updateMetaTable);
		){
			// TODO This will change dramatically if we don't have need for MySQL or other centralized DB
			updateMetaDataStatement.setString(1, TraceState.COMPLETED.dbEnumVal);
			updateMetaDataStatement.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected void printBatchFailure(TypedQuery typedQuery, BatchUpdateException e) {
		this.printBatchFailure(typedQuery.preparedStatement, e);
	}
	
	protected void printBatchFailure(PreparedStatement preparedStatement, BatchUpdateException e) {
		// This exception is trumped by maximum MySQL packet size limitations (1GB).
		// We can't really hit this unless we can deal with packet limitations.
		// java.sql.BatchUpdateException: Data truncation: Data too long for column 'deltaData' at row 1
		List<Integer> updateCounts = Arrays.asList(ArrayUtils.toObject(e.getUpdateCounts()));
		if(updateCounts.contains(Statement.EXECUTE_FAILED)){
			e.printStackTrace();
			int minIndex = 0;
			int maxIndex = Integer.MAX_VALUE;
			for(int i = 0; i < updateCounts.size(); i++){
				if(updateCounts.get(i) == Statement.EXECUTE_FAILED){
					// How do I get the statement corresponding to the failed one,
					// so I can null out the data column?
					// System.out.println("Failed at index "+i);
					if(minIndex == 0){
						minIndex = i;
					}
					if(i > maxIndex){
						maxIndex = i;
					}
				}
			}
			System.out.println("Bulk trace updates failed at indices within range: "+minIndex+" to "+maxIndex);
		} else {
			e.printStackTrace();
		}
	}
	
	public void close(PreparedStatement statement){
		if(null != statement){
			try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void close(ResultSet resultSet){
		if(null != resultSet){
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
}
