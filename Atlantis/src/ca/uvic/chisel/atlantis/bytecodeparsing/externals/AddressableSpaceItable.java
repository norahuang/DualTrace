package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;

import ca.uvic.chisel.atlantis.bytecodeparsing.base.CommonITableHeader;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
public class AddressableSpaceItable extends CommonITableHeader {
	
	public AddressableSpaceItable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getAddressableSpaceItable(), traceXml);
		
	}
	
	/**
	 * Parse on demand for the ModuleRec objects, of which we have 'rowCount' of.
	 * 
	 * @param moduleNumber
	 * @return
	 */
	public AddressRangeRec getAddressRange(int index){
		// There are this.rowCount of these, and are all direct addressable via the offset file.	
		return new AddressRangeRec(this, index);
	}
	
	/** ver Oct 2013
	 * Table-specific additional header information.
	 * Note: this is at offset 32+, until any Table data.
	 */
	
	/** ver Oct 2013
	 * Array of RowCount contiguous records. 
	 * 
	 * Number of elements is 'rowCount'. See getAddressRange(long) for random access rather
	 * than storing in a container.
	 */
	 // AddressRange[] addrRecords;
	 
	 
	 public class AddressRangeRec {
		 
		 final static long BYTE_SIZE_OF_ADDRESS_RANGE_REC = (2*64) / 8; // two longs
		 
		 AddressRangeRec(AddressableSpaceItable parentParser, long index){
			 long byteOffset = parentParser.tableFp + (index * BYTE_SIZE_OF_ADDRESS_RANGE_REC);
			 parentParser.seekToByteAddress(byteOffset);
			 this.first = parentParser.getNextLong();
			 this.last = parentParser.getNextLong();
		 }
		 
		 /** ver Oct 2013
		  * First byte of a page range, always a multiple of 4 kB.\
		  * 
		  * 64-bit long
		  */
		 public final long first;
		 
		 /** ver Oct 2013
		  * Last byte (included) of a page range. Always greater than First and one less than a multiple of 4 kB.
		  * 
		  * 64-bit long
		  */
		 public final long last;
	 }
		
}
