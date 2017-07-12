package ca.uvic.chisel.atlantis.deltatree;

import javax.management.RuntimeErrorException;

import ca.uvic.chisel.atlantis.datacache.MemoryQueryResults;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;
import ca.uvic.chisel.atlantis.models.PostDBMemoryReferenceValue;
import ca.uvic.chisel.atlantis.models.PostDBRegistryValue;

public class DeltaConverter {

	private static final String FIELD_DELIM = ";";
	
	private static final String ROW_DELIM = ":";
	
	public static String getDBString(Delta d) {
		StringBuffer buffer = new StringBuffer();
		
		for(MemoryReference memRef : d.getBothMemAndRegReferences()) {
			String addressName;
			if(memRef.getType() == EventType.MEMORY){
				addressName = Long.toHexString(memRef.getAddress());
			} else {
				addressName = memRef.getRegName();
			}
			
			memRef.getMemoryContent().fetchMemoryValue();
			if(memRef.getRegName() == null && memRef.getMemoryContent().getMemoryValue().length() != memRef.getMemoryContent().getByteWidth()*2){
				throw new RuntimeErrorException(new Error("Length of memory value inserting into DB does not match intended byte width"));
			}
			buffer.append(addressName + FIELD_DELIM + memRef.getMemoryContent().getMemoryValue() + FIELD_DELIM + memRef.getLineNumber() + FIELD_DELIM + memRef.getType().ordinal() + ROW_DELIM);
			memRef.getMemoryContent().clearCachedMemoryValue();
		}
		return buffer.toString();
	}
	
	public static void convertDeltaQueryResults(String dbString, MemoryQueryResults memRefObj) {
		
		for(String rowSplit : dbString.split(ROW_DELIM)) {
			
			if(rowSplit == null || rowSplit.isEmpty()) {
				continue;
			}
			
			String[] fieldSplits = rowSplit.split(FIELD_DELIM);
			boolean isBefore = (fieldSplits.length == 5 ? Integer.parseInt(fieldSplits[4]) == 1 : false);
			EventType type = EventType.getFromId(Integer.parseInt(fieldSplits[3]));
			if(EventType.REGISTER == type || EventType.FLAGS == type){
				MemoryReference memRef = new MemoryReference(
					fieldSplits[0], 
					null,
					new PostDBRegistryValue(fieldSplits[0], fieldSplits[1].length()/2, fieldSplits[1]), 
					Integer.parseInt(fieldSplits[2]), 
					type,
					isBefore);
				
				memRefObj.addRegisterRef(memRef);
			} else {
				Long address = Long.parseLong(fieldSplits[0], 16);
				MemoryReference memRef = new MemoryReference(
					address, 
					new PostDBMemoryReferenceValue(address, (long)fieldSplits[1].length()/2, fieldSplits[1]), 
					Integer.parseInt(fieldSplits[2]), 
					type,
					isBefore);
				
				memRefObj.addMemRef(memRef);
			}
		}
		
		return;
	}
	
}
