package ca.uvic.chisel.bfv.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalStartLineComparator;

public class IntervalUtils {

	/**
	 * Removes the intervals from {@code newIntervals} that are already contained in {@code currentIntervals}
	 */
	public static List<Interval> removeContainedIntervals(List<Interval> newIntervals, List<Interval> currentIntervals) {
		
		List<Interval> clone = new ArrayList<>(newIntervals);
		List<Interval> toRemove = new ArrayList<>();
		
		for(Interval newInterval : newIntervals) {
			for(Interval currentInterval : currentIntervals) {
				if(currentInterval.encloses(newInterval)) {
					toRemove.add(newInterval);
				}
			}
		}
		
		clone.removeAll(toRemove);
		return clone;
	}

	public static boolean lineContainedInIntervals(int lineNum, List<Interval> intervals) {
		for(Interval interval : intervals) {
			if(interval.contains(lineNum)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Note: This assumes that the intervals passed in are all non-overlapping
	 * 
	 * Returns true if the set of coverage intervals completely covers the interval passed in and
	 * false otherwise.
	 */
	public static boolean completelyCovered(IInterval interval, List<? extends IInterval> coverage) {
		
		// sort the coverage by their start line
		Collections.sort(coverage, new IntervalStartLineComparator());
		
		int i;
		IInterval coverageInterval = null;
		
		for(i = 0; i<coverage.size(); i++) {
			IInterval current = coverage.get(i);
			
			if(current.contains(interval.getStartValue())) {
				coverageInterval = new Interval(interval.getStartValue(), current.getEndValue());
				i++;
				break;
			}
		}
		
		for(;i<coverage.size(); i++) {
			IInterval current = coverage.get(i);
			
			if(current.getStartValue() != coverageInterval.getEndValue() + 1 && !(current.intersects(coverageInterval))) {
				break;
			}
			
			coverageInterval.setEnd(current.getEndValue());
		}
		return coverageInterval == null ? false : coverageInterval.encloses(interval);
	}

	public static List<Interval> mergeNewIntervalsWithOld(List<Interval> newIntervals, List<Interval> oldIntervals) {
		
		List<Interval> currentList = new ArrayList<>(newIntervals);
		List<Interval> nextList = new ArrayList<>();
		
		for(Interval old : oldIntervals) {
			
			nextList.clear();
			
			for(Interval current : currentList) {
				if(!current.intersects(old)) {
					nextList.add(current);
				}
				else if(old.encloses(current)) {
					continue;
				} else if(current.encloses(old)) {
					// we need to do a split
					addLeftSplit(nextList, old, current);
					addRightSplit(nextList, old, current);
					
				} else if(current.getStartValue() < old.getStartValue()) {
					addLeftSplit(nextList, old, current);
				} else {
					addRightSplit(nextList, old, current);
				}
			}
			
			List<Interval> temp = currentList;
			currentList = nextList;
			nextList = temp;
		}
		
		return currentList;
	}

	private static void addRightSplit(List<Interval> nextList, Interval old, Interval current) {
		Interval right = new Interval(old.getEndValue() + 1, current.getEndValue());
		addIfLengthPositive(nextList, right);
	}
	
	private static void addLeftSplit(List<Interval> nextList, Interval old, Interval current) {
		Interval left = new Interval(current.getStartValue(), old.getStartValue() - 1);
		addIfLengthPositive(nextList, left);
	}

	private static void addIfLengthPositive(List<Interval> nextList,Interval interval) {
		if(interval.getEndValue() - interval.getStartValue() >= 0) {
			nextList.add(interval);
		}
	}

}
