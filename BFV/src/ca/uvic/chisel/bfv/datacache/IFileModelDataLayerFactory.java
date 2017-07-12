package ca.uvic.chisel.bfv.datacache;

import java.io.File;

public interface IFileModelDataLayerFactory {

	public static final String EXTENSION_ID = "ca.uvic.chisel.bfv.IFileModelDataLayerFactory";
	
	public static final String CLASS_ATTRBIUTE = "class";
	
	public static final String PRIORITY_ATTRIBUTE = "priority";
	
	public static final int PRIORITY_DEFAULT = 0;
	
	public IFileModelDataLayer getFileModelDataLayerInstance(File blankFile) throws Exception;

	/**
	 * File was needed for MySQL backend, folder needed for SQLite backend.
	 * 
	 * @param file
	 * @param folder
	 * @return
	 */
	public boolean checkFileModelForIndex(File file, File folder);
	
	public void clearFileModelDataLayerInstance(IFileModelDataLayer fileModel);

	public void removeIndexForFile(File blankFile, File file);

}