package ca.uvic.chisel.atlantis.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.functionparsing.LightweightThreadFunctionBlock;
import ca.uvic.chisel.atlantis.functionparsing.ThreadFunctionBlock;

public class ThreadFunctionBlockDbConnection extends DbConnectionManager {

	private InsertThreadFunctionBlock insertThreadFunctionBlockStatement;
	private SelectThreads getThreadsStatement;
	private PreparedStatement installTableStatement;
	private SelectLWThreadFunctionBlocksByRange getLWThreadFunctionBlocksInRangeStatement;
	private SelectLWThreadFunctionBlockForLine getLWThreadFunctionBlocksForLineStatement;
	private SelectNextLWThreadFunctionBlockForLine getNextLWThreadFunctionBlocksForLineStatement;
	
	private final InstructionDbConnection instructionDb;
	
	private int insertBlockCount;
	
	protected static final String THREAD_FUNCTION_BLOCK_TABLE_NAME = "thread_function_blocks";
	
	public ThreadFunctionBlockDbConnection(File originalFile, InstructionDbConnection instructionDb) throws Exception {
		super(THREAD_FUNCTION_BLOCK_TABLE_NAME, originalFile);
		this.instructionDb = instructionDb;
		// Can't pass arbitrary stuff into the automatic init pipeline.
		// Huh? What does that mean? I needed to put the
		// dependencyPreparedStatements into refresh, so I did.
		// this.intializeDependencyPreparedStatements();
	}
	
	public boolean insertThreadFunctionBlock(ThreadFunctionBlock block) {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			
			InsertThreadFunctionBlock q = insertThreadFunctionBlockStatement;
			q.setParam(q.startLineNumber, block.getStartLineNumber());
			q.setParam(q.threadId, block.getThreadId());
			q.setParam(q.startInstruction, block.getStartInstruction().getIdGlobalUnique());
			q.setParam(q.endInstruction, block.getEndInstruction().getIdGlobalUnique());
			// TODO This is questionable...we used -1 when id was numeric, now blank string...is that ok?
			q.setParam(q.functionStartInstruction,
					block.getFunctionStartInstruction() == null
					? new InstructionId("") : block.getFunctionStartInstruction().getIdGlobalUnique());
			q.setParam(q.xOffset, block.getXOffset());
			q.setParam(q.width, block.getWidth());
			q.setParam(q.xEndOffset, block.getXOffset() + block.getWidth());
			
			insertThreadFunctionBlockStatement.addBatch();
			
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
	
	public List<Integer> getThreads() {
		try {
			refreshConnection();
			
			TypedResultSet rs = getThreadsStatement.executeQuery();
			ArrayList<Integer> threads = new ArrayList<Integer>();
			
			while(rs.next()) {
				threads.add(rs.resultSet.getInt(1));
			}
			
			rs.close();
			
			return threads;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Provide the block containing the line provided, or if none, the block following most nearly.
	 * Useful for when a block is required for lines that are special, like thread changes. 
	 * 
	 * @param lineNumber
	 * @return
	 */
	public LightweightThreadFunctionBlock getLightweightThreadFunctionBlocksContainingOrFollowingLine(int lineNumber) {
		LightweightThreadFunctionBlock block = this.getLightweightThreadFunctionBlockForLine(lineNumber);
		if(null != block){
			return block;
		}
		
		try {
			refreshConnection();
			
			SelectNextLWThreadFunctionBlockForLine q = getNextLWThreadFunctionBlocksForLineStatement;
			
			if(q.preparedStatement.isClosed()){
				// When changing trace files, this can be closed suddenly.
				return null;
			}
			
			q.setParam(q.startLineNumberLeftBound, lineNumber);
			
			TypedResultSet rs = q.executeQuery();
			
			try{
				if(rs.next()) {
					LightweightThreadFunctionBlock tfb = new LightweightThreadFunctionBlock(
							rs.get(q.xOffset),
							rs.get(q.width),
							rs.get(q.startLineNumber),
							rs.get(q.endinst_module),
							rs.get(q.funcinst_module),
							rs.get(q.endinst_moduleId),
							rs.get(q.funcinst_moduleId),
							rs.get(q.funcinst_moduleOffset),
							rs.get(q.endinst_instructionName).equalsIgnoreCase("call"),
							rs.get(q.functionStartInstruction),
							rs.get(q.threadId)
					);
					
					rs.close();
					
					return tfb;
				}
			} catch(NullPointerException e){
				// When we are changing trace files, we get null exceptions within the simple getLong() calls
				return null;
			}
			
			return null;		
		} catch (SQLException e) {
			if(e.getMessage().equals("No operations allowed after statement closed.")){
				return null;
			} else {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	/**
	 * Find the block that contains the line. Provides null if there is no block that contains the line.
	 * See {@link ThreadFunctionBlockDbConnection#getLightweightThreadFunctionBlocksContainingOrFollowingLine(int)}
	 * if blocks are required for all lines in the trace (for example, blocks nearest to lines representing thread
	 * switches, etc.).
	 * 
	 * @param lineNumber
	 * @return
	 */
	public LightweightThreadFunctionBlock getLightweightThreadFunctionBlockForLine(int lineNumber) {
		try {
			refreshConnection();
			
			SelectLWThreadFunctionBlockForLine q = getLWThreadFunctionBlocksForLineStatement;
			
			if(q.preparedStatement.isClosed()){
				// When changing trace files, this can be closed suddenly.
				return null;
			}
			
			q.setParam(q.startLineNumberLeftBound, lineNumber);
			q.setParam(q.startLineNumberRightBound, lineNumber);
			
			TypedResultSet rs = q.executeQuery();
			
			try{
				if(rs.next()) {
					LightweightThreadFunctionBlock tfb = new LightweightThreadFunctionBlock(
							rs.get(q.xOffset),
							rs.get(q.width),
							rs.get(q.startLineNumber),
							rs.get(q.endinst_module),
							rs.get(q.funcinst_module),
							rs.get(q.endinst_moduleId),
							rs.get(q.funcinst_moduleId),
							rs.get(q.funcinst_moduleOffset),
							rs.get(q.endinst_instructionName).equalsIgnoreCase("call"),
							rs.get(q.functionStartInstruction),
							rs.get(q.threadId)
					);
					
					rs.close();
					
					return tfb;
				}
			} catch(NullPointerException e){
				// When we are changing trace files, we get null exceptions within the simple getLong() calls
				return null;
			}
			
			return null;		
		} catch (SQLException e) {
			if(e.getMessage().equals("No operations allowed after statement closed.")){
				return null;
			} else {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public ArrayList<LightweightThreadFunctionBlock> getLightweightThreadFunctionBlocks(int thread, long start, long end) {
//		long startTime = System.nanoTime();
		try {
			refreshConnection();
			
			SelectLWThreadFunctionBlocksByRange q = getLWThreadFunctionBlocksInRangeStatement;
			
			if(q.preparedStatement.isClosed()){
				// When changing trace files, this can be closed suddenly.
				return null;
			}
			
			q.setParam(q.leftXOffsetBound, end);
			q.setParam(q.rightXEndOffsetBound, start);
			q.setParam(q.threadIdParam, thread);
			
			TypedResultSet rs = q.executeQuery();
			ArrayList<LightweightThreadFunctionBlock> blocks = new ArrayList<LightweightThreadFunctionBlock>();
			try{
				while(rs.next()) {
					LightweightThreadFunctionBlock tfb = new LightweightThreadFunctionBlock(
							rs.get(q.xOffset),
							rs.get(q.width),
							rs.get(q.startLineNumber),
							rs.get(q.endinst_module),
							rs.get(q.funcinst_module),
							rs.get(q.endinst_moduleId),
							rs.get(q.funcinst_moduleId),
							rs.get(q.funcinst_moduleOffset),
							rs.get(q.endinst_instructionName).equalsIgnoreCase("call"),
							rs.get(q.functionStartInstruction),
							rs.get(q.threadId)
					);
					
					blocks.add(tfb);
				}
			} catch(NullPointerException e){
				// When we are changing trace files, we get null exceptions within the simple getLong() calls
				return null;
			}
			
//			long endTime = System.nanoTime();
//			double seconds = (double)(endTime - startTime) / 1000000000.0;
//			System.err.println("getLWThreadFunctionBlocksInRange: " + seconds + " (" + (endTime - startTime) + ")");
			
			return blocks;		
		} catch (SQLException e) {
			if(e.getMessage().equals("No operations allowed after statement closed.")){
				return null;
			} else {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public void cancelThreadFuncBlockQueries(){
		// Both may be fresh (no previous job),
		// but we can still try and fail to cancel.
		try{
			getLWThreadFunctionBlocksInRangeStatement.cancel();
		} catch(SQLException e){
			 // Perhaps there isn't a valid one to cancel.
		} catch(NullPointerException e){
			// Gets set to null after long waits?
		}
	}
	
	public void finalizeInsertionBatch() {
		try {
			refreshConnection(); // Can close via timeout, this safely opens only if closed already.
			insertThreadFunctionBlockStatement.executeBatch();
			connection.commit();
			
			insertBlockCount = 0;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void intializeDependencyPreparedStatements() throws SQLException {		
		getLWThreadFunctionBlocksInRangeStatement = AtlantisQueries.mk(connection, new SelectLWThreadFunctionBlocksByRange());
		
		getLWThreadFunctionBlocksForLineStatement = AtlantisQueries.mk(connection, new SelectLWThreadFunctionBlockForLine());
		
		getNextLWThreadFunctionBlocksForLineStatement = AtlantisQueries.mk(connection, new SelectNextLWThreadFunctionBlockForLine());
	}
	
	@Override
	public void initializePreparedStatements() throws SQLException {
		insertThreadFunctionBlockStatement = AtlantisQueries.mk(connection, new InsertThreadFunctionBlock());
		getThreadsStatement = AtlantisQueries.mk(connection, new SelectThreads());
		
		this.intializeDependencyPreparedStatements();
	}

	@Override
	public boolean createTables() {
		try{	
			TableState state = this.getTableState();
			
			if(state == TableState.MISSING){			
				
				refreshConnection();
				
				installTableStatement = AtlantisQueries.mk(connection, AtlantisQueries.createThreadFunctionTable);
				
				installTableStatement.executeUpdate();
				connection.commit();
				
				this.saveTableStateToMetaDataTable(TableState.CREATED);
				return true;
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
			insertThreadFunctionBlockStatement.cancel();
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createIndices() throws SQLException {
		// do nothing
	}

}
