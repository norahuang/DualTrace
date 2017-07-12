package ca.uvic.chisel.bfv.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.BigFileDocument;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.utils.IntervalUtils;
import ca.uvic.chisel.bfv.utils.RegionUtils;

public class DocumentContentManager {
	
	private static final int PAGE_RADIUS = 500;
	private static final int PAGE_OUT_DISTANCE = 500;
	IFileModelDataLayer fileModel;
	
	protected List<Interval> linesPagedIn;

	protected long numberOfLines;

	protected BigFileDocument document;
	protected final String fileDelimeterString;
	
	protected List<IntervalLineTracker> trackers;
	
	protected class DocumentChange {
		public List<Interval> getToPageIn() {
			return toPageIn;
		}

		public List<Interval> getToPageOut() {
			return toPageOut;
		}
		
		public List<Interval> getAllChangingIntervals() {
			List<Interval> all = new ArrayList<>(toPageIn);
			all.addAll(toPageOut);
			return all;
		}

		private List<Interval> toPageIn;
		private List<Interval> toPageOut;
		
		protected DocumentChange(List<Interval> toPageIn, List<Interval> toPageOut) {
			this.toPageIn = toPageIn;
			this.toPageOut = toPageOut;
		}
	}
	
	public DocumentContentManager(IFileModelDataLayer fileModel) throws Exception {
		this.fileModel = fileModel;
		linesPagedIn = new ArrayList<>();
		this.numberOfLines = fileModel.getNumberOfLines();
		this.fileDelimeterString = fileModel.getFileDelimiter();
		trackers = new ArrayList<>();
		System.gc();
	}
	
	public List<Interval> getLinesPagedIn() {
		ArrayList<Interval> result = new ArrayList<Interval>();
		result.addAll(linesPagedIn);
		return result;
	}
	
	/**
	 * This method creates the document that will be used by the BigFileViewer as its master document (not projection).
	 * It will create a BigFileDocument, in order to use our custom line tracker, which saves us memory.
	 * It will also page in the correct amount of empty lines into the document so that it starts off with zero lines
	 * paged in.
	 */
	public void initializeDocument() throws Exception {
		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
		progressDialog.run(true, false, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					int totalWork = (int) (fileDelimeterString.length() * (numberOfLines - 1));
					StringBuffer emptyLines = new StringBuffer((int) (fileDelimeterString.length() * (numberOfLines - 1)));
					monitor.beginTask("Preparing Editor...", totalWork);
					for(int i = 0; i < numberOfLines - 1; i++) {
						emptyLines.append(fileDelimeterString);
						monitor.worked(fileDelimeterString.length());
					}
					
					emptyLines.trimToSize();
					document = new BigFileDocument(DocumentContentManager.this);
					document.replace(0, 0, emptyLines.toString());
					monitor.worked(totalWork);
					monitor.done();
					updateDocument(0, 0);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public long getNumberOfLinesLong() {
		return numberOfLines;
	}
	
	public void resetLoadedIntervals() throws BadLocationException {
		pageOutIntervals(new ArrayList<>(linesPagedIn));
	}
	
	/**
	 * Determine which intervals must be paged in given the passed in startLine
	 */
	protected List<Interval> calculateIntervalsToLoad(int startLine) throws Exception {
		List<Interval> intervalsToPage = new ArrayList<>();
		
		List<IInterval> collapsedRegions = getCollapsedIntervals(fileModel.getRegions());
		
		// then page in 500 to the left
		List<Interval> pageInTheLeft = RegionUtils.pageInTheLeft(startLine, collapsedRegions, PAGE_RADIUS);
		intervalsToPage.addAll(pageInTheLeft);
		
		// first page in 500 to the right
		List<Interval> pageInToTheRight = RegionUtils.pageInToTheRight(startLine + 1, collapsedRegions, PAGE_RADIUS, (int)fileModel.getNumberOfLines() - 1);
		intervalsToPage.addAll(pageInToTheRight);
		return intervalsToPage;
	}
	
	/**
	 * Calculates the intervals that need to be paged out and paged in and returns them as a 
	 * DocumentChange object.  This can be used to predict what will happen before actually 
	 * performing a document update.
	 */
	public DocumentChange getDocumentChangesForVisibleInterval(int startLine, int endLine) throws Exception {
		List<Interval> toPageIn = new ArrayList<Interval>();
		List<Interval> toPageOut = new ArrayList<Interval>();
		
		if(!willUpdateChangeDocument(startLine, endLine)) {
			return new DocumentChange(toPageIn, toPageOut);
		}
		
		List<Interval> potentialPageIns = calculateIntervalsToLoad(startLine);
		
		toPageIn = IntervalUtils.mergeNewIntervalsWithOld(potentialPageIns, linesPagedIn);
		toPageOut = getIntervalsToPageOut(potentialPageIns);
		
		return new DocumentChange(toPageIn, toPageOut);
	}

	/**
	 * Returns the current document.
	 */
	public IDocument getDocument() {
		return document;
	}
	
	/**
	 * Returns true if the document content would be changed, false if it wouldn't.
	 * Does not actually perform the change that will occur.
	 */
	public boolean willUpdateChangeDocument(int startLine, int endLine) {
		
		List<Interval> coveredAreas = new ArrayList<>();
		coveredAreas.addAll(linesPagedIn);
		coveredAreas.addAll(RegionUtils.getCollapsedIntervals(fileModel.getRegions()));
		
		return !(IntervalUtils.completelyCovered(new Interval(startLine, endLine), coveredAreas));
	}
	
	/**
	 * Returns true if the document content is changed, false if it isn't changed.
	 */
	public boolean updateDocument(int startLine, int endLine) throws Exception {
		return updateDocument(startLine, endLine, getDocumentChangesForVisibleInterval(startLine, endLine));
	}
	
	/**
	 * Convenience method so that you can get both the lines loaded intervals and whether or not it was changed
	 * without calculating newIntervals twice
	 */
	public boolean updateDocument(int startLine, int endLine, DocumentChange documentChange) throws Exception {
		// Fix issue with invalid and excessive memory queries when paging occurs
		pagingOngoing = true;
		
		boolean updated = false;
		
		// the startLine and endLine must be covered or else we need to get new lines
		if(willUpdateChangeDocument(startLine, endLine)) {
			// page in the new data
			pageInNewIntervals(documentChange.getToPageIn());
			
			updated = true;
		}
		
		pageOutIntervals(documentChange.getToPageOut());
		
		pagingOngoing = false;
		
		return updated;
	}
	public boolean pagingOngoing = false;

	
	public boolean lineLoaded(int lineNum) {
		return loadedIntervalsContainLine(lineNum);
	}
	
	/**
	 * Iterates through the current set of intervals, and if any of them are too distant from the current visible interval
	 * we page them out.
	 */
	private void pageOutIntervals(List<Interval> toRemove) throws BadLocationException {
		for(Interval removal : toRemove) {
			int intervalStart = (int)removal.getStartValue();
			int intervalEnd = (int)removal.getEndValue();
			
			// the case where we are looking at the last line in the file is a little different, we cannot look at the beginning of the next line for our offset,
			// and we must add 1 less newline to the file.
			if(intervalEnd + 1 == document.getNumberOfLines()) {
				int currentDocStartOffset = document.getLineOffset(intervalStart);
				
				int currentDocOffsetLength = document.getLength() - document.getLineOffset(intervalStart);
				
				alertTrackersPagingChanges(removal, IntervalEvent.PAGE_OUT);
				
				document.replace(currentDocStartOffset, currentDocOffsetLength, StringUtils.repeat(fileDelimeterString, (intervalEnd) - intervalStart));
				linesPagedIn.remove(removal);
				
			} else {
				int currentDocStartOffset = document.getLineOffset(intervalStart);
				int currentDocOffsetLength = document.getLineOffset(intervalEnd + 1) - document.getLineOffset(intervalStart);
				
				alertTrackersPagingChanges(removal, IntervalEvent.PAGE_OUT);
				
				// This interval will be removed from lines paged in when replace is called
				document.replace(currentDocStartOffset, currentDocOffsetLength, StringUtils.repeat(fileDelimeterString, (intervalEnd + 1) - intervalStart));
				linesPagedIn.remove(removal);
			}
		}
	}

	private List<Interval> getIntervalsToPageOut(List<Interval> newIntervals) {
		List<Interval> currentIntervals = new ArrayList<Interval>(linesPagedIn);
		List<Interval> toPageOut = new ArrayList<>();
		
		for(Interval interval : currentIntervals) {
			boolean pageOut = true;
			for(Interval newInterval : newIntervals) {
				int distance = Math.max((int)interval.getStartValue() - (int)newInterval.getEndValue(), (int)newInterval.getStartValue() - (int)interval.getEndValue());
				
				if(distance <= PAGE_OUT_DISTANCE) {
					pageOut = false;
					break;
				}
			}
			
			if(pageOut) {
				toPageOut.add(interval);
			}
		}
		
		return toPageOut;
	}

	/**
	 * Replaces the content of the document with the contents of each new interval.
	 */
	private void pageInNewIntervals(List<Interval> newIntervals) throws Exception {
		for(Interval interval : newIntervals) {
			int intervalStart = (int)interval.getStartValue();
			int intervalEnd = (int)interval.getEndValue();
			
			long numLines = fileModel.getNumberOfLines();
			if(numLines == 0) {
				return;
			}
			
			// fileModel uses 0 indexed file lines
			String data = fileModel.getFileLines(intervalStart, intervalEnd);
			
			// But when I increment intervalEnd like I do in pageOutDistantIntervals(), it creates problems!
			// But I need to in order to prevent the final empty line from showing up in the viewer...
			// The blank line is the lesser of evils at the moment, and I can't figure it out.
			int currentDocStartOffset = document.getLineOffset(intervalStart);
			int currentDocOffsetLength;
			
			// this if else check is important if there is only 1 line in the file
			if(intervalEnd + 1 == numLines) {
				currentDocOffsetLength = document.getLength() - currentDocStartOffset;
			} else {
				currentDocOffsetLength = document.getLineOffset(intervalEnd + 1) - currentDocStartOffset;
			}

			alertTrackersPagingChanges(interval, IntervalEvent.PAGE_IN);
			document.replace(currentDocStartOffset, currentDocOffsetLength, data);
			linesPagedIn.add(interval);
		}
	}

	private void alertTrackersPagingChanges(Interval interval, IntervalEvent event) {
		for(IntervalLineTracker tracker : trackers) {
			tracker.registerIntervalChange(interval, event);
		}
	}

	/**
	 * @return 	true if the lineNum is contained in at least one of the current intervals.
	 * 			false otherwise.
	 */
	private boolean loadedIntervalsContainLine(int lineNum) {
		return IntervalUtils.lineContainedInIntervals(lineNum, this.linesPagedIn);
	}
	
	public IntervalLineTracker createLineTracker() {
		IntervalLineTracker tracker = new IntervalLineTracker(this, fileModel);
		trackers.add(tracker);
		return tracker;
	}
	
	protected List<IInterval> getCollapsedIntervals(Collection<RegionModel> regions) {
		List<IInterval> collapsedRegions =  new ArrayList<>();
		collapsedRegions.addAll(RegionUtils.getCollapsedIntervals(regions));
		return collapsedRegions;
	}
	
	public String getFileDelimeterString() {
		return fileDelimeterString;
	}
}
