package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.functionparsing.Function;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;

public class FunctionDbConnection extends DbConnectionManager {

	protected static final String FUNCTION_TABLE_NAME = "functions";
	
	private int insertFunctionCount;
	private InsertFunction insertFunctionStatement;
	@Deprecated
	private UpdateFunction updateFunctionStatement;
	
	public FunctionDbConnection(File originalFile) throws Exception {
		super(FUNCTION_TABLE_NAME, originalFile);
	}
	
	/**
	 * Although the DB is set up to receive more, it really only receives the instruction id
	 * when it is first filled. Perhaps Murray intended to store additional information later,
	 * or didn't see that he hadn't (need to) make use of these other fields.
	 * 
	 * I am giving this alternative one for the purpose of a refactor I am performing, but I
	 * am leaving the other one for reference.
	 * 
	 * @param func
	 * @return
	 */
	public boolean insertIfUniqueFunction(Instruction funcInst) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertFunction q = insertFunctionStatement;
			q.setParam(q.firstInstruction, funcInst.getIdGlobalUnique());
			q.setParam(q.name, "");
			q.setParam(q.startsThread, false);
			q.setParam(q.unknownStart, false);
			q.setParam(q.oddReturn, false);
			
			insertFunctionStatement.addBatch();
			
			insertFunctionCount++;
			if((insertFunctionCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * See the {@link FunctionDbConnection#insertIfUniqueFunction(Instruction)}
	 * 
	 * @param func
	 * @return
	 */
	@Deprecated
	public boolean insertIfUniqueFunction(Function func) {
		
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertFunction q = insertFunctionStatement;
			q.setParam(q.firstInstruction, func.getFirst().getIdGlobalUnique());
			q.setParam(q.name, func.getName());
			q.setParam(q.startsThread, func.getStartsThread());
			q.setParam(q.unknownStart, func.getUnknownStart());
			q.setParam(q.oddReturn, func.getOddReturn());
			
			q.addBatch();
			
			insertFunctionCount++;
			if((insertFunctionCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public int getNumberUniqueFunctions(){
		try {
			CountFunctions q = AtlantisQueries.mk(connection, new CountFunctions());
			TypedResultSet rs = q.executeQuery();
			rs.next();
			int res = rs.resultSet.getInt(1);
			rs.close();
			q.close();
			return res;
			
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * Currently unused, but could be useful.
	 * 
	 * @param parent
	 * @param callDb
	 * @param instructionDb
	 */
	@Deprecated
	public void retrieveCallees(Function parent, CallDbConnection callDb, InstructionDbConnection instructionDb) {
		try {
			
			SelectCallees q = AtlantisQueries.mk(connection, new SelectCallees());
			
			q.setParam(q.cll_callingFunctionStart, parent.getFirst().getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			ArrayList<Function> callees = new ArrayList<Function>();
			
			while(rs.next()) {				
				Instruction first = new Instruction(rs.get(q.firstInstruction), 0,
						rs.get(q.inst_instructionName), "", rs.get(q.inst_module),
						rs.get(q.inst_moduleId), rs.get(q.inst_moduleOffset), null,
						rs.get(q.inst_parentFunctionId));
				Function func = new Function(first, rs.get(q.name));
				func.setStartsThread(rs.get(q.startsThread));
				func.setUnknownStart(rs.get(q.unknownStart));
				func.setOddReturn(rs.get(q.oddReturn));
				func.setFromParent(parent);
				
				callees.add(func);
			}
			
			parent.attachCallees(callees);
			rs.close();
			q.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Unused, but could be useful.
	 * 
	 * @param callDb
	 * @param instructionDb
	 * @return
	 */
	@Deprecated
	public List<Function> getTopLevelOrUnknownStartFunctions(CallDbConnection callDb, InstructionDbConnection instructionDb) {
		try {
			
			SelectTopLevelAndUnknownFunctions q = AtlantisQueries.mk(connection, new SelectTopLevelAndUnknownFunctions());
			
			TypedResultSet rs = q.executeQuery();
			ArrayList<Function> functions = new ArrayList<Function>();
			
			while(rs.next()) {
				Instruction first = new Instruction(rs.get(q.firstInstruction), 0,
						rs.get(q.inst_instructionName), "", rs.get(q.inst_module),
						rs.get(q.inst_moduleId), rs.get(q.inst_moduleOffset), null,
						rs.get(q.inst_parentFunctionId));
				Function func = new Function(first, rs.get(q.name));
				func.setStartsThread(rs.get(q.startsThread));
				func.setUnknownStart(rs.get(q.unknownStart));
				func.setOddReturn(rs.get(q.oddReturn));
				
				functions.add(func);
			}
			rs.close();
			q.close();
			return functions;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Deprecated
	public boolean updateFunction(Function func) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			finalizeInsertionBatch(); // make sure we have valid state before updating
			
			UpdateFunction q = updateFunctionStatement;
			
			q.setParam(q.name, func.getName());
			q.setParam(q.startsThread, func.getStartsThread());
			q.setParam(q.unknownStart, func.getUnknownStart());
			q.setParam(q.oddReturn, func.getOddReturn());
			q.setParam(q.firstInstruction, func.getFirst().getIdGlobalUnique());
			
			q.executeUpdate();
			connection.commit();
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public List<Function> getFunctionsFromModule(String module, InstructionDbConnection instructionDb) {
		try {
			
			SelectFunctionsFromModule q = AtlantisQueries.mk(connection,new SelectFunctionsFromModule());
					
			q.setParam(q.inst_moduleParam, module);
			
			TypedResultSet rs = q.executeQuery();
			ArrayList<Function> functions = new ArrayList<Function>();
			
			while(rs.next()) {
				Instruction first = new Instruction(rs.get(q.firstInstruction), 0,
						rs.get(q.inst_instructionName), "", rs.get(q.inst_module),
						rs.get(q.inst_moduleId), rs.get(q.inst_moduleOffset), null,
						rs.get(q.inst_parentFunctionId));
				Function func = new Function(first, rs.get(q.name));
				functions.add(func);
			}
			rs.close();
			q.close();
			return functions;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Function getSingleFunctionFromModule(String module, Long offset, InstructionDbConnection instructionDb) {
		try {
			
			SelectSingleFunctionFromModule q = AtlantisQueries.mk(connection,new SelectSingleFunctionFromModule());
					
			q.setParam(q.inst_moduleParam, module);
			q.setParam(q.inst_offsetParam, offset);
			
			TypedResultSet rs = q.executeQuery();
			Function func = null;
			while(rs.next()) {
				Instruction first = new Instruction(rs.get(q.firstInstruction), 0,
						rs.get(q.inst_instructionName), "", rs.get(q.inst_module),
						rs.get(q.inst_moduleId), rs.get(q.inst_moduleOffset), null,
						rs.get(q.inst_parentFunctionId));
				func = new Function(first, rs.get(q.name));				
			}
			rs.close();
			q.close();
			return func;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void finalizeInsertionBatch() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertFunctionStatement.executeBatch();
			connection.commit();
			
			insertFunctionCount = 0;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertFunctionCount = 0;
		insertFunctionStatement = AtlantisQueries.mk(connection, new InsertFunction());
		updateFunctionStatement = AtlantisQueries.mk(connection, new UpdateFunction());
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createFunctionTable);
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
			insertFunctionStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIndices() throws SQLException {
		createIndex(tableName, "firstInstruction", "FirstIns");
	}

}
