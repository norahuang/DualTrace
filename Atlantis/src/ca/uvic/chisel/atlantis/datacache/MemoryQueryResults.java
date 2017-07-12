package ca.uvic.chisel.atlantis.datacache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;


import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import ca.uvic.chisel.atlantis.models.MemoryReference;

/**
 * This collects memory references, for an arbitrary span of lines, and produces sorted references for
 * a single line, as required.
 * 
 * Although not essential to this class, the users of it (currently) expect these references to be single-byte
 * entries. This was done for performance reasons. Even though it seems wasteful to break memory down to the
 * single byte level, it was too costly to perform the address overlap analyses required to update memory
 * state for a given line.
 * 
 *
 */
public class MemoryQueryResults {
	
	private final List<MemoryReference> registerAndFlagEntries;
	
	/**
	 * Memory references, in no particular order, spanning the full gamut of minCoveredLine to maxCoveredLine.
	 */
	private final TreeSet<MemoryReference> memoryEntries;
	
	private int lastLineCollated = -1;
	// Would prefer Long for keys, but String needed for some UI stuff later
	private SortedMap<Long, MemoryReference> onlyRefsValidForLine = new TreeMap<>();
	private SortedMap<String, MemoryReference> allRegistersValidForLine = new TreeMap<>();

	/**
	 * Extra lines from earlier than the requested target line, serves as paging.
	 */
	private int minCoveredLine;

	/**
	 * Extra lines further beyond the requested targetLine., serves as paging.
	 */
	private int maxCoveredLine;

	/**
	 * Line for which memory was being retrieved. other specific lines will be fulfilled,
	 * but this is the triggering 'seed' line for the result set.
	 */
	private int targetLine;
	
	public MemoryQueryResults(int minCoveredLine, int targetLine, int maxCoveredLine){	
		registerAndFlagEntries = new ArrayList<MemoryReference>();
		memoryEntries = new TreeSet<MemoryReference>();
		this.minCoveredLine = minCoveredLine;
		this.maxCoveredLine = maxCoveredLine;
		this.targetLine = targetLine;
	}
	
	public void addMemRef(MemoryReference memRef){
		//boolean remove = this.memoryEntries.remove(memRef);
		boolean add = this.memoryEntries.add(memRef);	
	}
	
	/**
	 * Get all memory (only) entries in this set, from the min line to the max loaded.
	 * The caller will be responsible for determining how these will be used; entries will
	 * naturally overshadow each other depending on which trace line they occur on.
	 */
	public TreeSet<MemoryReference> getUnfilteredMemoryEntries(){
		return this.memoryEntries;
	}
	
	public void addRegisterRef(MemoryReference memRef) {
		this.registerAndFlagEntries.add(memRef);
	}

	/**
	 * Get all register and flag entries in this set, from the min line to the max loaded.
	 * The caller will be responsible for determining how these will be used; entries will
	 * naturally overshadow each other depending on which trace line they occur on.
	 */
	public List<MemoryReference> getUnfilteredRegisterAndFlagEntries() {
		return this.registerAndFlagEntries;
	}
	
	/**
	 * 
	 * Memory (only) references, sorted and valid for the line most recently requested in {@link MemoryQueryResults#collateMemoryAndRegisterResults(int)}.
	 * Set of Entry objects as required for some UI components.
	 *
	 * @return
	 */
	public Set<Entry<Long, MemoryReference>> getMemoryEntries(){
		return this.onlyRefsValidForLine.entrySet();
	}
	
	/**
	 * Memory (only) references, sorted and valid for the line most recently requested in {@link MemoryQueryResults#collateMemoryAndRegisterResults(int)}.
	 * 
	 * @return
	 */
	public SortedMap<Long, MemoryReference> getMemoryList(){
		return this.onlyRefsValidForLine;
	}
	
	/**
	 * Register and FLAG references, sorted and valid for the line most recently requested in {@link MemoryQueryResults#collateMemoryAndRegisterResults(int)}.
	 * 
	 * @return
	 */
	public SortedMap<String, MemoryReference> getRegisterList(){
		return this.allRegistersValidForLine;
	}
	
	/**
	 * When using this an instance of this class as a paged result set spanning from minimum line to maximum (as seen in constructor),
	 * call this method to update the sorted and valid memory result lists, used in all the unfiltered accessors.
	 * 
	 * @param lineNumber
	 */
	public void collateMemoryAndRegisterResults(int lineNumber){
		if(lastLineCollated == lineNumber){
			return;
		}
		collateMemoryResults(lineNumber);
		collateRegisterResults(lineNumber);
	}
	
	/**
	 * Filter memory entries to only include those that are valid for the line requested.
	 * Caches, so no waste will occur.
	 * Benchmarked at 0.5 seconds for the 82million line trace, near it's end, where 1,205,189
	 * elements were reduced to 585,319 elements.
	 */
	private void collateMemoryResults(int lineNumber){
		onlyRefsValidForLine = new TreeMap<>();
		this.lastLineCollated = lineNumber;
		long startTime = System.currentTimeMillis();
		long maxlinenumber = 0;
		for(MemoryReference memRef : memoryEntries) {
			// Because the memory references used here are a mere byte wide,
			// we simply overwrite what is already there with the newer bytes.
			if(memRef.getLineNumber() <= lineNumber){
				MemoryReference prevRef = onlyRefsValidForLine.get(memRef.getAddress());
				if(null == prevRef || memRef.getLineNumber() > prevRef.getLineNumber()){
					onlyRefsValidForLine.put(memRef.getAddress(), memRef);
				}
			}
		}
		
		long endTime = System.currentTimeMillis();
//		System.out.println("Memory elements before collation: "+memoryEntries.size());
//		System.out.println("Memory elements after collation: "+allRefsValidForLine.size());
		System.out.println("Collation duration "+lineNumber+": " + (endTime-startTime)/1000.0);
	}
	
	private void collateRegisterResults(int lineNumber){
		allRegistersValidForLine = new TreeMap<>();
		
		long startTime = System.currentTimeMillis();
		for(MemoryReference memRef : registerAndFlagEntries) {
			// TODO this is where some special aggregation may new to occur, since we are currently
			// just overwriting what is already there with the newest stuff.
			if(memRef.getLineNumber() <= lineNumber){
				MemoryReference prevRef = allRegistersValidForLine.get(memRef.getRegName());
				if(null == prevRef || memRef.getLineNumber() > prevRef.getLineNumber()){
					allRegistersValidForLine.put(memRef.getRegName(), memRef);
				}
			}
		}
		
		long endTime = System.currentTimeMillis();
//		System.out.println("Memory elements before collation: "+memoryEntries.size());
//		System.out.println("Memory elements after collation: "+allRefsValidForLine.size());
//		System.out.println("Collation duration "+lineNumber+": " + (endTime-startTime)/1000.0);
	}

}
