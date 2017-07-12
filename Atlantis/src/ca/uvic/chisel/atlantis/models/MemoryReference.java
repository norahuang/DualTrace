package ca.uvic.chisel.atlantis.models;

import javax.management.RuntimeErrorException;

import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.deltatree.Delta;

public class MemoryReference implements java.io.Serializable, Comparable<MemoryReference> {

	private static final long serialVersionUID = 8619712922185676013L;

	public enum EventType {
		FLAGS(0), MEMORY(1), REGISTER(2);
		
		private int id;
		
		EventType(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		/**
		 * Returns the EventType with the corresponding id or null if one cannot be found
		 */
		public static EventType getFromId(int id) {

			for(EventType value : EventType.values()) {
				if(value.getId() == id) {
					return value;
				}
			}
			
			return null;
		}
	}

	final private String regName;
	final private String subRegName;
	final private Long startMemoryAddress;
	final private Long endMemoryAddress;
	final private JITMemoryValue memoryValue;
	final private int lineNumber;
	final private EventType type;
	final private boolean isBefore;

	public MemoryReference(Long address, PreDBMemoryReferenceValue value, int lineNumber, EventType type, boolean isBefore) {
		this.startMemoryAddress = address;
		this.endMemoryAddress = address + value.valueByteWidth;
		this.regName = null;
		this.subRegName = null;
		this.memoryValue = value;
		if(startMemoryAddress.compareTo(value.byteAddress) != 0){
			throw new RuntimeErrorException(new Error("Mismatch between value and container start address."));
		}
		this.lineNumber = lineNumber;
		this.type = type;
		this.isBefore = isBefore;
	}
	
	public MemoryReference(String regName, String subregister, PostDBRegistryValue value, int lineNumber, EventType type, boolean isBefore) {
		this.startMemoryAddress = 0L;
		this.endMemoryAddress = 0L;
		this.regName = regName;
		this.subRegName = subregister;
		this.memoryValue = value;
		this.lineNumber = lineNumber;
		this.type = type;
		this.isBefore = isBefore;
	}
	
	private MemoryReference(long address, int width) {
		this.startMemoryAddress = address;
		this.endMemoryAddress = address + width;
		
		this.regName = null;
		this.subRegName = null;
		this.memoryValue = null;
		this.lineNumber = 0;
		this.type = null;
		this.isBefore = false;
	}
	
	static public MemoryReference createDummyMemRef(long address, int width){
		return new MemoryReference(address, width);
	}
	
	public String getRegName(){
		return regName;
	}
	
	public String getSubRegister(){
		return subRegName;
	}

	public Long getAddress() {
		return startMemoryAddress;
	}
	
	/**
	 * End address is non-inclusive, that is, the memory at that location is not a part of this reference.
	 * 
	 * A single byte reference would have, for example, start 0xF2, end 0xF3.
	 * 
	 * @return
	 */
	public Long getEndAddress(){
		return endMemoryAddress;
	}
	
	public String getAddressAsHexString() {
		return BinaryFormatParser.toHex(startMemoryAddress);
	}
	
	public String getEndAddressAsHexString() {
		return BinaryFormatParser.toHex(endMemoryAddress);
	}

	public JITMemoryValue getMemoryContent() {
		return memoryValue;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public EventType getType() {
		return type;
	}
	
	public boolean isBefore() {
		return isBefore;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof MemoryReference) {
			MemoryReference other = (MemoryReference) obj;
			return (other.getAddress().compareTo(this.getAddress()) == 0) &&
					other.getLineNumber() == getLineNumber() && 
					other.getType().equals(getType()) && 
					other.isBefore() == this.isBefore() && 
					other.getMemoryContent().equals(getMemoryContent())
					;
		}
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		String memContent = this.getMemoryContent().isPreRead() ? this.getMemoryContent().getMemoryValue() : this.getMemoryContent().toString();
		if(type == EventType.MEMORY){
			return "Line "+this.lineNumber+" [0x" + getAddressAsHexString() +" - 0x"+ getEndAddressAsHexString() + "] => " + memContent;
		} else {
			return "Line "+this.lineNumber+"[" + getRegName() + "] => " + memContent;
		}
	}

	public Delta owningDelta;
	/**
	 * For debugging purposes only.
	 * @param d
	 */
	public void addOwningDelta(Delta d) {
		this.owningDelta = d;
	}

	@Override
	public int compareTo(MemoryReference arg0) {
		if (this.startMemoryAddress.compareTo(arg0.startMemoryAddress) != 0){
		   return this.startMemoryAddress.compareTo(arg0.startMemoryAddress);
		}
		else if ((this.endMemoryAddress.compareTo(arg0.endMemoryAddress) != 0)){
		   return this.endMemoryAddress.compareTo(arg0.endMemoryAddress);
		}
		else{
			if (this.lineNumber == arg0.lineNumber){
				return 0;
			} 
			else if (this.lineNumber > arg0.lineNumber){
			    return 1;
			}
			else{
				return -1;
			}
		}
			
	}
}
