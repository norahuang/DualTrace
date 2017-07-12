package ca.uvic.chisel.bfv.editor;

import static ca.uvic.chisel.bfv.editor.IntervalEvent.PAGE_IN;
import static ca.uvic.chisel.bfv.editor.IntervalEvent.PAGE_OUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ILineTracker;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.LineRegion;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalStartLineComparator;
import ca.uvic.chisel.bfv.utils.BfvStringUtils;

/**
 * This class is responsible for tracking the lineNumber -> offset and offset -> lineNumber conversion
 * of a document.  This class acts as BFV's replacement for the DefaultLineTracker, which is incredibly
 * memory inefficient, and created memory limitations when opening larger files.
 * 
 * It keeps a list of intervals, spanning the lines in the document, and classifies them 3 different ways.
 * 
 * 		The first classification is that the lines are empty, which means they contain only a newline.
 * 
 * 		The second classification is that they are paged in lines, and an offset is kept to track which line in the original document
 * 		they represent.
 * 
 * 		The final classification is that they contain text that we cannot map to the original document, and must keep track of
 * 		independantly.
 * 
 * This class also keeps track of mapping {@code offsetChanges} that can occur if the document it is tracking for 
 * is a projection document.  For example, if a replace occurs which replaces some text with empty text, that will create
 * a positive offset change.  A text insert will create a negative offset change.
 * 
 * In order to get the paged in offset lengths of lines, it uses the index file that is used by the document content manager.
 */
public class IntervalLineTracker implements ILineTracker {

	/**
	 * This class represents a change in the offset between projection document line, and master document line.
	 * This can occur when text is either inserted or deleted from the document.  The idea is that the projected
	 * line number + offset change = the master document line number.
	 */
	private class ProjectionOffsetChange {
		protected long lineNumber;
		protected long lineOffset;
		
		private ProjectionOffsetChange(long lineNumber, long lineOffset) {
			this.lineNumber = lineNumber;
			this.lineOffset = lineOffset;
		}

		public void setLineNumber(long lineNumber) {
			this.lineNumber = lineNumber;
		}

		/**
		 * Gets the line number that this offset change starts at.  This represents the line where text was either
		 * inserted or deleted at.
		 */
		public long getLineNumber() {
			return lineNumber;
		}

		/**
		 * Gets the offset value at the line in order to map the projection document line to the master
		 * document line.
		 */
		public long getLineOffset() {
			return lineOffset;
		}
		
		@Override
		public String toString() {
			
			if(lineOffset > 0) {
				return "+" + lineOffset + "@" + lineNumber;
			}
			
			return lineOffset + "@" + lineNumber;
		}
	}
	
	protected DocumentContentManager documentContentManager;
	protected IFileModelDataLayer fileModel;

	/**
	 * keeps track of page in or page out events that we are expecting to see soon.
	 */
	protected Map<IntervalEvent, Interval> incomingIntervalEvents;
	
	/**
	 * Keeps track of the different intervals of the document and what they are.  Each interval
	 * is either a paged in interval, a paged out interval, or an unexpected text interval.  The
	 * intervals will always sequentially cover all lines in the document.
	 */
	protected List<DocumentProjectionInterval> projectionIntervals;
	protected IntervalStartLineComparator intervalComparator = new IntervalStartLineComparator();
	
	protected boolean initialized = false;
	protected List<DocumentProjectionInterval> initialProjectionIntervals;
	
	protected List<ProjectionOffsetChange> offsetChanges;
	
	/**
	 * Comparator used to sort offset changes by their line number.
	 */
	private Comparator<ProjectionOffsetChange> offsetChangeComparator  = new Comparator<ProjectionOffsetChange>() {
		@Override
		public int compare(ProjectionOffsetChange o1, ProjectionOffsetChange o2) {
			return Long.compare(o1.lineNumber, o2.lineNumber);
		}
	};

	// This method is only intended to be called from the DocumentContentManager class
	public IntervalLineTracker(DocumentContentManager documentContentManager, IFileModelDataLayer fileModel) {
		this.documentContentManager = documentContentManager;
		this.fileModel = fileModel;
		
		incomingIntervalEvents = new HashMap<>();
		projectionIntervals = new ArrayList<>();
		
		initialProjectionIntervals = getInitialProjectionIntervals(documentContentManager);
		offsetChanges = new ArrayList<>();
		
	}

	/**
	 * Initializes the projection intervals with the intervals that are already paged in by the document content manager
	 * at the point at which this tracker is created.  This is the most important for projection document trackers, since
	 * the projection tracker may not be initialized until a first region is created, at which point much of the document
	 * may already be paged in.
	 */
	protected List<DocumentProjectionInterval> getInitialProjectionIntervals(DocumentContentManager documentContentManager) {
		List<Interval> pagedInIntervals = documentContentManager.getLinesPagedIn();
		Collections.sort(pagedInIntervals, intervalComparator);
		
		List<DocumentProjectionInterval> intitialProjectionIntervals = new ArrayList<>();
		long currentStartLine = 0;
		
		for(Interval pagedIn : pagedInIntervals) {
			
			if(currentStartLine < pagedIn.getStartValue()) {
				intitialProjectionIntervals.add(new PagedOutProjectionInterval(currentStartLine, pagedIn.getStartValue() - 1, getDelimiter().length()));
			}
			
			intitialProjectionIntervals.add(new PagedInProjectionInterval(fileModel, pagedIn.getStartValue(), pagedIn.getEndValue(), 0));
			currentStartLine = pagedIn.getEndValue() + 1;
		}
		
		long lastLine = documentContentManager.getNumberOfLinesLong() - 1;
		
		if(currentStartLine < lastLine) {
			intitialProjectionIntervals.add(new PagedOutProjectionInterval(currentStartLine, lastLine, getDelimiter().length()));
		}
		return intitialProjectionIntervals;
	}
	
	/**
	 * Return the file delimiter string use by the document content manager.  We assume that
	 * only one delimiter is used in the whole file.
	 */
	@Override
	public String[] getLegalLineDelimiters() {
		return new String[] {documentContentManager.getFileDelimeterString()};
	}

	/**
	 * Return the file delimiter string use by the document content manager.  We assume that
	 * only one delimiter is used in the whole file.  If the last line is passed in an
	 * empty string is returned.
	 */
	@Override
	public String getLineDelimiter(int line) throws BadLocationException {
		String delim = getLineDelimString(line);
		return delim.isEmpty() ? null : delim;
	}
	
	/**
	 * Return the file delimiter string use by the document content manager.  We assume that
	 * only one delimiter is used in the whole file.  If the last line is passed in an
	 * empty string is returned.
	 */
	public String getLineDelimString(int line) throws BadLocationException {
		if(isLastLine(line)) {
			return "";
		}
		
		return documentContentManager.getFileDelimeterString();
	}

	/**
	 * Count the number of lines in a string.
	 */
	@Override
	public int computeNumberOfLines(String text) {
		Matcher m = Pattern.compile("\n").matcher(text);
		int count = 0;
		while (m.find()) {
		    count++;
		}
		return count;
	}

	/**
	 * returns the number of lines that are contained in the document.
	 */
	@Override
	public int getNumberOfLines() {
		if(projectionIntervals.isEmpty()) {
			return 1;
		}

		Collections.sort(projectionIntervals, intervalComparator);
		return (int)projectionIntervals.get(projectionIntervals.size() - 1).getEndValue() + 1;
	}

	/**
	 * Returns the number of lines starting at a given character offset, with a given length (of characters).
	 */
	@Override
	public int getNumberOfLines(int offset, int length) throws BadLocationException {
		int startLine = getLineNumberOfOffset(offset);
		int endLine = getLineNumberOfOffset(offset + length);
		return endLine - startLine + 1;
	}

	/**
	 * Get the character offset of a given line.
	 */
	@Override
	public int getLineOffset(int line) throws BadLocationException {
		
		if(line < 0) {
			return 0;
		}
		
		return getLineInformation(line).getOffset();
	}

	/**
	 * Returns true if the line passed in is greater than or equal to the last line in the document.
	 * false otherwise.
	 */
	protected boolean isLastLine(long line) {
		return line >= getNumberOfLines() - 1;
	}
	
	/**
	 * Returns the number of characters of a line in the document (including the delimiter character).
	 * The projection intervals themselves are responsible for calculating these values.
	 */
	@Override
	public int getLineLength(int line) throws BadLocationException {
		if(!isLinePagedIn(line) && isLastLine(line)) {
			// Special Case: where it is the last line and not paged in, there is no newline (TODO maybe this should be handled by the interval itself)
			return 0;
		} else {
			return (int)getProjectionIntervalForLine(line).getIntervalCharLength(line);
		}
	}

	/**
	 * Returns true if the projection interval of the line passed in is a paged in interval.  Both 
	 * actual paged in intervals, as well as unpredicted intervals are considered paged in.  This assumption 
	 * works because even if an unpredicted intervals never contain empty lines.
	 */
	protected boolean isLinePagedIn(int line) throws BadLocationException {
		return getProjectionIntervalForLine(line).isPagedIn();
	}

	/**
	 * Returns the document projection interval which contains the line passed in.  This will throw a bad location exception
	 * if you ask it for a line that is past the end of the document.  This should not be possible to fail if you ask
	 * for a line number that is not past the end of the document.
	 */
	protected DocumentProjectionInterval getProjectionIntervalForLine(int line) throws BadLocationException {
		DocumentProjectionInterval matchingInterval = null;
		for(DocumentProjectionInterval interval : projectionIntervals) {
			if(interval.contains(line)) {
				matchingInterval = interval;
			}
		}
		
		if(matchingInterval == null) {
			throw new BadLocationException("We found a line that was not contained in our intervals, this should not have been possible.");
		}
		
		return matchingInterval;
	}

	/**
	 * Returns the line number that the passed in offset corresponds to.
	 */
	@Override
	public int getLineNumberOfOffset(int offset) throws BadLocationException {
		return (int) getLineInformationOfOffset(offset).getLineNum();
	}

	/**
	 * Calculates the line information for the line that corresponds with the given offset.  To do so, it iterates
	 * through the different projection intervals and sums up their offset lengths until they sum up to the offset passed in.
	 * At that point it knows that it is on the correct line.
	 */
	@Override
	public LineRegion getLineInformationOfOffset(int offset) throws BadLocationException {
		int totalOffset = 0;
		int delimiterLength = getDelimiter().length();
		
		for(DocumentProjectionInterval projectionInterval : projectionIntervals) {
			int offsetRemaining = offset - totalOffset;
			long intervalOffsetLength = projectionInterval.getIntervalCharLength();
			
			if(intervalOffsetLength > offsetRemaining) {
				// at this point we know that the line we want is somewhere past the projection interval that we are looking at.
				if(!projectionInterval.isPagedIn()) {
					int lineOffsetFromIntervalStart = offsetRemaining / delimiterLength;
					
					return new LineRegion(offset, delimiterLength, projectionInterval.getStartValue() + lineOffsetFromIntervalStart, delimiterLength);
				} else {
					// at this point we know that the line we want is somewhere inside the projection interval that we are looking at.
					for(long line=projectionInterval.getStartValue(); line <= projectionInterval.getEndValue(); line++) {
						
						long lineOffsetLength = getLineLength((int)line);
						
						if(lineOffsetLength > offsetRemaining) {
							String lineDelim = getLineDelimString((int)line);
							return new LineRegion(totalOffset, lineOffsetLength, line, lineDelim.length());
						}
						
						totalOffset += lineOffsetLength;
						offsetRemaining = offset - totalOffset;
					}
				}
			}
			totalOffset += intervalOffsetLength;
		}
		
		// we are after all of the intervals, that means that we are on the last line already
		return getLineInformation(getNumberOfLines() - 1);
	}

	/**
	 * Calculates the line information (including the offset) of the given line.  It does this
	 * by iterating through the projection intervals and summing their offsets until it reaches the line 
	 * that we are interested in.
	 */
	@Override
	public LineRegion getLineInformation(int line) throws BadLocationException {
		long totalOffset = 0;
		
		for(DocumentProjectionInterval projectionInterval : projectionIntervals) {
			if(line > projectionInterval.getEndValue()) {
				// add the whole thing
				totalOffset += projectionInterval.getIntervalCharLength();
			} else if(line > projectionInterval.getStartValue()) {
				// add part of the thing
				totalOffset += projectionInterval.getIntervalCharLength(projectionInterval.getStartValue(), (long) (line - 1));
			} else {
				// we are past the end, the sorting prevents any more intervals applying
				break;
			}
		}
		
		int lastLine = getNumberOfLines() - 1;
		if(line > lastLine) {
			return new LineRegion(totalOffset, 0, line, 0);
		}
		
		return new LineRegion(totalOffset, getLineLength(line), line, getLineDelimString(line).length());
	}

	
	/**
	 * This method handles all of the projection interval changes that are caused by the change to the
	 * document.  One of three kinds of replaces can happen.  The first is where an expected region of the 
	 * document is paged in.  The second is where a part of the document is just deleted (corresponding 
	 * to a region collapsing in a projection document).  The last is where lines are inserted.  The three cases
	 * are all handled differently, see the comments inside the method to see how.
	 */
	@Override
	public void replace(int offset, int length, String text) throws BadLocationException {
		Interval linesBeingReplaced = null;
		
		if(!projectionIntervals.isEmpty()) {
			linesBeingReplaced = getLinesCovered(offset, length);
		}
		
		if("".equals(text) && length != 0) {
			
			int oldLastLine = getNumberOfLines() - 1;
			// remove the current projection intervals in that range.
			clearSlotForProjectionInterval(linesBeingReplaced);
			
			long numLinesReplaced = linesBeingReplaced.length();
			
			// if this removes the last line, add in an empty last line.
			if(linesBeingReplaced.getEndValue() == oldLastLine) {
				projectionIntervals.add(new PagedOutProjectionInterval(linesBeingReplaced.getEndValue(), linesBeingReplaced.getEndValue(), getDelimiter().length()));
				numLinesReplaced--;
			}
			
			// now we just need to shift all of the intervals after this interval (by -length)
			shiftIntervals(linesBeingReplaced.getEndValue(), (int)(-1 * numLinesReplaced));
			
		} else if(length == 0 && !"".equals(text)) {
			if(!initialized) {
				intializeProjectionIntervals();
				initialized = true;
			} else {
				// this is an insert
				handleInsertOnly(offset, length, text);
			}
		} else {
			if(linesBeingReplaced.getStartValue() > linesBeingReplaced.getEndValue()) {
				// this will happen when length = 0 and text = "", we are paging in/out the empty last line.
				linesBeingReplaced.setEnd(linesBeingReplaced.getStartValue());
			}
			
			// this is an page in
			commitIncomingIntervals(linesBeingReplaced);
		}
		
		Collections.sort(projectionIntervals, intervalComparator);
	}

	private void intializeProjectionIntervals() {
		projectionIntervals.addAll(initialProjectionIntervals);
		initialProjectionIntervals.clear();
		initialProjectionIntervals = null;
	}

	/**
	 * This method handles the replace case where the text coming in is a pure insert, meaning text is being added
	 * but none is being removed.  All empty lines will be added to the projection intervals as paged out lines, and all
	 * non-empty lines are added as unpredicted intervals.  All intervals after the lines added will be shifted.
	 */
	private void handleInsertOnly(int offset, int length, String text) throws BadLocationException {
		
		long insertStartLineNum = getLineNumberOfOffset(offset);
		
		List<DocumentProjectionInterval> projectionIntervalAdditions = getProjectionIntervalInserts(insertStartLineNum, text);
		
		
		// if the last line is empty, I think that we can remove it because it will never change anything.
		
		// This is some nasty last line behavior.  If we are the last line, and its paged out, that means that it is completely empty.
		// an insert to an empty last line, which does not end in newline, will replace the empty last line.  Think about it and get back to me.
		if(insertStartLineNum == getNumberOfLines() - 1 && !isLinePagedIn((int)insertStartLineNum) && !text.endsWith("\n")) {
			this.clearSlotForProjectionInterval(new Interval(insertStartLineNum, insertStartLineNum));
		} else {
		}
		
		int shiftLineCount = 0;
		for(DocumentProjectionInterval projectionInterval : projectionIntervalAdditions) {
			shiftLineCount += projectionInterval.length();
		}
		
		// make sure they intervals are split on that line
		splitProjectionIntervalsAcrossLine(insertStartLineNum);
		// shift the intervals to make room
		shiftIntervals(insertStartLineNum, shiftLineCount);
		
		projectionIntervals.addAll(projectionIntervalAdditions);
		
		Collections.sort(projectionIntervals, intervalComparator);
	}

	/**
	 * This method reads an insert a line at a time and turns it into a list of paged out and unpredicted projection
	 * intervals.
	 */
	protected List<DocumentProjectionInterval> getProjectionIntervalInserts(long lineNumber, String text) throws BadLocationException {
		
		List<DocumentProjectionInterval> projectionIntervalAdditions = new ArrayList<>();
		
		DocumentProjectionInterval currentInterval = null;
		
		int initialOffset = calculateOffsetChangeAtline(lineNumber);
		
		String[] lines = BfvStringUtils.betterSplit(text, getDelimiter(), true);
		
		for(String line : lines) {
			
			if("".equals(line)) {
				continue;
			}
			
			if(getDelimiter().equals(line) || "".equals(line)) {
				// add an empty line
				if(currentInterval == null || currentInterval.isPagedIn()) {
					if(currentInterval != null)  {
						projectionIntervalAdditions.add(currentInterval);
					}
					
					currentInterval = new PagedOutProjectionInterval(lineNumber, lineNumber, getDelimiter().length());
				} else {
					currentInterval.setEnd(currentInterval.getEndValue() + 1);
				}
			} else {
				// we weren't able to predict this addition, so track its stats separately
				if(currentInterval == null || !currentInterval.isPagedIn()) {
					// flush the current interval
					if(currentInterval != null)  {
						projectionIntervalAdditions.add(currentInterval);
					}
					
					currentInterval = new UnpredictedProjectionInterval(lineNumber, initialOffset, line.length());
				} else {
					((UnpredictedProjectionInterval)currentInterval).appendLine(line.length());
				}
			}
			
			lineNumber++;
		}
		
		if(currentInterval != null)  {
			projectionIntervalAdditions.add(currentInterval);
		}

		return projectionIntervalAdditions;
	}

	/**
	 * This method iterates through the offset changes to determine what the current
	 * offset change at a line is.  Assumes that the offset changes are sorted
	 */
	private int calculateOffsetChangeAtline(long lineNumber) {
		int offset = 0;
		
		for(ProjectionOffsetChange offsetChange : offsetChanges) {
			if(offsetChange.getLineNumber() >= lineNumber) {
				break;
			}
			
			offset += offsetChange.getLineOffset();
		}
		
		return offset;
	}

	/**
	 * This method will shift any intervals at or after this line by the offset passed in.
	 */
	private void shiftIntervals(long insertStartLineNum, int lineOffset) {
		for(DocumentProjectionInterval projectionInterval : projectionIntervals) {
			if(projectionInterval.getStartValue() >= insertStartLineNum) {
				projectionInterval.shift(lineOffset);
			}
		}
		
		for(ProjectionOffsetChange offsetChange : offsetChanges) {
			if(offsetChange.getLineNumber() >= insertStartLineNum) {
				offsetChange.setLineNumber(offsetChange.getLineNumber() + insertStartLineNum);
			}
		}
		
		offsetChanges.add(new ProjectionOffsetChange(insertStartLineNum, lineOffset));
		Collections.sort(offsetChanges, offsetChangeComparator);
	}

	/**
	 * This method will look for an interval which contains this line, and will split it into two intervals
	 * if necessary such that there is one interval ends on this line while the next begins on this line.  This
	 * is useful if you are looking to insert at that line, and there is an interval already wrapped around it.
	 */
	private void splitProjectionIntervalsAcrossLine(long insertStartLineNum) {
		DocumentProjectionInterval intervalToSplit = null;
		
		for(DocumentProjectionInterval interval : projectionIntervals) {
			// if the interval has the number, and is not already split on it
			if(interval.contains(insertStartLineNum) && !(interval.getStartValue() == insertStartLineNum)) {
				intervalToSplit = interval;
				break;
			}
		}
		
		if(intervalToSplit == null) {
			return;
		}
		
		projectionIntervals.remove(intervalToSplit);
		
		DocumentProjectionInterval leftInterval = createDocumentProjectionInterval(intervalToSplit.getStartValue(), insertStartLineNum - 1, intervalToSplit.getLineOffsetFromFile(), intervalToSplit.isPagedIn());
		DocumentProjectionInterval rightInterval = createDocumentProjectionInterval(insertStartLineNum, intervalToSplit.getEndValue(), intervalToSplit.getLineOffsetFromFile(), intervalToSplit.isPagedIn());
		projectionIntervals.add(leftInterval);
		projectionIntervals.add(rightInterval);
		
		Collections.sort(projectionIntervals, intervalComparator);
	}

	/**
	 * This method makes room in the projection intervals for an interval with the same dimensions as linesBeingReplaced.
	 * It does so by removing the parts of all projection intervals that overlap with linesBeingReplaced.  Any intervals that
	 * lose all of their lines this way will be removed.
	 */
	protected void clearSlotForProjectionInterval(Interval linesBeingReplaced) {
		List<Interval> toRemove = new ArrayList<>();
		List<DocumentProjectionInterval> toAdd = new ArrayList<>();
		
		for(DocumentProjectionInterval projectionInterval : projectionIntervals) {
			
			// these first two cases are those that cover the !contains
			if(projectionInterval.getEndValue() < linesBeingReplaced.getStartValue()) {
				continue;
			} else if(projectionInterval.getStartValue() > linesBeingReplaced.getEndValue()) {
				break;
			}
			
			toRemove.add(projectionInterval);
			
			// At this point we know that we have some sort of intersection
			List<DocumentProjectionInterval> splits = splitIntersectingIntervals(projectionInterval, linesBeingReplaced);
			toAdd.addAll(splits);
		}
		
		// perform the split
		projectionIntervals.removeAll(toRemove);
		projectionIntervals.addAll(toAdd);
	}

	/**
	 * This method will break if you pass it 2 intervals that do not intersect
	 */
	protected List<DocumentProjectionInterval> splitIntersectingIntervals(DocumentProjectionInterval toSplit, Interval splitter) {
		List<DocumentProjectionInterval> splits = new ArrayList<>();
		
		if(toSplit.getStartValue() < splitter.getStartValue()) {
			splits.add(createDocumentProjectionInterval(toSplit.getStartValue(), splitter.getStartValue() - 1, toSplit.getLineOffsetFromFile(), toSplit.isPagedIn()));
		} 
		
		if(toSplit.getEndValue() > splitter.getEndValue()) {
			splits.add(createDocumentProjectionInterval(splitter.getEndValue() + 1, toSplit.getEndValue(), toSplit.getLineOffsetFromFile(), toSplit.isPagedIn()));
		}
		
		return splits;
	}
	
	/**
	 * This method will return an interval which represents the lines covered starting at character offset
	 * {@code offset} + length.
	 */
	private Interval getLinesCovered(int offset, int length) throws BadLocationException {
		int finalOffset = offset + length;
		
		int startLine = getLineNumberOfOffset(offset);
		int endLine = getLineNumberOfOffset(finalOffset);
		
		int lastLine = getNumberOfLines() - 1;
		
		// not handling the special case here, where the last line is empty because it is paged out, but would otherwise not be empty....
		if((endLine != lastLine)) {
			endLine--;
		}
		
		if((endLine == lastLine && finalOffset == (getLineInformation(endLine).getOffset()))) {
			endLine--;
		}
		
		return new Interval(startLine, endLine);
	}

	@Override
	public void set(String text) {
	}
	
	/**
	 * This method allows other classes to tell the tracker to expect a page in event.
	 */
	public void registerIntervalChange(Interval interval, IntervalEvent event) {
		
		if(!incomingIntervalEvents.isEmpty()) {
			incomingIntervalEvents.clear();
		}
		
		incomingIntervalEvents.put(event, interval);
	}
	
	public DocumentProjectionInterval createDocumentProjectionInterval(long start, long end, int lineOffsetFromFile, boolean isPagedIn) {
		if(isPagedIn) {
			return new PagedInProjectionInterval(fileModel, start, end, lineOffsetFromFile);
		} else {
			return new PagedOutProjectionInterval(start, end, getDelimiter().length());
		}
	}

	protected String getDelimiter() {
		return getLegalLineDelimiters()[0];
	}
	
	/**
	 * This method commits page in and page out events, which it is expecting.  To do so, it clears a spot for the 
	 * page event intervals, and puts in new intervals that contain the correct data.
	 * This method is currently assuming that there is an empty slot in the intervals waiting for it
	 * @param projectionOfIntervalEvent 
	 */
	public void commitIncomingIntervals(Interval projectionOfIntervalEvent) {
		
		if(incomingIntervalEvents.containsKey(PAGE_IN)) {
			Interval fileInterval = incomingIntervalEvents.get(PAGE_IN);
			
			int fileOffset = (int)(fileInterval.getStartValue() - projectionOfIntervalEvent.getStartValue());
			PagedInProjectionInterval pagedInProjectionInterval = new PagedInProjectionInterval(fileModel, fileInterval.getStartValue() - fileOffset, fileInterval.getEndValue() - fileOffset, fileOffset);
			clearSlotForProjectionInterval(pagedInProjectionInterval);
			
			projectionIntervals.add(pagedInProjectionInterval);
		}
		
		else if(incomingIntervalEvents.containsKey(PAGE_OUT)) {
//			Interval interval = incomingIntervalEvents.get(PAGE_OUT);
			
			PagedOutProjectionInterval pagedOutProjectionInterval = new PagedOutProjectionInterval(projectionOfIntervalEvent.getStartValue(), projectionOfIntervalEvent.getEndValue(), getDelimiter().length());
			clearSlotForProjectionInterval(pagedOutProjectionInterval);
			projectionIntervals.add(pagedOutProjectionInterval);
		}
		
		incomingIntervalEvents.clear();
	}
}
