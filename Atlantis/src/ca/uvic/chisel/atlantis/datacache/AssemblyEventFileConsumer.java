package ca.uvic.chisel.atlantis.datacache;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.database.AssemblyEventDbConnection;
import ca.uvic.chisel.atlantis.database.DbConnectionManager.TableState;
import ca.uvic.chisel.atlantis.models.AssemblyChangedEvent;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;

public class AssemblyEventFileConsumer implements IFileContentConsumer<TraceLine> {

	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();
	
	private int startLineNum;
	private int lineNumber;
	private String lastId;
	private AssemblyChangedEvent lastEvent;
	private Pattern eventNames = Pattern.compile("\\s(\\w+)(\\.dll|\\.exe)?\\+[a-fA-F0-9]*");
	private final AssemblyEventDbConnection assemblyEventDatabase;
	
	public AssemblyEventFileConsumer(AssemblyEventDbConnection assemblyEventDatabase) {
		this.assemblyEventDatabase = assemblyEventDatabase;
	}

	@Override
	public boolean verifyDesireToListen() {
		assemblyEventDatabase.createTables();
		return assemblyEventDatabase.getTableState() != TableState.POPULATED;
	}
	
	/**
	 * This method should reset the state of the reader so that it will reset it's data from scratch.
	 */
	@Override
	public void readStart() {
		startLineNum = 0;
		lineNumber = 0;
		lastId = null;
		lastEvent = null;
	}
	
	@Override
	public int handleInputLine(String line, TraceLine lineData) {
		timeAccumulator.progCheckIn();
		Matcher eventMatcher = eventNames.matcher(line);
		if(eventMatcher.find()) {
			if(lastId == null) {
				startLineNum = lineNumber;
				lastId = StringUtils.defaultString(eventMatcher.group(1));
			} else if(!StringUtils.equals(lastId, eventMatcher.group(1))) {				
				// we are done one
				int pixelStartingPoint = (null == lastEvent) ? 0 : lastEvent.getPixelEnd();
				int actualStartLine = (null == lastEvent) ? 0 : startLineNum;
				AssemblyChangedEvent event = new AssemblyChangedEvent(lastId, actualStartLine, lineNumber - actualStartLine, pixelStartingPoint);
				timeAccumulator.dbCheckIn();
				this.assemblyEventDatabase.saveAssemblyEventMarker(event);
				timeAccumulator.progCheckIn();
				lastId = StringUtils.defaultString(eventMatcher.group(1));
				lastEvent = event;
				startLineNum = lineNumber;
			}
		}
		
		lineNumber++;
		timeAccumulator.checkOutAll();
		return 1;
	}

	@Override
	public void readFinished() {
		timeAccumulator.progCheckIn();
		if(lastId != null) {
			int pixelStartingPoint = (null == lastEvent) ? 0 : lastEvent.getPixelEnd();
			AssemblyChangedEvent event = new AssemblyChangedEvent(lastId, startLineNum, lineNumber - startLineNum, pixelStartingPoint);
			timeAccumulator.dbCheckIn();
			this.assemblyEventDatabase.saveAssemblyEventMarker(event);
			timeAccumulator.progCheckIn();
		}
		
		timeAccumulator.dbCheckIn();
		
		// Do final bulk save to database
		this.assemblyEventDatabase.executeInsertAssemblyEventMarkerBatch();
		this.assemblyEventDatabase.doFinalCommit();

		timeAccumulator.checkOutAll();
		
		System.out.println(this.getClass()+":"+"Total time to preparing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commitassembly event to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println();
	}
	
	@Override
	public void indexCreationAborted() {
		this.assemblyEventDatabase.abortCommitAndDropTable();
	}	
	
	@Override
	public boolean requiresPostProcessing() {
		return false;
	}

	@Override
	public void doPostProcessing(IProgressMonitor monitor) {}
}
