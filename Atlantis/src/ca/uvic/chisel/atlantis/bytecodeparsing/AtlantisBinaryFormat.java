package ca.uvic.chisel.atlantis.bytecodeparsing;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

public class AtlantisBinaryFormat {
	
	public final File providedFile;
	public final File binaryFolder;
	
	// Interesting binary files
	private File execVtable;
	private File execOffsets;
	private File inisItable;
	private File threadItable;
	private File syscallOffsets;
	private File syscallVtable;
	private File traceXml;
	
	// Less interesting and unused
	private File addressableSpaceItable;
	private File contextMapItable;
	private File execPrevNextColumn;
	private File moduleItable;
	
	// Interesting binary files
	private static final  String execVtableFilename = "exec.vtable";
	private static final  String execOffsetsFilename = "exec.offsets";
	private static final  String insItableFilename = "ins.itable";
	private static final  String threadItableFilename = "thread.itable";
	private static final  String syscallOffsetsFilename = "syscall.offsets";
	private static final  String syscallVtableFilename = "syscall.vtable";
	private static final  String traceXmlFilename = "trace.xml";
	
	// Less interesting and unused
	private static final  String addressableSpaceItableFilename = "address_space.itable";
	private static final  String contextMapItableFilename = "context_map.itable";
	private static final  String execPrevNextColumnFilename = "exec.prev_next.column";
	private static final  String moduleItableFilename = "module.itable";
	
	private static final HashMap<File, AtlantisBinaryFormat> fileToBinaryRegistry = new HashMap<>();

	public AtlantisBinaryFormat(File anyFile){
		File folder;
		if(anyFile.isDirectory()){ //Type() == IResource.FOLDER){
			folder = anyFile;
		} else {
			folder = anyFile.getParentFile();
		}
		initializeFileReferences(folder);
		this.binaryFolder = folder;
		this.providedFile = anyFile;
	}
	
	private File filter(File folder, final String targetFileName){
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.equals(targetFileName);
			}
		};
		File[] results = folder.listFiles(filter);
		return (null == results || results.length == 0) ? null : results[0];
	}
	
	private void initializeFileReferences(File folder){
		// take the directory, and cherry pick the files we know about.
		this.execVtable = filter(folder, execVtableFilename);
		this.execOffsets = filter(folder, execOffsetsFilename);
		this.inisItable = filter(folder, insItableFilename);
		this.threadItable = filter(folder, threadItableFilename);
		this.syscallOffsets = filter(folder, syscallOffsetsFilename);
		this.syscallVtable = filter(folder, syscallVtableFilename);
		this.traceXml = filter(folder, traceXmlFilename);
		
		this.addressableSpaceItable = filter(folder, addressableSpaceItableFilename);
		this.contextMapItable = filter(folder, contextMapItableFilename);
		this.execPrevNextColumn = filter(folder, execPrevNextColumnFilename);
		this.moduleItable = filter(folder, moduleItableFilename);
	}
	
	/**
	 * Checks if the file provided is one of the expected binary format files,
	 *  and more importantly, that all the other expected binary format files
	 *  are present too.
	 * @param currentFile
	 * @return
	 */
	public boolean isCompleteBinaryFileSystem(){
		boolean exists = false;
		boolean notNull = this.execVtable != null &&  
				this.execOffsets != null &&  
				this.inisItable != null &&  
				this.threadItable != null &&  
				this.syscallOffsets != null &&  
				this.syscallVtable != null &&  
				this.traceXml != null &&  
				// plus even the boring ones
				this.addressableSpaceItable != null &&   
				this.contextMapItable != null &&  
				this.execPrevNextColumn != null &&  
				this.moduleItable != null;
		if(notNull){
			exists = this.execVtable.exists() &&  
				this.execOffsets.exists() &&  
				this.inisItable.exists() &&  
				this.threadItable.exists() &&  
				this.syscallOffsets.exists() &&  
				this.syscallVtable.exists() &&  
				this.traceXml.exists() &&  
				// plus even the boring ones
				this.addressableSpaceItable.exists() &&   
				this.contextMapItable.exists() &&  
				this.execPrevNextColumn.exists() &&  
				this.moduleItable.exists();
		}
		return exists;
		
		
	}
	
	public long getBinaryFileSystemSize(){
		long size = 0;
		size += this.execVtable.length();
		size += this.execOffsets. length();
		size += this.inisItable.length();
		size += this.threadItable.length();
		size += this.syscallOffsets.length();
		size += this.syscallVtable.length();
		size += this.traceXml.length();

		size += this.addressableSpaceItable.length();
		size += this.contextMapItable.length();
		size += this.execPrevNextColumn.length();
		size += this.moduleItable.length();
		
		return size;
	}
	
	/**
	 * 
	 * @param directory	Either the binary folder or a file in the folder which will be resolved to the folder.
	 * @return
	 */
	public static boolean isCompleteBinaryFileSystem(File directory){
		AtlantisBinaryFormat binaries = new AtlantisBinaryFormat(directory);
		return binaries.isCompleteBinaryFileSystem();
	}
	
	public File getExecVtableFile() {
		return execVtable;
	}

	public File getExecOffsetsFile() {
		return execOffsets;
	}
	
	public File getInsItableFile() {
		return inisItable;
	}

	public File getAddressableSpaceItable() {
		return addressableSpaceItable;
	}

	public File getContextMapItable() {
		return contextMapItable;
	}

	public File getExecPrevNextColumn() {
		return execPrevNextColumn;
	}

	public File getModuleItableFile() {
		return moduleItable;
	}

	public File getThreadItableFile() {
		return threadItable;
	}

	public File getSyscallOffsetsFile() {
		return syscallOffsets;
	}

	public File getSyscallVtableFile() {
		return syscallVtable;
	}

	public File getTraceXmlFile() {
		return traceXml;
	}

	public static void registerEmptyFileBinaryFormatPair(File emptyFile, AtlantisBinaryFormat binFormat){
		// This could be in RegistryUtils instead...but I felt it was a good fit here, rather than exposing
		// it to BFV.
		fileToBinaryRegistry.put(emptyFile, binFormat);
	}
	
	public static AtlantisBinaryFormat getRegisteredBinaryDataFormat(File emptyFile) {
		return fileToBinaryRegistry.get(emptyFile);
	}

}
