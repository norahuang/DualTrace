package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.codehaus.preon.buffer.ByteOrder;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;

/**
 * 
 * 
 * TODO If this needs to be thread safe, each caller should own its own
 * RandomAccessFile and possible seek position, and buffers. I would prefer
 * to force all callers to use different instances of this class to perform
 * synchronous reads.
 * 
 *
 */
abstract public class BinaryFileParser {

	protected AtlantisBinaryFormat binaryFormat;
	protected ITraceXml traceXml;
	
	protected HugeBitBuffer bitBuffer; // Extending Preon...needed to adapt to allow use of abritrarily large files.

	public BinaryFileParser(AtlantisBinaryFormat binaryFormat, File inputFile, ITraceXml traceXml){
		this.binaryFormat = binaryFormat;
		this.traceXml = traceXml;
		prepareRandomAccessFile(inputFile);
	}
	
	protected void prepareRandomAccessFile(File inputFile) {
		// Check to see if the final version of the index exists, not the temp version.
		if(inputFile.exists()){
			this.bitBuffer = new HugeBitBuffer(inputFile.getPath());
		}
	}
	
	/**
	 * Perform a random access read, to the absolute seek position requested.
	 * Does not rewind the read position afterwards. See {@link #getCurrentBitAddress()}
	 * and {@link #seekToBitAddress(long)} for a method to do so from the caller.
	 * 
	 * NB Caller should clear buffer when it is done with.
	 * 
	 */
	public ByteBuffer randomAccessRead(long absoluteSeekBytePositionOffset, int byteReadLength){
		long originalBitPosition = this.bitBuffer.getBitPos();
		this.bitBuffer.setBitPos(absoluteSeekBytePositionOffset * 8); // bits not bytes
		ByteBuffer result = getNextChunkOfBytes(byteReadLength);
		this.bitBuffer.setBitPos(originalBitPosition);
		return result;
	}
	
	/**
	 * Get the current bit address. Do not confuse for a byte address.
	 * @return
	 */
	public long getCurrentBitAddress(){
		return this.bitBuffer.getBitPos();
	}
	

	/**
	 * 	Set the current bit address. Do not confuse for a byte address.
	 * 
	 * @param fileOffsetBit
	 */
	public void seekToBitAddress(long fileOffsetBit) {
		this.bitBuffer.setBitPos(fileOffsetBit);
	}
	
	/**
	 * 	Get the current byte address. Do not confuse for a bit address.
	 * 
	 * @param fileOffsetByte
	 */
	public void seekToByteAddress(long fileOffsetByte) {
		seekToBitAddress(fileOffsetByte * 8);
	}
	
	public void skipPadBytes(int byteToSkip) {
		this.bitBuffer.setBitPos(this.bitBuffer.getBitPos() + byteToSkip * 8);
	}
	
	/**
	 * Unaligned bit version of {@link BinaryFileParser#getNextChunkOfBytes(int)}
	 * 
	 * This byte buffer can be used even when the buffer position is not 8-bit aligned.
	 * This is necessary whenever a boolean has been requested. The Preon getByteBuffer() method
	 * checks alignment and throws exceptions when it is not 8-bit aligned.
	 * 
	 * In the traces, bits in the trace are always 1 byte large, and thus are always aligned.
	 * The trace format is *not* packed. Nonetheless, this implementation behaved better than
	 * a previous one that used readAsByteBuffer.directly.
	 * 
	 * NB Caller should clear this buffer after use.
	 * 
	 * @param byteCount
	 * @return
	 */
	public ByteBuffer getNextChunkOfBytesCloned(int lengthInBytes) {
		// The implementation in Preon enforces 8-bit alignment, and have private methods.
		// We will give up the memory (and time) benefits of slicing, which is ok given the life cycle of
		// objects used in parsing (is it really ok for the time???)
		// Well, it appears that the byte alignment isn't really about the data, but about the requested
		// buffer size. Preon enforces this with an exception.
		// We will make a bit buffer, and manually place the bytes therein into a byte buffer.
		
		byte[] byteList = new byte[lengthInBytes];
		for(int i = 0; i < lengthInBytes; i++){
			byteList[i] = (byte) ( (int) this.getNextMiniInt(8) );
		}
		ByteBuffer buff = ByteBuffer.wrap(byteList);
		buff.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		return buff;
    }
	
	/**
	 * In case the caller has a varying series of elements to read, we can pass back a temporary byte buffer
	 * that is pre-filled on its behalf.
	 * 
	 * Could be better to have a non-thread-safe buffer that gets expanded as needed,
	 * and re-used for calls to this.
	 * 
	 * NB Caller should clear this buffer after usage.
	 * 
	 * @param byteCount
	 * @return
	 */
	public ByteBuffer getNextChunkOfBytes(int byteCount) {
		// OH! But didn't I realize that bits take up a byte? And therefore everything is always byte aligned (packed)?
		// See getNextBooleanByte() for where booleans come from a byte.
//		if ((this.bitBuffer.getBitPos() % 8) != 0 || (byteCount % 8) != 0) {
			// Automatically pop it over, because we can't require callers
			// to always know if the stream is not bit aligned due to
			// fetch of a boolean some time earlier.
			// It's a bait-and-switch regarding the slice though...
			// System.out.println("Study when this happens, in particular on the 91GB file.");
		// This turns out to be better than the previous readAsByteBuffer. Is there any reason at all to use that older approach?
		return getNextChunkOfBytesCloned(byteCount);
//		} else {
//			ByteBuffer buffer = this.bitBuffer.readAsByteBuffer(byteCount).order(java.nio.ByteOrder.LITTLE_ENDIAN);
//			return buffer;
//		}
	}
	
	public String getNextStringOfByteLength(int byteCount) {
		StringBuilder str = new StringBuilder();
		ByteBuffer byteBuff = this.bitBuffer.readAsByteBuffer(byteCount);
		byteBuff.order(java.nio.ByteOrder.LITTLE_ENDIAN);
//		CharBuffer charBuff = byteBuff.asCharBuffer();
		for(int i = 0; i < byteCount; i++){
			str.append((char)byteBuff.get(i)); // In Java, chars (and strings) are always UTF-16 encoded
		}
		// try clearing to improve memory usage
		byteBuff.clear();
		// TODO Shorter strings than allowed by the requested bytecount get extra spaces. Can we even distinguish
		// between valid trailing space? I am leaving trailing space in for now, but I'd love to trim it.
		return str.toString();
	}
	
	/**
	 * In the trace format there is only byte sized booleans.
	 * Do not dare to think that the Preon native getNextBoolean() is what you want.
	 * The trace format has no single bit booleans.
	 * @return
	 */
	public Boolean getNextBooleanByte(){
		// return this.bitBuffer.readAsBoolean(); //ByteOrder.LittleEndian);
		int booleanByte = this.getNextMiniInt(8);
		return booleanByte == 0;
	}
	
	public Integer getNextMiniInt(int bitsInInt){
		int readAsInt = this.bitBuffer.readAsInt(bitsInInt, intEndianess);
//		System.out.println("Int "+readAsInt+" using "+intEndianess);
		return readAsInt;
	}
	
	/**
	 * For extremely unusual occasion where an address (64-bit) might not be zero padded.
	 * Occurs in the LcSlot parsing.
	 * @param bitsInInt
	 * @return
	 */
	public Long getNextMiniLong(int bitsInInt){
		long readAsLong = this.bitBuffer.readAsLong(bitsInInt, intEndianess); // 72,132,362,237,902,848
//		ByteBuffer buffer = this.bitBuffer.readAsByteBuffer(bitsInInt/8);     //        360,781,709,313
//		long readAsLong = buffer.getLong();
////		System.out.println("Int "+readAsLong+" using "+intEndianess);
//		System.out.println(BinaryFormatParser.toHex(buffer));
		return readAsLong;
	}
	
	ByteOrder intEndianess = ByteOrder.LittleEndian;
	
	public Integer getNextInt() {
		int readAsInt = this.bitBuffer.readAsInt(32, intEndianess);
//		System.out.println("Int "+readAsInt+" using "+intEndianess);
		return readAsInt;
	}
	
	public Long getNextLong() {
		long readAsLong = this.bitBuffer.readAsLong(64, intEndianess); //ByteOrder.LittleEndian
		return readAsLong;
	}
	
	/**
	 * Rather than using this object as documented, convert to Java string.
	 * 
	 * The UStr is a string with the length explicitly defined in a 64-bit long at the head of the
	 * object. It is also null terminated. Note that the length as defined does not include that null terminator.
	 * 
	 * It is UTF-16 encoded.
	 *
	 * uStr: UTF-16 null-terminated string with explicit size
	 * 	Size: Length*2 + 10
	 * 	Offset	Type	Name	Description
	 * 	0		i16		Length	String length in UTF-16 “code units” (not characters), not including trailing null. The actual number of Unicode characters is always lesser or equal to Length. About “code units”, see 
	 * 							http://msdn.microsoft.com/en-us/library/windows/desktop/dd374081.aspx
	 * 	2		uc[Length+1]			String contents followed by a null character (16-bit 0)
	 * 
	 * @param dissasemblyOffset
	 * @return
	 */
	public String getUStrAtByteAddress(long byteAddress) {
		long originalBitAddress = this.getCurrentBitAddress();
		this.seekToByteAddress(byteAddress);
		
		// Original spec said i64, but hard evidence from ins.vtable showed string length as 16 bit (2-byte) number.
		int uStrLength = this.getNextMiniInt(16);
		
		// No + 1 on this range because we want to leave out the last one (the null character)
		int stringByteLength = (int) (uStrLength) * 2; // 2 byte chars
		// In Java, chars (and strings) are always UTF-16 encoded...can we simplify this?
		// byte[] byteArray = this.getByteArrayFromBuffer(this.getNextChunkOfBytes(stringByteLength));
		// API said this was potentially faster than the loop I wrote in getByteArrayFromBuffer (when I failed to find the extant method).
		ByteBuffer subBuffer = this.getNextChunkOfBytes(stringByteLength);
		byte[] byteArray = new byte[subBuffer.remaining()];
		subBuffer.get(byteArray, 0, subBuffer.remaining());
		
		byte swap;
		for(int i = 0; i < byteArray.length; i = i + 2){
			swap = byteArray[i];
			byteArray[i] = byteArray[i+1];
			byteArray[i+1] = swap;
		}
		String uStr = new String(byteArray, StandardCharsets.UTF_16);
		
		this.seekToBitAddress(originalBitAddress);
		// try clearing to improve memory usage
		subBuffer.clear();
		return uStr;
	}
	
	/** ver Feb 2015
	 * Rather than getting this object as documented, convert to Java string.
	 * 
	 * The CStr is a string with the length explicitly defined in a 64-bit long at the head of the
	 * object. It is also null terminated. Note that the length as defined does not include that null terminator.
	 * 
	 * It is ASCII encoded.
	 * 
	 * cStr: null-terminated character (8 bits) string with explicit size
	 * 	Size: Length + 9
	 * 	Offset	Type	Name	Description
	 * 	0		i16		Length	String length in characters (bytes), not including trailing null [documentation stated i64 type, but file evidence was for i16]
	 * 	2		ac[Length+1]			String contents followed by a null character (0)
	 * 
	 * @param byteAddress
	 * @return
	 */
	public String getCStrAtByteAddress(long byteAddress) {
		long originalBitAddress = this.getCurrentBitAddress();
		this.seekToByteAddress(byteAddress);
		
		// Original spec said i64, but hard evidence from ins.vtable showed string length as 16 bit (2-byte) number.
		int cStrLength = this.getNextMiniInt(16);
		// byte[] byteArray = this.getByteArrayFromBuffer(this.getNextChunkOfBytes(cStrLength));
		// API said this was potentially faster than the loop I wrote in getByteArrayFromBuffer (when I failed to find the extant method).
		ByteBuffer subBuffer = this.getNextChunkOfBytes(cStrLength);
		byte[] byteArray = new byte[subBuffer.remaining()];
		subBuffer.get(byteArray, 0, subBuffer.remaining());
		String cStr = new String(byteArray, StandardCharsets.US_ASCII);
		
		this.seekToBitAddress(originalBitAddress);
		// try clearing to improve memory usage
		subBuffer.clear();
		return cStr;
	}
	
	/**
	 * Preferred to calling {@link ByteBuffer#array()} for when there is no backing array.
	 * 
	 * @param byteBuffer
	 * @return
	 */
	@Deprecated
	public byte[] getByteArrayFromBuffer(ByteBuffer byteBuffer){
		int remaining = byteBuffer.remaining();
		byte[] byteArray = new byte[remaining];
		for(int i = 0; i < remaining; i++){
			byteArray[i] = byteBuffer.get(i);
		}
		return byteArray;
	}

	/**
	 * Computes offset of the next null character (8-bit). Rewinds afterwards.
	 * 
	 * @return
	 */
	public long getNextNullCharacterBitAddress() {
		long originalBitOffset = this.getCurrentBitAddress();
		
		long result;
		char currChar;
		do {
			result = this.getCurrentBitAddress();
			currChar = (char) (int) this.getNextMiniInt(8);
		} while('\u0000' != currChar);
		
		this.seekToBitAddress(originalBitOffset);
		
		return result;
	}
	
}
