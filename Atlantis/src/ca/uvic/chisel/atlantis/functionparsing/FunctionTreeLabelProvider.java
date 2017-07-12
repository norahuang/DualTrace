package ca.uvic.chisel.atlantis.functionparsing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;


public class FunctionTreeLabelProvider implements ILabelProvider {
	
	private Image normalFunction;
	private Image module;
	
	public FunctionTreeLabelProvider() {
		normalFunction = module = null;
	}
	
	private Image loadIcon(String name) {
		try {
			
			URL imageURL = getClass().getResource("/icons/" + name);
			return ImageDescriptor.createFromURL(imageURL).createImage();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void loadImagesIfMissing() {
		if(normalFunction == null) {
			normalFunction = loadIcon("function_normal.gif");
			module = loadIcon("function_module.gif");
		}
	}
	
	@Override
	public void removeListener(ILabelProviderListener arg0) { }
	
	@Override
	public boolean isLabelProperty(Object arg0, String arg1) { return false; }
	
	@Override
	public void dispose() { }
	
	@Override
	public void addListener(ILabelProviderListener arg0) { }
	
	@Override
	public String getText(Object arg0) {
		return arg0.toString();
	}
	
	@Override
	public Image getImage(Object funcObject) {
		loadImagesIfMissing();
		
		if(!new FunctionTreeContentProvider(null).asFOM(funcObject).isFunction()) {
			return module;
		}
		
		return normalFunction;
	}
}
