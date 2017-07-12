package ca.uvic.chisel.bfv.editor;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

/**
 * This class represents an line interval in the document which is paged in, that is, all of the lines 
 * in this region correspond to lines in the document.
 */
public class PagedInProjectionInterval extends DocumentProjectionInterval {

	private final IFileModelDataLayer fileModel;
	private final Map<Long, Pair<Long, Long>> lineSizes;

	public PagedInProjectionInterval(IFileModelDataLayer fileModel, long start, long end, int lineOffsetFromFile) {
		super(start, end, lineOffsetFromFile);
		this.fileModel = fileModel;
		this.lineSizes = fileModel.getAllLineCharLengthsBetween(start, end);
	}

	@Override
	public boolean isPagedIn() {
		return true;
	}
	
	/**
	 * Calculates the number of characters in the interval by using the file model's index file.  First it
	 * unprojects its line numbers using its line offset.
	 */
	@Override
	public long getIntervalCharLength(long projectedStartLine, long projectedEndLine) {
		
		if(!this.contains(projectedStartLine) || !this.contains(projectedEndLine)) {
			return -1;
		}
		
		long fileStartLine = projectedStartLine + getLineOffsetFromFile();
		long fileEndLine = projectedEndLine + getLineOffsetFromFile();
		
		// Used to be simpler, but caching *here* greatly improves performance for the binary format.
		// Also supplanted a cache in the old format model. Can compare correctness against simpler direct methods.
		// return (int)(fileModel.getOffsetFromIndexForLine(fileEndLine + 1) - fileModel.getOffsetFromIndexForLine(fileStartLine));
		// int val = fileModel.getCharLengthBetween(fileStartLine, fileEndLine);
		
		if(fileStartLine == fileEndLine){
			// Asking for single line's span
			return this.lineSizes.get(fileStartLine).getLeft();
		} else {
			// Asking for...
			// If we do this too much, we *could* change the key, or make a second cache, of strings for intervals.
			return sumSpan(fileStartLine, fileEndLine);
		}
	}
	
	/**
	 * We cannot use the page line size cache for in-editor requests. Those will be routed here
	 * all of the time, but the editor contains few lines, so performance should be fine.
	 * 
	 * @param fileStartLine
	 * @param fileEndLine
	 * @return
	 */
	private long sumSpan(long fileStartLine, long fileEndLine){
		if(fileStartLine == this.start){
			// Caching the size-from-start-of-page, since that is a super common use case...
			// In fact, it appears that we only *ever* get from the start of the page until some point in it.
			// This can be confirmed by comparing the start of the requested span to the page start.
			// This caching is effective then!
			// System.out.println("From page start: "+start+" span "+fileStartLine+" to "+fileEndLine+"--Not using loop");
			return this.lineSizes.get(fileEndLine).getRight();
		} else {
			// This may never happen. The system simply ends up always requesting from the top of the page, not the top of the editor.
			// System.out.println(start+": "+fileStartLine+" to "+fileEndLine+"--Looping for line lengths");
			int sum = 0;
			for(long line = fileStartLine; line <= fileEndLine; line++){
				sum += this.lineSizes.get(line).getLeft();
			}
			return sum;
		}
	}
}
