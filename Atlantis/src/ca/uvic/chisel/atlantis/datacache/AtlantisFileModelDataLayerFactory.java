package ca.uvic.chisel.atlantis.datacache;

import java.io.File;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayerFactory;

// TODO introduce a class hierarchy between this and the FileModelDataLayerFactory
public class AtlantisFileModelDataLayerFactory implements IFileModelDataLayerFactory {

		private static AtlantisFileModelDataLayer currentInstance;
	
		public AtlantisFileModelDataLayerFactory() {}
		
		@Override
		public void removeIndexForFile(File blankFile, File file){
			try {
				getFileModelDataLayerInstance(blankFile).removeIndexForFile(file);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		/**
		 * This will lazily instantiate an instance of the FileModelCache object.  Should the object not yet be initialized, 
		 * OR if we are changing the document that we are looking at, then it will initialize the object.
		 * 
		 * TODO, we could choose to keep the information about old files in the cache, and simply turn this into a Multiton.
		 * Also, we may need to watch input files to ensure that they do not get out of sync.
		 * 
		 * @param blankFile the document that we are viewing
		 * @return the singleton instance of the class.
		 * @throws Exception 
		 */
		@Override
		public IFileModelDataLayer getFileModelDataLayerInstance(File blankFile) throws Exception {
			// XXX check to see if this check passes when it is the same file
			if(null == currentInstance || null == currentInstance.getBlankFile() || !currentInstance.getBlankFile().equals(blankFile)) {
				if(currentInstance != null) {
					currentInstance.clearListeners();
					currentInstance.dispose();
				}
				
				File f = AtlantisFileUtils.convertBlankFileToBinaryFormatDirectory(blankFile);
				AtlantisBinaryFormat binaryFileSet = new AtlantisBinaryFormat(f);
				if(binaryFileSet.isCompleteBinaryFileSystem()){
					currentInstance = new BinaryFormatFileModelDataLayer(blankFile);
				} else {
					currentInstance = new TextFormatAtlantisFileModelDataLayer(blankFile);
				}
			}
			
			return currentInstance;
		}
	
		@Override
		public boolean checkFileModelForIndex(File file, File folder) {
			return AtlantisFileModelDataLayer.isFileIndexed(file, folder);
		}

		@Override
		public void clearFileModelDataLayerInstance(IFileModelDataLayer fileModel) {
			if(currentInstance == fileModel){
				currentInstance.clearListeners();
				currentInstance = null;
			}
		}
		
}
