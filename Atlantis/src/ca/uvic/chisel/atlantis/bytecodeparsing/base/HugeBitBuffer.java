/**
 * Copyright (C) 2009-2010 Wilfred Springer
 *
 * This file is part of Preon.
 *
 * Preon is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Preon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Preon; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 * 
 * The Chisel Group version of this derived file was based on Wilfred Springer's project https://github.com/preon, commit a7ae2aee59fe8f8c87661d1a96668dce0720363a
 */
package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.BitBufferException;
import org.codehaus.preon.buffer.BitBufferUnderflowException;
import org.codehaus.preon.buffer.ByteOrder;
import org.codehaus.preon.buffer.DefaultBitBuffer;
import org.codehaus.preon.buffer.SlicedBitBuffer;

/**
 * The original class sets the size of the file mapping to the size of the file,
 * but this does not work for files with a number of bytes larger than {@link Integer#MAX_VALUE},
 * throwing and exception when such a file is used.
 * Notably, the position parameter for {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
 * is long, and allows for extremely huge file positions, but the window is small. This is only a problem
 * because of Preon's behavior.
 * 
 * This class abstracts over the re-mapping necessary for extremely large files, and fixes the flaw in
 * the original constructor.
 * 
 * - fix constructor
 * - override the validateInputParams() to allow re-mapping the file whenever
 *   parameters have been requested outside the current window
 *   
 *   Since the original {@link DefaultBitBuffer} class has all private members, I need to shadow all of them,
 *   or I need to extend {@link BitBuffer} instead.
 * 
 * The Chisel Group version of this derived file was based on Wilfred Springer's project https://github.com/preon, commit a7ae2aee59fe8f8c87661d1a96668dce0720363a
 * 
 * @author everbeek
 *
 */
// TODO Change to using a re-exported jar of thechiselgroup/preon, rather than using this version of the functionality.
public class HugeBitBuffer implements BitBuffer {

    static Log log = LogFactory.getLog(HugeBitBuffer.class);

    protected ByteBuffer byteBuffer;
    
    protected long defaultBufferByteSize;
    
    protected long fileByteSize;

    protected long bufferBitPosReadNext;

    protected long bitBufBitSize;
    
    protected long bufferHeadBitPos;

    /**
     * Only exists if the filename constructor was used. If it doesn't exist, then
     * the ability to use actually huge files will not be available.
     */
	private File file;

    
    /**
     * Constructs a new instance.
     *
     * @param inputByteBuffer input buffered byte stream
     */
    public HugeBitBuffer(ByteBuffer inputByteBuffer, long fileSize) {
        // TODO: I think we should use #limit() instead of #capacity()
        this(inputByteBuffer, ((long) (inputByteBuffer.capacity())) << 3, 0L, fileSize);
    }

    /**
     * Constructs a new instance.
     *
     * @param inputByteBuffer
     * @param bitBufBitSize
     * @param bitPos
     * @param fileByteSize
     */
    private HugeBitBuffer(ByteBuffer inputByteBuffer, long bitBufBitSize, long bitPos, long fileByteSize) {
        this.byteBuffer = inputByteBuffer;
        this.bitBufBitSize = bitBufBitSize;
        this.bufferBitPosReadNext = bitPos;
        this.fileByteSize = fileByteSize;
        this.bufferHeadBitPos = 0;
    }
	
    /** Read byte buffer containing binary stream and set the bit pointer position to 0. */
    public HugeBitBuffer(String fileName) {

        this.file = new File(fileName);

        // Open the file and then get a org.codehaus.preon.channel.channel from the stream
        try (
    		FileInputStream fis = new FileInputStream(file);
    		FileChannel fc = fis.getChannel();
		){
            // Get the file's size and then map it into memory
            this.fileByteSize = fc.size();
            this.defaultBufferByteSize = Math.min(fileByteSize, Integer.MAX_VALUE);
            fc.close();
            
            this.mapByteBuffer(0);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected void mapByteBuffer(long reqBitPos){
    	long reqBytePos = reqBitPos/8L;
    	if(reqBytePos > this.fileByteSize){
    		throw new BitBufferException("Attempt to go to position of file larger than file size: "+(reqBytePos)+" vs "+this.fileByteSize+" in file: "+file.getAbsolutePath());
    	}
        // Open the file and then get a org.codehaus.preon.channel.channel from the stream
        try (
    		FileInputStream fis = new FileInputStream(file);
    		FileChannel fc = fis.getChannel();
		){
            long effectiveBufferByteSize = this.defaultBufferByteSize;
            // Why bother making it smaller than the file?
            if(reqBytePos + defaultBufferByteSize + 1 > this.fileByteSize){
            	// Use smaller buffer size.
            	effectiveBufferByteSize = this.fileByteSize - reqBytePos; // fileSize 10, pos 9, buffer 3, needs to become buffer 1.
            }
            this.byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, reqBytePos, effectiveBufferByteSize);
            bitBufBitSize = ((long) (this.byteBuffer.capacity())) * 8L;
            bufferHeadBitPos = reqBitPos;
            bufferBitPosReadNext = 0;
        } catch (Exception e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
    }

    // JavaDoc inherited

    public void setBitPos(long bitPos) {
        this.bufferBitPosReadNext = bitPos;
    }

    // JavaDoc inherited

    public long getBitPos() {
        return this.bufferBitPosReadNext;
    }

    // JavaDoc inherited

    public long getBitBufBitSize() {
        return bitBufBitSize;
    }

    // readBits

    // JavaDoc inherited

    public long readBits(int nrBits) {
        return readAsLong(nrBits);
    }

    // JavaDoc inherited

    public long readBits(long bitPos, int nrBits) {
        return readAsLong(bitPos, nrBits);
    }

    // JavaDoc inherited

    public long readBits(int nrBits, ByteOrder byteOrder) {
        return readAsLong(nrBits, byteOrder);
    }

    // JavaDoc inherited

    public long readBits(long bitPos, int nrBits, ByteOrder byteOrder) {

        if (nrBits <= 8)
            return readAsByte(bitPos, nrBits, byteOrder);
        else if (nrBits <= 16)
            return readAsShort(bitPos, nrBits, byteOrder);
        else if (nrBits <= 32)
            return readAsInt(bitPos, nrBits, byteOrder);
        else if (nrBits <= 64)
            return readAsLong(bitPos, nrBits, byteOrder);
        else
            throw new BitBufferException("Wrong number of bits to read ("
                    + nrBits + ").");
    }

    // boolean

    /**
     * {@inheritDoc}
     * 
     * Reads the boolean as a *byte-sized* element currently. Bit sized reads
     * are possible, but currently restricted in the Atlantis implementation.
     */
    public boolean readAsBoolean() {
        return readAsBooleanByteSized(bufferBitPosReadNext, ByteOrder.BigEndian);
    }

    /**
     * {@inheritDoc}
     * 
     * Reads the boolean as a *byte-sized* element currently. Bit sized reads
     * are possible, but currently restricted in the Atlantis implementation.
     */
    public boolean readAsBoolean(long bitPos) {
        return readAsBooleanByteSized(bitPos, ByteOrder.BigEndian);
    }

    /**
     * {@inheritDoc}
     * 
     * Reads the boolean as a *byte-sized* element currently. Bit sized reads
     * are possible, but currently restricted in the Atlantis implementation.
     */
    public boolean readAsBoolean(ByteOrder byteOrder) {
        return readAsBooleanByteSized(bufferBitPosReadNext, byteOrder);
    }
    
    /**
     * {@inheritDoc}
     * 
     * Reads the boolean as a *byte-sized* element currently. Bit sized reads
     * are possible, but currently restricted in the Atlantis implementation.
     */
    public boolean readAsBoolean(long bitPos, ByteOrder byteOrder) {
    	return readAsBooleanByteSized(bitPos, byteOrder);
    }

    /**
     * Trace docs indicate that bool is a "one-byte value that is either 0 or 1".
     */
    public boolean readAsBooleanByteSized(long bitPos, ByteOrder byteOrder) {
        int result = getResultAsInt(bitPos, 8, byteOrder, 8);
        if(result > 1 || result < 0){
        	System.out.println("boolean reads as less than 0 or greater than 1");
    		throw new BitBufferException("Byte-sized boolean read at" + bitPos
                    + "has a value other than 0 or 1, when parsed as a full byte. Expect 0 padded.");
        }
        return result == 1;
    }
    
    /**
     * The trace documentation specifies that bools are 8-bits wide. Originally, we
     * supported bit reading, but given that the trace files do not need this, I am
     * guarding against accidental use, by deprecating this strict bit sized bool
     * method, as well as with thrown exceptions for when read attempts are not byte aligned.
     * 
     * If bit reading is needed, adjust the exceptions (they are no longer exceptional)
     * and remove the deprecation on this method.
     */
    @Deprecated
    public boolean readAsBooleanBitSized(long bitPos, ByteOrder byteOrder) {
        int result = getResultAsInt(bitPos, 1, byteOrder, 1);
        return result == 1;
    }

    // signed byte

    // JavaDoc inherited

    public byte readAsByte(int nrBits) {
        return readAsByte(bufferBitPosReadNext, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public byte readAsByte(int nrBits, ByteOrder byteOrder) {
        return readAsByte(bufferBitPosReadNext, nrBits, byteOrder);
    }

    // JavaDoc inherited

    public byte readAsByte(int nrBits, long bitPos) {
        return readAsByte(bitPos, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public byte readAsByte(long bitPos, int nrBits, ByteOrder byteOrder) {
        return (byte) getResultAsInt(bitPos, nrBits, byteOrder, 8);
    }

    // signed short
    // JavaDoc inherited

    public short readAsShort(int nrBits) {
        return readAsShort(bufferBitPosReadNext, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public short readAsShort(long bitPos, int nrBits) {
        return readAsShort(bitPos, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public short readAsShort(int nrBits, ByteOrder byteOrder) {
        return readAsShort(bufferBitPosReadNext, nrBits, byteOrder);
    }

    // JavaDoc inherited

    public short readAsShort(long bitPos, int nrBits, ByteOrder byteOrder) {
        return (short) getResultAsInt(bitPos, nrBits, byteOrder, 16);
    }

    // signed int

    // JavaDoc inherited

    public int readAsInt(int nrBits) {
        return readAsInt(bufferBitPosReadNext, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public int readAsInt(long bitPos, int nrBits) {
        return readAsInt(bitPos, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public int readAsInt(int nrBits, ByteOrder byteOrder) {
        return readAsInt(bufferBitPosReadNext, nrBits, byteOrder);
    }

    // JavaDoc inherited

    public int readAsInt(long bitPos, int nrBits, ByteOrder byteOrder) {
        if (getNrNecessaryBytes(bitPos, nrBits) > 4)
            return (int) getResultAsLong(bitPos, nrBits, byteOrder, 32);
        else
            return getResultAsInt(bitPos, nrBits, byteOrder, 32);
    }

    // signed long

    // JavaDoc inherited

    public long readAsLong(int nrBits) {
        return readAsLong(bufferBitPosReadNext, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public long readAsLong(long bitPos, int nrBits) {
        return readAsLong(bitPos, nrBits, ByteOrder.BigEndian);
    }

    // JavaDoc inherited

    public long readAsLong(int nrBits, ByteOrder byteOrder) {
        return readAsLong(bufferBitPosReadNext, nrBits, byteOrder);
    }

    // JavaDoc inherited

    public long readAsLong(long bitPos, int nrBits, ByteOrder byteOrder) {
        return getResultAsLong(bitPos, nrBits, byteOrder, 64);
    }

    // private methods

    /**
     * Return the minimum number of bytes that are necessary to be read in order to read specified bits
     *
     * @param bitPos position of the first bit to read in the bit buffer
     * @param nrBits number of bits to read
     * @return number of bytes to read
     */
    private static int getNrNecessaryBytes(long bitPos, int nrBits) {
        return (int) (((bitPos % 8) + nrBits + 7) / 8);
    }

    /**
     * This method shifts to the right the integer buffer containing the specified bits, so that the last specified bit is
     * the last bit in the buffer.
     *
     * @param numberBuf integer buffer containing specified bits
     * @param bitPos    position of the first specified bit
     * @param nrBits    number of bits to read
     * @return shifted integer buffer
     */
    private static int getRightShiftedNumberBufAsInt(int numberBuf,
                                                     long bitPos, int nrBits, ByteOrder byteOrder) throws BitBufferException {

        // number of bits integer buffer needs to be shifted to the right in
        // order to reach the last bit in last byte
        long shiftBits;
        if (byteOrder == ByteOrder.BigEndian)
            shiftBits = 7 - ((nrBits + bitPos + 7) % 8);
        else
            shiftBits = bitPos % 8;

        return numberBuf >> shiftBits;
    }

    /**
     * This method shifts to the right the long buffer containing the specified bits, so that the last specified bit is the
     * last bit in the buffer.
     *
     * @param numberBuf long buffer containing specified bits
     * @param bitPos    position of the first specified bit
     * @param nrBits    number of bits to read
     * @return shifted integer buffer
     */
    private static long getRightShiftedNumberBufAsLong(long numberBuf,
                                                       long bitPos, int nrBits, ByteOrder byteOrder) throws BitBufferException {

        // number of bits integer buffer needs to be shifted to the right in
        // order to reach the last bit in last byte
        long shiftBits;
        if (byteOrder == ByteOrder.BigEndian)
            shiftBits = 7 - ((nrBits + bitPos + 7) % 8);
        else
            shiftBits = bitPos % 8;

        return numberBuf >> shiftBits;
    }

    /**
     * Return a value of integer type representing the buffer storing specified bits
     *
     * @param byteOrder       Endian.Little of Endian.Big order of storing bytes
     * @param nrReadBytes  number of bytes that are necessary to be read in order to read specific bits
     * @param firstBytePos position of the first byte that is necessary to be read
     * @return value of all read bytes, containing specified bits
     */
    private int getNumberBufAsInt(ByteOrder byteOrder, int nrReadBytes,
                                  int firstBytePos) {

        int result = 0;
        int bytePortion = 0;
        for (int i = 0; i < nrReadBytes; i++) {
            bytePortion = 0xFF & (byteBuffer.get(firstBytePos++));

            if (byteOrder == ByteOrder.LittleEndian)
                // reshift bytes
                result = result | bytePortion << (i << 3);
            else
                result = bytePortion << ((nrReadBytes - i - 1) << 3) | result;
        }

        return result;
    }

    /**
     * Return a value of long type representing the buffer storing specified bits.
     *
     * @param byteOrder       Endian.Little of Endian.Big order of storing bytes
     * @param nrReadBytes  number of bytes that are necessary to be read in order to read specific bits
     * @param firstBytePos position of the first byte that is necessary to be read
     * @return value of all read bytes, containing specified bits
     */
    private long getNumberBufAsLong(ByteOrder byteOrder, int nrReadBytes,
                                    int firstBytePos) {

        long result = 0L;
        long bytePortion = 0L;
        for (int i = 0; i < nrReadBytes; i++) {
            bytePortion = 0xFF & (byteBuffer.get(firstBytePos++));

            if (byteOrder == ByteOrder.LittleEndian)
                // reshift bytes
                result = result | bytePortion << (i << 3);
            else
                result = bytePortion << ((nrReadBytes - i - 1) << 3) | result;
        }

        return result;
    }

    /**
     * Check if all input parameters are correct, otherwise throw BitBufferException
     *
     * @param reqBitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     */
    private void validateInputParams(long reqBitPos, int nrBits) {
    	validateInputParams(reqBitPos, nrBits, -1);
    }
    private void validateInputParams(long reqBitPos, int nrBits, Integer maxNrBitsRead) {
    	// If the requested bytes are to the left of the loaded buffer, or if the request lay beyond
    	// the capacity and limit of the current buffer...
    	if(reqBitPos < this.bufferHeadBitPos //+ this.bufferBitPosReadNext
			|| reqBitPos + nrBits - 1 > this.bufferHeadBitPos + this.byteBuffer.capacity()*8L
			){
    		// buffer size is set to the file size or Integer.MAX_VALUE, which ever is
    		// smaller. If the request is for more than is available, the caller has made
    		// a mistake, and it will come up in further processing.
    		this.mapByteBuffer(reqBitPos);
    	}
    	
    	if (reqBitPos % 2 != 0) {
    		// An error was once discovered that had odd bit position. Also, we have
    		// bit level read capacity, but the current traces do not have any such
    		// requirements, so I will use this to help guard against such occurrences.
    		// It may be removed if such an event is no longer exceptional.
    		throw new BitBufferException("Bit position desired is an odd number (" + nrBits
                    + ") should be divisible by two. All elements in current trace documentation are byte-aligned.");
    	}
    	
        if (nrBits < 1) {
            throw new BitBufferException("Number of bits to read (" + nrBits
                    + ") should greater than zero.");
        }

        if (reqBitPos < 0)
            throw new BitBufferException("Bit position (" + reqBitPos
                    + ") should be positive.");

        if (-1 != maxNrBitsRead && maxNrBitsRead != 1 && maxNrBitsRead != 8 && maxNrBitsRead != 16
                && maxNrBitsRead != 32 && maxNrBitsRead != 64)
            throw new BitBufferException("Max number of bits to read ("
                    + maxNrBitsRead + ") should be either 1, 8, 16, 32 or 64.");

        if (-1 != maxNrBitsRead && nrBits > maxNrBitsRead)
            throw new BitBufferException("Cannot read " + nrBits
                    + " bits using " + maxNrBitsRead
                    + " bit long numberBuf (reqBitPos=" + reqBitPos + ").");

        // if (nrBits <= maxNrBitsRead / 2 && log.isWarnEnabled())
        // log.warn("It is not recommended to read " + nrBits + " using "
        // + maxNrBitsRead + " bit long numberBuf (bitPos=" + bitPos
        // + ").");

    }

    /**
     * Return an integral mask with a given number of 1's at least significant bit positions.
     *
     * @param nrBits number of bits to read
     * @return integer value of the mask
     */
    private static int getMaskAsInt(int nrBits) {
        return 0xFFFFFFFF >>> (32 - nrBits);
    }

    /**
     * Return an integral mask with a given number of 1's at least significant bit positions.
     *
     * @param nrBits number of bits to read
     * @return long value of the mask
     */
    private static long getMaskAsLong(int nrBits) {
        return 0xFFFFFFFFFFFFFFFFL >>> (64 - nrBits);
    }

    /**
     * Calculates the value represented by the given bits.
     *
     * @param bitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param byteOrder        order of reading bytes (either Endian.Big or Endian.Little)
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     * @return the integer value represented by the given bits
     */
    private int getResultAsInt(long bitPos, int nrBits, ByteOrder byteOrder,
                               int maxNrBitsRead) {
    	// TODO The maxNrBitsRead and nrBits don't seem to be semantically optimized...
    	// It is not clear what each is responsible for determining, or if both are needed.

        // check if input params are correct otherwise throw BitBufferException
        validateInputParams(bitPos, nrBits, maxNrBitsRead);

        // min number of bytes covering specified bits
        int nrReadBytes = getNrNecessaryBytes(bitPos, nrBits);

        // buffer containing specified bits
        int numberBuf = getNumberBufAsInt(byteOrder, nrReadBytes,
                (int) ((bitPos - this.bufferHeadBitPos) >> 3)); // old was (int) (bitPos >> 3));

        // mask leaving only specified bits
        int mask = getMaskAsInt(nrBits);

        // apply the mask for to the right shifted number buffer with the
        // specific bits to the most right
        int result = mask
                & getRightShiftedNumberBufAsInt(numberBuf, bitPos, nrBits,
                byteOrder);

        // increase bit pointer position by the number of read bits
        this.bufferBitPosReadNext = bitPos + nrBits;

        return result;
    }

    /**
     * Calculates the value represented by the given bits.
     *
     * @param bitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param byteOrder        order of reading bytes (either Endian.Big or Endian.Little)
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     * @return the long value represented by the given bits
     */
    private long getResultAsLong(long bitPos, int nrBits, ByteOrder byteOrder,
                                 int maxNrBitsRead) {

        // check if input params are correct otherwise throw BitBufferException
        validateInputParams(bitPos, nrBits, maxNrBitsRead);

        // min number of bytes covering specified bits
        int nrReadBytes = getNrNecessaryBytes(bitPos, nrBits);

        // buffer containing specified bits
        long numberBuf = getNumberBufAsLong(byteOrder, nrReadBytes,
                (int) ((bitPos - this.bufferHeadBitPos) >> 3)); // (int) (bitPos >> 3));
        // mask leaving only specified bits
        long mask = getMaskAsLong(nrBits);

        // apply the mask for to the right shifted number buffer with the
        // specific bits to the most right
        long result = mask
                & getRightShiftedNumberBufAsLong(numberBuf, bitPos, nrBits,
                byteOrder);

        // increase bit pointer position by the number of read bits
        this.bufferBitPosReadNext = bitPos + nrBits;

        return result;
    }

    /**
     * Getter for inputByteBuf.
     *
     * @return Returns the inputByteBuf.
     */
    protected ByteBuffer getInputByteBuf() {
        return byteBuffer;
    }

    // JavaDoc inherited

    public BitBuffer slice(long length) {
        BitBuffer result = new SlicedBitBuffer(duplicate(), length);
        setBitPos(getBitPos() + length);
        return result;
    }

    // JavaDoc inherited

    public BitBuffer duplicate() {
        return new HugeBitBuffer(byteBuffer.duplicate(), bitBufBitSize, bufferBitPosReadNext, fileByteSize);
    }

    public ByteBuffer readAsByteBuffer(int byteLength)
            throws BitBufferUnderflowException {
    	
        if ((this.bufferBitPosReadNext % 8) != 0) {
            throw new BitBufferException(
                    "8-bit alignment exception. Bit position (" + bufferBitPosReadNext
                            + ") should be 8-bit aligned");
        }

        int bitsToRead = byteLength << 3; // == (length * 8L)
        // check if input params are correct otherwise throw BitBufferException
        validateInputParams(bufferBitPosReadNext, bitsToRead); // this was designed for the int/long ones. Works here too???

        if (getBitPos() + bitsToRead > getBitBufBitSize() + this.bufferHeadBitPos) { // old was if (getBitPos() + bitsToRead > getBitBufBitSize()) {
            throw new BitBufferUnderflowException(getBitPos(), bitsToRead);
        }

//        long slicey = bufferBitPosReadNext < bufferHeadBitPos ? bufferBitPosReadNext : bufferBitPosReadNext - bufferHeadBitPos; // this makes no sense, it cannot be right...
        long slicey = bufferBitPosReadNext/8;
        int sliceStartPosition = (int) ((slicey) >>> 3);// == (bitPos / 8)

        ByteBuffer slicedByteBuffer = this.slice(byteBuffer,
                sliceStartPosition, byteLength); // why would I try using bitsToRead);?

        // ByteBuffer byteBuffer = ByteBuffer.wrap(this.byteBuffer.array(),
        // (int) (this.bitPos >>> 3), length);

        this.bufferBitPosReadNext = bufferBitPosReadNext + bitsToRead;

        return slicedByteBuffer;
    }

    public ByteBuffer readAsByteBuffer() {
        ByteBuffer buffer =  byteBuffer.duplicate();
        buffer.rewind();
        return buffer;
    }

    /**
     * Work around that allows creation of a sub-view of a larger byte buffer.
     * <p/>
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5071718
     *
     * @param byteBuffer    - Original {@link ByteBuffer} to be sliced
     * @param slicePosition - Start position of the slice (e.g. sub-view) in the byte buffer
     * @param length        - Length of the slice (e.g. sub-view) in bytes, measured from the positions
     * @return Returns the sliced {@link ByteBuffer}. Original buffer is left in its original state;
     */
    private ByteBuffer slice(ByteBuffer byteBuffer, int slicePosition,
                             int length) {

        int currentBufferPosition = this.byteBuffer.position();
        int currentBufferLimit = this.byteBuffer.limit();

        this.byteBuffer.position(slicePosition).limit(slicePosition + length);

        ByteBuffer slicedByteBuffer = this.byteBuffer.slice();

		// Revert the buffer in its original state
		this.byteBuffer.position(currentBufferPosition).limit(
				currentBufferLimit);

		return slicedByteBuffer;
	}

    public long getActualBitPos() {
        return bufferBitPosReadNext;
    }

}
