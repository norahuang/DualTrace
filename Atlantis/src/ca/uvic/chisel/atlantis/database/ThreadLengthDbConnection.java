package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreadLengthDbConnection extends DbConnectionManager {

	private InsertThreadLength insertThreadLengthStatement;
	private SelectThreadLength getThreadLengthStatement;
	
	protected static final String THREAD_LENGTH_TABLE_NAME = "thread_lengths";
	
	public ThreadLengthDbConnection(File originalFile) throws Exception {
		super(THREAD_LENGTH_TABLE_NAME, originalFile);
	}

	
	public void insertThreadLength(int threadId, long length) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			insertThreadLengthStatement.setParam(insertThreadLengthStatement.threadId, threadId);
			insertThreadLengthStatement.setParam(insertThreadLengthStatement.length, length);
			insertThreadLengthStatement.executeUpdate();
			connection.commit();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public long getThreadLength(int threadId) {
		try {
			refreshConnection();
			
			getThreadLengthStatement.setParam(getThreadLengthStatement.threadId, threadId);
			
			TypedResultSet rs = getThreadLengthStatement.executeQuery();
			rs.next();
			
			return rs.get(getThreadLengthStatement.length);
			
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertThreadLengthStatement = AtlantisQueries.mk(connection, new InsertThreadLength());
		getThreadLengthStatement = AtlantisQueries.mk(connection, new SelectThreadLength());
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createThreadLengthTable);
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
	public void preAbortCommitAndDropTable() {
		// Nada
		return;
	}

	@Override
	protected void createIndices() throws SQLException {	
		createIndex(tableName, "threadId", "Thread");
		createIndex(tableName, "length", "Length");
	}

}
