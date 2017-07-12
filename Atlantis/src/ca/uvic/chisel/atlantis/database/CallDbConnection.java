package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ca.uvic.chisel.atlantis.functionparsing.Function;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;

public class CallDbConnection extends DbConnectionManager {

	private InsertCall insertCallStatement;
	private int insertCallCount;
	
	protected static final String CALL_TABLE_NAME = "function_calls";
	
	public CallDbConnection(File originalFile) throws Exception {
		super(CALL_TABLE_NAME, originalFile);
	}

	/**
	 * Inserts are unique by the {@link Instruction} ID.
	 * 
	 * @param from
	 * @param to
	 * @param at
	 * @return
	 */
	@Deprecated
	public boolean insertCall(Function from, Function to, Instruction at) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertCall q = insertCallStatement;
			q.setParam(q.callInstruction, at.getIdGlobalUnique());
			q.setParam(q.callingFunctionStart, from.getFirst().getIdGlobalUnique());
			q.setParam(q.calledFunctionStart, to.getFirst().getIdGlobalUnique());
			
			insertCallStatement.addBatch();
			
			insertCallCount++;
			if((insertCallCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean insertCall(Instruction fromFuncStartInst, Instruction toFuncStartInst, Instruction at) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertCall q = insertCallStatement;
			q.setParam(q.callInstruction, at.getIdGlobalUnique());
			q.setParam(q.callingFunctionStart, fromFuncStartInst.getIdGlobalUnique());
			q.setParam(q.calledFunctionStart, toFuncStartInst.getIdGlobalUnique());
			
			insertCallStatement.addBatch();
			
			insertCallCount++;
			if((insertCallCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public int getNumberUniqueCalls(){
		try {
			CountCalls countCallsStatement = AtlantisQueries.mk(connection, new CountCalls());
			TypedResultSet rs = countCallsStatement.executeQuery();
			rs.next();
			int res = rs.resultSet.getInt(1);
			rs.close();
			countCallsStatement.close();
			return res;
			
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public void finalizeInsertionBatch() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertCallStatement.executeBatch();
			connection.commit();
			
			insertCallCount = 0;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertCallStatement = AtlantisQueries.mk(connection, new InsertCall());
		insertCallCount = 0;
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){
				try(
					PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createCallDbTable)
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
		try {
			insertCallStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIndices() throws SQLException {
		createIndex(tableName, "callInstruction", "CallIns");
		createIndex(tableName, "callingFunctionStart", "CallerFuncStart");
		createIndex(tableName, "calledFunctionStart", "CalledFuncStart");
	}

}
