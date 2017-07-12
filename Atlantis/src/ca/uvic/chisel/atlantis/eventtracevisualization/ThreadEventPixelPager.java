package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.ArrayList;
import java.util.List;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.models.IEventMarkerModel;
import ca.uvic.chisel.atlantis.models.TraceThreadEvent;

public class ThreadEventPixelPager extends TraceEventPager<TraceThreadEvent> {

	public ThreadEventPixelPager(AtlantisFileModelDataLayer fileModel) {
		super(fileModel);
		this.blockSize = 100000; // pixels!
	}

	@Override
	protected List<TraceThreadEvent> getModelsFromDataStore(int startPixelXCoordinate, int endPixelXCoordinate){
		try {
			return fileModel.getThreadEventsInPixelRange(startPixelXCoordinate, endPixelXCoordinate);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected List<TraceThreadEvent> getModelsFromDataStoreForLines(int lineStart, int lineEnd){
		try {
			return fileModel.getThreadEvents(lineStart, lineEnd);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected ArrayList<Integer> getPixelRangeCoveringLine(int lineStart) {
		try{
			// Get the center of the marker, and grab as much pixel space to either side as we normally use.
			IEventMarkerModel event = fileModel.getThreadEventContainingLine(lineStart);
			int pixelStart = event.getPixelStart();
			int pixelEnd = event.getPixelStart();
			
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
