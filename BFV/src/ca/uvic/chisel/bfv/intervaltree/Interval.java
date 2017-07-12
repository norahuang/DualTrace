package ca.uvic.chisel.bfv.intervaltree;

public class Interval implements Comparable<Interval>, IInterval {
	protected long start;
	protected long end;
	
	public Interval(long start, long end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public long getStartValue() {
		return start;
	}

	@Override
	public void setStart(long start) {
		this.start = start;
	}

	@Override
	public long getEndValue() {
		return end;
	}

	@Override
	public void setEnd(long end) {
		this.end = end;
	}
	
	@Override
	public long length() {
		return Math.abs(start - end) + 1;
	}
	
	@Override
	public long distanceFromStart(long value) {
		return Math.abs(value - start);
	}
	
	@Override
	public double percentageAlongInterval(long value) {
		return (double)(distanceFromStart(value)) / (double) length();
	}
	
	@Override
	public long valueAtPercent(double percent) throws Exception {
		if(percent < 0.0 || percent > 1.0) {
			throw new Exception("percent must be between 0 and 1");
		}
		
		long increment = (long)(this.length() * percent);
		
		if(start > end) {
			increment *= -1;
		}
		
		return start + increment;
	}
	
	/**
	 * Checks to see if this interval encloses the other.  In this context, enclosure means that 
	 * this interval must contain all of the lines that the other interval contains, and also that
	 * the inner interval must not contain the start or end line of the outer interval.
	 * 
	 * @param other The other interval to check to see if this interval encloses it.
	 */
	@Override
	public boolean strictEncloses(IInterval other) {
		return this.start < other.getStartValue() && this.end > other.getEndValue();
	}
	
	/**
	 * Checks to see if this interval encloses the other.  In this context, enclosure means that 
	 * this interval must contain all of the lines that the other interval contains.
	 * 
	 * @param other The other interval to check to see if this interval encloses it.
	 */
	@Override
	public boolean encloses(IInterval other) {
		return this.start <= other.getStartValue() && this.end >= other.getEndValue();
	}
	
	/**
	 * @param value
	 * @return	true if this interval contains time (inclusive)
	 */
	@Override
	public boolean contains(long value) {
		return value <= end && value >= start;
	}
	
	/**
	 * @param other
	 * @return	return true if this interval intersects other
	 */
	@Override
	public boolean intersects(IInterval other) {
		return other.getEndValue() >= start && other.getStartValue() <= end;
	}
	
	@Override
	public boolean intersects(long otherStart, long otherEnd) {
		return otherEnd >= start && otherStart <= end;
	}
	
	/**
	 * Return -1 if this interval's start time is less than the other, 1 if greater
	 * In the event of a tie, -1 if this interval's end time is less than the other, 1 if greater, 0 if same
	 * @param other
	 * @return 1 or -1
	 */
	@Override
	public int compareTo(Interval other) {		
		if(start < other.getStartValue())
			return -1;
		else if(start > other.getStartValue())
			return 1;
		else if(end < other.getEndValue())
			return -1;
		else if(end > other.getEndValue())
			return 1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object obj) {
		
		if(!(obj instanceof Interval)) {
			return false;
		}
		
		Interval other = (Interval) obj;
		
		return other.start == start && other.end == end;
	}

	/**
	 * @param startInterval The interval that the initial point is on
	 * @param value the point on the initial interval
	 * @param endInterval the final interval that is being projected onto
	 * @return the point on the second interval that corresponds to same distance along the first interval.
	 * @throws Exception 
	 */
	public static long project(Interval startInterval, long value, Interval endInterval) throws Exception {
		
		if(!startInterval.contains(value)) {
			throw new Exception("Point not contained on interval.");
		}

		return endInterval.valueAtPercent(startInterval.percentageAlongInterval(value));
	}
	
	@Override
	public String toString() {
		return "[" + start + "," + end + "]";
	}
}









