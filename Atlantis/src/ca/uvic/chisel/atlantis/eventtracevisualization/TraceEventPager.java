package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.models.IEventMarkerModel;

/**
 * @author Patrick Gorman
 *
 * This class handles the paging in and out of the different events that are shown 
 * in the different TraceVisualizationViews.
 * 
 * Child classes can page on the basis of lines or graphical pixel windows, depending
 * on the DB facilities and view requirements.
 */
public abstract class TraceEventPager<E extends IEventMarkerModel> {

	private Map<Integer, List<E>> loadedBlocks;
	private LinkedList<E> combinedLoadedBlocks;
	
	protected AtlantisFileModelDataLayer fileModel;
	
	private static final int DEFAULT_BLOCK_SIZE = 1000;
	
	protected int blockSize = DEFAULT_BLOCK_SIZE;

	public TraceEventPager(AtlantisFileModelDataLayer fileModel) {
		assert fileModel != null;
		
		this.fileModel = fileModel;
		
		loadedBlocks = new TreeMap<>();
		combinedLoadedBlocks = new LinkedList<>();
	}
	
	/**
	 * returns true if this class does not contain the elements for the blocks in the given lines.
	 * returns false otherwise.
	 */
	public boolean isUpdateNeeded(int startLocation, int endLocation) {
		int startBlock = getBlockNumber(startLocation);
		int endBlock = getBlockNumber(endLocation);
		
		return !loadedBlocks.keySet().contains(startBlock) || !loadedBlocks.keySet().contains(endBlock);
	}
	
	public List<E> getModelsToCoverLineNumber(int lineNumber) {
		// Null exception here when changing traces? Other times too?
		ArrayList<Integer> locations = getPixelRangeCoveringLine(lineNumber);
		return getModelsForRange(locations.get(0), locations.get(1));
	}
	
	public List<E> getModelsForRange(int startLocation, int endLocation) {
		assert endLocation - startLocation > this.blockSize;
		if(isUpdateNeeded(startLocation, endLocation)) {
			doUpdate(startLocation, endLocation);
		}
		
		return combinedLoadedBlocks;
	}
	
	private void doUpdate(int startLocation, int endLocation) {
		
		unloadDistantBlocks(startLocation, endLocation);

		// load visible blocks
		loadBlockForLocation(startLocation);
		loadBlockForLocation(endLocation);
		
		combinedLoadedBlocks = new LinkedList<>();
		for(List<E> block: loadedBlocks.values()){
			combinedLoadedBlocks.addAll(block);
		}
	}

	private void loadBlockForLocation(int location) {
		int blockNum = getBlockNumber(location);
		if(!loadedBlocks.containsKey(blockNum)) {
			int blockStart = blockNum * blockSize;
			int blockEnd = blockStart + blockSize - 1;
			
			loadedBlocks.put(blockNum, getModelsFromDataStore(blockStart, blockEnd));
		}
	}

	/**
	 * Calculates which blocks are too far away from the current viewPort, and removes them
	 * from the cache.
	 */
	
	private void unloadDistantBlocks(int startLocation, int endLocation) {
		List<Integer> keysToRemove = new ArrayList<>();
		
		// unload distant blocks
		for(Entry<Integer, List<E>> kvp : loadedBlocks.entrySet()) {
			
			int blockNum = kvp.getKey();
			int blockStart = blockNum * blockSize;
			int blockEnd = blockStart + blockSize - 1;
			
			// three cases:
			if(blockStart <= endLocation && blockEnd >= startLocation) {
				// 1. There is overlap, and we need to keep it
				continue;
			}
			else if(blockEnd < startLocation) {
				// 2. it is before startLocation, and we need to see how much
				int difference = startLocation - blockEnd;
				
				if(difference > (blockSize / 2)) {
					keysToRemove.add(kvp.getKey());
				}
			} else {
				// 3. it is after endLocation, and we need to see how much
				int difference = blockStart - endLocation;
				
				if(difference > (blockSize / 2)) {
					keysToRemove.add(kvp.getKey());
				}
			}
		}
		
		for(int key : keysToRemove) {
			loadedBlocks.remove(key);
		}
	}

	protected abstract List<E> getModelsFromDataStore(int startLocation, int endLocation);
	
	protected abstract List<E> getModelsFromDataStoreForLines(int lineStart, int lineEnd);
	
	protected abstract ArrayList<Integer> getPixelRangeCoveringLine(int lineStart);
	
	/**
	 * Returns a which block this location appears in.  Block numbers go from 0 to blockCount -1 
	 */
	private int getBlockNumber(int location) {
		int result = (location / blockSize);
		return result;
	}
}
