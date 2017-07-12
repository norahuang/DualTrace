package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import ca.uvic.chisel.atlantis.functionparsing.BasicBlock;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;

public class BasicBlockDbConnection extends DbConnectionManager {

	private InsertBasicBlock insertBasicBlockStatement;
	private int insertBlockCount;
	
	protected static final String BASICBLOCK_TABLE_NAME = "function_basic_blocks";
	
	public BasicBlockDbConnection(File originalFile) throws Exception {
		super(BASICBLOCK_TABLE_NAME, originalFile);
	}
	
	/**
	 * Unused, but could perhaps be useful.
	 * 
	 * @param block
	 * @param jumpDb
	 * @param instructionDb
	 * @return
	 */
	@Deprecated// Not used anymore. Might be useful later.
	public boolean retrieveSuccessors(BasicBlock block, JumpDbConnection jumpDb, InstructionDbConnection instructionDb) {
		try {
			RetrieveSuccessors q = AtlantisQueries.mk(connection, new RetrieveSuccessors());
			
			q.setParam(q.jumpInstruction, block.getEnd().getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			
			while(rs.next()) {
				Instruction start = new Instruction(rs.get(q.startInstruction), rs.get(q.i1_firstLineNumber),
						rs.get(q.i1_instructionName), rs.get(q.i1_instructionText), rs.get(q.i1_module),
						rs.get(q.i1_moduleId), rs.get(q.i1_moduleOffset), null,
						rs.get(q.i1_parentFunctionId));
				Instruction end = new Instruction(rs.get(q.endInstruction), rs.get(q.i2_firstLineNumber),
						rs.get(q.i2_instructionName), rs.get(q.i2_instructionText), rs.get(q.i2_module),
						rs.get(q.i2_moduleId), rs.get(q.i2_moduleOffset), null,
						rs.get(q.i2_parentFunctionId));
				
				BasicBlock newSuccessor = new BasicBlock(start, end, rs.get(q.length));
				
				block.addSuccessor(newSuccessor);
			}
			
			rs.close();
			q.preparedStatement.close();
			return true;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Deprecated// Not used anymore. Might be useful later.
	public boolean tryoutRetrieveSuccessors(BasicBlock block, JumpDbConnection jumpDb, InstructionDbConnection instructionDb) {
		try {
			// Use of static instance of AtlantisQueries is temporary, will later move each query class outside of that class,
			// keeping it in the same file though.
			RetrieveSuccessors q = AtlantisQueries.mk(connection, new RetrieveSuccessors());
			q.prepare(connection);
			
			q.setParam(q.jumpInstruction, block.getEnd().getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			
			while(rs.resultSet.next()) {
				Instruction start = new Instruction(rs.get(q.startInstruction), rs.get(q.i1_firstLineNumber),
						rs.get(q.i1_instructionName), rs.get(q.i1_instructionText), rs.get(q.i1_module),
						rs.get(q.i1_moduleId), rs.get(q.i1_moduleOffset), null,
						rs.get(q.i1_parentFunctionId));
				Instruction end = new Instruction(rs.get(q.endInstruction), rs.get(q.i2_firstLineNumber),
						rs.get(q.i2_instructionName), rs.get(q.i2_instructionText), rs.get(q.i2_module),
						rs.get(q.i2_moduleId), rs.get(q.i2_moduleOffset), null,
						rs.get(q.i2_parentFunctionId));
				
				BasicBlock newSuccessor = new BasicBlock(start, end, rs.get(q.length));
				block.addSuccessor(newSuccessor);
			}
			
			rs.close();
			q.preparedStatement.close();
			return true;
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public BasicBlock getDisconnectedBasicBlockByStartInstruction(Instruction startInstruction, InstructionDbConnection instructionDb) {
		try {
			
			GetDisconnectedBasicBlocks q = AtlantisQueries.mk(connection, new GetDisconnectedBasicBlocks());

			q.setParam(q.startInstructionParam, startInstruction.getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			
			if(!rs.next()) {
				System.err.println("Error requesting first basic block of function.");
				return null;
			}
			
			Instruction start = new Instruction(rs.get(q.startInstruction), 0,
					rs.get(q.i1_instructionName), "", rs.get(q.i1_module),
					rs.get(q.i1_moduleId), rs.get(q.i1_moduleOffset), null, rs.get(q.i1_parentFunctionId));
			Instruction end = new Instruction(rs.get(q.endInstruction), 0,
					rs.get(q.i2_instructionName), "", rs.get(q.i2_module),
					rs.get(q.i2_moduleId), rs.get(q.i2_moduleOffset), null, rs.get(q.i2_parentFunctionId));
			
			BasicBlock result = new BasicBlock(start, end, rs.get(q.length));
			
			rs.close();
			q.preparedStatement.close();
			return result;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<BasicBlock> getDisconnectedSuccessors(BasicBlock source, JumpDbConnection jumpDb, InstructionDbConnection instructionDb) {
		try {
			FetchDisconnectedSuccessors q = AtlantisQueries.mk(connection, new FetchDisconnectedSuccessors());
			
			q.setParam(q.jump_jumpInstruction, source.getEnd().getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			
			List<BasicBlock> results = new LinkedList<BasicBlock>();
			while(rs.next()) {
				Instruction start = new Instruction(rs.get(q.startInstruction), 0,
						rs.get(q.i1_instructionName), "", rs.get(q.i1_module),
						rs.get(q.i1_moduleId), rs.get(q.i1_moduleOffset), null,
						rs.get(q.i1_parentFunctionId));
				Instruction end = new Instruction(rs.get(q.endInstruction), 0,
						rs.get(q.i2_instructionName), "", rs.get(q.i2_module),
						rs.get(q.i2_moduleId), rs.get(q.i2_moduleOffset), null,
						rs.get(q.i2_parentFunctionId));
				
				BasicBlock block = new BasicBlock(start, end, rs.get(q.length));
				block.setLoadedAsBranchTaken(rs.get(q.jump_branchTaken));
				
				results.add(block);
			}
			
			rs.close();
			q.preparedStatement.close();
			return results;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Deprecated // Not used anymore, but might be useful later.
	public BasicBlock getFirstPredecessor(BasicBlock child, JumpDbConnection jumpDb) {
		try {
			FetchFirstPredecessor q = AtlantisQueries.mk(connection, new FetchFirstPredecessor());
			
			q.setParam(q.targetInstruction, child.getStart().getIdGlobalUnique());
			
			TypedResultSet rs = q.executeQuery();
			
			if(!rs.next()) {
				return null;
			}
			
			Instruction start = new Instruction(rs.get(q.startInstruction), 0, "", "", "", 0, 0, null, null);
			Instruction end = new Instruction(rs.get(q.endInstruction), 0, "", "", "", 0, 0, null, null);
			
			BasicBlock firstPredecessor = new BasicBlock(start, end, rs.get(q.length));
			
			rs.close();
			q.preparedStatement.close();
			return firstPredecessor;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean insertBasicBlock(BasicBlock block) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertBasicBlock q = insertBasicBlockStatement;
			q.setParam(q.startInstruction, block.getStart().getIdGlobalUnique());
			q.setParam(q.endInstruction, block.getEnd().getIdGlobalUnique());
			q.setParam(q.length, block.getLength());
			
			insertBasicBlockStatement.addBatch();
			
			insertBlockCount++;
			if((insertBlockCount % 1000) == 0) {
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
			insertBasicBlockStatement.executeBatch();
			connection.commit();
			
			insertBlockCount = 0;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertBasicBlockStatement = AtlantisQueries.mk(connection, new InsertBasicBlock());
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){			
				try(
				PreparedStatement installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createBasicBlockTabl);
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
			insertBasicBlockStatement.cancel();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIndices() throws SQLException {
		createIndex(tableName, "startInstruction", "BlockStartIns");
		createIndex(tableName, "endInstruction", "BlockEndIns");
		createIndex(tableName, "length", "BlockLength");
	}

}
