package ca.uvic.chisel.bfv.intervaltree;

public interface IInterval {

	public long getStartValue();

	public long getEndValue();
	
	public void setStart(long value);
	
	public void setEnd(long value);
	
	public boolean contains(long value);
	
	public long length();
	
	public long distanceFromStart(long value);
	
	public double percentageAlongInterval(long value);
	
	public long valueAtPercent(double percent) throws Exception;
	
	/**
	 * Checks to see if this interval encloses the other.  In this context, enclosure means that 
	 * this interval must contain all of the lines that the other interval contains, and also that
	 * the inner interval must not contain the start or end line of the outer interval.
	 * 
	 * @param other The other interval to check to see if this interval encloses it.
	 */
	public boolean strictEncloses(IInterval other);
	
	/**
	 * Checks to see if this interval encloses the other.  In this context, enclosure means that 
	 * this interval must contain all of the lines that the other interval contains.
	 * 
	 * @param other The other interval to check to see if this interval encloses it.
	 */
	public boolean encloses(IInterval other);
	
	/**
	 * @param other
	 * @return	return true if this interval intersects other
	 */
	public boolean intersects(IInterval other);
	
	public boolean intersects(long otherStart, long otherEnd);
}
