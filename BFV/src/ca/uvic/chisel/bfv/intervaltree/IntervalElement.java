package ca.uvic.chisel.bfv.intervaltree;

/**
 * The Interval class maintains an interval with some associated data
 * @author Kevin Dolan
 * 
 * @param <Type> The type of data being stored
 */
public class IntervalElement<Type> implements Comparable<IntervalElement<Type>>{

	private Type data;
	private Interval interval;
	
	public IntervalElement(Interval interval, Type data) {
		this.interval = interval;
		this.data = data;
	}

	public Type getData() {
		return data;
	}

	public void setData(Type data) {
		this.data = data;
	}
	
	public Interval getInterval() {
		return interval;
	}

	@Override
	public int compareTo(IntervalElement<Type> o) {
		return this.interval.compareTo(o.interval);
	}
}
