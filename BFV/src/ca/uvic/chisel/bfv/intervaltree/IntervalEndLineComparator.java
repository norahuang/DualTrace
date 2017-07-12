package ca.uvic.chisel.bfv.intervaltree;

import java.util.Comparator;

public class IntervalEndLineComparator implements Comparator<IInterval> {
	private final boolean ascending;

	public IntervalEndLineComparator(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public int compare(IInterval r1, IInterval r2) {
		int compareResult = Long.compare(r1.getEndValue(), r2.getEndValue());
		return ascending ? compareResult : -1 * compareResult;
	}
}