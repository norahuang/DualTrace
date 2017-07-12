package ca.uvic.chisel.bfv.utils;

import static ca.uvic.chisel.bfv.testutils.TestUtils.assertContainsExactly;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;

public class TestIntervalUtils {
	
	@Test
	public void testCoverageFullOverlap() {
		
		Interval testCoverageInterval = new Interval(1, 100);
		
		List<IInterval> coverage = new ArrayList<>();
		coverage.add(new Interval(0, 15));
		coverage.add(new Interval(16, 30));
		coverage.add(new Interval(31, 85));
		coverage.add(new Interval(86, 101));

		assertTrue(IntervalUtils.completelyCovered(testCoverageInterval, coverage));
	}
	
	@Test
	public void testCoverageFullOverlapOnBoundary() {
		
		Interval testCoverageInterval = new Interval(1, 100);
		
		List<IInterval> coverage = new ArrayList<>();
		coverage.add(new Interval(1, 15));
		coverage.add(new Interval(16, 30));
		coverage.add(new Interval(31, 85));
		coverage.add(new Interval(86, 100));

		assertTrue(IntervalUtils.completelyCovered(testCoverageInterval, coverage));
	}
	
	@Test
	public void testCoverageNonFullOverlapInMiddle() {
		
		Interval testCoverageInterval = new Interval(1, 100);
		
		List<IInterval> coverage = new ArrayList<>();
		coverage.add(new Interval(0, 15));
		coverage.add(new Interval(16, 30));
		coverage.add(new Interval(32, 85));
		coverage.add(new Interval(86, 101));

		assertFalse(IntervalUtils.completelyCovered(testCoverageInterval, coverage));
	}
	
	@Test
	public void testCoverageNonFullOverlapAtStart() {
		
		Interval testCoverageInterval = new Interval(1, 100);
		
		List<IInterval> coverage = new ArrayList<>();
		coverage.add(new Interval(2, 15));
		coverage.add(new Interval(16, 30));
		coverage.add(new Interval(31, 85));
		coverage.add(new Interval(86, 101));

		assertFalse(IntervalUtils.completelyCovered(testCoverageInterval, coverage));
	}
	
	@Test
	public void testCoverageNonFullOverlapAtEnd() {
		
		Interval testCoverageInterval = new Interval(1, 100);
		
		List<IInterval> coverage = new ArrayList<>();
		coverage.add(new Interval(0, 15));
		coverage.add(new Interval(16, 30));
		coverage.add(new Interval(31, 85));
		coverage.add(new Interval(86, 99));

		assertFalse(IntervalUtils.completelyCovered(testCoverageInterval, coverage));
	}
	
	@Test
	public void testMergeNoOld() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 10));
		newIntervals.add(new Interval(11, 20));
		newIntervals.add(new Interval(21, 30));
		newIntervals.add(new Interval(31, 40));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, new ArrayList<Interval>()), newIntervals);
	}
	
	@Test
	public void testMergeNoNew() {
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(0, 10));
		oldIntervals.add(new Interval(11, 20));
		oldIntervals.add(new Interval(21, 30));
		oldIntervals.add(new Interval(31, 40));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(new ArrayList<Interval>(), oldIntervals), new ArrayList<Interval>());
	}
	
	@Test
	public void testMergeLeftSplitOnly() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 10));
		newIntervals.add(new Interval(21, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(5, 15));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 4));
		results.add(new Interval(21, 30));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeRightSplitOnly() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(5, 15));
		newIntervals.add(new Interval(21, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(0, 10));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(11, 15));
		results.add(new Interval(21, 30));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeMiddleSplitOnly() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 15));
		newIntervals.add(new Interval(21, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(5, 10));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 4));
		results.add(new Interval(11, 15));
		results.add(new Interval(21, 30));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeLeftThenMiddleSplit() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(16, 30));
		oldIntervals.add(new Interval(5, 10));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 4));
		results.add(new Interval(11, 15));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeLeftThenRightSplit() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(16, 30));
		oldIntervals.add(new Interval(0, 4));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(5, 15));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeRigthThenMiddleSplit() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(16, 30));
		oldIntervals.add(new Interval(5, 10));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 4));
		results.add(new Interval(11, 15));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeMiddleThenLeftSplit() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(11, 19));
		oldIntervals.add(new Interval(26, 30));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 10));
		results.add(new Interval(20, 25));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeMiddleThenRightSplit() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(5, 30));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(11, 19));
		oldIntervals.add(new Interval(0, 9));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(10, 10));
		results.add(new Interval(20, 30));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeContainedRemoved() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(5, 10));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(5, 10));
		
		List<Interval> results = new ArrayList<>();
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	@Test
	public void testMergeComplicated() {
		List<Interval> newIntervals = new ArrayList<>();
		newIntervals.add(new Interval(0, 10));
		newIntervals.add(new Interval(20, 30));
		newIntervals.add(new Interval(30, 40));
		
		List<Interval> oldIntervals = new ArrayList<>();
		oldIntervals.add(new Interval(27, 28));
		oldIntervals.add(new Interval(6, 24));
		oldIntervals.add(new Interval(36, 45));
		oldIntervals.add(new Interval(36, 45));
		
		List<Interval> results = new ArrayList<>();
		results.add(new Interval(0, 5));
		results.add(new Interval(25, 26));
		results.add(new Interval(29, 30));
		results.add(new Interval(30, 35));
		
		assertContainsExactly(IntervalUtils.mergeNewIntervalsWithOld(newIntervals, oldIntervals), results);
	}
	
	
}
