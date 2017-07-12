package ca.uvic.chisel.bfv.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an line interval in the document which was not predicted by the document content
 * manager, and cannot successfully be mapped to a page in, however, which is not empty.  It stores a list which 
 * contains the lengths of all of the lines in the interval.
 */
public class UnpredictedProjectionInterval extends DocumentProjectionInterval {

	protected List<Integer> lineLengths = new ArrayList<>();
	
	public UnpredictedProjectionInterval(long start, long end, int lineOffsetFromFile, List<Integer> initialLineLengths) {
		super(start, end, lineOffsetFromFile);
		lineLengths.addAll(initialLineLengths);
	}
	
	public UnpredictedProjectionInterval(long line, int lineOffsetFromFile, int initialLineLength) {
		super(line, line, lineOffsetFromFile);
		lineLengths.add(initialLineLength);
	}

	@Override
	public boolean isPagedIn() {
		return true;
	}

	@Override
	public long getIntervalCharLength(long startValue, long endValue) {
		
		if(!this.contains(startValue) || !this.contains(endValue)) {
			return -1;
		}
		
		long length = 0;
		
		for(long i = startValue; i <= endValue; i++) {
			int lineLengthIndex = (int)(i - start);
			length += lineLengths.get(lineLengthIndex);
		}
		
		return length;
	}

	public void appendLine(int newLineLength) {
		end++;
		lineLengths.add(newLineLength);
	}

}
