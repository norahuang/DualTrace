package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames20150121;

public class ModuleRec {
	
	static private final int BYTE_SIZE_MODULE_REC = (6*64) / 8; // six long members
	static private final int BYTE_SIZE_MODULE_REC_20150121 = (6*64 + 2*64) / 8; // six long members, plus 2 long skip (unknown if padding or undocumented data)

	public final String winNameValue; // from a UStr

	public final String deviceNameValue; // from a UStr

	ModuleRec(ModuleItable parentParser, int moduleNumber){
		int recSize = BYTE_SIZE_MODULE_REC;
		if(parentParser.getTraceXml().getXedVersion().equals(RegisterNames20150121.VERSION)){
			recSize = BYTE_SIZE_MODULE_REC_20150121;
		}
		
		long moduleRecOffset = parentParser.tableFp + moduleNumber * recSize;
		
		parentParser.seekToByteAddress(moduleRecOffset);
		
		// Likely unused, but we may need it for the module names (*.exe, *.dll).
		this.loadEid = parentParser.getNextLong();

		this.unloadEid = parentParser.getNextLong();

		this.startAddr = parentParser.getNextLong();

		this.endAddr = parentParser.getNextLong();

		this.winNameFp = parentParser.getNextLong();

		this.deviceNameFp = parentParser.getNextLong();
		
		// Not sure why. Not in spec, but required to parse module recs correctly
		if(parentParser.getTraceXml().getXedVersion().equals(RegisterNames20150121.VERSION)){
			lastFileWriteTimestamp = parentParser.getNextLong();
			versionInfoUnion = parentParser.getNextLong();
		} else {
			lastFileWriteTimestamp = 0;
			versionInfoUnion = 0;
		}
		
		// Grab some goods
		this.winNameValue = parentParser.getUStrAtByteAddress(this.winNameFp);
		this.deviceNameValue = parentParser.getUStrAtByteAddress(this.deviceNameFp);
	}

	// Likely unused, but we may need it for the module names (*.exe, *.dll).
	/** ver Oct 2013
	 * Load time, expressed through the approximate ID of the execution record about to be recorded at that point.
	 * 
	 * 64-bit long
	 */
	public final long loadEid; // load time, expressed as approx. execution record ID

	/** ver Oct 2013
	 * Unload time, expressed through the approximate ID of the execution record about to be recorded at that point.
	 * Set to -1 if the trace recording ends before module is unloaded.
	 * 
	 * 64-bit long
	 */
	public final long unloadEid; // unload time, expressed as approx. execution record ID

	/** ver Oct 2013
	 * Memory address where the module was loaded
	 * 
	 * 64-bit long
	 */
	public final long startAddr; // memory address where module was loaded

	/** ver Oct 2013
	 * Memory address of the last module byte (inclusive)
	 * 
	 * 64-bit long
	 */
	public final long endAddr; // last memory address of bytes included in module

	/** ver Oct 2013
	 * FP to full path and file name, in usual Windows format, such as 
	 * C:\Windows\System32\mf.dll 
	 * FP is relative to start of file (absolute FP)
	 * 
	 * Pointer like ustr *, converted to Java String
	 * 
	 * 64-bit long
	 */
	public final long winNameFp; // pointer to string packed in this file, relative to top of file.

	/** ver Oct 2013
	 * FP to Low-level device path, such as 
	 * \Device\HarddiskVolume3\Windows\System32\mf.dll
	 * FP is relative to start of file (absolute FP)
	 * 
	 * Pointer like ustr *, converted to Java String
	 * 
	 * 64-bit long
	 */
	public final long deviceNameFp; // pointer to string packed in this file, relative to top of file.
	
	/** ver Jan 2015, not documented until April 2016 (via correspondence with David Ouellet at DRDC)
	 * LastFileWriteTime: Windows FILETIME units (https://msdn.microsoft.com/en-us/library/windows/desktop/ms724284(v=vs.85).aspx) stored as a uint64_t
	 * 
	 * * 64-bit long
	 */
	public final long lastFileWriteTimestamp; // Windows FILETIME timestamp

	
    /** ver Jan 2015, not documented until April 2016 (via correspondence with David Ouellet at DRDC)
     * VersionInfo: union of either:
     * an array of 4 uint16_t (gives a version number of the form w.x.y.z with each uint16_t being of the the number); or
     * a uint64_t (packed representation of the version number)
     * 
     * the values are 0 if an older trace was converted to the new format.
     * 
     * * 64-bit long
	 */
	public final long versionInfoUnion; // might be either a packed int or a w.x.y.z int (four 16 bit ones)

}
