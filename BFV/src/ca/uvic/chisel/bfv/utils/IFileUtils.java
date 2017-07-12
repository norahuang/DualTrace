package ca.uvic.chisel.bfv.utils;

import java.io.File;

import org.eclipse.core.resources.IFile;

/**
 * To retrieve, use:
 * IFileUtils fileUtil = RegistryUtils.getFileUtils();
 *
 */
public interface IFileUtils {
	
	public static final String EXTENSION_ID = "ca.uvic.chisel.bfv.utils.IFileUtils";
	
	public static final String CLASS_ATTRBIUTE = "class";
	
	public static final String PRIORITY_ATTRIBUTE = "priority";
	
	public static final int PRIORITY_DEFAULT = 0;
	
	
	public static final String COMMENTS_FILE_SUFFIX = "_comments.xml";
	public static final String REGIONS_FILE_SUFFIX = "_regions.xml";
	public static final String TAGS_FILE_SUFFIX = "_tags.xml";
	public static final String MESSAGETYPES_FILE_NAME = "messagetypes.xml";
	
	public static final BfvFileUtils inst = new BfvFileUtils();
	
	/**
	 * The empty files fed to the Eclipse editor have to go into a secret directory, and
	 * have the same file name as their owner, in order to present a good file name in
	 * the editor. The path will be wrong if a user hovers over the editor tab, but
	 * that is far preferable to having an odd name visible, or to clobbering the file
	 * that actually contains file lines.
	 * 
	 */
	public static final String TMP_DIRECTORY_NAME = ".tmp";
	
	public File convertFileToBlankFile(File file);
	
	public File convertBlankFileToActualFile(File incomingFile);
	
	/**
	 * Convenience method to locate meta data xml files, etc.
	 * @param targetSuffix
	 * @return
	 */
	public String convertFilePathToSuffix(String filePath, String targetSuffix);
	
	public String getCommentFileName(String filePath);
	
	public String getRegionFileName(String filePath);

	public String getTagFileName(String filePath);
	
	public String getMessageTypeFileName(String filePath);
	
	public IFile getCommentFile(String filePath);
	
	public IFile getRegionFile(String filePath);

	public IFile getTagFile(String filePath);
	
	public IFile getMessageTypeFile(String filePath);
	
	public IFile getFileFromFileName(String filePath);
	

	
	/**
	 * Given an IFile, ensure all parent directories exist.  If not, they
	 * will be created.
	 * @param file
	 */
	public void createParentFolders(IFile file);

	public void createEmptyFile(IFile file);
}
