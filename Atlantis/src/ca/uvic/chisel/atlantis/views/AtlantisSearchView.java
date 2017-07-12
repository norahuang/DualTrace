package ca.uvic.chisel.atlantis.views;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.ui.progress.IProgressConstants;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.datacache.BinaryFormatFileModelDataLayer;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.bfv.views.CombinedFileSearchView;

public class AtlantisSearchView extends CombinedFileSearchView {

	public static final String ID = "ca.uvic.chisel.atlantis.views.AtlantisSearchView";

	public void searchFunctions(final InstructionId targetInstructionId, final boolean searchCallerInsteadOfCallee) {	
			System.out.println("Searching database function table backend");
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			System.out.println("Starting database function table search at time "+dateFormat.format(date));

			RegexSearchInput dummyInput = new RegexSearchInput(targetInstructionId.toString(), false, false, false);
			try {
				if (dummyInput.getScope() != null) {
					dummyQuery = TextSearchQueryProvider.getPreferred().createQuery(dummyInput);
				}
				dummyQuery.run(null);
				searchResults.removeAll();

				Job searchJob = new Job("Search Assembly") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						setProperty(IProgressConstants.PROPERTY_IN_DIALOG, Boolean.FALSE);
						setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
						
						// Only search the currently active file
						IFileUtils fileUtil = RegistryUtils.getFileUtils();
						File f = fileUtil.convertBlankFileToActualFile(activeEditor.getCurrentBlankFile());
						currentFile = BfvFileUtils.convertFileIFile(f);

						// TODO Add the monitor to this search system
						if(searchCallerInsteadOfCallee){
							((BinaryFormatFileModelDataLayer)fileModel).performFunctionCallerSearch(searchResults, targetInstructionId, currentFile, (AtlantisTraceEditor) activeEditor);
						} else {
							((BinaryFormatFileModelDataLayer)fileModel).performFunctionSearch(searchResults, targetInstructionId, currentFile, (AtlantisTraceEditor) activeEditor);
						}

						return Status.OK_STATUS;
					}
				};
				searchJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						// XXX This should probably not be used here; fileModel use shouldn't be presumed
						// even though it is passed all the way down to DbTextSearchVisitor#locateMatches()
						// Instead, shall we make methods on FileDbSearchQuery to parallel the searchInFile() call hierarchy?
						// NB The cancel call should not affect us when we are done due to finishing, but allows us to cancel
						// when we are done due to the user clicking the Cancel button
						// NB It is fairly important to cancel the MySQL queries underlying this.
						// It seems like this listener should be passed into the deep places,
						// rather than being attached to a job at a level where no worked() call can conceivably be made on it,
						// but I copied Eclipse, and got it to function...
						fileModel.cancelCurrentlyRunningSearchStatement();
						IFile activeEmpty = BfvFileUtils.convertFileIFile(activeEditor.getEmptyFile());
						updateMatches(searchResults.getMatches(activeEmpty), targetInstructionId.toString());
						
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						System.out.println("Finishing database function table  search at time "+dateFormat.format(date));
					}
				});
				searchJob.setUser(true);
				searchJob.schedule();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	
}
