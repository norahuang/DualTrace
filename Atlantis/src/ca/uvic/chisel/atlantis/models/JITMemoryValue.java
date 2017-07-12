package ca.uvic.chisel.atlantis.models;

import javax.management.RuntimeErrorException;

/**
 * Basis of wrapper to allow for read-on-demand, just-in-time (JIT) managing of large memory
 * changes. These occur with syscalls frequently. Extending classes can have JIT properties,
 * or can simply have the values stored in memory.
 */
public abstract class JITMemoryValue {
	
	static final String ERROR_MESSAGE = "Attempt to use String value without having first fetched it. Mis-use results in memory problems, so explicit fetch required.";

	public abstract Long hexStringLength();
	
	public String getMemoryValue(){
		if(!isPreRead()){
			throw new RuntimeErrorException(new Error(ERROR_MESSAGE));
		}
		return getMemoryValueImpl();
	}
	
	abstract protected String getMemoryValueImpl();
	
	public abstract Long getByteWidth();

	abstract boolean isPreRead();
	
	/**
	 * Required call prior to call to {@link JITMemoryValue#getMemoryValue()}. Should ensure
	 * that {@link JITMemoryValue#isPreRead()} returns true, and that if the value is a not
	 * explicitly stored, that it will be available for use. Classes extending that do not
	 * have deferred reading do not strictly require calls to this.
	 */
	public void fetchMemoryValue(){
		// Do nothing
	};
	
	/**
	 * As a further optimization of runtime memory usage, after fetching large values, we may
	 * clear them from memory. There might be some time passing before the value is needed again,
	 * because we are only reading it at DB write time. This will occur when the leaf is written,
	 * when the parent is written (between 0 and 999 leaves later), and when that parent's parent
	 * is written (between 0 and 1,000,000 leaves later, or 1000 parents later), and again when the
	 * great grandparent is written (between 0 and 1,000,000,000 leaves later).
	 * 
	 * I would prefer lower probability (i.e. 0%) of memory blow ups to the savings of keeping larger
	 * values in memory from the leaf writes cachign results for parent writes.
	 */
	public void clearCachedMemoryValue(){
		// Do nothing
	};

	
}
