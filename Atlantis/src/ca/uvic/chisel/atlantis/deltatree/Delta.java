package ca.uvic.chisel.atlantis.deltatree;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import com.google.common.collect.Range;

import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;
import ca.uvic.chisel.atlantis.models.PreDBMemoryReferenceValue;
import edu.emory.mathcs.backport.java.util.Arrays;

public class Delta implements java.io.Serializable {
	
	private static final long serialVersionUID = 2492068299842487967L;
	
	private RangeMap<Long, MemoryReference> references;
	private Map<String, MemoryReference> registerReferences;

	private long highestLineNumber;

	/**
	 * For debugging purposes, not generally useful.
	 */
	private int dbNodeId = -1;
	private int dbParentNodeId = -1;
	private int nodeStartLine = -1;
	private int nodeEndLine = -1;
	private double level = -1;
	
	@Override
	public String toString(){
		return "Node ID: "+dbNodeId+" ParentNode ID: "+dbParentNodeId+", lines "+nodeStartLine+" to "+nodeEndLine+" level: "+this.level;
	}
	
	public Delta() {
		// NOTE: Using a TreeMap here gave us a large memory gain, I am not entirely sure why.
		references = TreeRangeMap.create();
		registerReferences = new TreeMap<String, MemoryReference>();
	}
	
	public void setDbDebugData(int dbNodeId, int dbParentNodeId, int nodeStartLine, int nodeEndLine) {
		this.dbNodeId = dbNodeId;
		this.dbParentNodeId = dbParentNodeId;
		this.nodeStartLine = nodeStartLine;
		this.nodeEndLine = nodeEndLine;
		// branchFactor is 1000, so the factor of 1000 that the start and end are apart is the level.
		// 1,999,999 - 1,000,000 = 999999, which is short by 1, but when rounded is level 3.
		// +1 because implementation goes from [...]0 to [...]999 for the 1000
		this.level = Math.log((nodeEndLine - nodeStartLine + 1))/Math.log(1000);
		// if((nodeEndLine - nodeStartLine + 1) >= 1000){
		//	System.out.println("Logarithmic line difference is level: "+this.level);
		// }
	}
	
	/**
	 * Requires any iterated calls to be with sorted container, because for memory values, we cannot ignore past values
	 * just because there is a newer value present.
	 * 
	 * Adds new references to the delta. If the reference overlaps with existing references in the delta,
	 * those references are transformed into smaller references that no longer overlap with the incoming one,
	 * then all of them are added. Overlap can be complete, with a new reference subsuming; in the middle, cutting
	 * the old reference into two pieces with a gap for the new reference; or overlapping just the start or end of the
	 * existing reference, leading to a truncation of the pre-existing reference.
	 * 
	 * This is a hassle, and a lot of work, but it is necessary to keep runtime memory requirements low. Some syscalls
	 * result in gigantic changes to memory that caused heap and GC problems. To avoid this, references were changed so that
	 * they could read from the binary only at the time of DB write. In order to account for overlap of new references into
	 * these larger ones, we snip the references up in terms of start and end address (or width) rather than getting strings
	 * or bytes and actually copying and overwriting them in memory.
	 * 
	 * It would be possible to do explicit string overwriting for those memory references that are not too large. Indeed,
	 * those references have their strings read immediately rather than deferring to DB write time. I think that using the
	 * same code path for processing both styles of references is more important though.
	 */
	public void addReference(MemoryReference reference){
		
		// NB This is for debugging purposes. It is not desirable to run this all the time. Should I even commit it? Yes, it
		// has some valuable logic in it, for verifying overlap/overwrite validity.
		// For performance, don't use in production.
		// checkOverlapValidity(reference);
		
		this.highestLineNumber = Math.max(reference.getLineNumber(), this.highestLineNumber);
		
		if(addRegisterFlagReference(reference)){
			return;
		}
		
		addMemoryReference(reference);
	}
	
	protected boolean addRegisterFlagReference(MemoryReference reference){
		// For registers, they are always explicit, and always overwrite themselves.
		// We always keep register entries represented as the enclosing register and appropriate value
		if(reference.getType() == EventType.REGISTER) {
			String regName = reference.getRegName();
			long byteWidth = reference.getMemoryContent().getByteWidth();
			if(!(registerReferences.containsKey(regName)
					&& registerReferences.get(regName).getLineNumber() == this.highestLineNumber
					&& !registerReferences.get(regName).isBefore())) {
				// Prefer after-values for now, but we want to do something smart with
				// before-values (such as writing their values back in time in the DB).
				registerReferences.put(regName, reference);
			} else {
				if(!registerReferences.get(regName).isBefore() && reference.isBefore()){
					// nada needed. This is what we always want.
				} else if(reference.getSubRegister() != registerReferences.get(regName).getSubRegister()){
					// this is fine, some operations (e.g. "xchng al, ah", have repeat slot usage, and repeat memory merge attempts. 
				} else {
					System.out.println("Unexpectantly, didn't merge "+reference.getLineNumber()+" to update delta subsuming up to line "+this.highestLineNumber+" contains "+registerReferences.containsKey(regName)+" and before "+registerReferences.get(regName).isBefore());
				}
			}
			
			return true;
		}
		
		if(reference.getType() == EventType.FLAGS) {
			System.out.println("FLAGS not handled in Delta. Did they used to simply work the same way as memory references?");
			return true;
		}
		
		return false;
	}
	
	private void addMemoryReference(MemoryReference reference){
		// Get the tree's sub-map that overlap with the new entry
		// Any that it completely encompasses need to be removed, and those
		// that it merely overlaps with need to be handled differently
		// The resident references need to be truncated, and the new one added.
		// This means that at most two references can be truncated (one loses head, other tail),
		// and 0 or more may be removed entirely (allowing new one to overwrite them).
		// end address is non-inclusive, that is, the memory at that location is not a part of this reference.
		// NB The keys for the subRangeMap return are *not the same as those keying the container*. They get narrowed in to match
		// the range queried with:
		// For example, if rangeMap had the entries
		// 		[1, 5] => "foo", (6, 8) => "bar", (10, ..) => "baz"
		// then rangeMap.subRangeMap(Range.open(3, 12)) would return a range map with the entries
		// 		(3, 5) => "foo", (6, 8) => "bar", (10, 12) => "baz"
		RangeMap<Long, MemoryReference> overlappingReps = references.subRangeMap(Range.openClosed(reference.getAddress(), reference.getEndAddress()));
		
		ArrayList<MemoryReference> overwrittenRefsToRemove = new ArrayList<MemoryReference>();
		ArrayList<MemoryReference> snippedRefsToAdd = new ArrayList<MemoryReference>();
		for(MemoryReference resident: overlappingReps.asMapOfRanges().values()){
			// If overlapped in any fashion, we need to remove the resident. If it is fully enclosed in our new
			// memory reference, that's the end. If it isn't entirely enclosed, create a new reference for the
			// left, right, or both, that do not enclose into the new reference.
			assert resident.getAddress() < reference.getEndAddress() && resident.getEndAddress() > reference.getAddress();
			boolean eatsHead = resident.getAddress() >= reference.getAddress(); // inclusive start address
			boolean eatsTail = resident.getEndAddress() <= reference.getEndAddress(); // exclusive end address
			
			// If there is any overlap with the resident, we need to remove it. Left, right, middle, whole thing, remove it.
			overwrittenRefsToRemove.add(resident);
			
			// If there is any memory range to the left or right of the new value, that is not overwritten, we preserve it
			// by making a new reference for just that left over range.
			if(!eatsHead){
				PreDBMemoryReferenceValue headToKeep = ((PreDBMemoryReferenceValue) resident.getMemoryContent()).cutTailOff(reference.getAddress());
				MemoryReference headRef = new MemoryReference(resident.getAddress(), headToKeep, resident.getLineNumber(), resident.getType(), resident.isBefore());
				headRef.addOwningDelta(resident.owningDelta);
				snippedRefsToAdd.add(headRef);
			}
			if(!eatsTail){
				PreDBMemoryReferenceValue tailToKeep = ((PreDBMemoryReferenceValue) resident.getMemoryContent()).cutHeadOff(reference.getEndAddress());
				MemoryReference tailRef = new MemoryReference(reference.getEndAddress(), tailToKeep, resident.getLineNumber(), resident.getType(), resident.isBefore());
				tailRef.addOwningDelta(resident.owningDelta);
				snippedRefsToAdd.add(tailRef);
			}
		}
		
		for(MemoryReference oldRef: overwrittenRefsToRemove){
			// If there is any overlap with the resident, we need to remove it. Left, right, middle, whole thing, remove it.
			references.remove(Range.openClosed(oldRef.getAddress(), oldRef.getEndAddress()));
		}
		
		for(MemoryReference snippedRef: snippedRefsToAdd){
			// If there is any overlap with the resident, we need to remove it. Left, right, middle, whole thing, remove it.
			references.put(Range.openClosed(snippedRef.getAddress(), snippedRef.getEndAddress()), snippedRef);
		}
		
		// Add the new reference, having dealt with any overlapping resident references.
		references.put(Range.openClosed(reference.getAddress(), reference.getEndAddress()), reference);
		// System.out.println("References span: "+references.span());
	}

	private void checkOverlapValidity(MemoryReference reference) {
		{
			boolean overlap = false;
			String id = "";
			Long overlapLineNumber = -1L;
			MemoryReference overlappedMemoryReference = null;
			if(reference.getType() == EventType.REGISTER) {
				// Checked for Atlantis runtime, but might cause problems for Gibraltar runtime...
				String regName = reference.getRegName();
				id = regName;
				overlap = registerReferences.containsKey(regName);
			}
			else if(reference.getType() == EventType.FLAGS) {
				// Dunno
			} else {
				RangeMap<Long, MemoryReference> overlappingReps = references.subRangeMap(Range.openClosed(reference.getAddress(), reference.getEndAddress()));
				overlap = !overlappingReps.asMapOfRanges().isEmpty();
				if(overlap){
					Entry<Range<Long>, MemoryReference> one = overlappingReps.asDescendingMapOfRanges().entrySet().iterator().next();
					overlapLineNumber = (long) one.getValue().getLineNumber();
					overlappedMemoryReference = one.getValue();
					if(overlappedMemoryReference.owningDelta == null){
						System.out.println("Null owningDelta on memoryReference. How did this happen?");
					}
				}
				id = reference.getAddress()+"->"+reference.getEndAddress();
			}

			if(reference.getLineNumber() < this.highestLineNumber && overlap){
				throw new RuntimeErrorException(new Error("Attempt to use old memory reference from line "+reference.getLineNumber()+" to update delta subsuming up to line "+this.highestLineNumber));
			}
			if(overlap && reference.getType() != EventType.REGISTER){
				if(overlappedMemoryReference.owningDelta != null){
					if(overlappedMemoryReference.owningDelta.nodeEndLine < reference.owningDelta.nodeStartLine){
						// This is fine. Totally valid always.
					} else if(overlappedMemoryReference.owningDelta.dbParentNodeId == reference.owningDelta.dbParentNodeId
							&& overlappedMemoryReference.owningDelta.dbNodeId < reference.owningDelta.dbNodeId
							){
						// System.out.println("Delta Overlapped with Older Sibling. That's fine.");
					} else if((overlappedMemoryReference.owningDelta.dbParentNodeId < reference.owningDelta.dbParentNodeId)
						|| (overlappedMemoryReference.owningDelta.dbNodeId < reference.owningDelta.dbParentNodeId
								&& overlappedMemoryReference.owningDelta.level > reference.owningDelta.level)){
						// System.out.println("Leaf or other low level overwriting older node");
					} else {
						// Incomplete logic implementation, but still useful for debugging. Sorted out a problem with it.
						// If the previous reference is one level higher than the replacing one (parent or sibling thereof),
						// but the parent of the replacing one is newer than the previous (higher id),
						// then we can safely overwrite.
						// This is incomplete; leaves will incorrectly trigger this logic when they are being applied
						// when we are near the end of the file and are working with very high level node snapshots.
						// But, this is for debugging purposes, so it's ok.
						System.out.println("Overlap, with older delta, which isn't a sibling, and isn't a sibling's child-leaf");
						System.out.println("older delta: "+overlappedMemoryReference.owningDelta.toString());
						System.out.println("newer delta: "+reference.owningDelta.toString());
					}
				} else {
					System.out.println("Overlap, but by newer delta: "+reference.getLineNumber()+" vs line: "+overlapLineNumber+" event type: "+reference.getType().toString()+" ("+id+")");
					System.out.println("older delta: "+overlappedMemoryReference.owningDelta.toString());
					System.out.println("newer delta: "+reference.owningDelta.toString());
				}
			}
		}
	}
	
	public void combineDeltas(Delta other) {
		// We must put the references in order of their lines.
		int size = other.references.asDescendingMapOfRanges().size();
		MemoryReference[] refArray = new MemoryReference[size];
		refArray = other.references.asDescendingMapOfRanges().values().toArray(refArray);
		Comparator<MemoryReference> lineComparator = new Comparator<MemoryReference>(){
			@Override
			public int compare(MemoryReference r1, MemoryReference r2) {
				return r1.getLineNumber() - r2.getLineNumber();
			}
		};
		Arrays.sort(refArray, lineComparator);
		for(int i = 0; i < refArray.length ; i++){
			this.addReference(refArray[i]);
		}
	}
	
	public Collection<MemoryReference> getRegisterReferences() {
		return registerReferences.values();
	}
	
	public Collection<MemoryReference> getMemoryReferences() {
		return references.asMapOfRanges().values();
	}
	

	public Collection<MemoryReference> getBothMemAndRegReferences(){
		ArrayList<MemoryReference> combined = new ArrayList<MemoryReference>();
		combined.addAll(registerReferences.values());
		combined.addAll(references.asMapOfRanges().values());
		return combined;
	}
}