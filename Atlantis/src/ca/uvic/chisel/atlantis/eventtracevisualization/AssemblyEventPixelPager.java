package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.models.AssemblyChangedEvent;
import ca.uvic.chisel.atlantis.models.IEventMarkerModel;

public class AssemblyEventPixelPager extends TraceEventPager<AssemblyChangedEvent> {

	public AssemblyEventPixelPager(AtlantisFileModelDataLayer fileModel) {
		super(fileModel);
		this.blockSize = 100000; // pixels!
	}

	@Override
	protected List<AssemblyChangedEvent> getModelsFromDataStore(int startPixelXCoordinate, int endPixelXCoordinate){
		try {
			return fileModel.getAssemblyEventInPixelRange(startPixelXCoordinate, endPixelXCoordinate);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	@Deprecated
	protected List<AssemblyChangedEvent> getModelsFromDataStoreForLines(int lineStart, int lineEnd){
		try {
			return fileModel.getAssemblyEvents(lineStart, lineEnd);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected ArrayList<Integer> getPixelRangeCoveringLine(int lineStart) {
		try{
			// Get the center of the marker, and grab as much pixel space to either side as we normally use.
			IEventMarkerModel event = fileModel.getAssemblyEventContainingLine(lineStart);
			int pixelStart = event.getPixelStart();
			int pixelEnd = event.getPixelEnd();
			
			int midPixel = pixelStart + (pixelEnd - pixelStart)/2;
			int blockToUse = this.blockSize;
			int startRangePixel = Math.max(midPixel - blockToUse/2, 0);
			blockToUse -= midPixel - startRangePixel;
			int endRangePixel = midPixel + blockToUse;
			
			ArrayList<Integer> range = new ArrayList<Integer>();
			range.add(startRangePixel);
			range.add(endRangePixel);
			return range;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
