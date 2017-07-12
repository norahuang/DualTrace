package ca.uvic.chisel.bfv.datacache;

import java.io.File;


public class FileModelDataLayerFactory implements IFileModelDataLayerFactory {

	private static FileModelDataLayer currentInstance;
	
	
	public FileModelDataLayerFactory() {}
	
	/**
	 * This will lazily instantiate an instance of the FileModelCache object.  Should the object not yet be initialized, 
	 * OR if we are changing the document that we are looking at, then it will initialize the object.
	 * 
	 * TODO, we could choose to keep the information about old files in the cache, and simply turn this into a Multiton.
	 * Also, we may need to watch input files to ensure that they do not get out of sync.
	 * 
	 * @param blankFile the file that we are viewing
	 * @return the singleton instance of the class.
	 * @throws Exception 
	 */
	public IFileModelDataLayer getFileModelDataLayerInstance(File blankFile) throws Exception {
		// XXX check to see if this check passes when it is the same file
		if(null == currentInstance || null == currentInstance.blankFile || !currentInstance.blankFile.equals(blankFile)) {
			if(currentInstance != null) {
				currentInstance.clearListeners();
				currentInstance.dispose();
			}
			
			currentInstance = new FileModelDataLayer(blankFile);
		}
		
		return currentInstance;
	}

	@Override
	public boolean checkFileModelForIndex(File file, File folder) {
		return FileModelDataLayer.isFileIndexed(file);
	}
	
	@Override
	public void removeIndexForFile(File blankFile, File file) {
		if(!this.checkFileModelForIndex(file, null)){
			return;
		}
		currentInstance.removeIndexForFile(file);
	}
		
	@Override
	public void clearFileModelDataLayerInstance(IFileModelDataLayer fileModel) {
		if(currentInstance == fileModel){
			currentInstance.clearListeners();
			currentInstance = null;
		}
	}
	
}
