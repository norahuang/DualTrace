package ca.uvic.chisel.bfv.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayerFactory;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

public class BigFileIndexedFileDecorator implements ILabelDecorator {

	public static final String ID = "ca.uvic.chisel.bfv.views.BigFileIndexedFileDecorator";
	
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
		// TODO Check if the resource has a BFV index file. If so, decorate it.
		// Need to get at the FIleModelLayer, and the FileLineFileBackend, to ask about index files.
		IResource objectResource = (IResource) obj;
		
		if (objectResource.getType() != IResource.FILE) {
			return null;
		}
		// IFile ifile= workspace.getRoot().getFileForLocation(location);
		IFile file = (IFile)objectResource;
		
		IFileModelDataLayerFactory fileModelFactory = RegistryUtils.getFileModelDataLayerFactory();
		if(fileModelFactory.checkFileModelForIndex(BfvFileUtils.convertFileIFile(file), null)){
			return label+" ["+INDEXED_DECORATOR_SUFFIX+"]";
		} else {
			return null;
		}
	}

}
