package ca.uvic.chisel.atlantis.datacache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.resources.IFile;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ModuleRec;
import ca.uvic.chisel.atlantis.database.BasicBlockDbConnection;
import ca.uvic.chisel.atlantis.database.CallDbConnection;
import ca.uvic.chisel.atlantis.database.FunctionDbConnection;
import ca.uvic.chisel.atlantis.database.InstructionDbConnection;
import ca.uvic.chisel.atlantis.database.JumpDbConnection;
import ca.uvic.chisel.atlantis.database.MemoryDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadFunctionBlockDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadLengthDbConnection;
import ca.uvic.chisel.atlantis.database.TraceFileLineDbConnection;
import ca.uvic.chisel.atlantis.functionparsing.LightweightThreadFunctionBlock;
import ca.uvic.chisel.atlantis.models.AssemblyChangedEvent;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.TraceThreadEvent;

import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.atlantis.views.AtlantisIndexedFileDecorator;
import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.datacache.FileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.gibraltar.Gibraltar;


// This class follows a Singleton pattern
abstract public class AtlantisFileModelDataLayer extends FileModelDataLayer {
	
	protected Gibraltar gibraltar;

	public static boolean isFileIndexed(File file, File folder){
		// We want the file index as well as the DB index here.
		// Why did it used to be both? I need to change this to only check the DB...
		//		return FileLineFileBackend.checkIfFileHasIndex(file) && TraceFileLineDbConnection.checkIfFileHasIndex(file);
		return TraceFileLineDbConnection.checkIfFileHasIndexThatIsComplete(file, folder);
	}
	
	public AtlantisFileModelDataLayer(File blankFile) throws Exception {
		super(blankFile);
	}
	
	@Override
	protected void constructorInit() {
		File binaryFolder = AtlantisFileUtils.convertBlankFileToBinaryFormatDirectory(blankFile);
		// Need this before consumers are created. Could have done consumer creation after
		// but I am fond of self sufficient constructors.
		try {
			this.gibraltar = new Gibraltar(binaryFolder.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getFinalizingText(){
		return "Finalizing memory and function reconstruction...";
	}
	
	@Override
	public String getFileBuildText(){
		return "Reconstructing complete memory state, function calls...";
	}
		
	@Override
	public void dispose() {
		gibraltar.dispose();
	}
	
	@Override
	public void updateDecorator(){
		IDecoratorManager idm = PlatformUI.getWorkbench().getDecoratorManager();
		idm.update(AtlantisIndexedFileDecorator.ID);
	}
	
	abstract protected void createBackend() throws Exception;
	
	/**
	 * This is an early initialization method.
	 * 
	 * This method allows you define the different line consumers that will be used when reading the file.  This method
	 * is intended to be extended by anyone extending the framework.
	 */
	@Override
	protected ArrayList<IFileContentConsumer> createFileLineConsumers() throws Exception {
		createBackend();
		
		return gibraltar.getConsumers();
	}
	
	
	abstract public void removeIndexForFile(IFile file);
	
	//-----------------------------------------------------------------------\\
	//-----------------		    Line Data Stuff           -------------------\\
	//-----------------------------------------------------------------------\\
	public boolean isExecutionMode64Bit(int lineNumber){
		return gibraltar.binaryFileBackend.isExecutionMode64Bit(lineNumber);
	}
	
	
	//-----------------------------------------------------------------------\\
	//-----------------		Function Parsing Stuff        -------------------\\
	//-----------------------------------------------------------------------\\
	
	public InstructionDbConnection getInstructionDb() {
		return gibraltar.instructionDbBackend;
	}
	
	public BasicBlockDbConnection getBasicBlockDb() {
		return gibraltar.basicBlockDbBackend;
	}
	
	public JumpDbConnection getJumpDb() {
		return gibraltar.jumpDbBackend;
	}
	
	public FunctionDbConnection getFunctionDb() {
		return gibraltar.functionDbBackend;
	}
	
	public CallDbConnection getCallDb() {
		return gibraltar.callDbBackend;
	}
	
	public ThreadFunctionBlockDbConnection getThreadFunctionBlockDb() {
		return gibraltar.threadFunctionBlockDatabase;
	}
	
	public ThreadLengthDbConnection getThreadLengthDbConnection() {
		return gibraltar.threadLengthDatabase;
	}
	
	//-----------------------------------------------------------------------\\
	//---------------------		Other  Stuff      ---------------------------\\
	//-----------------------------------------------------------------------\\
	
	abstract public void getSearchResultsForTraceLines(String regexSearch, IFile iFileCurrentDoc, boolean useRegexSearch, TextSearchRequestor searchResults, CharSequence searchInput, SearchFileModelJob monitorUpdateJob)  throws CoreException;
	
	//-----------------------------------------------------------------------\\
	//------------------		Memory Delta Stuff        -------------------\\
	//-----------------------------------------------------------------------\\
	
	abstract public String getMemoryChangesForLine(int lineNumber);
	
	public  AsyncResult<MemoryQueryResults> getMemoryEventsAsync(int lineNumber, IProgressMonitor monitor) {
		
		if(monitor.isCanceled()) {
			return AsyncResult.cancelled();
		}
			
		AsyncResult<MemoryQueryResults> memoryReferencesResult = gibraltar.memoryAccessDatabase.getMemoryReferencesAsync(lineNumber, monitor);
		
		if(memoryReferencesResult.isCancelled()) {
			return AsyncResult.cancelled();
		}
		
		return new AsyncResult<>(gibraltar.memoryAccessDatabase.getMostRecentMemoryResults(), Status.OK_STATUS);
	}
	
	public boolean cancelMemoryRegisterEventsAsync(int lineNumber){
		// I don't have access to the monitor, so using the lineNumber is how I will avoid
		// clobbering a more recent query when the original has already been cancelled.
		return gibraltar.memoryAccessDatabase.cancelQueryForMemoryReferencesAsync(lineNumber);
	}
	
	//-----------------------------------------------------------------------\\
	//------------------	  Assembly Event Stuff        -------------------\\
	//-----------------------------------------------------------------------\\
	
	/**
	 * Get names of assembly for use in visualization labels.
	 * @return
	 */
	public List<String> getDistinctAssemblyEventNames() {
		return gibraltar.assemblyEventDatabase.getDistinctAssemblyNames();
	}
	
	public int getFinalAssemblyEventEndPoint() {
		return gibraltar.assemblyEventDatabase.getFinalAssemblyEventEndPoint();
	}
	
	@Deprecated
	public List<AssemblyChangedEvent> getAssemblyEvents(int startLine, int endLine) throws Exception {
		return gibraltar.assemblyEventDatabase.getAssemblyEventsInRange(startLine, endLine);
	}
	
	public List<AssemblyChangedEvent> getAssemblyEventInPixelRange(int startPixelCoordinate, int endPixelCoordinate) throws Exception {
		return gibraltar.assemblyEventDatabase.getAssemblyEventsInPixelRange(startPixelCoordinate, endPixelCoordinate);
	}
	
	public AssemblyChangedEvent getAssemblyEventContainingLine(int lineNumber) throws Exception {
		List<AssemblyChangedEvent> markers = gibraltar.assemblyEventDatabase.getAssemblyEventForLine(lineNumber);
		if(markers == null || markers.size() == 0){
			Throwable throwable = new Throwable("Missing assembly event for line "+lineNumber);
			BigFileApplication.showErrorDialog("Error navigating to line", "Your database appears to be incompatible with the current version of Atlantis. Please re-index trace files to fix. Error retrieving assembly event for line " + lineNumber, throwable );
			return null;
		}
		return markers.get(0);
	}
	
	//-----------------------------------------------------------------------\\
	//------------------	  Thread Event Stuff          -------------------\\
	//-----------------------------------------------------------------------\\
	
	/**
	 * Get names of threads for use in visualization labels.
	 * @return
	 */
	@Deprecated
	public List<String> getDistinctThreadNames() {
		return gibraltar.threadEventDatabase.getDistinctThreadNames();
	}
	
	@Deprecated
	public int getFinalThreadEventEndPoint() {
		return gibraltar.threadEventDatabase.getFinalThreadEventEndPoint();
	}
	
	@Deprecated
	public List<TraceThreadEvent> getThreadEvents(int startLine, int endLine) throws Exception {
		return gibraltar.threadEventDatabase.getThreadEvents(startLine, endLine);
	}
	
	@Deprecated
	public List<TraceThreadEvent> getThreadEventsInPixelRange(int startPixelCoordinate, int endPixelCoordinate) throws Exception {
		return gibraltar.threadEventDatabase.getThreadEventsInPixelRange(startPixelCoordinate, endPixelCoordinate);
	}
	
	@Deprecated
	public TraceThreadEvent getThreadEventContainingLine(int lineNumber) throws Exception {
		List<TraceThreadEvent> markers = gibraltar.threadEventDatabase.getThreadEventForLine(lineNumber);
		if(markers == null || markers.size() == 0){
			Throwable throwable = new Throwable("Missing thread event for line "+lineNumber);
			BigFileApplication.showErrorDialog("Error navigating to line", "Your database appears to be incompatible with the current version of Atlantis. Please re-index trace files to fix. Error retrieving thread event for line " + lineNumber, throwable );
			return null;
		}
		return markers.get(0);
	}
	
	public LightweightThreadFunctionBlock getThreadBlockContainingOrFollowingLine(int lineNumber) throws Exception {
		LightweightThreadFunctionBlock tfb = gibraltar.threadFunctionBlockDatabase.getLightweightThreadFunctionBlocksContainingOrFollowingLine(lineNumber);
		if(tfb == null){
			Throwable throwable = new Throwable("Missing thread event for line "+lineNumber);
			BigFileApplication.showErrorDialog("Error navigating to line", "Your database appears to be incompatible with the current version of Atlantis. Please re-index trace files to fix. Error retrieving thread function block for line " + lineNumber, throwable );
			return null;
		}
		return tfb;
	}
	
	//-----------------------------------------------------------------------\\
	//------------------	     Module Stuff             -------------------\\
	//-----------------------------------------------------------------------\\
	public ArrayList<ModuleRec> getAllModules() {
		// An uncommon case where we re-parse binary files rather than making a table.
		ArrayList<ModuleRec> modules = gibraltar.binaryFileBackend.getAllModules();
		return modules;
	}
	
	public long getFunctionRetLine(int startLineNumber) {
		return gibraltar.instructionDbBackend.getFunctionRetLine(startLineNumber);
	}

}