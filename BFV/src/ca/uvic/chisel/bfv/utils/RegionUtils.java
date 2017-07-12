package ca.uvic.chisel.bfv.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.LineRange;

import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.intervaltree.IntervalEndLineComparator;
import ca.uvic.chisel.bfv.intervaltree.IntervalStartLineComparator;

public class RegionUtils {

	/**
	 * This will add in all of the required intervals to the left of the lineNum
	 */
	public static List<Interval> pageInTheLeft(int lineNum, List<IInterval> collapsedRegions, int pageRadius) {
		
		List<Interval> results = new ArrayList<>();
		
		// reverse sorted by their endLine
		Collections.sort(collapsedRegions, new IntervalEndLineComparator(false));
		
		int remainingLines = pageRadius;
		
		int i = RegionUtils.getFirstRegionPositionToLeft(lineNum, collapsedRegions);
		
		if(i == -1) {
			results.add(new Interval(Math.max(0, lineNum - pageRadius + 1), lineNum));
			return results;
		}
		
		int lastStartLine = lineNum;
		
		//continue from where we left off
		for(; i<collapsedRegions.size(); i++) {
			
			if(remainingLines <= 0 || lastStartLine < 0) {
				break;
			}
			
			IInterval currentRegion = collapsedRegions.get(i);
			
			long startInterval = (int)currentRegion.getEndValue() + 1;
			long endInterval = lastStartLine;
			
			
			if(startInterval <= endInterval) {
				results.add(new Interval(Math.max(startInterval, endInterval - remainingLines + 1), endInterval));
			}
			
			remainingLines -= (endInterval - startInterval + 1);
					
			lastStartLine = (int)currentRegion.getStartValue() - 1;
		}
		
		// there is the potential that we aren't done, in that case, read from last.end -> end of file (or as far as we can go at least)
		if(remainingLines > 0 && lastStartLine >= 0) {
			long startInterval = 1;
			long endInterval = lastStartLine;
			
			results.add(new Interval(Math.max(startInterval, endInterval - remainingLines + 1), endInterval));
		}
		
		return results;
	}

	public static int getFirstRegionPositionToLeft(int lineNum, List<IInterval> collapsedRegions) {
		boolean found = false;
		
		int i;
		// iterate through the regions and find the first one that has some part of itself after startLine
		for(i=0; i<collapsedRegions.size(); i++) {
			IInterval collapsedRegion = collapsedRegions.get(i);
			
			if(!((int) collapsedRegion.getStartValue() - 1 >= lineNum)) {
				found = true;
				break;
			} 
		}
		
		return found ? i : -1;
	}

	/**
	 * This method will add in all of the intervals to the right of startLine that should be paged in.
	 * @return 
	 */
	public static List<Interval> pageInToTheRight(int startLine, List<? extends IInterval> collapsedRegions, int pageRadius, int lastLine) {
		
		List<Interval> results = new ArrayList<>();
		
		Collections.sort(collapsedRegions, new IntervalStartLineComparator(true));
		
		int remainingLines = pageRadius;
		
		int i = RegionUtils.getFirstRegionPositionToRight(startLine, collapsedRegions);
		
		if(i == -1) {
			// Index 0, so -1 on lastLine
			results.add(new Interval(startLine, Math.min(startLine + pageRadius - 1, lastLine)));
			return results;
		}
		
		int lastEndLine = startLine;
		
		//continue from where we left off, don't reinitialize i
		for(; i<collapsedRegions.size(); i++) {
			if(remainingLines <= 0) {
				break;
			}
			
			IInterval currentRegion = collapsedRegions.get(i);
			
			long startInterval = lastEndLine;
			long endInterval = currentRegion.getStartValue() - 1;
			
			if(startInterval <= endInterval) {
				results.add(new Interval(startInterval, Math.min(startInterval + remainingLines - 1, endInterval)));
			}
			
			remainingLines -= (endInterval - startInterval + 1);
					
			lastEndLine = (int)currentRegion.getEndValue() + 1;
		}
		
		// there is the potential that we aren't done, in that case, read from last.end -> end of file (or as far as we can go at least)
		if(remainingLines > 0) {
			long startInterval = Math.min(lastLine, lastEndLine);
			// Index 0, so -1 on lastLine
			long endInterval = lastLine;
			
			results.add(new Interval(startInterval, Math.min(startInterval + remainingLines - 1, endInterval)));
		}
		
		return results;
	}
	
	/**
	 * counts and returns the number of lines that are not within collapsed regions between startLine and endLine.
	 */
	public static int getNumberLinesNotContained(int startLine, int endLine, int lastLine, List<? extends IInterval> collapsedRegions) {
		endLine = Math.min(endLine, lastLine);
		
		List<Interval> intervalsRight = pageInToTheRight(startLine, collapsedRegions, endLine + 1, lastLine);
		
		int count = 0;
		
		for(Interval i : intervalsRight) {
			if(i.contains(endLine)) {
				count += endLine - i.getStartValue();
			} else if(i.getStartValue() <= endLine){
				count += i.length();
			}
		}
		
		return count;
	}
	
	public static int getTotalCollapsedRegionLength(List<IInterval> collapsedRegions) {
		int count = 0;
		
		for(IInterval i : collapsedRegions) {
			count += i.length();
		}
		
		return count;
	}
	
	public static int getFirstRegionPositionToRight(int startLine, List<? extends IInterval> collapsedRegions) {
		
		boolean found = false;
		
		int i;
		// iterate through the regions and find the first one that has some part of itself after startLine
		for(i=0; i<collapsedRegions.size(); i++) {
			IInterval collapsedRegion = collapsedRegions.get(i);
			
			if(!(collapsedRegion.getEndValue() < startLine)) {
				found = true;
				break;
			} 
		}
		
		return found ? i : -1;
	}

	public static List<ILineRange> getCollapsedRegionLineRanges(Collection<RegionModel> allRegions) {
		List<RegionModel> collapsedRegions = getCollapsedRegions(allRegions);
		
		List<ILineRange> lineRanges = new ArrayList<>();
		
		for(RegionModel region : collapsedRegions) {
			lineRanges.add(new LineRange(region.getStartLine(), region.getEndLine() - region.getStartLine() + 1));
		}
		
		return lineRanges;
	}
	
	public static List<RegionModel> getCollapsedRegions(Collection<RegionModel> allRegions) {
		List<RegionModel> results = new ArrayList<>();
		
		// perform a tree traversal and pull out all of the collapsed regions.
		// TODO see if there is a default sort, so that I can pull them out in order and not have to resort them.
		for(RegionModel region : allRegions) {
			RegionUtils.getCollapsedRegionsFromSubtree(results, region);
		}
		
		return results;
	}
	
	/**
	 * Similar to getCollapsedRegionsFromSubtree, however it does not want the entire region, only the part
	 * that is not visible (eg not including the first line)
	 */
	public static List<Interval> getCollapsedIntervals(Collection<RegionModel> allRegions) {
		return getIntervalsFromRegions(getCollapsedRegions(allRegions));
	}

	public static List<Interval> getIntervalsFromRegions(Collection<RegionModel> regions) {
		List<Interval> results = new ArrayList<>();
		for(RegionModel collapsedRegion : regions) {
			// Since the first line of a region is not actually collapsed, go from [start+1, end]
			results.add(new Interval(collapsedRegion.getStartValue() + 1, collapsedRegion.getEndValue()));
		}
		return results;
	}
	
	public static List<Interval> getIntervalsFromRegions(RegionModel ... regions) {
		List<RegionModel> regionList = new ArrayList<>();
		for(RegionModel region : regions) {
			regionList.add(region);
		}
		return getIntervalsFromRegions(regionList);
	}
	
	/**
	 * Similar to getCollapsedRegionsFromSubtree, however it does not want the entire region, only the part
	 * that is not visible (eg not including the first line)
	 */
	public static List<Interval> getCollapsedIntervals(RegionModel ... regions) {
		List<RegionModel> regionList = new ArrayList<>();
		for(RegionModel region : regions) {
			regionList.add(region);
		}
		return getCollapsedIntervals(regionList);
	}
	
	/**
	 * This method need only contain the root-most collapsed regions, since all of the children are contained in it anyways.
	 * Also, not doing this compilicates things.
	 */
	public static void getCollapsedRegionsFromSubtree(List<RegionModel> results, RegionModel region) {
		if(region.isCollapsed()) {
			results.add(region);
			return;
		}
		
		for(RegionModel child : region.getChildren()) {
			getCollapsedRegionsFromSubtree(results, child);
		}
	}

	/**
	 * Assumptions: 
	 * 
	 * 1. We do not have overlapping collapsed regions
	 */
	public static List<Interval> getRegionIntervalsUpToLine(int lineNumber, List<Interval> regions) {
		
		List<Interval> results = new ArrayList<>();
		
		Collections.sort(regions, new IntervalStartLineComparator());
		
		for(Interval region : regions) {
			
			if(region.getStartValue() > lineNumber) {
				break;
			}
			
			results.add(region);
			lineNumber += region.length();
		}
		
		return results;
	}
}

