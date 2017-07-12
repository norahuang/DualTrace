package ca.uvic.chisel.atlantis.views;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayerFactory;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

public class AtlantisIndexedFileDecorator implements ILabelDecorator {

	public static final String ID = "ca.uvic.chisel.atlantis.views.AtlantisIndexedFileDecorator";
	
	private final String INDEXED_DECORATOR_SUFFIX = "processed";
	
	@Override
	public void addListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void removeListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	

	@Override
	public Image decorateImage(Image arg0, Object arg1) {
		// No image decoration
		return null;
	}

	@Override
	public String decorateText(String label, Object obj) {
		// TODO Check if the resource has a BFV index file. If so, decorate it. Folders only, so inspect for an exec.vtable file, essentially...
		// Need to get at the FileModelLayer, and the FileLineFileBackend, to ask about index files.
		IResource objectResource = (IResource) obj;
		
		if (objectResource.getType() != IResource.FOLDER) {
			return null;
		}
		
		IFolder folder = (IFolder)objectResource;
		
		// Get the size of the binaries. Some indication is useful.
		IFileStore store;
		String traceSizeLabel = "";
		try {
			store = EFS.getStore(folder.getLocationURI());
			long traceSize = 0;
			IFileInfo[] childInfos = store.childInfos(EFS.NONE, null);
			for(int i = 0; i < childInfos.length; i++){
				if(childInfos[i].getName().contains("sqlite")){
					continue;
				}
				traceSize += childInfos[i].getLength();
			}
			double gigs = (0.0+traceSize) / Math.pow(1024, 3);
			traceSizeLabel = "("+String.format("%.2g", gigs)+" GiB)";
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IFile childFile = folder.getFile("exec.vtable");
		
		File baseFile = BfvFileUtils.convertFileIFile(childFile); //this.binaryFileSet.baseFile;
		File blankFile = RegistryUtils.getFileUtils().convertFileToBlankFile(baseFile);
		File fakeActualFile = RegistryUtils.getFileUtils().convertBlankFileToActualFile(blankFile);
		
		IFileModelDataLayerFactory fileModelFactory = RegistryUtils.getFileModelDataLayerFactory();
		if(fileModelFactory.checkFileModelForIndex(fakeActualFile, BfvFileUtils.convertIFolderToFile(folder))){
			return label+" ["+INDEXED_DECORATOR_SUFFIX+"]"+" "+traceSizeLabel;
		} else {
			return label+" "+traceSizeLabel;
		}
	}

}
