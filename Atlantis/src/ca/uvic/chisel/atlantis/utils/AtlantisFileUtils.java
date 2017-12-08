package ca.uvic.chisel.atlantis.utils;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;


public class AtlantisFileUtils extends BfvFileUtils implements IFileUtils {

	// This system is not implemented in the most easily traced way, partly because of how
	// we have to meet Eclipse plugin architecture requirements, and also because we are
	// essentially trying to open a folder as a file, besides the other indirection usual
	// to Atlantis.
	
	// Whatever this extension is, it must be registered in the plugin.xml as a file type for
	// the editor extensions, for which the class "ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor"
	// is registered. The extension ".trace" worked, and I didn't think of a nicer one to add.
	public static final String BINARY_FORMAT_TMP_FILE_EXTENSION = ".trace";
	
	@Override
	public File convertFileToBlankFile(File file){
		AtlantisBinaryFormat binFormat = new AtlantisBinaryFormat(file);
		if(binFormat.isCompleteBinaryFileSystem()){
			IFile f = AtlantisFileUtils.convertFileIFile(file);
			IFile emptyFile = f.getProject().getFile(new Path(BfvFileUtils.TMP_DIRECTORY_NAME+"/"+f.getParent().getProjectRelativePath().toString()+BINARY_FORMAT_TMP_FILE_EXTENSION));
			return AtlantisFileUtils.convertFileIFile(emptyFile);
		} else {
			return super.convertFileToBlankFile(file);
		}
	}
	
	public static File getChannelTypeSettingFile(File file){
		IFile f = AtlantisFileUtils.convertFileIFile(file);
		IFile emptyFile = f.getProject().getFile(new Path(BfvFileUtils.TMP_DIRECTORY_NAME+"/"+"channelTypes.json"));
		File jsonFile = AtlantisFileUtils.convertFileIFile(emptyFile);
		if(!jsonFile.exists() && !jsonFile.isDirectory())
		{
		    try {
		    	jsonFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return jsonFile;
	}
	
	public static void createChannelTypeSettingFile(File jsonFile){
		if(!jsonFile.exists() && !jsonFile.isDirectory())
		{
		    try {
		    	jsonFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public File convertBlankFileToActualFile(File blankFile) {
		File noDotTrace = new File(blankFile.getAbsolutePath().replace(AtlantisFileUtils.BINARY_FORMAT_TMP_FILE_EXTENSION, ""));
		File directory = super.convertBlankFileToActualFile(noDotTrace);
		AtlantisBinaryFormat binaries = new AtlantisBinaryFormat(directory);
		if(binaries.isCompleteBinaryFileSystem()){
			// There is no single file for the binary system to pass back/
			// We may change our mind later and return something...
			return binaries.binaryFolder;
		} else {
			return super.convertBlankFileToActualFile(blankFile);
		}
	}
	
	static public File convertBlankFileToBinaryFormatDirectory(File blankFile){
		// Cannot override static methods in Java, nor call instance methods from static scope, so we need to get the instance...
		IFileUtils fileUtils = RegistryUtils.getFileUtils();
		File candidateActualFile = fileUtils.convertBlankFileToActualFile(blankFile);
		File folder;
		if(null != candidateActualFile){
			String newPath = candidateActualFile.getPath().toString().replace(BINARY_FORMAT_TMP_FILE_EXTENSION, "");
			File sansTraceExtension = new File(newPath);
			folder = sansTraceExtension;
		} else {
			folder = blankFile;
		}
		
		if(!folder.isDirectory()){
			folder = folder.getParentFile();
		}
		return folder;
	}

	public static IFile convertFileIFile(File f) {
		return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(Path.fromOSString(f.getAbsolutePath()));
	}
	
	public static File convertFileIFile(IFile f) {
		return f.getRawLocation().makeAbsolute().toFile();
	}
	
	public static File convertIFolderToFile(IFolder f) {
		if(null == f){
			return null;
		}
		return f.getRawLocation().makeAbsolute().toFile();
	}
	
}
