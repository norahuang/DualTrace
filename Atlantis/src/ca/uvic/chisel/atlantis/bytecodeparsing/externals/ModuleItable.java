package ca.uvic.chisel.atlantis.bytecodeparsing.externals;


import java.util.ArrayList;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.CommonITableHeader;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsRec;

// Currently, the only thing Atlantis needs is the module name,
// but I am not yet sure that the name is not referenced in trace text.
// If ids are used and not module names (*.exe, *.dll), then complete
// this as best as possible. Otherwise ignore it as unused.
public class ModuleItable extends CommonITableHeader {
	
	public ModuleItable(AtlantisBinaryFormat binaryFormat, ITraceXml traceXml){
		super(binaryFormat, binaryFormat.getModuleItableFile(), traceXml);
		
		this.extraFp = this.getNextLong();
		
		this.extraSize = this.getNextLong();
	}
	
	/**
	 * Rather than create a DB table for module information, access it directly from the binary files.
	 * This is already done when paging in disassembly text to the editor, so we are already committed
	 * to having the binary files present and accessible.
	 * 
	 * @return
	 */
	public ArrayList<ModuleRec> getAllModules(){
		ArrayList<ModuleRec> modules = new ArrayList<ModuleRec>();
		for(int moduleId = 0; moduleId < this.rowCount; moduleId++){
			modules.add(new ModuleRec(this, moduleId));
		}
		return modules;
	}
	
	/**
	 * Parse on demand for the ModuleRec objects, of which we have 'rowCount' of.
	 * 
	 * @param insRec.moduleId
	 * @return
	 */
	public ModuleRec getModuleRec(InsRec insRec){
		if(-1 == insRec.moduleId){
			// Id is -1 when the code running is not in a module but is free-floating code.
			return null;
		}
		// There are this.rowCount of these, and are all direct addressable via the offset file.	
		return new ModuleRec(this, insRec.moduleId);
	}
	
	/** ver Oct 2013
	 * Table-specific additional header information.
	 * Note: this is at offset 32+, until any Table data.
	 */
	
	/** ver Oct 2013
	 * Absolute file pointer to a buffer containing collections of strings pointed to by module records.
	 * Points to buf[ExtraSize] *
	 * 
	 * 64-bit long
	 */
	public final long extraFp;
	
	/** ver Oct 2013
	 * Total size of the buffer pointed to by ExtraFP.
	 * 
	 * 64-bit long
	 */
	public final long extraSize;
	
	/** ver Oct 2013
	 * Array of RowCount contiguous records.
	 * 
	 * See {@link #getModuleRec(int)} for parse-on-demand access rather than retrieving from a container.
	 * 
	 * Number of elements is 'rowCount'
	 */
	 // ModuleRec[] moduleRec;
		
}
