package ca.uvic.chisel.atlantis.views.search.db;

import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.core.text.FileCharSequenceProvider;
import org.eclipse.search.internal.core.text.FileCharSequenceProvider.FileCharSequenceException;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.NewSearchUI;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.views.search.binaryformat.SearchFileModelJob;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

/**
 * The visitor that does the actual work.
 */
public class DbTextSearchVisitor {

	public static class ReusableMatchAccess extends TextSearchMatchAccess {

		private int fOffset;
		private int fLength;
		private IFile fFile;
		private String fContent;
		private int fLineNumber;

		public void initialize(IFile file, int offset, int length, String content, int lineNumber) {
			fFile= file;
			fOffset= offset;
			fLength= length;
			fContent= content;
			fLineNumber = lineNumber;
		}
		
		public int getLineNumber(){
			return fLineNumber;
		}

		@Override
		public IFile getFile() {
			return fFile;
		}

		@Override
		public int getMatchOffset() {
			return fOffset;
		}

		@Override
		public int getMatchLength() {
			return fLength;
		}

		@Override
		public int getFileContentLength() {
			return fContent.length();
		}

		@Override
		public char getFileContentChar(int offset) {
			return fContent.charAt(offset);
		}

		@Override
		public String getFileContent(int offset, int length) {
			return fContent.subSequence(offset, offset + length).toString(); // must pass a copy!
		}
		
		public String getFileContent() {
			return fContent;
		}
	}


	private final DbTextSearchResultCollector fCollector;
	// private final Matcher fMatcher;
	// Got rid of matcher, using string instead
	private final String fInputPatternString;
	private final boolean fUseRegexSearch;

	private IProgressMonitor fProgressMonitor;

	private int fNumberOfScannedFiles;
	private int fNumberOfFilesToScan;
	private IFile fCurrentFile;

	private final MultiStatus fStatus;

	private final FileCharSequenceProvider fFileCharSequenceProvider;

	private final ReusableMatchAccess fMatchAccess;

	public DbTextSearchVisitor(TextSearchRequestor collector, Pattern searchPattern, boolean useRegexSearch) {
		// Didn't want to rewrite the interface, but I want to force out extension of the TextSearchRequestor.
		// Wait, is that truly necessary? Is ours actually different than the one it is based on?
		fCollector= (DbTextSearchResultCollector) collector;
		fUseRegexSearch = useRegexSearch;
		fStatus= new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK, SearchMessages.TextSearchEngine_statusMessage, null);

		// fMatcher= searchPattern.pattern().length() == 0 ? null : searchPattern.matcher(new String());
		// Leave incoming Pattern because the larger framework uses it.
		fInputPatternString = searchPattern.toString();

		fFileCharSequenceProvider= new FileCharSequenceProvider();
		fMatchAccess= new ReusableMatchAccess();
	}

	public IStatus search(IFile[] files, IProgressMonitor parentRunMonitor, IFileModelDataLayer fileModel) {
		fProgressMonitor= parentRunMonitor == null ? new NullProgressMonitor() : parentRunMonitor;
		// If we search multiple files, we may need to copy the Eclipse code a bit closer...
        fNumberOfScannedFiles= 0;
        fNumberOfFilesToScan= files.length;
        fCurrentFile= null;
        
        SearchFileModelJob monitorUpdateJob = new SearchFileModelJob(fileModel, fCurrentFile, fProgressMonitor);

        try {
        	String taskName= fInputPatternString == null ? SearchMessages.TextSearchVisitor_filesearch_task_label :  Messages.format(SearchMessages.TextSearchVisitor_textsearch_task_label, fInputPatternString);
        	
            fProgressMonitor.beginTask(taskName, (int)monitorUpdateJob.getTotalNumberOfLines());
            monitorUpdateJob.setSystem(true);
            monitorUpdateJob.schedule();
            try {
	            fCollector.beginReporting();
	            
	            // Likely jam this here, bypass the processFiles() method.
	            // See related files if we need multi-file search.
	            fCurrentFile= files[0];
	            boolean res= processFile(fCurrentFile, fileModel, monitorUpdateJob);
	            
	            return fStatus;
            } finally {
                monitorUpdateJob.cancel();
            }
        } finally {
            fProgressMonitor.done();
            monitorUpdateJob.done(Status.OK_STATUS);
            fileModel.cancelCurrentlyRunningSearchStatement();
            fCollector.endReporting();
        }
	}
	
	

	public IStatus search(TextSearchScope scope, IProgressMonitor parentRunMonitor, IFileModelDataLayer fileModel) {
		return search(scope.evaluateFilesInScope(fStatus), parentRunMonitor, fileModel);
    }

	/**
	 * @return returns a map from IFile to IDocument for all open, dirty editors
	 */
	private Map evalNonFileBufferDocuments() {
		Map result= new HashMap();
		IWorkbench workbench= SearchPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorReference[] editorRefs= pages[x].getEditorReferences();
				for (int z= 0; z < editorRefs.length; z++) {
					IEditorPart ep= editorRefs[z].getEditor(false);
					if (ep instanceof ITextEditor && ep.isDirty()) { // only dirty editors
						evaluateTextEditor(result, ep);
					}
				}
			}
		}
		return result;
	}

	private void evaluateTextEditor(Map result, IEditorPart ep) {
		IEditorInput input= ep.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			if (!result.containsKey(file)) { // take the first editor found
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				ITextFileBuffer textFileBuffer= bufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
				if (textFileBuffer != null) {
					// file buffer has precedence
					result.put(file, textFileBuffer.getDocument());
				} else {
					// use document provider
					IDocument document= ((ITextEditor) ep).getDocumentProvider().getDocument(input);
					if (document != null) {
						result.put(file, document);
					}
				}
			}
		}
	}

	public boolean processFile(IFile file, IFileModelDataLayer fileModel, SearchFileModelJob monitorUpdateJob) {
		// I excluded the possibility of multi-file searching here. Check related classes for how this looks if needed.
		IPath candidatePath = file.getProjectRelativePath();
		try {
			// if (!fCollector.acceptFile(file) || fMatcher == null) {
		    if (!fCollector.acceptFile(file) || fInputPatternString == null) {
		    	return true;
		    }
	
			locateMatches(file, null, fileModel, monitorUpdateJob);
	
		} catch (UnsupportedCharsetException e) {
			String[] args= { getCharSetName(file), candidatePath.makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_unsupportedcharset, args);
			fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
		} catch (IllegalCharsetNameException e) {
			String[] args= { getCharSetName(file), candidatePath.makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_illegalcharset, args);
			fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
		} catch (CoreException e) {
			String[] args= { getExceptionMessage(e), candidatePath.makeRelative().toString()};
			String message= Messages.format(SearchMessages.TextSearchVisitor_error, args);
			fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
		} catch (StackOverflowError e) {
			String message= SearchMessages.TextSearchVisitor_patterntoocomplex0;
			fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
			return false;
		} finally {
			fNumberOfScannedFiles++;
		}
		if (fProgressMonitor.isCanceled())
			throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);

		return true;
	}

	private boolean hasBinaryContent(CharSequence seq, IFile file) throws CoreException {
		IContentDescription desc= file.getContentDescription();
		if (desc != null) {
			IContentType contentType= desc.getContentType();
			if (contentType != null && contentType.isKindOf(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT))) {
				return false;
			}
		}

		// avoid calling seq.length() at it runs through the complete file,
		// thus it would do so for all binary files.
		try {
			int limit= FileCharSequenceProvider.BUFFER_SIZE;
			for (int i= 0; i < limit; i++) {
				if (seq.charAt(i) == '\0') {
					return true;
				}
			}
		} catch (IndexOutOfBoundsException e) {
		} catch (FileCharSequenceException ex) {
			if (ex.getCause() instanceof CharConversionException)
				return true;
			throw ex;
		}
		return false;
	}

	// This is where we needed to gt DB search set up, approximately.
	// I only needed to come this deep because when I have it shallower,
	// the results do not get sorted *in the GUI*, and I couldn't see a way to force them to be.
	// Getting deeper worked, luckily.
	private void locateMatches(IFile file, CharSequence searchInput, IFileModelDataLayer fileModel, SearchFileModelJob monitorUpdateJob) throws CoreException {
		
		if(fileModel != null && fileModel instanceof AtlantisFileModelDataLayer) {
			
			AtlantisFileModelDataLayer dataLayer = (AtlantisFileModelDataLayer) fileModel;
			
			// Maybe the results from this page and we call it multiple times...maybe...
			dataLayer.getSearchResultsForTraceLines(fInputPatternString, file, fUseRegexSearch, fCollector, searchInput, monitorUpdateJob);
		}
		

		// TODO In the old locateMatches() code, it looked at the fMatcher [Matcher class], and saw if the
		// search from the most recent find() call was of length zero, and excluded it if so.
		// In order to do that, or to highlight the matched region of the line, we'd need
		// to apply the search here as well as lower down in the DB.
		
//			try {
//				fMatcher.reset(searchInput);
//				int k= 0;
//				while (fMatcher.find()) {
//					int start= fMatcher.start();
//					int end= fMatcher.end();
//					if (end != start) { // don't report 0-length matches
//						fMatchAccess.initialize(file, start, end - start, searchInput);
//						boolean res= fCollector.acceptPatternMatch(fMatchAccess);
//						if (!res) {
//							return; // no further reporting requested
//						}
//					}
//					if (k++ == 20) {
//						if (fProgressMonitor.isCanceled()) {
//							throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
//						}
//						k= 0;
//					}
//				}
//			} finally {
//				fMatchAccess.initialize(null, 0, 0, new String()); // clear references
//			}
	}


	private String getExceptionMessage(Exception e) {
		String message= e.getLocalizedMessage();
		if (message == null) {
			return e.getClass().getName();
		}
		return message;
	}

	private IDocument getOpenDocument(IFile file, Map documentsInEditors) {
		IDocument document= (IDocument)documentsInEditors.get(file);
		if (document == null) {
			ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
			ITextFileBuffer textFileBuffer= bufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			if (textFileBuffer != null) {
				document= textFileBuffer.getDocument();
			}
		}
		return document;
	}

	private String getCharSetName(IFile file) {
		try {
			return file.getCharset();
		} catch (CoreException e) {
			return "unknown"; //$NON-NLS-1$
		}
	}

}

