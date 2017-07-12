package ca.uvic.chisel.atlantis.models;

/**
 * Registers do not need JIT functionality, but need to be handled in many of the same contexts.
 * 
 *
 */
public class PostDBRegistryValue extends JITMemoryValue {
	
	/**
	 * Explicitly read, cached, pre-read, memo-ized value. Should only be set if value is 64 bits or less.
	 */
	protected String memoryValue = null;
	
	final String regName;
	
	final Long valueByteWidth;
	
	public PostDBRegistryValue(String regName, long valueByteWidth, String memoryValue){
		this.regName = regName;
		this.valueByteWidth = valueByteWidth;
		this.memoryValue = memoryValue;
	}

	/**
	 * String length of the value
	 */
	@Override
	public Long hexStringLength(){
		return valueByteWidth * 2;
	}

	@Override
	protected String getMemoryValueImpl() {
		return memoryValue;
	}

	@Override
	public Long getByteWidth() {
		return valueByteWidth;
	}
	
	@Override
	public boolean isPreRead(){
		return true;
	}

}
