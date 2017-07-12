
package ca.uvic.chisel.bfv.datacache;

import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;


import ca.uvic.chisel.bfv.filebackend.FileLineFileBackend;

// TODO properly handle the errors in this class
/**
 * Consumes lines, feeding them back to the {@link FileLineFileBackend} to create
 * the file based index.
 */
public class FileLineFileBackendConsumer implements IFileContentConsumer<TextLine> {

	private FileLineFileBackend fileLineBackend;
	
	private long lastLineNumber = -1;
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
	
	public FileLineFileBackendConsumer(FileLineFileBackend fileLineBackend) {
		this.fileLineBackend = fileLineBackend;
	}
	
	@Override
	public boolean verifyDesireToListen(){
		fileLineBackend.initialize();
		return fileLineBackend.isFreshlyInitialized();
	}
	
	@Override
	public void readStart() {
		currentLineNumber = 0L;
		totalCharsRead = 0L;
		
		endsInNewline = true;
	}

	@Override
	public int handleInputLine(String line, TextLine lineData) {
		// Block of line is not relevant...this should be refactored out.
		this.fileLineBackend.saveFileLine(currentLineNumber, totalCharsRead, line, null);
		totalCharsRead += line.length(); // Next offset
		
		if(!line.endsWith("\n") && !line.endsWith("\r\n")) {
			endsInNewline = false;
		}
		
		currentLineNumber++;
		
		return 1;
	}
	
	@Override
	public void readFinished() {
		
		if(endsInNewline) {
			this.fileLineBackend.saveFileLine(currentLineNumber, totalCharsRead, "", null);
			currentLineNumber++;
		}
		
		
		// Close the file handle, rename the file, so we know we have completed processing on it.
		this.fileLineBackend.finish();
		
		// Since consumers run in parallel, and this has no significant final step of batch committing,
		// there is no point in reporting its duration.
		// System.out.println("Total time to insert and commit file to db: "+ (System.currentTimeMillis() - this.startTime)/1000+"s");
		System.out.println("File line indexing completed.");
	}
	
	@Override
	public void indexCreationAborted(){
		this.fileLineBackend.abortAndDeleteIndex();
	}
	
	public static int findLastLine(File currentFile) {
		try {
			LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(currentFile.toPath(), Charset.forName("ISO-8859-1")));
			lnr.skip(currentFile.length());
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
