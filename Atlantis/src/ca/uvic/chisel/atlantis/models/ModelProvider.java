package ca.uvic.chisel.atlantis.models;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public enum ModelProvider {
	INSTANCE;
	
	// A bit troublesome, since incoming values will each be setting it...but the caller
	// will inevitably be doing so for all three types in response to an action on a specific line.
	// It should work alright. We could require all three entry sets to be added to, removed from, or cleared
	// at the same time with a single method, if there are doubts.
	int currentLine = -1;
	
	private TreeMap<String, MemoryReference> watchedMap;

	private MemoryQueryResults newMemoryReferences = new MemoryQueryResults(-1, -1, -1);

	private ModelProvider() {
		watchedMap = new TreeMap<String, MemoryReference>();
	}
	
	public  Collection<MemoryReference> getMemoryValues() {		
		return newMemoryReferences.getMemoryList().values();
	}

	/**
	 * Needed to provide to GUI TableViewer object. Do not use other than placing
	 * into setInput() method for TableViewers.
	 * 
	 * @return
	 */
	public Set<Entry<Long, MemoryReference>> getMemoryList() {		
		return newMemoryReferences.getMemoryEntries();
	}
	
	/**
	 * Needed to provide to GUI TableViewer object. Do not use other than placing
	 * into setInput() method for TableViewers.
	 * 
	 * @return
	 */
	public Set<Entry<String, MemoryReference>> getWatchedList() {		
		return watchedMap.entrySet();
	}
	
	
	public MemoryReference getMemory(String memoryAddress) {		
		return newMemoryReferences.getMemoryList().get(Long.parseLong(memoryAddress, 16));
	}
	
	public MemoryReference getRegister(String regName) {		
		return newMemoryReferences.getRegisterList().get(regName);
	}
	
	public MemoryReference getWatched(String memoryAddress) {		
		return watchedMap.get(memoryAddress);
	}
	
	public int lastUpdatedTraceLine = -1;
	public void setMemoryQueryResults(MemoryQueryResults newMemoryReferences, int traceLineNumber){
		this.newMemoryReferences = newMemoryReferences;
		this.currentLine = traceLineNumber;
		lastUpdatedTraceLine = traceLineNumber;
	}
	
	public MemoryQueryResults getMemoryQueryResults(){
		return this.newMemoryReferences;
	}

	public void removeWatchedEntry(String memRefIdentifier) {
		watchedMap.remove(memRefIdentifier);
	}
	
	public void clearMemoryAndRegisterData() {
		newMemoryReferences = new MemoryQueryResults(-1, -1, -1);
	}

	public void clearWatchedEntries() {
		watchedMap.clear();
		currentLine = -1;
	}
	
	public void updateWatchedEntries(List<String> watchedLocations){
		SortedMap<Long, MemoryReference> memEntries = this.newMemoryReferences.getMemoryList();
		for (String s : watchedLocations) {
			Long sLong = Long.parseLong(s, 16); //.toUpperCase()))
			if (memEntries.keySet().contains(sLong)){ 
				watchedMap.put(memEntries.get(sLong).getAddressAsHexString(), memEntries.get(sLong));
			} else {
				// Place holders so that the UI shows something useful when no value has been assigned
				// Should be ok with a constant 'false' argument for the isBefore...right?
				// If we only display after instances, this is not an issue. The isBefore is
				// more useful for writing values backward in time while parsing than for Atlantis runtime. 7ff fffd e2c8 07FF FFFD E2C8
				System.out.println("Check this");
				PostDBMemoryReferenceValue memRefVal = new PostDBMemoryReferenceValue(sLong, 1L, "??");
				MemoryReference placeHolderMemRef = new MemoryReference(sLong, memRefVal, 0, EventType.MEMORY, false);
//				ModelProvider.INSTANCE.addWatchedEntry(memRef, traceLineNumber);
				watchedMap.put(s, placeHolderMemRef);
			}
		}
	}
	
	public int getCurrentLine(){
		return currentLine;
	}
}


