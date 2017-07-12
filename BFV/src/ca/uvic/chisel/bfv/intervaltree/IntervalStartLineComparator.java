package ca.uvic.chisel.bfv.intervaltree;

import java.util.Comparator;

public class IntervalStartLineComparator implements Comparator<IInterval> {
	private final boolean ascending;

	public IntervalStartLineComparator(boolean ascending) {
		this.ascending = ascending;
	}
	
	public IntervalStartLineComparator() {
		this(true);
	}

	@Override
	public int compare(IInterval r1, IInterval r2) {
		int compareResult = Long.compare(r1.getStartValue(), r2.getStartValue());
		return ascending ? compareResult : -1 * compareResult;
	}
}