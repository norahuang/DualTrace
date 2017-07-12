package ca.uvic.chisel.gibraltar;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatFileBackend;
import ca.uvic.chisel.atlantis.database.AssemblyEventDbConnection;
import ca.uvic.chisel.atlantis.database.BasicBlockDbConnection;
import ca.uvic.chisel.atlantis.database.CallDbConnection;
import ca.uvic.chisel.atlantis.database.FunctionDbConnection;
import ca.uvic.chisel.atlantis.database.InstructionDbConnection;
import ca.uvic.chisel.atlantis.database.JumpDbConnection;
import ca.uvic.chisel.atlantis.database.MemoryDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadEventDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadFunctionBlockDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadLengthDbConnection;
import ca.uvic.chisel.atlantis.database.TraceFileLineDbConnection;
import ca.uvic.chisel.atlantis.datacache.AssemblyEventFileConsumer;
import ca.uvic.chisel.atlantis.datacache.ThreadEventFileConsumer;
import ca.uvic.chisel.atlantis.datacache.TraceFileLineDbConsumer;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.atlantis.functionparsing.InstructionFileLineConsumer;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;


/**
 * Gibraltar is the data Edifice for Atlantis. It produces and provides access to the data
 * derived from the raw binary trace format. It can be called from command line to do
 * offline processing to produce the Edifice, and can be called on directly by Java
 * programs.
 * 
 * It is possible to provide further non-Java access should that be desirable. It is intended
 * to be runnable stand alone, even though it does have data type coupling with Atlantis.
 * 
 */
public class Gibraltar {
	
	public TraceFileLineDbConnection fileDbBackend;
	public BinaryFormatFileBackend binaryFileBackend;
	public AtlantisBinaryFormat binaryFileSet; // Don't initialize to null. Must stay default.
	private ArrayList<IFileContentConsumer> consumers = new ArrayList<IFileContentConsumer>();;

	public Gibraltar(IFile originalFile) throws Exception{
		this(originalFile.getRawLocation().makeAbsolute().toString());
	}
		
	public Gibraltar(String pathToBinaryFormatFolder) throws Exception{
		File folder = new File(pathToBinaryFormatFolder);
		this.binaryFileSet = new AtlantisBinaryFormat(folder);
		this.binaryFileBackend = new BinaryFormatFileBackend(this.binaryFileSet);
		initConsumers();
	}
	
	private void initConsumers() throws Exception{
		File originalFile = this.binaryFileSet.binaryFolder;
		this.assemblyEventDatabase = new AssemblyEventDbConnection(originalFile);
		AssemblyEventFileConsumer assemblyEventConsumer = new AssemblyEventFileConsumer(this.assemblyEventDatabase);
		
		this.threadEventDatabase = new ThreadEventDbConnection(originalFile);
		ThreadEventFileConsumer threadEventConsumer = new ThreadEventFileConsumer(this.threadEventDatabase);
		
		this.fileDbBackend = new TraceFileLineDbConnection(originalFile);
		TraceFileLineDbConsumer traceDBFileLineConsumer = new TraceFileLineDbConsumer((TraceFileLineDbConnection)fileDbBackend, originalFile);
		
		int branchOutFactor = 1000;
		this.memoryAccessDatabase = new MemoryDbConnection(originalFile, branchOutFactor);
		MemoryDeltaTree treeConsumer = new MemoryDeltaTree(memoryAccessDatabase, branchOutFactor, binaryFileBackend);
		
		this.instructionDbBackend = new InstructionDbConnection(originalFile);
		
		this.basicBlockDbBackend = new BasicBlockDbConnection(originalFile);
		this.jumpDbBackend = new JumpDbConnection(originalFile);
		this.functionDbBackend = new FunctionDbConnection(originalFile);
		this.callDbBackend = new CallDbConnection(originalFile);
		this.threadFunctionBlockDatabase = new ThreadFunctionBlockDbConnection(originalFile, this.instructionDbBackend);
		this.threadLengthDatabase = new ThreadLengthDbConnection(originalFile);
		this.instructionConsumer = new InstructionFileLineConsumer(
				instructionDbBackend, 
				basicBlockDbBackend, 
				jumpDbBackend,
				functionDbBackend,
				callDbBackend,
				threadFunctionBlockDatabase,
				threadLengthDatabase,
				treeConsumer);
		
		// Prepare queries here. Used to be in constructors, after table creation, but that created cyclical dependency
		// because tables need to exist before a query can refer to them, and some queries are prepared in one
		// table owning class rather than another; the other table may not have been created yet.
		this.assemblyEventDatabase.initializePreparedStatements();
		this.threadEventDatabase.initializePreparedStatements();
		this.fileDbBackend.initializePreparedStatements();
		this.memoryAccessDatabase.initializePreparedStatements();
		this.instructionDbBackend.initializePreparedStatements();
		this.basicBlockDbBackend.initializePreparedStatements();
		this.jumpDbBackend.initializePreparedStatements();
		this.functionDbBackend.initializePreparedStatements();
		this.callDbBackend.initializePreparedStatements();
		this.threadFunctionBlockDatabase.initializePreparedStatements();
		this.threadLengthDatabase.initializePreparedStatements();
		
		
		// StrawManConsumer has been useful for testing some things...but don't leave it uncommented when committing.
		// consumers.add(new StrawManConsumer());
		consumers.add(traceDBFileLineConsumer);
		consumers.add(assemblyEventConsumer);
		consumers.add(threadEventConsumer);
		consumers.add(treeConsumer);
		// Ensure that instructionConsumer is added last.
		// This is a terrible solution to a problem of SQLite system
		// tables locking up during index creation, which only happened
		// if instructionConsumer was dealt with before the rest.
		consumers.add(instructionConsumer);
	}

	public ArrayList<IFileContentConsumer> getConsumers() {
			return consumers;
	}
	
	public long getDbFileSizeOnDisk(){
		// Arbitrarily pick a DbDConneciton implementation instance and ask for the DB file name.
		File db = new File(instructionDbBackend.database);
		return db.length();
	}
	
	public void dispose(){
		this.assemblyEventDatabase.closeConnection();
		this.threadEventDatabase.closeConnection();
//		this.fileDbBackend.closeConnection();
		this.instructionDbBackend.closeConnection();
		this.basicBlockDbBackend.closeConnection();
		this.jumpDbBackend.closeConnection();
		this.functionDbBackend.closeConnection();
		this.callDbBackend.closeConnection();
		this.threadFunctionBlockDatabase.closeConnection();
		this.threadLengthDatabase.closeConnection();
		this.memoryAccessDatabase.closeConnection();
	}
	
	
	/*
	 * Use this class for queries and insertions; insertion one is made internal to the
	 * MemoryDeltaTree consumer class.
	 */
	public MemoryDbConnection memoryAccessDatabase;
	public AssemblyEventDbConnection assemblyEventDatabase;
	public ThreadEventDbConnection threadEventDatabase;

		public InstructionDbConnection instructionDbBackend;
		public BasicBlockDbConnection basicBlockDbBackend;
		public JumpDbConnection jumpDbBackend;
		public FunctionDbConnection functionDbBackend;
		public CallDbConnection callDbBackend;
		public ThreadFunctionBlockDbConnection threadFunctionBlockDatabase;
		public ThreadLengthDbConnection threadLengthDatabase;
		
	// Only a member so that we can deal with instruction id generation. This would
	// not be necessary if we didn't support textual traces.
	public InstructionFileLineConsumer instructionConsumer;
		

		
}
