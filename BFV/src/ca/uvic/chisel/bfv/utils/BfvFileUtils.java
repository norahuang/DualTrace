package ca.uvic.chisel.bfv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class BfvFileUtils implements IFileUtils {
	
	/**
	 * The empty files fed to the Eclipse editor have to go into a secret directory, and
	 * have the same file name as their owner, in order to present a good file name in
	 * the editor. The path will be wrong if a user hovers over the editor tab, but
	 * that is far preferable to having an odd name visible, or to clobbering the file
	 * that actually contains file lines.
	 * 
	 */
	public static final String TMP_DIRECTORY_NAME = ".tmp";
	
	@Override
	public File convertFileToBlankFile(File file){
		// The blank file directory will be placed at project root, so prepend this onto the
		// part of the path that begins with the project path. I do it this way because I cannot
		// anticipate relative or absolute paths as arguments.
		if(!file.toString().contains(TMP_DIRECTORY_NAME)){
			IFile f = convertFileIFile(file);
			IFile bF = f.getProject().getFile(new Path(TMP_DIRECTORY_NAME+"/"+f.getProjectRelativePath().toString()));
			return BfvFileUtils.convertFileIFile(bF);
		} else {
			return file;
		}
	}
	
	@Override
	public File convertBlankFileToActualFile(File incomingFile) {
		if(incomingFile.toString().contains(TMP_DIRECTORY_NAME)){
			String newPath = incomingFile.getAbsolutePath().toString().replace(TMP_DIRECTORY_NAME + "\\", "");
			return new File(newPath);
		} else {
			return incomingFile;
		}
	}
	
	/**
	 * Convenience method to locate meta data xml files, etc.
	 * @param targetSuffix
	 * @return
	 */
	@Override
	public String convertFilePathToSuffix(String filePath, String targetSuffix) {
		if(-1 == filePath.lastIndexOf('.')){
			// For directory of formats that are not in a single file.
			return filePath+"\\"+targetSuffix;
		}
		return filePath.substring(0, filePath.lastIndexOf('.')) + targetSuffix;
	}
	

	
	@Override
	public String getCommentFileName(String filePath) {
		return convertFilePathToSuffix(filePath,  COMMENTS_FILE_SUFFIX);
	}
	
	@Override
	public String getRegionFileName(String filePath) {
		return convertFilePathToSuffix(filePath, REGIONS_FILE_SUFFIX);
	}

	@Override
	public String getTagFileName(String filePath) {
		return convertFilePathToSuffix(filePath, TAGS_FILE_SUFFIX);
	}
	
	@Override
	public String getMessageTypeFileName(String filePath) {
		return filePath + "/" + MESSAGETYPES_FILE_NAME;
	}
	
	
	
	@Override
	public IFile getCommentFile(String filePath) {
		return getFileFromFileName(getCommentFileName(filePath));
	}
	
	@Override
	public IFile getRegionFile(String filePath) {
		return getFileFromFileName(getRegionFileName(filePath));
	}

	@Override
	public IFile getTagFile(String filePath) {
		return getFileFromFileName(getTagFileName(filePath));
	}
	
	@Override
	public IFile getMessageTypeFile(String filePath) {
		return getFileFromFileName(getMessageTypeFileName(filePath));
	}
	
	@Override
	public IFile getFileFromFileName(String filePath) {
		IPath doc = Path.fromOSString(filePath);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(doc);
	}
	
	/**
	 * Given an IFile, ensure all parent directories exist.  If not, they
	 * will be created.
	 * @param file
	 */
	@Override
	public void createParentFolders(IFile file) {
		IContainer parent = file.getParent();
		if (!parent.exists()) {
			String absolutePath = parent.getRawLocation().toOSString();
			File javaFile = new File(absolutePath);
			javaFile.mkdirs();

			//Refresh the project so Eclipse knows about the newly-created resources
			try {
				file.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void createEmptyFile(IFile file) {
		byte[] emptyBytes = "".getBytes();
		InputStream source = new ByteArrayInputStream(emptyBytes);
		try {
			createParentFolders(file);
			if(!file.exists()){
				file.create(source, false, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}finally{
			try {
				source.close();
			} catch (IOException e) {
				// Don't care
			}
		}
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
