package ca.uvic.chisel.bfv.editor;

/**
 * This class represents an line interval in the document which is paged out or empty.
 */
public class PagedOutProjectionInterval extends DocumentProjectionInterval {

	protected int delimLength;

	public PagedOutProjectionInterval(long start, long end, int delimLength) {
		super(start, end, 0);
		this.delimLength = delimLength;
	}

	@Override
	public boolean isPagedIn() {
		return false;
	}

	/**
	 * Calculates the character length of a line interval simply by counting line delimiters
	 */
	@Override
	public long getIntervalCharLength(long startValue, long endValue) {
		
		if(!this.contains(startValue) || !this.contains(endValue)) {
			return -1;
		}
		
		return (endValue - startValue + 1) * delimLength;
	}

}
