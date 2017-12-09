package ca.uvic.chisel.atlantis.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

import ca.uvic.chisel.atlantis.datacache.BinaryFormatFileModelDataLayer;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;

/**
 * Atlantis/Gibraltar Database Overview
 * 
 * Each binary trace file set is processed by Gibraltar into an SQLite database file,
 * which is then used by Atlantis for many parts of its functionality.
 * 
 * The tables include assembly_events, filemetadata, thread_function_blocks,
 * thread_lengths. The assembly_events table is partially deprecated alongside the Assembly Events View,
 * but will probably be useful to keep anyway. The filemetadata table is useful for bookkeeping.
 * 
 * Additional tables are related to functions, and might offer the greatest leverage for future
 * features: function_basicblocks, function_calls, function_jumps, functions. These abstract over
 * trace lines to identify functions, and related components that functions consist of analytically.
 * 
 * The most important table is arguable the memory_snapshot_delta_tree, the data structure that allows
 * for very fast reconstruction of the complete memory state for large files.
 * 
 * Deprecated tables include filedatatablestatus, trace_file_text_lines, and thread_change_events.
 * As mentioned above, assemvly_events is partially deprecated.
 * 
 * Many tables have instruction id columns, which may be joined with each other for various purposes.
 * Some have trace line number columns. The difference between the two are that a repeating instruction
 * (say, in a loop) will have only one instruction id, but will have multiple line numbers associated
 * with it.
 * 
 * Besides the database, Atlantis still accesses the binary format files for various pieces of information.
 * The database is therefore not the sole datasource for Atlantis. Whenever possible and efficient enough,
 * using the binary format for data should be preferred.
 * 
 */

/**
 * Consider moving all tables names into this class. Also consider using
 * reflection to make sure that the field names stay the same as the String
 * table names therein. This would allow easier reading and usage of the queries
 * in SQL environments, and make documentation that much easier. It would
 * somewhat dissociate tables from their owning classes though.
 *
 */
class TableNames {

}

/**
 * There is funny inheritance in here (AtlantisQueries class inheritance chain
 * all the way to the first non-TypedQuery class). It is purely to allow a
 * single commit to have a cleaner diff. It may be removed thereafter.
 *
 */
/**
 * These are legacy tables from when multiple traces were entered into a MySQL
 * database. The system now relies on an individual SQLite database file per
 * trace. It should be possible to remove this 'filedatatablestatus' table, and
 * all dependencies on it. Until then, it still needs to be created and logical
 * requirements met (completed tables set to be 'populated'). The
 * 'filemetatable' on the other hand is likely still needed, so that we can
 * label a trace database file as completed. There is not another way to know if
 * the Gibraltar processing has completed or not.
 */
class TableStateQueries extends TableNames {
	// I should test all these to see if they are still needed in SQLite.
	/**
	 * {@link DbConnectionManager}
	 */
	public static final String queryTableState = "SELECT status FROM " + DbConnectionManager.FILE_DATA_STATUS_TABLE
			+ " WHERE tableName LIKE :tableName";

	public static final String registerTraceFile = "INSERT INTO " + DbConnectionManager.FILE_META_DATA_TABLE
			+ " (traceState) VALUES(?)";

	public static final String removeTrace = "DELETE FROM " + DbConnectionManager.FILE_META_DATA_TABLE;

	public static final String updateFileDataTable = "REPLACE INTO " + DbConnectionManager.FILE_DATA_STATUS_TABLE
			+ " (tablename, filename, status, entryDate) VALUES (?,?,?, CURRENT_TIMESTAMP)";

	public static final String removeMetaData = "DELETE FROM " + DbConnectionManager.FILE_DATA_STATUS_TABLE
			+ " WHERE tableName LIKE ?";

	public static final String updateMetaTable = "UPDATE " + DbConnectionManager.FILE_META_DATA_TABLE
			+ " SET traceState = ?";
}

/**
 * The 'assembly_events' table,
 * {@link AssemblyEventDbConnection#ASSEMBLY_EVENT_TABLE_NAME}, is a linear
 * record of sections of the trace line's originating assembly or module, that
 * is, for example, which dll a section of the trace originates from. When a
 * system call is made, the lines may originate in ntdll.dll, for example. This
 * table contains the start of each section, and its length. The pixel fields
 * are used in the Assembly View, a deprecated view that shows the sections as
 * non-overlapping horizontal colored blocks, with vertical arcs between them at
 * transition points. The Function Module view is a dominant view, with higher
 * quality and more useful information.
 * 
 * If the Assembly View is removed, this table might be kept, but should at
 * least have the pixel fields removed from it. We probably still want to have
 * available the thread line coverages, and also the list of assembly names
 * (thought that might be collected elsewhere).
 * 
 * startLineNum: - is can be joined into many other tables. id: - does not
 * intentionally join with other tables, and identifies the assembly module
 * uniquely. assemblyTypeId: - appears to be a deprecated field of unknown
 * origin. See {@link AssemblyEventType}.
 *
 */
class CreateAssemblyTable extends TableStateQueries {
	/**
	 * {@link AssemblyEventDbConnection}
	 */
	public static final String createAssemblyTable = "CREATE TABLE "
			+ AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " (" + " startLineNum INT PRIMARY KEY NOT NULL,"
			+ " numLines INT NOT NULL," + " id VARCHAR(255) NOT NULL," + " assemblyTypeId INT NOT NULL,"
			+ " viewStartXPixel INT NOT NULL, " + " viewEndXPixel INT NOT NULL" + " )";
}

class InsertAssemblyEventMarker extends TypedQuery {
	IntegerParameter startLineNum = new IntegerParameter("startLineNum");
	IntegerParameter numLines = new IntegerParameter("numLines");
	StringParameter id = new StringParameter("id");
	IntegerParameter assemblyTypeId = new IntegerParameter("assemblyTypeId");
	IntegerParameter viewStartXPixel = new IntegerParameter("viewStartPixel");
	IntegerParameter viewEndXPixel = new IntegerParameter("viewEndPixel");
	{
		this.q = "INSERT INTO " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME
				+ " (startLineNum, numLines, id, assemblyTypeId, viewStartXPixel, viewEndXPixel) " + "VALUES" + " (:"
				+ startLineNum + ", :" + numLines + ", :" + id + ", :" + assemblyTypeId + ", :" + viewStartXPixel
				+ ", :" + viewEndXPixel + ")";
	}
}

class FindNumberfMarkers extends TypedQuery {
	{
		// if this works poorly with *, then use:
		// startLineNum+", "+numLines+", "+id+", "+assemblyTypeId+",
		// "+viewStartXPixel+", "+viewEndXPixel
		this.q = "SELECT COUNT(*) FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME;
	}
}

/**
 * Serves as common {@link TypedResult} provider for assembly table SELECT *
 * queries.
 *
 */
class AssemblyEventAllSelectFields extends TypedQuery {
	String associatedTable = AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME;
	IntegerResult startLineNum = new IntegerResult("startLineNum");
	IntegerResult numLines = new IntegerResult("numLines");
	StringResult id = new StringResult("id");
	IntegerResult assemblyTypeId = new IntegerResult("assemblyTypeId");
	IntegerResult viewStartXPixel = new IntegerResult("viewStartXPixel");
	IntegerResult viewEndXPixel = new IntegerResult("viewEndPixel");
}

// 1 interesting
/**
 * Result fields in {@link AssemblyEventAllSelectFields}
 */
class FindAssemblyMarkerByLine extends TypedQuery {
	IntegerParameter leftBoundary = new IntegerParameter("leftBoundary");
	IntegerParameter rightBoundary = new IntegerParameter("rightBoundary");
	{
		this.q = "SELECT * FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " AS t1" + " WHERE :"
				+ leftBoundary + " <= t1.startLineNum AND t1.startLineNum <= :" + rightBoundary;
	}
}

// 2 interesting
/**
 * Result fields in {@link AssemblyEventAllSelectFields}
 */
class FindAssemblyMarkerContainingLine extends TypedQuery {
	IntegerParameter leftInnerBoundary = new IntegerParameter("leftInnerBoundary");
	IntegerParameter rightInnerBoundary = new IntegerParameter("rightInnerBoundary");
	{
		this.q = "SELECT * FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " AS t1"
				+ " WHERE t1.startLineNum <= :" + leftInnerBoundary + " AND :" + rightInnerBoundary
				+ " <= t1.startLineNum + t1.numLines";
	}
}

// 3 interesting
/**
 * Result fields in {@link AssemblyEventAllSelectFields}
 */
class FindAssemblyMarkerByPixel extends TypedQuery {
	IntegerParameter leftBoundary = new IntegerParameter("leftBoundary");
	IntegerParameter rightBoundary = new IntegerParameter("rightBoundary");
	{
		this.q = "SELECT * FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " AS t1" + " WHERE :"
				+ leftBoundary + " >= t1.viewStartXPixel AND t1.viewEndXPixel >= :" + rightBoundary;
	}
}

class FindDistinctAssemblyNames extends TypedQuery {
	{
		this.q = "SELECT DISTINCT(t1.id) FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " AS t1 ";
	}
}

class FindAssemblyEventEndpoint extends TypedQuery {
	{
		this.q = "SELECT MAX(t1.viewEndXPixel) FROM " + AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " AS t1 ";
	}
}

/**
 * The 'function_basic_blocks' table
 * {@link BasicBlockDbConnection#BASICBLOCK_TABLE_NAME} contains information
 * about what repeating sections of code in the trace comprise functions.
 * Wikipedia can put it better than I, but a basic block is a piece of code that
 * cannot be jumped into or out of, and will always be executed in order
 * whenever the program is run, if the basic block's first instruction is run at
 * all. A basic block may be either run or not run, because they are often
 * delimited by a conditional jump instruction preceding them. In terms of
 * source code, it would be a chunk of code with no return value, and ending
 * whenever a conditional was encountered.
 * 
 * This table is used by at least two views currently, the Function
 * Decomposition view (graph of basic blocks of a selected function), and the
 * Function Module View (shows a function sequence chart color coded and labeled
 * by the assembly module the function belongs to).
 * 
 * Detection of functions is not a solved problem. Some functions cannot be
 * detected and reconstructed from source code, but their code will nonetheless
 * be present in the analysis. They will simply not be accessible as a function
 * per se.
 * 
 * startInstruction: - can be joined against any instruction id in any other
 * table. It represents the first instruction that is executed for this block,
 * that is, not a call, but the instruction following a call, or following a
 * jump. endInstruction: - can be joined against any instruction id in any other
 * table. it represents the final instruction that is executed for this block,
 * likely a ret, call, or jump. length - the number of lines in the basic block
 */
class CreateBasicBlockTable extends CreateAssemblyTable {
	/**
	 * {@link BasicBlockDbConnection}
	 */
	public static final String createBasicBlockTabl = "CREATE TABLE " + BasicBlockDbConnection.BASICBLOCK_TABLE_NAME
			+ " (" + " startInstruction VARCHAR(100) PRIMARY KEY NOT NULL, " + " endInstruction VARCHAR(100) NOT NULL, "
			+ " length INT NOT NULL" + " )";
}

class InsertBasicBlock extends TypedQuery {
	InstructionIdParameter startInstruction = new InstructionIdParameter("startInstruction");
	InstructionIdParameter endInstruction = new InstructionIdParameter("endInstruction");
	IntegerParameter length = new IntegerParameter("length");
	{
		q = "INSERT OR IGNORE INTO " + BasicBlockDbConnection.BASICBLOCK_TABLE_NAME
				+ " (startInstruction,endInstruction,length) VALUES(:" + startInstruction + ",:" + endInstruction + ",:"
				+ length + ")";
	}
}

/**
 * {@link TypedResult} in common to several Basic Block table queries.
 *
 */
class BasicBlockCommonResults extends TypedQuery {
	public final InstructionIdResult startInstruction = new InstructionIdResult("startInstruction");
	public final InstructionIdResult endInstruction = new InstructionIdResult("endInstruction");
	public final StringResult i1_instructionName = new StringResult("i1.instructionName");
	public final StringResult i2_instructionName = new StringResult("i2.instructionName");
	public final StringResult i1_module = new StringResult("i1.module");
	public final StringResult i2_module = new StringResult("i2.module");
	public final IntegerResult i1_moduleId = new IntegerResult("i1.moduleId");
	public final IntegerResult i2_moduleId = new IntegerResult("i2.moduleId");
	public final LongResult i1_moduleOffset = new LongResult("i1.moduleOffset");
	public final LongResult i2_moduleOffset = new LongResult("i2.moduleOffset");
	public final IntegerResult length = new IntegerResult("length");
	public final LongResult i1_firstLineNumber = new LongResult("i1.firstLineNumber");
	public final LongResult i2_firstLineNumber = new LongResult("i2.firstLineNumber");
	public final StringResult i1_instructionText = new StringResult("i1.instructionText");
	public final StringResult i2_instructionText = new StringResult("i2.instructionText");
	public final InstructionIdResult i1_parentFunctionId = new InstructionIdResult("i1.parentFunctionId");
	public final InstructionIdResult i2_parentFunctionId = new InstructionIdResult("i2.parentFunctionId");
}

// 4 interesting
/**
 * Fetches the basic block starting with the specified instruction, as well as
 * the instruction data for both that starting instruction as well as for the
 * end instruction of the block.
 *
 */
class GetDisconnectedBasicBlocks extends BasicBlockCommonResults {
	InstructionIdParameter startInstructionParam = new InstructionIdParameter("startInstruction");

	{
		q = "SELECT " + startInstruction + "," + endInstruction + "," + i1_instructionName + ", " + i2_instructionName
				+ ", " + i1_module + ", " + i2_module + ", " + i1_moduleId + ", " + i2_moduleId + ", " + i1_moduleOffset
				+ ", " + i2_moduleOffset + ", " + i1_parentFunctionId + ", " + i2_parentFunctionId + ", " + length + " "
				+ " FROM " + BasicBlockDbConnection.BASICBLOCK_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS i1 ON i1.instructionId = startInstruction"
				+ " LEFT JOIN " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS i2 ON i2.instructionId = endInstruction" + " WHERE startInstruction = :" + startInstructionParam;
	}
}

// 5 interesting
/**
 * Fetches basic block and associated start and end instructions where the start
 * instruction matches the jump instruction specified. used in the Function
 * Recomposition view, which renders a graph of basic blocks that compose a
 * target function. This query provides those basic blocks. Joining with the
 * function_jumps table provides all possible routes through the function, as
 * executed for the trace. Without the function_jumps table, we cannot know what
 * blocks follow from what other blocks; the one to many nature of that table
 * results in branching structures from conditional instructions.
 *
 */
@Deprecated
class FetchDisconnectedSuccessors extends BasicBlockCommonResults {
	BooleanResult jump_branchTaken = new BooleanResult("jump.branchTaken");
	InstructionIdParameter jump_jumpInstruction = new InstructionIdParameter("jump_jumpInstruction");
	{
		q = "SELECT " + startInstruction + "," + endInstruction + "," + i1_instructionName + ", " + i2_instructionName
				+ ", " + i1_module + ", " + i2_module + ", " + i1_moduleId + ", " + i2_moduleId + ", " + i1_moduleOffset
				+ ", " + i2_moduleOffset + ", " + i1_parentFunctionId + ", " + i2_parentFunctionId + ", " + length
				+ ", " + jump_branchTaken + " " + " FROM " + BasicBlockDbConnection.BASICBLOCK_TABLE_NAME
				+ " LEFT JOIN " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS i1 ON i1.instructionId = startInstruction" + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS i2 ON i2.instructionId = endInstruction"
				+ " LEFT JOIN " + JumpDbConnection.JUMP_TABLE_NAME + " AS jump ON jump.jumpInstruction = :"
				+ jump_jumpInstruction + " WHERE startInstruction = jump.targetInstruction";
	}
}

// 6 interesting
/**
 * Fetches basic block that ends with a jump instruction where that jump goes to
 * our specified target instruction. Not currently used (method that references
 * is deprecated and not referenced anywhere).
 *
 */
@Deprecated
class FetchFirstPredecessor extends BasicBlockCommonResults {
	InstructionIdParameter targetInstruction = new InstructionIdParameter("targetInstruction");

	{
		q = "SELECT " + startInstruction + ", " + endInstruction + ", " + length + " " + " FROM "
				+ BasicBlockDbConnection.BASICBLOCK_TABLE_NAME + ", " + JumpDbConnection.JUMP_TABLE_NAME
				+ " WHERE endInstruction = jumpInstruction AND" + " targetInstruction = :" + targetInstruction
				+ " LIMIT 1";
	}
}

/**
 * Nearly identical to {@link FetchDisconnectedSuccessors}, differs only in
 * selected fields.
 *
 */
@Deprecated
class RetrieveSuccessors extends BasicBlockCommonResults {
	InstructionIdParameter jumpInstruction = new InstructionIdParameter("jumpInstruction");

	// 7 interesting
	{
		this.q = "SELECT " + startInstruction + "," + endInstruction + "," + length + "," + i1_firstLineNumber + ","
				+ i2_firstLineNumber + "," + i1_moduleOffset + "," + i2_moduleOffset + "," + i1_module + "," + i2_module
				+ "," + i1_instructionName + "," + i2_instructionName + "," + i1_instructionText + ","
				+ i2_instructionText + "," + i1_parentFunctionId + ", " + i2_parentFunctionId + ", " + i1_moduleId + ","
				+ i2_moduleId + " " + " FROM " + BasicBlockDbConnection.BASICBLOCK_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS i1 ON i1.instructionId = startInstruction"
				+ " LEFT JOIN " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS i2 ON i2.instructionId = endInstruction" + " LEFT JOIN " + JumpDbConnection.JUMP_TABLE_NAME
				+ " ON targetInstruction = startInstruction" + " WHERE jumpInstruction = :" + jumpInstruction;
	}
}

/**
 * The 'function_calls' table {@link BasicBlockDbConnection#CALL_TABLE_NAME}
 * contains listing of all 'call' instructions. All three columns are
 * instruction ids, and therefore can join on any table with instruction ids in
 * it. There are no active uses of this table, but it is simple and could be
 * useful later.
 * 
 * callInstruction: - the instruction id where the 'call' instruction occurs
 * callingFunctionStart: - the instruction id pointing to the function in which
 * this 'call' instruction occurs. That is, the owning/parent function to the
 * callInstruction. calledFunctionStart: - the instruction id for the function
 * that is being called
 *
 */
class CreateCallLineTable extends CreateBasicBlockTable {
	/**
	 * {@link CallDbConnection}
	 */
	public static final String createCallDbTable = "CREATE TABLE " + CallDbConnection.CALL_TABLE_NAME + " ("
			+ " callInstruction VARCHAR(100) PRIMARY KEY NOT NULL, " + " callingFunctionStart VARCHAR(100) NOT NULL, "
			+ " calledFunctionStart VARCHAR(100) NOT NULL" + " )";
}

class InsertCall extends TypedQuery {
	InstructionIdParameter callInstruction = new InstructionIdParameter("callInstruction");
	InstructionIdParameter callingFunctionStart = new InstructionIdParameter("callingFunctionStart");
	InstructionIdParameter calledFunctionStart = new InstructionIdParameter("calledFunctionStart");
	{
		q = "INSERT OR IGNORE INTO " + CallDbConnection.CALL_TABLE_NAME
				+ " (callInstruction, callingFunctionStart, calledFunctionStart) " + "VALUES(:" + callInstruction
				+ ", :" + callingFunctionStart + ", :" + calledFunctionStart + ")";
	}
}

class CountCalls extends TypedQuery {
	{
		q = "SELECT COUNT(*) FROM " + CallDbConnection.CALL_TABLE_NAME;
	}
}

/**
 * Queries to determine if a trace file has been properly processed. The
 * checkTableExists is only used in legacy MySQL using code, which is of
 * questionable value. We are unlikely to go back to MySQL.
 *
 */
class TableExistsQueries extends CreateCallLineTable {
	/**
	 * {@link DbBackendSQLite}
	 */
	// public static final String checkFileExistsinDb =
	// "SELECT traceState FROM " + DbConnectionManager.FILE_META_DATA_TABLE+
	// " WHERE fileName = '"+traceFileFolder.toString()+"'");

	public static final String checkFileExistsinDb = "SELECT traceState FROM "
			+ DbConnectionManager.FILE_META_DATA_TABLE;

	/**
	 * MySQL only. SQLite does not need this.
	 */
	public static final String checkTableExists = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name = ?";
}

@Deprecated
class DiffQueries extends TableExistsQueries {
	/**
	 * {@link DiffResultsDbConnection}
	 */
	public static final String createDiffTable = "CREATE TABLE " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME
			+ " (" + " regionNumber INT PRIMARY KEY NOT NULL," + " leftStartLine INT NOT NULL,"
			+ " leftEndLine INT NOT NULL," + " leftOffset LONG NOT NULL," + " leftLength INT NOT NULL,"
			+ " rightStartLine INT NOT NULL," + " rightEndLine INT NOT NULL," + " rightOffset LONG NOT NULL,"
			+ " rightLength INT NOT NULL," + " isMatch BOOLEAN NOT NULL" + // 255
																			// showed
																			// truncated
																			// lines+
			" )";

	@Deprecated
	public static final String insertDiffRegion = "INSERT INTO " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME
			+ " (regionNumber, leftStartLine, leftEndLine, leftOffset, leftLength, rightStartLine, rightEndLine, rightOffset, rightLength, isMatch) VALUES(?,?,?,?,?,?,?,?,?,?)";

	@Deprecated
	public static final String getMatchingRegion = "SELECT * FROM " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME
			+ " AS t1" + " WHERE isMatch = true";

	@Deprecated
	public static final String getDifferentRegions = "SELECT * FROM " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME
			+ " AS t1" + " WHERE isMatch = false";

	@Deprecated
	public static final String fetchAllRegions = "SELECT * FROM " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME;

	@Deprecated
	public static final String fetchRegionByNumber = "SELECT * FROM " + DiffResultsDbConnection.DIFF_RESULTS_TABLE_NAME
			+ " AS t1" + " WHERE ? = t1.regionNumber";
}

/**
 * The 'functions' table {@link BasicBlockDbConnection#FUNCTION_TABLE_NAME} is
 * used to store the first instruction for all identified functions, as well as
 * the name of the function (if discovered from a dll or assigned by the user in
 * Atlantis after parsing).
 * 
 * firstInstruction: - the instruction id corresponding to the first instruction
 * of the function name: - the name of the function derived from suer input or
 * from user provided dll startsThread: - indicates whether the function starts
 * a new thread unknownStart: - the start address of this function is the
 * instruction entered at from a return which had no address on the known stack.
 * If this is set, so should oddReturn. oddReturn: - this function contains an
 * instruction that was returned to without a return address on the known stack
 *
 */
class CreateFunctionTable extends DiffQueries {
	/**
	 * {@link FunctionDbConnection}
	 */
	public static final String createFunctionTable = "CREATE TABLE " + FunctionDbConnection.FUNCTION_TABLE_NAME + " ("
			+ " firstInstruction VARCHAR(100) PRIMARY KEY NOT NULL, " + " name VARCHAR(100) NOT NULL, "
			+ " startsThread BIT NOT NULL, " + " unknownStart BIT NOT NULL, " + " oddReturn BIT NOT NULL " + " )";
}

class InsertFunction extends TypedQuery {
	InstructionIdParameter firstInstruction = new InstructionIdParameter("firstInstruction");
	StringParameter name = new StringParameter("name");
	BooleanParameter startsThread = new BooleanParameter("startsThread");
	BooleanParameter unknownStart = new BooleanParameter("unknownStart");
	BooleanParameter oddReturn = new BooleanParameter("oddReturn");

	{
		this.q = "INSERT OR IGNORE INTO " + FunctionDbConnection.FUNCTION_TABLE_NAME
				+ " (firstInstruction, name, startsThread, unknownStart, oddReturn) " + "VALUES(:" + firstInstruction
				+ ", :" + name + ", :" + startsThread + ", :" + unknownStart + ", :" + oddReturn + ")";
	}
}

@Deprecated
class UpdateFunction extends TypedQuery {
	StringParameter name = new StringParameter("name");
	BooleanParameter startsThread = new BooleanParameter("startsThread");
	BooleanParameter unknownStart = new BooleanParameter("unknownStart");
	BooleanParameter oddReturn = new BooleanParameter("oddReturn");
	InstructionIdParameter firstInstruction = new InstructionIdParameter("firstInstruction");
	{
		this.q = "UPDATE " + FunctionDbConnection.FUNCTION_TABLE_NAME + " SET name = :" + name + ", startsThread = :"
				+ startsThread + ", unknownStart = :" + unknownStart + ", oddReturn = :" + oddReturn
				+ " WHERE firstInstruction = :" + firstInstruction;
	}
}

/**
 * Common {@link TypedResult} columns for the 'functions' table.
 *
 */
class CommonFunctionResults extends TypedQuery {
	public final StringResult name = new StringResult("name");
	public final BooleanResult startsThread = new BooleanResult("startsThread");
	public final BooleanResult unknownStart = new BooleanResult("unknownStart");
	public final BooleanResult oddReturn = new BooleanResult("oddReturn");
	public final InstructionIdResult firstInstruction = new InstructionIdResult("firstInstruction");
	public final LongResult inst_firstLineNumber = new LongResult("inst.firstLineNumber");
	public final StringResult inst_module = new StringResult("inst.module");
	public final IntegerResult inst_moduleId = new IntegerResult("inst.moduleId");
	public final LongResult inst_moduleOffset = new LongResult("inst.moduleOffset");
	public final StringResult inst_instructionName = new StringResult("inst.instructionName");
	public final StringResult inst_instructionText = new StringResult("inst.instructionText");
	public final InstructionIdResult inst_parentFunctionId = new InstructionIdResult("inst.parentFunctionId");

}

class CountFunctions extends TypedQuery {
	{
		this.q = "SELECT COUNT(*) FROM " + FunctionDbConnection.FUNCTION_TABLE_NAME;
	}
}

// 8 interesting
/**
 * Fetch all functions from the specified module (i.e. dll).
 *
 */
class SelectFunctionsFromModule extends CommonFunctionResults {
	StringParameter inst_moduleParam = new StringParameter("inst_module");
	{
		this.q = "SELECT " + name + "," + firstInstruction + "," + inst_firstLineNumber + "," + inst_moduleOffset + ","
				+ inst_module + "," + inst_moduleId + "," + inst_instructionName + "," + inst_instructionText + ", "
				+ inst_parentFunctionId + " " + " FROM " + FunctionDbConnection.FUNCTION_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS inst ON inst.instructionId = firstInstruction"
				+ " WHERE inst.module = :" + inst_moduleParam + " ORDER BY inst.moduleOffset ASC";
	}
}

/**
 * Fetch all functions from the specified module (i.e. dll).
 *
 */
class SelectSingleFunctionFromModule extends CommonFunctionResults {
	StringParameter inst_moduleParam = new StringParameter("inst_module");
	LongParameter inst_offsetParam = new LongParameter("inst_offsetParam");
	{
		this.q = "SELECT " + name + "," + firstInstruction + "," + inst_firstLineNumber + "," + inst_moduleOffset + ","
				+ inst_module + "," + inst_moduleId + "," + inst_instructionName + "," + inst_instructionText + ", "
				+ inst_parentFunctionId + " " + " FROM " + FunctionDbConnection.FUNCTION_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS inst ON inst.instructionId = firstInstruction"
				+ " WHERE inst.module = :" + inst_moduleParam + " AND inst.moduleOffset = :" + inst_offsetParam
				+ " ORDER BY inst.moduleOffset ASC";
	}
}


/**
 * Selects all functions that either start threads or have an unknown start.
 * Currently unused.
 *
 */
@Deprecated
class SelectTopLevelAndUnknownFunctions extends CommonFunctionResults {
	{
		this.q = "SELECT " + name + "," + firstInstruction + "," + startsThread + "," + unknownStart + "," + oddReturn
				+ "," + inst_firstLineNumber + "," + inst_moduleOffset + "," + inst_module + "," + inst_moduleId + ","
				+ inst_instructionName + "," + inst_instructionText + ", " + inst_parentFunctionId + " " + " FROM "
				+ FunctionDbConnection.FUNCTION_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS inst ON inst.instructionId = firstInstruction"
				+ " WHERE startsThread = 1 OR unknownStart = 1";
	}
}

// 10 interesting
/**
 * Finds all of the functions called by the target function.
 *
 * Currently unused but could be useful.
 */
@Deprecated
class SelectCallees extends CommonFunctionResults {
	InstructionIdParameter cll_callingFunctionStart = new InstructionIdParameter("cll_callingFunctionStart");
	{
		this.q = "SELECT " + name + "," + firstInstruction + "," + startsThread + "," + unknownStart + "," + oddReturn
				+ "," + inst_firstLineNumber + "," + inst_moduleOffset + "," + inst_module + "," + inst_moduleId + ","
				+ inst_instructionName + "," + inst_instructionText + ", " + inst_parentFunctionId + " " + " FROM "
				+ FunctionDbConnection.FUNCTION_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS inst ON inst.instructionId = firstInstruction"
				+ " LEFT JOIN " + CallDbConnection.CALL_TABLE_NAME
				+ " AS cll ON cll.calledFunctionStart = firstInstruction" + " WHERE cll.callingFunctionStart = :"
				+ cll_callingFunctionStart;
	}
}

/**
 * The 'instructions' table
 * {@link BasicBlockDbConnection#INSTRUCTION_TABLE_NAME} the core table that
 * stores the unique instructions in a trace. It does not represent the linear
 * execution of the trace, the lines manifest in it, but instead represents the
 * instructions that may be executed once or may times in the trace. For the
 * linear execution, see {@link CreateTraceFileLineTable} and
 * {@link TraceFileLineDbConnection#FILE_LINE_TABLE_NAME}, and also the raw
 * binary files.
 * 
 * instructionId: - the unique id for each instruction, currently comprised of
 * the module id, a colon separater, and the address of the instruction within
 * its module (e.g. 0:28745, where 0 is an Atlantis internal id for 7zip.dll,
 * and 28745 is the address position for the instruction in question). This
 * format is generated during trace processing by Gibraltar, and is a convenient
 * reification of otherwise separate attributes of each instruction.
 * firstLineNumber: - the first trace line on which this instruction appears
 * moduleOffset: - the address (actually offset) of the instruction within the
 * module or dll that it belongs to module: - the name of the module or dll that
 * the instruction belongs to moduleId: - the internal numeric id of the module
 * or dll that the instruction belongs to instructionName: - the name of the
 * instruction, e.g., 'call', 'movx', 'ret', etc., without any parameters
 * instructionText: - the full instruction including parameters, e.g., 'mov rdi,
 * qword ptr [rax+0x2c8]'
 */
class CreateInstructionTable extends CreateFunctionTable {
	/**
	 * {@link InstructionDbConnection}
	 */
	public static final String createInstructionTable = "CREATE TABLE " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
			+ " (" + " instructionId VARCHAR(100) PRIMARY KEY NOT NULL, " + // Meant
																			// to
																			// be
																			// unique,
																			// required
																			// to
																			// be
																			// module
																			// combined
																			// with
																			// moduleOffset
																			// numeric
																			// representation
																			// from
																			// binary
																			// format.
			" firstLineNumber BIGINT NOT NULL, " + " moduleOffset BIGINT NOT NULL, " + " module VARCHAR(100) NOT NULL, "
			+ " moduleId INT NOT NULL, " + " instructionName VARCHAR(20) NOT NULL, "
			+ " instructionText VARCHAR(100) NOT NULL, " + " parentFunctionId VARCHAR(100) " + " )";
}

class InsertInstruction extends TypedQuery {
	InstructionIdParameter instructionId = new InstructionIdParameter("instructionId");
	LongParameter firstLineNumber = new LongParameter("firstLineNumber");
	LongParameter moduleOffset = new LongParameter("moduleOffset");
	StringParameter module = new StringParameter("module");
	IntegerParameter moduleId = new IntegerParameter("moduleId");
	StringParameter instructionName = new StringParameter("instructionName");
	StringParameter instructionText = new StringParameter("instructionText");
	InstructionIdParameter parentFunctionId = new InstructionIdParameter("parentFunctionId");

	{
		this.q = "INSERT OR IGNORE INTO " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " (instructionId, firstLineNumber, moduleOffset, module, moduleId, "
				+ "  instructionName, instructionText, parentFunctionId) " + "VALUES(:" + instructionId + ", :"
				+ firstLineNumber + ", :" + moduleOffset + ", :" + module + ", :" + moduleId + ", " + ":"
				+ instructionName + ", :" + instructionText + ", :" + parentFunctionId + ")";
	}
}

class CommonInstructionResults extends TypedQuery {
	InstructionIdResult instructionId = new InstructionIdResult("instructionId");
	LongResult firstLineNumber = new LongResult("firstLineNumber");
	LongResult moduleOffset = new LongResult("moduleOffset");
	StringResult module = new StringResult("module");
	IntegerResult moduleId = new IntegerResult("moduleId");
	StringResult instructionName = new StringResult("instructionName");
	StringResult instructionText = new StringResult("instructionText");
	InstructionIdResult parentFunctionId = new InstructionIdResult("parentFunctionId");
}

/**
 * Retrieve the instruction based on the instruction id. Somewhat surprisingly,
 * only used in the function recomposition view at this time.
 *
 */
class SelectInstruction extends CommonInstructionResults {
	InstructionIdParameter instructionIdParam = new InstructionIdParameter("instructionId");
	{
		this.q = "SELECT * FROM " + InstructionDbConnection.INSTRUCTION_TABLE_NAME + " WHERE instructionId = :"
				+ instructionIdParam;
	}
}

/**
 * Select the specific line data.
 *
 */
class GetLineDataForLine extends CommonInstructionResults {
	IntegerParameter lineNumber = new IntegerParameter("lineNumber");

	{
		this.q = "SELECT * FROM " + InstructionDbConnection.INSTRUCTION_TABLE_NAME + " AS i1 " + " LEFT JOIN "
				+ TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS l1 ON l1.instructionId = i1.instructionId "
				+ " WHERE traceLineNumber = :" + lineNumber;
	}
}

// 11 interesting
/**
 * Retrieves all instructions in a given module, that are between the provided
 * module offsets or addresses. This produces a list of instructions in their
 * static executable order, not in trace line order (as-executed order).
 *
 */
class SelectInstructionsInRange extends CommonInstructionResults {
	StringParameter moduleParam = new StringParameter("module");
	LongParameter offsetLeftBound = new LongParameter("offsetLeftBound");
	LongParameter offsetRightBound = new LongParameter("offsetRightBound");

	{
		this.q = "SELECT * FROM " + InstructionDbConnection.INSTRUCTION_TABLE_NAME + " WHERE module = :" + moduleParam
				+ " AND moduleOffset >= :" + offsetLeftBound + " AND moduleOffset <= :" + offsetRightBound
				+ " ORDER BY moduleOffset ASC";
	}
}

class GetFunctionRetLineNumber extends AssemblyEventAllSelectFields {
	IntegerParameter funCallLineNumber = new IntegerParameter("funCallLineNumber");
	{
		this.skipParameterCheck = true;
		this.q = " SELECT startLineNum,Min(startLineNum)" + " FROM "
				+ AssemblyEventDbConnection.ASSEMBLY_EVENT_TABLE_NAME + " WHERE id = ( "
				+ "SELECT substr(module, 1, instr(module,'.')-1) AS moduleId FROM "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME + " WHERE instructionId = ( "
				+ " WITH SR AS (select startInstruction,Max(startLineNumber) FROM "
				+ ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME + " WHERE startLineNumber < :"
				+ funCallLineNumber + " ) SELECT startInstruction FROM SR) " + ") AND startLineNum > :"
				+ funCallLineNumber;

	}
}

/**
 * This finds all modules present in the trace, identified by name.
 *
 */
class SelectModules extends TypedQuery {
	StringResult module = new StringResult("module");
	{
		this.q = "SELECT DISTINCT " + module + " FROM " + InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " WHERE module != 'NO_MODULE' ORDER BY module ASC";
	}
}

/**
 * The 'function_jumps' table {@link BasicBlockDbConnection#JUMP_TABLE_NAME}
 * stores references to all types of jump instructions. It associates the jump
 * with its target location, and whether that jump was ever taken (for
 * conditional jumps that can also pass through to the next adjacent
 * instruction).
 * 
 * jumpId: - assigned id for the jump jumpInstruction: - instruction id
 * corresponding to the 'jump' instruction targetInstruction: - instruction id
 * for the target that would be jumped to branchTaken: - did the jump get
 * performed, or did the conditional fail and pass onto adjacent instruction? A
 * conditional can occur twice in the table if the execution used that
 * instruction twice, with different conditional outcome each time.
 * 
 * This table is currently only referenced by deprecated queries, which
 * themselves are only used by deprecated and unused methods. Consider removal
 * of this table.
 */
@Deprecated
class CreateJumpTable extends CreateInstructionTable {
	/**
	 * {@link JumpDbConnection}
	 */
	public static final String createJumpTable = "CREATE TABLE " + JumpDbConnection.JUMP_TABLE_NAME + " ("
			+ " jumpId INT NOT NULL, " + " jumpInstruction VARCHAR(100) NOT NULL, "
			+ " targetInstruction VARCHAR(100) NOT NULL, " + " branchTaken BIT NOT NULL,"
			+ " PRIMARY KEY (jumpInstruction, targetInstruction, branchTaken)" + " )";
}

class InsertJump extends TypedQuery {
	IntegerParameter jumpId = new IntegerParameter("jumpId");
	InstructionIdParameter jumpInstruction = new InstructionIdParameter("jumpInstruction");
	InstructionIdParameter targetInstruction = new InstructionIdParameter("targetInstruction");
	BooleanParameter branchTaken = new BooleanParameter("branchTaken");

	{
		this.q = "INSERT OR IGNORE INTO " + JumpDbConnection.JUMP_TABLE_NAME
				+ " (jumpId, jumpInstruction, targetInstruction, branchTaken) " + "VALUES" + "(:" + jumpId + ", :"
				+ jumpInstruction + ", :" + targetInstruction + ", :" + branchTaken + ")";
	}
}

/**
 * The 'memory_snapshot_delta_tree' table
 * {@link BasicBlockDbConnection#MEMORY_TABLE_NAME} contains the tree
 * datastructure responsible for fast lookup of memory state for any specific
 * line. See the paper by Cleary, Gorman, Verbeek, Storey, Salois, and Painchaud
 * "Reconstructing Program memory State from Multi-Gigabyte Instruction Traces
 * to Support Interactive Analysis". Also see the {@link MemoryDeltaTree} and
 * associated classes.
 * 
 * Basically, for each trace line, there is a leaf entry in the tree, which
 * specifies the memory and registry changes (if any) for the corresponding
 * line. The parents to those leaves contain the cumulative changes that
 * occurred over the 1000 leaves. The parents above those contain the cumulative
 * changes over those 1000 parent nodes, meaning they contain the cumulative
 * change over one million leaf nodes. Adjacent inner nodes can be combined,
 * starting from the first inner node to the latest of interest, to give the
 * cumulative memory state from the beginning of the trace to that point.
 * Additional leaf nodes may then be combined to get the state up to lines not
 * corresponding to a parent node. This was designed as a middle ground between
 * having full snapshots of memory at every nth line, and having to reconstruct
 * the entire memory one line at a time. This mechanism allows fewer comparisons
 * and queries and result objects, and serves as a great middle ground between
 * the space requirements of having many complete snapshots of memory versus the
 * time requirements of having to reconstruct memory from individual line
 * changes.
 * 
 * We use the terminology 'delta' for the diff or change in memory state, and a
 * single delta can contain multiple memory and register changes, for a single
 * line or multiple lines (in the case of inner nodes with accumulated changes).
 * 
 * id: - delta's assigned id. Starts at 0 and goes upward with each leaf. Jumps
 * to significantly larger values for each layer of inner nodes. parentId: -
 * joins to the id column in this table, refers to the immediate parent of the
 * current node startLine: - the first line related to the delta, equal to
 * endLine for leaf nodes endLine: - the last line related to the delta. Inner
 * parent nodes will span from startLine to endLine. deltaData: - the actual
 * delta or changes to registers and memory. The format is currently with all
 * elements separated by semicolons. Each change has four elements: the
 * register/memory address; the value that is changed to; line number; and
 * finally, {@link EventType} (FLAGS, MEMORY or REGISTER as 0, 1, or 2). Given
 * that internal nodes aggregate over 1000 leaf nodes at the first level, and
 * over 1 million leaf nodes at the level after that, the deltaData blob can be
 * quite large. Note that although there used to be a root node that was
 * singular, and covered from the first to last lines, it was not functionally
 * useful, and has been removed. Now, the tree is 'cut off', and will always
 * have more than one root, and a number of levels one shorter than it would
 * have had before.
 * 
 * The ids are structured like so currently (from a sample file): id startLine
 * endLine 0000000000 0 0 <-- actually id = 0, but leading zeros added for
 * educational purposes. 1000000000 0 999 1001000000 0 999999 1001001000 0
 * 3366661
 *
 * The design is such that the leaf nodes start at id 0 to 999999999, for a
 * total of up to 1 trillion leaf nodes. This is the most we can represent with
 * levels up to 4 and branching of 1000. After the leaf node's naturally
 * allocated space, we have the ids for the parents of them. Each has 1000 leaf
 * nodes that it summarizes, so the ids range from 1000000000 to 1000999999 with
 * the next parent set starting at 1001000000. That gives us 999999+1 parent
 * nodes, a million, or 1/1000 of a trillion. The parents of these each
 * summarize one thousand of them. You will note that the 1000 level three
 * nodes, grandparents, each summarize 1000 parent nodes, and by virtue of that
 * summarize 1,000,000 leaf nodes each, for a total of 1,000,000,000 across the
 * one thousand grand parent nodes. each level maxes out at one trillion lines
 * summarized. Back to the ids, you will notice that this grandparent level can
 * only number 1000, so the ids are between 1001000000 and 1001000999. Finally,
 * we have a node level starting at 1001001000. You might notice that after one
 * such node is assigned, the next id would be 1001001001, and looking at the
 * pattern in the ids, you would notice that we might expect another layer here.
 * This is the root node (which currently should not be committed to the DB,
 * because it is useless and takes up memory resources during parsing). This
 * root node summarizes 1 trillion leaf nodes. If we broke the numeral pattern
 * observed, the new node would represent 1 trillion nodes as well, but we can
 * only represent 1 trillion nodes total in this design. Therefore, the final
 * possible id with branching of 1000 and levels of 4 is 1001001000.
 */
class CreateMemoryTable extends CreateJumpTable {
	/**
	 * {@link MemoryDbConnection}
	 */
	public static final String createMemoryTable = "CREATE TABLE " + MemoryDbConnection.MEMORY_TABLE_NAME + " ("
			+ " id INT PRIMARY KEY NOT NULL," + " parentId INT," + " startLine INT NOT NULL," + " endLine INT NOT NULL,"
			+ " deltaData LONGBLOB" + " ) ";
}

class InsertMemoryReference extends TypedQuery {
	IntegerParameter id = new IntegerParameter("id");
	IntegerParameter parentId = new IntegerParameter("parentId");
	IntegerParameter startLine = new IntegerParameter("startLine");
	IntegerParameter endLine = new IntegerParameter("endLine");
	/**
	 * The deltaData is actually a LONGBLOB in the database, but we put in and
	 * take out as a string.
	 */
	StringParameter deltaData = new StringParameter("deltaData");
	{
		this.q = "INSERT INTO " + MemoryDbConnection.MEMORY_TABLE_NAME
				+ " (id, parentId, startLine, endLine, deltaData) " + "VALUES " + "(:" + id + ", :" + parentId + ", :"
				+ startLine + ", :" + endLine + ", :" + deltaData + ")";
	}
}

// 12 program does a lot interesting
/**
 * Retrieves memory deltas within the id span. The span is computed by Atlantis,
 * and will need to cherry pick as necessary to access parent nodes without
 * taking irrelevant leaf nodes. That is to say, the tree must be walked depth
 * first, but node ids are not assigned depth first, so they cannot be used
 * without computing which parent nodes are needed (and what their ids will be).
 * 
 * TOOD: Could we change the algorithm that assigns ids, or add a second id
 * column, that would assign ids in traversal order for this tree? It would make
 * line numbers and ids differ by an additional +1 for every 1000 nodes (as the
 * next integer is used for the parent node). Thus, finding the end node id
 * would still require computation. There is probably no performance benefit in
 * this idea, just algorithmic aesthetics.
 *
 */
class SelectMemRefByLine extends TypedQuery {
	IntegerResult id = new IntegerResult("id");
	IntegerResult parentId = new IntegerResult("parentId");
	IntegerResult startLine = new IntegerResult("startLine");
	IntegerResult endLine = new IntegerResult("endLine");
	/**
	 * The deltaData is actually a LONGBLOB in the database, but we put in and
	 * take out as a string.
	 */
	StringResult deltaData = new StringResult("deltaData");

	IntegerParameter idLeftBound = new IntegerParameter("idLeftBound");
	IntegerParameter idRightBound = new IntegerParameter("idRightBound");
	{
		this.q = "SELECT * FROM " + MemoryDbConnection.MEMORY_TABLE_NAME + " WHERE id >= :" + idLeftBound
				+ " AND id < :" + idRightBound;
	}
}

/**
 * Finds the node with the highest endLine, which should be the final leaf node
 * or perhaps a parent node covering it.
 * 
 * Why do we need this query? It is referenced, but it's not clear that it is
 * needed. We do indeed need to know the maximum line number, but is looking at
 * this table the best way to do it?
 *
 */
class SelectMaxLine extends TypedQuery {
	{
		this.q = "SELECT MAX(endLine) FROM " + MemoryDbConnection.MEMORY_TABLE_NAME;
	}
}

/**
 * The 'thread_change_events' table
 * {@link BasicBlockDbConnection#THREAD_EVENT_TABLE_NAME} contains the pixel
 * coordinates and offsets for the deprecated thread visualization.
 *
 */
class ThreadEventQueries extends CreateMemoryTable {
	/**
	 * {@link ThreadEventDbConnection}
	 */
	public static final String createThreadEventTable = "CREATE TABLE "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " (" + " tid INT, " + " lineNumber INT PRIMARY KEY, "
			+ " numberOfLines INT NOT NULL, " + " typeId INT NOT NULL, " + " viewStartXPixel INT NOT NULL, "
			+ " viewEndXPixel INT NOT NULL" + " )";

	@Deprecated
	public static final String insertThread = "INSERT INTO " + ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME
			+ " (tid, lineNumber, numberOfLines, typeId, viewStartXPixel, viewEndXPixel) VALUES(?,?,?,?,?,?)";

	@Deprecated
	public static final String selectNumberOfThreadEvents = "SELECT COUNT(*) FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME;

	@Deprecated
	public static final String selectThreadEventRangeByLine = "SELECT * FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " AS t1"
			+ " WHERE ? <= t1.lineNumber AND t1.lineNumber <= ?";

	@Deprecated
	public static final String selectThreadMarkerContainingLine = "SELECT * FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " AS t1"
			+ " WHERE t1.lineNumber <= ? AND ? <= t1.lineNumber + t1.numberOfLines";

	@Deprecated
	public static final String selectThreadMarkerByPixel = "SELECT * FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " AS t1"
			+ " WHERE ? >= t1.viewStartXPixel AND t1.viewEndXPixel >= ?"; // ?
																			// are
																			// end,
																			// start.
																			// End
																			// before
																			// start,
																			// start
																			// before
																			// end.;

	@Deprecated
	public static final String selectDistinctThreadNames = "SELECT DISTINCT(t1.tid) FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " AS t1 ";

	@Deprecated
	public static final String selectFinalThreadEndPoint = "SELECT MAX(t1.viewEndXPixel) FROM "
			+ ThreadEventDbConnection.THREAD_EVENT_TABLE_NAME + " AS t1 ";

	// It is probably time to get rid of the thread_change_results table
}

/**
 * The 'thread_function_blocks' table
 * {@link BasicBlockDbConnection#THREAD_FUNCTION_BLOCK_TABLE_NAME} contains the
 * graphical data for the Thread Functions View. The view relies on globally
 * precomputed information. Each function has a different color, and start and
 * stop coordinates within the view, corresponding to when the function is
 * active. Each thread is rendered fully separately. The entries are not
 * functions themselves, but are the segments of each function that execute
 * between any function calls and prior to returning.
 * 
 * startLineNumber: - the line number at which this function block entry begins.
 * Functions appear as many times as they are called in the trace. threadId: -
 * the binary format file derived identifying number for the thread
 * startInstruction: - the instruction id for the function entry's start point,
 * may be joined to instructions table, and the functions table only if this is
 * the very beginning of the function endInstruction: - the instruction id for
 * the functions final instruction in this block of the function
 * functionStartInstruction: - the instruction id that corresponds to the
 * function to which this block belongs. Contrasts with the startInstruction
 * that corresponds to the start of the visual function block. xOffset: - the
 * graphical leftmost pixel x-coordinate for where the function block begins
 * width: - the graphical width in pixels of the function block xEndOffset: -
 * the graphical rightmost pixel x-coordinate for where the function block ends.
 * Should be equal to xOffset - 1.
 *
 */
class CreateThreadFunctionTable extends ThreadEventQueries {
	/**
	 * {@link ThreadFunctionBlockDbConnection}
	 */
	public static final String createThreadFunctionTable = "CREATE TABLE "
			+ ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME + " ("
			+ " startLineNumber BIGINT NOT NULL, " + " threadId INT NOT NULL, "
			+ " startInstruction VARCHAR(100) NOT NULL, " + " endInstruction VARCHAR(100) NOT NULL, "
			+ " functionStartInstruction VARCHAR(100) NOT NULL, " + " xOffset BIGINT NOT NULL, "
			+ " width BIGINT NOT NULL, " + " xEndOffset BIGINT NOT NULL, " + // only
																				// exists
																				// for
																				// query
																				// optimization+
			" CONSTRAINT rangeSelectOptimizer PRIMARY KEY (threadId, xOffset, xEndOffset) " + " )";
}

class InsertThreadFunctionBlock extends TypedQuery {
	LongParameter startLineNumber = new LongParameter("startLineNumber");
	IntegerParameter threadId = new IntegerParameter("threadId");
	InstructionIdParameter startInstruction = new InstructionIdParameter("startInstruction");
	InstructionIdParameter endInstruction = new InstructionIdParameter("endInstruction");
	InstructionIdParameter functionStartInstruction = new InstructionIdParameter("functionStartInstruction");
	LongParameter xOffset = new LongParameter("xOffset");
	LongParameter width = new LongParameter("width");
	LongParameter xEndOffset = new LongParameter("xEndOffset");
	{
		this.q = "INSERT INTO " + ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME
				+ " (startLineNumber, threadId, startInstruction, endInstruction, functionStartInstruction, xOffset, width, xEndOffset) "
				+ "VALUES " + "(:" + startLineNumber + ", :" + threadId + ", :" + startInstruction + ", :"
				+ endInstruction + ", :" + functionStartInstruction + ", :" + xOffset + ", :" + width + ", :"
				+ xEndOffset + ")";
	}
}

/**
 * Fetches the unique threads found in the execution.
 *
 */
class SelectThreads extends TypedQuery {
	IntegerResult threadId = new IntegerResult("threadId");
	{
		this.q = "SELECT DISTINCT threadId FROM " + ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME
				+ " ORDER BY threadId ASC";
	}
}

class CommonThreadFunctionBlockResults extends TypedQuery {
	LongResult startLineNumber = new LongResult("startLineNumber");
	IntegerResult threadId = new IntegerResult("threadId");
	InstructionIdResult startInstruction = new InstructionIdResult("startInstruction");
	InstructionIdResult endInstruction = new InstructionIdResult("endInstruction");
	InstructionIdResult functionStartInstruction = new InstructionIdResult("functionStartInstruction");
	LongResult xOffset = new LongResult("xOffset");
	LongResult width = new LongResult("width");
	LongResult xEndOffset = new LongResult("xEndOffset");

	StringResult endinst_module = new StringResult("endinst.module");
	StringResult funcinst_module = new StringResult("funcinst.module");
	IntegerResult endinst_moduleId = new IntegerResult("endinst.moduleId");
	IntegerResult funcinst_moduleId = new IntegerResult("funcinst.moduleId");
	LongResult funcinst_moduleOffset = new LongResult("funcinst.moduleOffset");
	StringResult endinst_instructionName = new StringResult("endinst.instructionName");
}

// 12 interesting
/**
 * Finds the graphical function blocks on the basis of what the render window
 * needs. Provide the thread and the left and right pixel unit offsets required.
 *
 */
class SelectLWThreadFunctionBlocksByRange extends CommonThreadFunctionBlockResults {
	LongParameter leftXOffsetBound = new LongParameter("leftXOffsetBound");
	LongParameter rightXEndOffsetBound = new LongParameter("rightXEndOffsetBound");
	IntegerParameter threadIdParam = new IntegerParameter("threadId");

	{
		this.q = "SELECT " + startLineNumber + "," + xOffset + "," + width + ", " + endinst_module + ","
				+ funcinst_module + "," + endinst_moduleId + "," + funcinst_moduleId + "," + funcinst_moduleOffset + ","
				+ endinst_instructionName + "," + functionStartInstruction + "," + threadId + " " + " FROM "
				+ ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS endinst ON endinst.instructionId = endInstruction" + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS funcinst ON funcinst.instructionId = functionStartInstruction" + " WHERE xOffset <= :"
				+ leftXOffsetBound + " AND xEndOffset >= :" + rightXEndOffsetBound + " AND threadId = :" + threadIdParam
				+ " ORDER BY xOffset ASC";
	}
}

// 13 interesting
/**
 * Select graphical function blocks on the basis of line number spans. This is
 * used to make the Thread Functions view jump to a target line number. Contrast
 * this to when we are scrolling within that view, and we are querying on the
 * basis of the graphical window's pixel offsets.
 *
 */
class SelectLWThreadFunctionBlockForLine extends CommonThreadFunctionBlockResults {
	IntegerParameter startLineNumberLeftBound = new IntegerParameter("startLineNumberLeftBound");
	IntegerParameter startLineNumberRightBound = new IntegerParameter("startLineNumberRightBound");

	{
		this.q = "SELECT " + startLineNumber + "," + xOffset + "," + width + ", " + endinst_module + ","
				+ funcinst_module + "," + endinst_moduleId + "," + funcinst_moduleId + "," + funcinst_moduleOffset + ","
				+ endinst_instructionName + "," + functionStartInstruction + "," + threadId + " " + " FROM "
				+ ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS endinst ON endinst.instructionId = endInstruction" + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS funcinst ON funcinst.instructionId = functionStartInstruction" + " WHERE startLineNumber <= :"
				+ startLineNumberLeftBound + " AND startLineNumber + width -1 >= :" + startLineNumberRightBound;
	}
}

// 14 interesting
/**
 * Find the graphical function blocks following the specified line. Used when
 * navigating to distant lines in the trace line viewer, to keep the Function
 * Thread View in synch.
 *
 */
class SelectNextLWThreadFunctionBlockForLine extends CommonThreadFunctionBlockResults {
	IntegerParameter startLineNumberLeftBound = new IntegerParameter("startLineNumberLeftBound");

	{
		this.q = "SELECT " + startLineNumber + ", " + xOffset + "," + width + ", " + endinst_module + ","
				+ funcinst_module + "," + endinst_moduleId + "," + funcinst_moduleId + "," + funcinst_moduleOffset + ","
				+ endinst_instructionName + "," + functionStartInstruction + "," + threadId + " " + " FROM "
				+ ThreadFunctionBlockDbConnection.THREAD_FUNCTION_BLOCK_TABLE_NAME + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS endinst ON endinst.instructionId = endInstruction" + " LEFT JOIN "
				+ InstructionDbConnection.INSTRUCTION_TABLE_NAME
				+ " AS funcinst ON funcinst.instructionId = functionStartInstruction" + " WHERE :"
				+ startLineNumberLeftBound + " < startLineNumber" + " ORDER BY startLineNumber ASC" + " LIMIT 1";
	}
}

/**
 * The 'thread_lengths' table
 * {@link BasicBlockDbConnection#THREAD_LENGTH_TABLE_NAME} very simple lists the
 * length of each thread. It is used in the Thread Function View, to determine
 * the total width of the visualization.
 *
 */
class CreateThreadLengthTable extends CreateThreadFunctionTable {
	/**
	 * {@link ThreadLengthDbConnection}
	 */
	public static final String createThreadLengthTable = "CREATE TABLE "
			+ ThreadLengthDbConnection.THREAD_LENGTH_TABLE_NAME + " (" + " threadId INT PRIMARY KEY NOT NULL, "
			+ " length  BIGINT NOT NULL " + " )";
}

class InsertThreadLength extends TypedQuery {
	IntegerParameter threadId = new IntegerParameter("threadId");
	LongParameter length = new LongParameter("length");
	{
		this.q = "INSERT INTO " + ThreadLengthDbConnection.THREAD_LENGTH_TABLE_NAME + " (threadId, length) "
				+ "VALUES (:" + threadId + ", :" + length + ")";
	}
}

/**
 * Retrieves the length in lines for the desired thread. It is used in the
 * Thread Function View, to determine the total width of the visualization.
 *
 */
class SelectThreadLength extends TypedQuery {
	LongResult length = new LongResult("length");
	IntegerParameter threadId = new IntegerParameter("threadId");
	{
		this.q = "SELECT length FROM " + ThreadLengthDbConnection.THREAD_LENGTH_TABLE_NAME + " WHERE threadId = :"
				+ threadId;
	}
}

/**
 * The 'trace_file_text_lines' table
 * {@link BasicBlockDbConnection#FILE_LINE_TABLE_NAME} is the textual content of
 * each trace line. Originally used for the deprecated diff views, but is now
 * used as a join table for trace line numbers and instruction ids. Instructions
 * can occur on multiple lines, so the instructions table does not tell us where
 * the instructions each occur in the trace.
 * 
 * The trace editor is filled from the binary format directly (see
 * {@link BinaryFormatFileModelDataLayer#getFileLines}
 * 
 * BlockOffset and lineContents may be deprecated.
 */
class CreateTraceFileLineTable extends CreateThreadLengthTable {
	/**
	 * {@link TraceFileLineDbConnection}
	 */
	public static final String createTraceFileLineTable = "CREATE TABLE "
			+ TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " (" + " traceLineNumber INT PRIMARY KEY NOT NULL,"
			+ " instructionId VARCHAR(100)," + " blockOffset BIGINT NOT NULL," + " lineContents VARCHAR(2000) NOT NULL"
			+ // 255 showed truncated lines+
			" )";
}

class InsertTraceLine extends TypedQuery {
	IntegerParameter traceLineNumber = new IntegerParameter("traceLineNumber");
	InstructionIdParameter instructionId = new InstructionIdParameter("instructionId");
	LongParameter blockOffset = new LongParameter("blockOffset");
	StringParameter lineContents = new StringParameter("lineContents");

	{
		this.q = "INSERT INTO " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME
				+ " (traceLineNumber, instructionId, blockOffset, lineContents) " + "VALUES(:" + traceLineNumber + ", :"
				+ instructionId + ", :" + blockOffset + ", :" + lineContents + ")";
	}
}

class CommonFileLineResults extends TypedQuery {
	IntegerResult traceLineNumber = new IntegerResult("traceLineNumber");
	InstructionIdResult instructionId = new InstructionIdResult("instructionId");
	LongResult blockOffset = new LongResult("blockOffset");
	StringResult lineContents = new StringResult("lineContents");
}

/**
 * Used for looking up function instances, perhaps other uses too.
 *
 */
class FindAllInstructionInstances extends CommonFileLineResults {
	// The code using this query is limited to ints (Eclipse region stuff), this
	// it cannot receive BIGINT/long here
	InstructionIdParameter instructionIdParam = new InstructionIdParameter("instructionId");
	{
		this.q = "SELECT * FROM " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS t1"
				+ " WHERE t1.instructionId = :" + instructionIdParam;
	}
}

/**
 * Used for looking up all caller functions of a target function, as specified
 * by its instructionId.
 *
 */
class FindAllCallersOfFunction extends CommonFileLineResults {
	// The code using this query is limited to ints (Eclipse region stuff), this
	// it cannot receive BIGINT/long here
	InstructionIdParameter instructionIdParam = new InstructionIdParameter("instructionId");
	{
		this.q = "SELECT * FROM " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS t1" + " JOIN "
				+ CallDbConnection.CALL_TABLE_NAME + " AS f1 ON f1.callingFunctionStart = t1.instructionId"
				+ " WHERE f1.calledFunctionStart = :" + instructionIdParam + " " + " ORDER BY t1.traceLineNumber ASC";

	}
}

/**
 * Only appears to be used in the deprecated and non-scalable trace diff
 * classes.
 */
@Deprecated
class SelectLineRange extends CommonFileLineResults {
	// The code using this query is limited to ints (Eclipse region stuff), this
	// it cannot receive BIGINT/long here
	IntegerParameter traceLineLeftBound = new IntegerParameter("traceLineLeftBound");
	IntegerParameter traceLineRightBound = new IntegerParameter("traceLineRightBound");
	{
		this.q = "SELECT * FROM " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS t1" + " WHERE :"
				+ traceLineLeftBound + " <= t1.traceLineNumber AND t1.traceLineNumber <= :" + traceLineRightBound;
	}
}

/**
 * Only appears to be used in
 * {@link TraceFileLineDbConnection#getRegexMatchResults}. It can surely be
 * replaced by a different implementation of finding total line count.
 *
 */
class CountNumberOfLines extends TypedQuery {
	{
		this.q = "SELECT COUNT(*) FROM " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME;
	}
}

/**
 * Regex search through lines of the trace.
 * 
 * This regex search is not scalable. It is far too long for the 81 million line
 * trace. Can SQLite be used for such things, or do we need another kind of
 * index (ElasticSearch?) to do line searches?
 * 
 * Can the users of this method be adapted to other mechanisms closer to the
 * binary format, or adapted to use more intelligent ways of accessing the data
 * (not string search, but doing instruction name searches, memory value
 * searches, etc)?
 *
 */
class RegexSearchLineContents extends CommonFileLineResults {
	StringParameter contentsMatchTarget = new StringParameter("contentsMatchTarget");
	IntegerParameter traceLineLeftBound = new IntegerParameter("traceLineLeftBound");
	IntegerParameter traceLineRightBound = new IntegerParameter("traceLineRightBound");

	RegexSearchLineContents(boolean regexInsteadOfLike) {
		if (regexInsteadOfLike) {
			this.q = "SELECT traceLineNumber, lineContents, blockOffset FROM "
					+ TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS t1" + " WHERE t1.lineContents REGEXP :"
					+ contentsMatchTarget + " AND t1.traceLineNumber BETWEEN :" + traceLineLeftBound + " AND :"
					+ traceLineRightBound;
		} else {
			this.q = "SELECT traceLineNumber, lineContents, blockOffset FROM "
					+ TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " AS t1" + " WHERE t1.lineContents LIKE :"
					+ contentsMatchTarget + " AND t1.traceLineNumber BETWEEN :" + traceLineLeftBound + " AND :"
					+ traceLineRightBound;
		}

	}
}

class GetTraceLine extends CommonFileLineResults {
	IntegerParameter traceLineNumberParam = new IntegerParameter("traceLineNumberParam");

	GetTraceLine() {
		this.q = "SELECT * FROM " + TraceFileLineDbConnection.FILE_LINE_TABLE_NAME + " WHERE traceLineNumber = :"
				+ traceLineNumberParam;
	}
}

/**
 * The 'filemetadata' table {@link BasicBlockDbConnection#FILE_META_DATA_TABLE}
 * Used to determine if the database was completed. If processing went awry,
 * this will indicate that the DB is not valid.
 * 
 */
class MetaTableCreation extends CreateTraceFileLineTable {

	/**
	 * {@link DbBackendAbstract}
	 */
	// No ENUM in SQLite, can't use FIND_IN_SET() either, so...
	// http://stackoverflow.com/questions/6979813/alternative-to-find-in-set-in-sqlite
	/* + CHECK(('processing' || 'completed') = traceState) */
	public static final String createMetaDataTable = "CREATE TABLE IF NOT EXISTS "
			+ DbConnectionManager.FILE_META_DATA_TABLE + " (fileId INTEGER PRIMARY KEY   AUTOINCREMENT,"
			+ " fileWordSize INT," + // DEPRECATED FIELD, DELETE WHEN CONVENIENT
			" traceState VARCHAR(16)" + " DEFAULT 'processing'  )";
}

/**
 * The 'filedatatablestatus' table
 * {@link BasicBlockDbConnection#FILE_DATA_STATUS_TABLE} is probably not
 * necessary now that we use a single SQLite DB per trace. Investigate whether
 * we can remove this and attendant logic.
 *
 */
@Deprecated
class FileStatusTableCreation extends MetaTableCreation {

	// fileName is actually file path,
	// Windows path length could be set to VARCHAR(255), because on Windows
	// something like 259 is the max
	// path length usable by the system.
	// I have no idea what system this will run on though, so we'll go ahead
	// with a TEXT field.
	// Also, a desirable check breaks SQLite
	/* CHECK(('missing' || 'created' || 'populated' || 'deleting') = status) */
	/**
	 * {@link DbBackendMySQL}
	 */
	public static final String createFileStatusTable = "CREATE TABLE IF NOT EXISTS "
			+ DbConnectionManager.FILE_DATA_STATUS_TABLE + " (tableName VARCHAR(100) PRIMARY KEY NOT NULL,"
			+ " fileName TEXT(" + DbConnectionManager.MAX_FILE_PATH_BYTE_LENGTH + ")," + " status VARCHAR(16),"
			+ " entryDate DATETIME)";

}

@Deprecated
class SelectTraceFileState extends TypedQuery {
	StringResult traceState = new StringResult("traceState");
	{
		this.q = "SELECT traceState FROM " + DbConnectionManager.FILE_META_DATA_TABLE;
	}
}

@Deprecated
class DoesTableExist extends TypedQuery {
	StringParameter table_schema = new StringParameter("table_schema");
	StringParameter table_name = new StringParameter("table_name");
	{
		this.q = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = :" + table_name
				+ " AND table_name = :" + table_name;
	}
}

/**
 * Sundry queries and methods that create prepare queries.
 *
 */
public class AtlantisQueries extends FileStatusTableCreation {

	/**
	 * {@link RemoveTablesDbConnection}
	 */
	@Deprecated
	public static final String countAndLabelTablesForDeletion = "UPDATE `" + DbConnectionManager.FILE_DATA_STATUS_TABLE
			+ "` AS st" + " SET status='deleting'" + " WHERE st.fileName LIKE ?";

	@Deprecated
	public static final String selectTablesForTrace = "SELECT st.tableName FROM `"
			+ DbConnectionManager.FILE_DATA_STATUS_TABLE + "` AS st" + " WHERE st.fileName LIKE ?";

	/* METHODS */
	/*
	 * These methods include some that are terribly unsafe, that take unchecked
	 * table names for example.
	 */

	protected static NamedParameterPreparedStatement mk(Connection connection, String queryString) throws SQLException {
		return NamedParameterPreparedStatement.createNamedParameterPreparedStatement(connection, queryString);
	}

	/**
	 * Convenience method, to prevent need to call the prepare() method on the
	 * TypedQuery instances. Would do in a constructor, but would require more
	 * boilerplate in each subclass. I prefer a slightly longer instantiation
	 * line in this situation.
	 * 
	 * @param connection
	 * @param typedQuery
	 * @return
	 * @throws SQLException
	 */
	protected static <T extends TypedQuery> T mk(Connection connection, T typedQuery) throws SQLException {
		typedQuery.prepare(connection);
		return typedQuery;
	}

	public static PreparedStatement dropTable(Connection connection, String tableName) throws SQLException {
		return mk(connection, "DROP TABLE IF EXISTS " + tableName);
	}

	public static PreparedStatement createIndex(Connection connection, boolean ifNotExists, String tableName,
			String columnName, String indexName) throws SQLException {
		String ifNotExistsStr = "";
		if (ifNotExists) {
			ifNotExistsStr = " IF NOT EXISTS ";
		}
		return mk(connection, "CREATE INDEX " + ifNotExistsStr + tableName.replace("`", "") + indexName + " ON "
				+ tableName + " (" + columnName + ")");
	}

	public static PreparedStatement isTableEmpty(Connection connection, String tableName) throws SQLException {
		return mk(connection, "SELECT * FROM " + tableName + " LIMIT 1");
	}

	public static PreparedStatement truncateSQLite(Connection connection, String tableName) throws SQLException {
		return mk(connection, "DELETE FROM " + tableName);
	}

	public static PreparedStatement truncateMySQL(Connection connection, String tableName) throws SQLException {
		return mk(connection, "TRUNCATE TABLE " + tableName);
	}

	@Deprecated
	public static PreparedStatement createDiffResultIndex(Connection connection, String tableName) throws SQLException {
		return mk(connection, "CREATE INDEX IsMatchIndex " + " USING BTREE " + " ON " + tableName + " (isMatch) ");
	}

	@Deprecated
	public static PreparedStatement updateTableStatus(Connection connection, String dbName) throws SQLException {
		return mk(connection, "DELETE FROM `" + dbName + "`.`" + DbConnectionManager.FILE_DATA_STATUS_TABLE
				+ "` WHERE tableName = ?");
	}

	public static PreparedStatement updateMetaTableStatus(Connection connection, String dbName) throws SQLException {
		// the >= 0 part is to circumvent (not so safely) a feature of the MySQL
		// system, that prevents removal of things without specifying a key.
		return mk(connection, "DELETE FROM `" + dbName + "`.`" + DbConnectionManager.FILE_META_DATA_TABLE);
	}

}
