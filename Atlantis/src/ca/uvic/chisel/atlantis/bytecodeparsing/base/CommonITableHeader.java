package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import java.io.File;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;

/**
 * This is the beginning section of all itable files in the binary format. 
 *
 */
abstract public class CommonITableHeader extends BinaryFileParser {
	
	public ITraceXml getTraceXml(){
		return this.traceXml;
	}
	
	/**
	 * Provide the file that the extending class corresponds to.
	 * 
	 * @param binaryFormat
	 * @param childBinaryFile
	 * @param traceXml
	 */
	public CommonITableHeader(AtlantisBinaryFormat binaryFormat, File childBinaryFile, ITraceXml traceXml){
		super(binaryFormat, childBinaryFile, traceXml);

		// parse for fixed header values
		this.headerVer = this.getNextInt(); // ContextMapItable has weird values for this, verify it has the right format, then overlook.
		 
		this.recordVer = this.getNextInt();
		
		this.rowCount = this.getNextLong(); // ContextMapItable has 0 rowCount...I doubt it
		
		this.rowSize = this.getNextLong(); // ContextMapItable has 844433520132096...I doubt it
		
		this.tableFp = this.getNextLong();
		
//		System.out.println("Current bit address should be two ints and two longs in from 0: "+this.getCurrentBitAddress());
//		System.out.println("Header Version is "+this.headerVer+" for class "+this.getClass());
//		System.out.println("Record Version is "+this.recordVer+" for class "+this.getClass());
//		System.out.println("Row Count is "+this.rowCount+" for class "+this.getClass());
//		System.out.println("Row Size is "+this.rowSize+" for class "+this.getClass());
	}
	
	/** ver Oct 2013
	 * Header version number. Layout of the remainder of this header
	 * depends on its version. Only version 0 is currently supported for all tables.
	 * 
	 * 32-bit int
	 */
	final public int headerVer;
	
	/** ver Oct 2013
	 * Record layout version number. This refines the record type to use (which type or which
	 * version of this type). All known record types in this document only support version 0.
	 * 
	 * 32-bit int
	 */
	final public int recordVer;
	
	/** ver Oct 2013
	 * Number of records in the table
	 * 
	 * 64-bit long
	 */
	final public long rowCount;
	
	/** ver Oct 2013
	 * Size of each record, always sizeof(record) for a given RecordVer
	 * 
	 * 64-bit long
	 */
	final public long rowSize;
	
	/** ver Oct 2013
	 * File position (absolute, from start of file) of the first record (ID = 0) of the table. 
	 * 
	 * 64-bit long
	 */
	final public long tableFp;

	
	
}
