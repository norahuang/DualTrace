package ca.uvic.chisel.atlantis.models;

import javax.management.RuntimeErrorException;

import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMem;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;

/**
 * When parsing binary format files, memory can become extremely strained in the case of
 * syscalls that modify large swaths of memory. It can strain the heap, as well as cause
 * GC problems (automatic halting of GC due to lack of responsiveness).
 * 
 * Rather than try to manage heap and GC only to have them become strained again in the future,
 * using this class will allow deferred reading of memory values until they need to be written
 * to the DB. The potentially gigantic strings will reside in memory for a shorter and well
 * defined length of time. Costs include possible disk inefficiency, from not reading the large
 * value when the adjacent address was read, and double reading of all parent node memory values,
 * once for the parent, and once for the child they are derived from (see {@link MemoryDeltaTree},
 * where parents subsume children).
 * 
 * To assist with the second problem, memory values of 64 bits and smaller may be read immediately
 * into memory. The JIT behavior is thus adjusted to include caching.
 * 
 */
public class PreDBMemoryReferenceValue extends JITMemoryValue {
	
	/**
	 * Explicitly read, cached, pre-read, memo-ized value. Should only be set if value is 64 bits or less.
	 */
	protected String preSetMemoryValue;
	
	/**
	 * Per instance, use only one of the ContextMem, or pre-set String
	 */
	final ContextMem jitReferenceBuffer;
	
	final Long byteAddress;
	
	final Long valueByteWidth;
	
	public PreDBMemoryReferenceValue(Long byteAddress, Long valueWidth, ContextMem jitBufferContextMem){
		this.byteAddress = byteAddress;
		this.valueByteWidth = valueWidth;
		this.preSetMemoryValue = null;
		this.jitReferenceBuffer = jitBufferContextMem;
	}
	
	public PreDBMemoryReferenceValue(Long byteAddress, Long valueWidth, String memoryValue){
		this.byteAddress = byteAddress;
		this.valueByteWidth = valueWidth;
		this.preSetMemoryValue = memoryValue;
		if(this.preSetMemoryValue.length() != this.valueByteWidth * 2){
			throw new RuntimeErrorException(new Error("Mismatch of memory value string length and declared byte width for PreDBMemoryReferenceValue."));
		}
		this.jitReferenceBuffer = null; // no need for reference buffer when we got the value
	}
	
	/**
	 * String length of the value
	 */
	@Override
	public Long hexStringLength(){
		return valueByteWidth * 2;
	}
	
	/**
	 * If there has not been a call to {@link PreDBMemoryReferenceValue#fetchMemoryValue()} first,
	 * will throw an exception.
	 */
	@Override
	protected String getMemoryValueImpl(){
		// If preSetMemoryValue is available, give that. If it is safe to access the ContextMem's
		// value, use that. If it isn't safe, because we didn't call fetchMemoryValue(), throw an
		// exception. We should always be very deliberate about calling to access that, since memory
		// is at stake, and will cause Gibraltar to explode only for traces where the memory use
		// is bad enough (e.g. huge Adobe Acrobat file reads after syscalls).
		if(!isPreRead()){
			throw new RuntimeErrorException(new Error(ERROR_MESSAGE));
		} else if(null == this.preSetMemoryValue){
			return this.jitReferenceBuffer.memoryHexValue;
		} else {
			return this.preSetMemoryValue;
		}
	}
	
	@Override
	public boolean isPreRead(){
		return null != this.preSetMemoryValue ||
				(null != this.jitReferenceBuffer && null != this.jitReferenceBuffer.memoryHexValue);
	}
	
	@Override
	public void fetchMemoryValue(){
		if(isPreRead()){
			return;
		}
		// Have the ContextMem read its value into memory
		this.jitReferenceBuffer.performRead(this.byteAddress, this.valueByteWidth);
	}
	
	@Override
	public void clearCachedMemoryValue(){
		if(null != this.jitReferenceBuffer){
			this.jitReferenceBuffer.clearReadValue();
		}
	}
	
	boolean shouldPreRead(){
		return this.valueByteWidth <= 64/8; // Less than 64 bits, 8 bytes
	}

	public Long getByteWidth() {
		// Each character in the value is a nibble, each pair a byte. So FF is 8-bit/1-byte.
		// We only work with bytes, so these will always be byte-multiples.
		// return getMemoryValue().length() / 2;
		return valueByteWidth;
	}

	public PreDBMemoryReferenceValue cutTailOff(Long rightMostCutoffAddress) { // valueByteWidth=38, 35, 44, ca.uvic.chisel.atlantis.models.PreDBMemoryReferenceValue@43df23d3
		// end - start is actual width. End is non-inclusive. So, new cutoff can be equal to end, in which case width is unchanged.
		long newValByteWidth = (rightMostCutoffAddress - byteAddress);
		long bytesToRemove = valueByteWidth - newValByteWidth;
		PreDBMemoryReferenceValue tailess;
		if(isPreRead()){
			long oldStringWidth = this.getMemoryValue().length(); // byte length is half as long
			long newStringEndIndex = newValByteWidth * 2; // two chars needed to represent one byte, 0xF is 4-bits, 0xFF is 8-bits.
			long newStringWidth = newStringEndIndex; // because string 0 indexed, so width subtraction is correct
			String value = getMemoryValueImpl();
			tailess = new PreDBMemoryReferenceValue(byteAddress, newValByteWidth, value.substring(0, (int) (newStringEndIndex)));
		} else {
			// Work on the fence posts only
			tailess = new PreDBMemoryReferenceValue(byteAddress, newValByteWidth, this.jitReferenceBuffer);
		}
		return tailess;
	}
	
	public PreDBMemoryReferenceValue cutHeadOff(Long leftMostCutoffAddress) {
		// end - start is actual width. End is non-inclusive. So, new cutoff can be equal to end, in which case width is unchanged.
		long newValByteWidth = (int)((byteAddress+valueByteWidth) - leftMostCutoffAddress);
		long bytesToRemove = valueByteWidth - newValByteWidth;
		PreDBMemoryReferenceValue headless;
		if(isPreRead()){
			long oldStringWidth = this.getMemoryValue().length(); // byte length is half as long
			long newStringStartIndex = bytesToRemove * 2; // two chars needed to represent one byte, 0xF is 4-bits, 0xFF is 8-bits.
			long newStringWidth = oldStringWidth - newStringStartIndex; // because string 0 indexed, so width subtraction is correct
			String value = getMemoryValueImpl(); // 730074002E0037007A00
			headless = new PreDBMemoryReferenceValue(leftMostCutoffAddress, newValByteWidth, value.substring((int) (newStringStartIndex), (int) (newStringStartIndex+newStringWidth)));
		} else {
			// Work on the fence posts only, doesn't manipulate the string
			headless = new PreDBMemoryReferenceValue(leftMostCutoffAddress, newValByteWidth, this.jitReferenceBuffer);
		}
		return headless;
	}
}
