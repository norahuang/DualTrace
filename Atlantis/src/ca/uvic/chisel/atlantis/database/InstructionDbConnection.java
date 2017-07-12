package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.functionparsing.Instruction;

public class InstructionDbConnection extends DbConnectionManager {
	
	private InsertInstruction insertInstructionStatement;
	private SelectInstruction getInstructionStatement;
	private GetLineDataForLine getLineDataByForLine;
	private SelectInstructionsInRange getInstructionsInRangeStatement;
	private SelectModules getModulesStatement;
	private GetFunctionRetLineNumber getFunctionRetLineStatement;
	private int insertCount = 0;
	
	protected static final String INSTRUCTION_TABLE_NAME = "instructions";

	public InstructionDbConnection(File originalFile) throws Exception {
		super(INSTRUCTION_TABLE_NAME, originalFile);
	}
	
	public Instruction getInstruction(String id) {
		try {
			refreshConnection();
			
			SelectInstruction q = getInstructionStatement;
			q.setParam(q.instructionIdParam, new InstructionId(id));
			
			TypedResultSet rs = q.executeQuery();
			rs.next();
			
			Instruction inst = new Instruction(rs.get(q.instructionId),
					rs.get(q.firstLineNumber),
					rs.get(q.instructionName),
					rs.get(q.instructionText),
					rs.get(q.module),
					rs.get(q.moduleId),
					rs.get(q.moduleOffset),
					null,
					rs.get(q.parentFunctionId));
			
			rs.close();
			
			return inst;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Instruction getInstructionByLineNumber(int lineNumber){
		try {
			refreshConnection();
			
			Instruction inst = null;
			
			GetLineDataForLine q = getLineDataByForLine;
			q.setParam(q.lineNumber, lineNumber);
			
			TypedResultSet rs = q.executeQuery();
			// Not all lines have instructions, some are thread changes, etc.
			if(rs.next()){
				inst = new Instruction(rs.get(q.instructionId),
						rs.get(q.firstLineNumber),
						rs.get(q.instructionName),
						rs.get(q.instructionText),
						rs.get(q.module),
						rs.get(q.moduleId),
						rs.get(q.moduleOffset),
						null,
						rs.get(q.parentFunctionId));
			}
			
			rs.close();
			
			return inst;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<Instruction> getInstructionsInRangeInclusive(String module, long startModuleOffset, long endModuleOffset) {
		try {
			refreshConnection();
			
			SelectInstructionsInRange q = getInstructionsInRangeStatement;
			q.setParam(q.moduleParam, module);
			q.setParam(q.offsetLeftBound, startModuleOffset);
			q.setParam(q.offsetRightBound, endModuleOffset);
			
			TypedResultSet rs = q.executeQuery();
			List<Instruction> instructions = new ArrayList<Instruction>();
			
			while(rs.next()) {
				instructions.add(new Instruction(rs.get(q.instructionId),
						rs.get(q.firstLineNumber),
						rs.get(q.instructionName),
						rs.get(q.instructionText),
						rs.get(q.module),
						rs.get(q.moduleId),
						rs.get(q.moduleOffset),
						null,
						rs.get(q.parentFunctionId)));
			}
			
			rs.close();
			
			return instructions;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean insertInstruction(Instruction inst) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			InsertInstruction q = insertInstructionStatement;
			q.setParam(q.instructionId, inst.getIdGlobalUnique());
			// insertInstructionStatement.setLong(i++, inst.getBinaryFormatInstructionId());
			q.setParam(q.firstLineNumber, inst.getFirstLine());
			q.setParam(q.moduleOffset, inst.getModuleOffset());
			q.setParam(q.module, inst.getModule());
			q.setParam(q.moduleId, inst.getModuleId());
			q.setParam(q.instructionName, inst.getInstruction());
			q.setParam(q.instructionText, inst.getFullText());
			if(null == inst.getParentFunction()){
				q.setParam(new NullParameter(q.parentFunctionId.parameterName), Types.VARCHAR);
			} else {
				q.setParam(q.parentFunctionId, inst.getParentFunction());
			}
			
			insertInstructionStatement.addBatch();
			
			insertCount++;
			if((insertCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public List<String> getModules() {
		try {
			refreshConnection();
			
			TypedResultSet rs = getModulesStatement.executeQuery();
			ArrayList<String> modules = new ArrayList<String>();
			
			while(rs.next()) {
				modules.add(rs.get(getModulesStatement.module));
			}
			
			return modules;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void finalizeInsertionBatch() {
		try {
			insertInstructionStatement.executeBatch();
			connection.commit();
			
			insertCount = 0;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initializePreparedStatements() throws SQLException {
		insertInstructionStatement = AtlantisQueries.mk(connection, new InsertInstruction());
		
		getInstructionStatement = AtlantisQueries.mk(connection, new SelectInstruction());
		
		getLineDataByForLine = AtlantisQueries.mk(connection, new GetLineDataForLine());
		
		getInstructionsInRangeStatement = AtlantisQueries.mk(connection, new SelectInstructionsInRange());
		
		getModulesStatement = AtlantisQueries.mk(connection, new SelectModules());
		
		getFunctionRetLineStatement = AtlantisQueries.mk(connection, new GetFunctionRetLineNumber());
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){
				refreshConnection();
				try(
					PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createInstructionTable);
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
	protected void createIndices() throws SQLException {
		createIndex(tableName, "instructionId", "InsId");
		createIndex(tableName, "firstLineNumber", "LineNum");
		createIndex(tableName, "moduleOffset", "ModOffset");
		createIndex(tableName, "instructionName", "InsName");
	}

	@Override
	public void preAbortCommitAndDropTable() {
		try {
			insertInstructionStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public long getFunctionRetLine(int moduleId, int startLineNumber){
		long retLineNumber = 0;
		GetFunctionRetLineNumber q = getFunctionRetLineStatement;
		try {
			q.setParam(q.funStartLineNumber, startLineNumber);
			q.setParam(q.moduleIdParam, moduleId);
			TypedResultSet rs = q.executeQuery();
			if(rs.next()){
			retLineNumber = rs.get(q.firstLineNumber);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retLineNumber;
		
		 
		
	}
}
