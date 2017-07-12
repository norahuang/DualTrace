package ca.uvic.chisel.atlantis.views.search.binaryformat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

/**
 * This Job class mimics similar usage in the existing Eclipse search functionality.
 * It is used as a separate job to update the progress meter of the outer job.
 * I would bypass this structure and make the progress monitor made deeper down,
 * but Eclipse classes require a ProgressMonitor to be passed in, and I need a listener
 * attached to the progress monitor that deals with the search, and I cannot pass the
 * listener in due to method signatures. So, this is odd, but there was some insane
 * reasons behind it. 
 *
 */
public class SearchFileModelJob extends Job { // Move this to another file?
	// Used to track the *files* searched, but we're going to track the lines searched
	private IFileModelDataLayer fileModel;
	private final long fTotalLinesToScan;
	private final long numberOfLinesForMonitorIncrement;
	
	private long fLastNumberOfScannedLines = 0;
	private long fNumberofScannedLines = 0;
	private long fPrevLinesCountedToMeter = 0;
	
	private IProgressMonitor fProgressMonitor;

	private IFile fCurrentFile;
	
	// If we allow searching multiple files, work the outer fields below into this:
	// fNumberOfScannedFiles= 0;
    // fNumberOfFilesToScan= files.length;

	public SearchFileModelJob(IFileModelDataLayer fileModel, IFile currentFile, IProgressMonitor progressMonitor) {
		super(SearchMessages.TextSearchVisitor_progress_updating_job);
		this.fileModel = fileModel;
		this.fTotalLinesToScan = this.fileModel.getNumberOfLines();
		this.numberOfLinesForMonitorIncrement = (int)Math.ceil(Math.max(1.0, (double)this.fTotalLinesToScan/(double)Integer.MAX_VALUE));

		this.fCurrentFile = currentFile;
		this.fProgressMonitor = progressMonitor;
    }
	
	public long getTotalNumberOfLines() {
		return fTotalLinesToScan;
	}

	public void incrementLinesScanned(int incrementBy){
		this.fNumberofScannedLines += incrementBy;
		this.updateProgress();
	}
	
	@Override
	public IStatus run(IProgressMonitor inner) {
		// We have two progress meters...the original class TextSearchVisitor uses the inner for the loop,
		// and the fProgressMonitor for the cancellation and worked() calls.
		// Don't mess with it!
		while (!fProgressMonitor.isCanceled()) { // Don't need the inner progress monitor really... !inner.isCanceled()
			IFile file= fCurrentFile;
			if (file != null) {
				// I adapted the progress meter to work with lines, but didn't clean up the multi-file code.
				// If we need multi-file search, this needs fixing, otherwise it's just dandy.
				Object[] args= {fCurrentFile.getName(), new Long(this.fNumberofScannedLines), new Long(this.fTotalLinesToScan)};
				fProgressMonitor.subTask(Messages.format(SearchMessages.TextSearchVisitor_scanning, args));
				long steps= this.fNumberofScannedLines - fLastNumberOfScannedLines;
				fProgressMonitor.worked((int)steps);
				this.fLastNumberOfScannedLines = this.fNumberofScannedLines;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return Status.OK_STATUS;
			}
		}
		return Status.OK_STATUS;
	}
	
	private void updateProgress(){
		if(fNumberofScannedLines >= this.fPrevLinesCountedToMeter + numberOfLinesForMonitorIncrement){
			fProgressMonitor.worked(Math.round((fNumberofScannedLines - this.fPrevLinesCountedToMeter)/numberOfLinesForMonitorIncrement)); // Easier than detecting line lengths in bytes
			this.fPrevLinesCountedToMeter = this.fNumberofScannedLines;
		}
	}
}
