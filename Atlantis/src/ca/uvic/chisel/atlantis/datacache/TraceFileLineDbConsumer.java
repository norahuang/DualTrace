
package ca.uvic.chisel.atlantis.datacache;

import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.BatchUpdateException;

import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.database.TraceFileLineDbConnection;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;

// TODO properly handle the errors in this class
public class TraceFileLineDbConsumer implements IFileContentConsumer<TraceLine> {

	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();
	
	private TraceFileLineDbConnection traceDbConnection;
	
	private File traceFile;
	private long currentLineNumber;
	
	/**
	 * Current offset based off of total number of characters read so far.
	 */
	private long totalCharsRead;

	/**
	 * Used to track whether or not the last line that was saved to the database ended with a newline
	 * So that we can add an extra blank line to the storage.
	 */
	private boolean endsInNewline;
	
	public TraceFileLineDbConsumer(TraceFileLineDbConnection traceDbConnection, File traceFile) {
		this.traceFile = traceFile;
		this.traceDbConnection = traceDbConnection;
	}
	
	@Override
	public boolean verifyDesireToListen(){
		traceDbConnection.initialize();
		// Completed? Don't listen.
		return traceDbConnection.isFreshlyInitialized();
	}
	
	@Override
	public void readStart() {
		currentLineNumber = 0L;
		totalCharsRead = 0L;
		
		endsInNewline = true;
	}

	@Override
	public int handleInputLine(String line, TraceLine lineData) throws Exception {
		try {
			timeAccumulator.dbCheckIn();
			this.traceDbConnection.saveFileLine(currentLineNumber, totalCharsRead, line, lineData);
		} catch (Exception e) {
			this.indexCreationAborted();
			throw e;
		} finally {
			timeAccumulator.checkOutAll();
		}
		
		timeAccumulator.progCheckIn();
		totalCharsRead += line.length(); // Next offset
		
		if(!line.endsWith("\n") && !line.endsWith("\r\n")) {
			endsInNewline = false;
		}
		
		currentLineNumber++;
		
		timeAccumulator.checkOutAll();
		
		return 1;
	}
	
	@Override
	public void readFinished() {
		
		if(endsInNewline) {
			try{
				timeAccumulator.dbCheckIn();
				// null for the TraceLine object probably won't work out...dealing with a newline is annoying.
				this.traceDbConnection.saveFileLine(currentLineNumber, totalCharsRead, "", null);
			} catch (Exception e) {
				this.indexCreationAborted();
			} finally {
				timeAccumulator.checkOutAll();
			}
		}
		
		currentLineNumber++;
		
		timeAccumulator.dbCheckIn();
		
		// Do final bulk save to database
		try {
			this.traceDbConnection.executeInsertLineNumberBatch();
		} catch (BatchUpdateException e) {
			this.indexCreationAborted();
		}
		
		this.traceDbConnection.doFinalCommit();
		
		timeAccumulator.checkOutAll();
		
		System.out.println(this.getClass()+":"+"Total time to preparing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commit trace file to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println();
	}
	
	@Override
	public void indexCreationAborted(){
		this.traceDbConnection.abortCommitAndDropTable();
	}
	
	public static int findLastLine(File currentTraceDoc) {
		try {
			LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(currentTraceDoc.toPath(), StandardCharsets.ISO_8859_1));
			lnr.skip(currentTraceDoc.length());
			int lastLine = lnr.getLineNumber();
			lnr.close();
			return lastLine;
		} catch (Exception e) {}
		return 0;
	}	
	
	@Override
	public boolean requiresPostProcessing() {
		return false;
	}

	@Override
	public void doPostProcessing(IProgressMonitor monitor) {}
	
}
