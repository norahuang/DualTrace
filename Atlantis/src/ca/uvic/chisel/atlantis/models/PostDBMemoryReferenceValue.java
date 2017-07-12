package ca.uvic.chisel.atlantis.models;

/**
 * For use when retrieving from the database. May be adapted in a similar way as the {@link PreDBMemoryReferenceValue},
 * to have read-on-demand, but unlikely to be necessary.
 * 
 * When reading from the DB, we do not have the exact same memory constraints as when we are processing.
 * 
 *
 */
public class PostDBMemoryReferenceValue extends PreDBMemoryReferenceValue{
	
	public PostDBMemoryReferenceValue(Long byteAddress, Long valueByteWidth, String memoryValue){
		super(byteAddress, valueByteWidth, memoryValue);
	}

}
