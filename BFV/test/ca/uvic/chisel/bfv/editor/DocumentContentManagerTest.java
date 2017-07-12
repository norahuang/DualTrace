package ca.uvic.chisel.bfv.editor;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;

import ca.uvic.chisel.bfv.annotations.RegionModel;
import ca.uvic.chisel.bfv.datacache.BigFileDocument;
import ca.uvic.chisel.bfv.datacache.FileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.LineRegion;
import ca.uvic.chisel.bfv.editor.DocumentContentManager;
import ca.uvic.chisel.bfv.editor.DocumentContentManager.DocumentChange;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.testutils.TestUtils;

public class DocumentContentManagerTest {
	
	private DocumentContentManager contentManager;
	
	@Test
	public void testCalculateIntervalNoRegions() throws Exception {
		List<Interval> intervalsToLoad = getIntervalsForTest(137652, 3000);
		Interval[] expectedIntervals = { new Interval(2501, 3000), new Interval(3001, 3500) };

		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testCalculateIntervalNoCollapsedRegions() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(2800, 3200, false), mockRegion(3250, 3275, false));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(2501, 3000), new Interval(3001, 3500) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testCalculateIntervalEndOfFile() throws Exception {
		List<Interval> intervalsToLoad = getIntervalsForTest(137652, 137352);
		Interval[] expectedIntervals = { new Interval(137353, 137651), new Interval(136853, 137352) };

		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testCalculateIntervalBeginningOfFile() throws Exception {
		List<Interval> intervalsToLoad = getIntervalsForTest(137652, 250);
		Interval[] expectedIntervals = { new Interval(251, 750), new Interval(0, 250) };

		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testCalculateIntervalBeginningAndEndOfFile() throws Exception {
		List<Interval> intervalsToLoad = getIntervalsForTest(750, 400);
		Interval[] expectedIntervals = { new Interval(401, 749), new Interval(0, 400) };

		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testSingleCollapsedRegionOnRight() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(3099, 12000, true));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(3001, 3099), new Interval(12001, 12401),
				new Interval(2501, 3000) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testSingleCollapsedRegionOnLeft() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(1000, 2900, true));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(2901, 3000), new Interval(601, 1000),
				new Interval(3001, 3500) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testMultipleCollapsedRegionOnRight() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(3099, 5000, true), mockRegion(5100, 10000, true), mockRegion(10100, 12000, true));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(3001, 3099), new Interval(5001, 5100),
				new Interval(10001, 10100), new Interval(12001, 12201), new Interval(2501, 3000) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testMultipleCollapsedRegionOnLeft() throws Exception {
		
		List<RegionModel> regions = Arrays.asList(mockRegion(9000 ,9900, true), mockRegion(8000, 8900, true), mockRegion(5000, 7900, true));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 10000);
		Interval[] expectedIntervals = { new Interval(10001, 10500), new Interval(9901, 10000),
				new Interval(8901, 9000), new Interval(7901, 8000), new Interval(4801, 5000) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testSingleCollapsedRegionOnBoth() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(3099 ,7000, true), mockRegion(700, 2900, true));
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(3001, 3099), new Interval(7001, 7401),
				new Interval(2901, 3000), new Interval(301, 700) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testMultipleCollapsedRegionOnBoth() throws Exception {
		List<RegionModel> regions = Arrays.asList(
				mockRegion(3099 ,6999, true), 
				mockRegion(7099, 10000, true), 
				mockRegion(1500, 2900, true), 
				mockRegion(700, 1400, true)
		);
		
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(3001, 3099), new Interval(7000, 7099),
				new Interval(10001, 10301), new Interval(2901, 3000), new Interval(1401, 1500), new Interval(401, 700) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	@Test
	public void testOnFirstLineOfCollapsedRegion() throws Exception {
		List<RegionModel> regions = Arrays.asList(mockRegion(3000 ,10001, true)); 
		
		List<Interval> intervalsToLoad = getIntervalsForTest(regions, 137652, 3000);
		Interval[] expectedIntervals = { new Interval(10002, 10501),
				new Interval(2501, 3000) };
		
		TestUtils.assertContainsExactly(intervalsToLoad, expectedIntervals);
	}
	
	private void instantiateUndertest(int numLines, List<RegionModel> regions) throws Exception {
		FileModelDataLayer mockFileModel = mockFileModel(numLines, regions);
		
		contentManager = new DocumentContentManager(mockFileModel);
	}
	
	private List<Interval> getIntervalsForTest(int fileSize, int lineNum) throws Exception {
		instantiateUndertest(fileSize, Arrays.<RegionModel>asList());
		return contentManager.calculateIntervalsToLoad(lineNum);
	}
	
	private List<Interval> getIntervalsForTest(List<RegionModel> regions, int fileSize, int lineNum) throws Exception {
		instantiateUndertest(fileSize, regions);
		List<Interval> intervalsToLoad = contentManager.calculateIntervalsToLoad(lineNum);
		return intervalsToLoad;
	}
	
	private FileModelDataLayer mockFileModel(long numLines, List<RegionModel> regions) {
		FileModelDataLayer fileModel = mock(FileModelDataLayer.class);
		when(fileModel.getNumberOfLines()).thenReturn(numLines);
		when(fileModel.getRegions()).thenReturn(regions);
		return fileModel;
	}

	private RegionModel mockRegion(int startLine, int endLine, boolean collapsed) {
		RegionModel region = mock(RegionModel.class);
		
		when(region.getStartLine()).thenReturn(startLine);
		when(region.getEndLine()).thenReturn(endLine);
		when(region.getStartValue()).thenReturn((long)startLine);
		when(region.getEndValue()).thenReturn((long)endLine);
		when(region.isCollapsed()).thenReturn(collapsed);
		
		String toString = "[" + region.getStartLine() + ", " + region.getEndLine() + "," + region.isCollapsed() + "]";
		
		when(region.asInterval()).thenReturn(new Interval(startLine, endLine));
		when(region.toString()).thenReturn(toString);
		
		return region;
	}
	
	
}
