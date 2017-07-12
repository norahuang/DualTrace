package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ca.uvic.chisel.atlantis.functionparsing.Instruction;

public class JumpDbConnection extends DbConnectionManager {

	private InsertJump insertJumpStatement;
	private int insertJumpCount = 0;
	
	protected static final String JUMP_TABLE_NAME = "function_jumps";
	
	public JumpDbConnection(File originalFile) throws Exception {
		super(JUMP_TABLE_NAME, originalFile);
	}

	public boolean insertJump(Instruction fromLastInstructionOfFirstBasicBlock, Instruction toFirstInstructionOfNextBasicBloc, boolean branchTaken) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertJump q = insertJumpStatement;
			q.setParam(q.jumpId, insertJumpCount);
			q.setParam(q.jumpInstruction, fromLastInstructionOfFirstBasicBlock.getIdGlobalUnique());
			q.setParam(q.targetInstruction, toFirstInstructionOfNextBasicBloc.getIdGlobalUnique());
			q.setParam(q.branchTaken, branchTaken);
//			insertJumpStatement.setBoolean(i++, branchTaken); // for the ON DUPLICATE occasion
			// Oh, actually, whenever one basic block follows another, it is strictly either branched to or not. No on duplicate needed.
			
			insertJumpStatement.addBatch();
			
			insertJumpCount++;
			if((insertJumpCount % 1000) == 0) {
				finalizeInsertionBatch();
			}
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void finalizeInsertionBatch() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertJumpStatement.executeBatch();
			connection.commit();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertJumpStatement = AtlantisQueries.mk(connection, new InsertJump());
		
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){	
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createJumpTable);
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
			insertJumpStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIndices() throws SQLException {
		createIndex(tableName, "jumpId", "JumpId");
		createIndex(tableName, "jumpInstruction", "JumpIns");
		createIndex(tableName, "targetInstruction", "TargIns");
		createIndex(tableName, "branchTaken", "Branch");
	}

}
