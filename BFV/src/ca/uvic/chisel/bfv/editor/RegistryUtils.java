package ca.uvic.chisel.bfv.editor;

import java.io.File;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayerFactory;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;
import ca.uvic.chisel.bfv.utils.IFileUtils;

public class RegistryUtils {
	
	/**
	 * This method will look up in the Extension Registry in order to see what class it should use for the SyntaxHighlightingManager
	 * This method is not intended to be overwritten, rather if you would like to change how the syntax highlighting is done,
	 * implement the extension point differently.
	 * 
	 * If no element is found in the registry, this method will return null.
	 */
	public static final ISyntaxHighlightingManager getSyntaxHighlightingManagerFromRegistry(ProjectionViewer viewer, IFileModelDataLayer fileModel) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(ISyntaxHighlightingManagerFactory.EXTENSION_ID);
		
		IExtension[] extensions = extensionPoint.getExtensions();
		
		try {
			if(extensions.length == 0) {
				throw new Exception("No extension could be found for the ISyntaxHighlightingManager");
			}
			
			IConfigurationElement configElement = getConfigurationElementFromPriorityField(extensions, ISyntaxHighlightingManagerFactory.PRIORITY_ATTRBIUTE, ISyntaxHighlightingManagerFactory.PRIORITY_DEFAULT);
		
			if(configElement != null) {
				ISyntaxHighlightingManagerFactory factory = (ISyntaxHighlightingManagerFactory) configElement.createExecutableExtension(ISyntaxHighlightingManagerFactory.CLASS_ATTRBIUTE);
				return factory.createHighlightingManager(viewer, fileModel);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static final IFileModelDataLayerFactory getFileModelDataLayerFactory(){
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(IFileModelDataLayerFactory.EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		
		try {
			
			if(extensions.length == 0) {
				throw new Exception("No extension could be found for the IFileModelDataLayerFactory");
			}
			
			IConfigurationElement configElement = getConfigurationElementFromPriorityField(extensions, IFileModelDataLayerFactory.PRIORITY_ATTRIBUTE, IFileModelDataLayerFactory.PRIORITY_DEFAULT);
			
			IFileModelDataLayerFactory factory = (IFileModelDataLayerFactory) configElement.createExecutableExtension(IFileModelDataLayerFactory.CLASS_ATTRBIUTE);
			return factory;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static final IFileUtils getFileUtils(){
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(IFileUtils.EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		
		try {
			
			if(extensions.length == 0) {
				throw new Exception("No extension could be found for the IFileUtils");
			}
			
			IConfigurationElement configElement = getConfigurationElementFromPriorityField(extensions, IFileUtils.PRIORITY_ATTRIBUTE, IFileUtils.PRIORITY_DEFAULT);
			
			IFileUtils factory = (IFileUtils) configElement.createExecutableExtension(IFileUtils.CLASS_ATTRBIUTE);
			return factory;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * This method will look up in the Extension Registry in order to see what class it should use for the SyntaxHighlightingManager
	 * This method is not intended to be overwritten, rather if you would like to change how the syntax highlighting is done,
	 * implement the extension point differently.
	 * 
	 * If no element is found in the registry, this method will return null.
	 */
	public static final IFileModelDataLayer getFileModelDataLayerFromRegistry() {
		BigFileEditor editor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		File blankFile = editor.getCurrentBlankFile();
		return getFileModelDataLayerFromRegistry(blankFile);
	}
	
	/**
	 * See no-arg version as preferred, except when we haven't yet loaded to an editor.
	 * 
	 * @param blankFile
	 * @return
	 */
	public static final IFileModelDataLayer getFileModelDataLayerFromRegistry(File blankFile) {
		IFileModelDataLayerFactory factory = getFileModelDataLayerFactory();
		if(null != factory){
			try{
				return factory.getFileModelDataLayerInstance(blankFile);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static final void clearFileModelDataLayerFromRegistry(IFileModelDataLayer fileModel) {
		IFileModelDataLayerFactory factory = getFileModelDataLayerFactory();
		if(null != factory){
			try{
				factory.clearFileModelDataLayerInstance(fileModel);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static final IConfigurationElement getConfigurationElementFromPriorityField(IExtension[] extensions, String fieldName, int defaultPriorityValue) {
		IConfigurationElement currentConfig = null;
		int curMaxPriority = -1;
		
		if(extensions.length == 0) {
			return null;
		}
		
		for(IExtension extension : extensions) {
			IConfigurationElement[] configElements = extension.getConfigurationElements();
			String priorityString = configElements[0].getAttribute(ISyntaxHighlightingManagerFactory.PRIORITY_ATTRBIUTE);
			
			int priority;
			try {
				priority = Integer.parseInt(priorityString);
			} catch(NumberFormatException e) {
				e.printStackTrace();
				priority = defaultPriorityValue;
			}
			
			if(priority > curMaxPriority) {
				currentConfig = configElements[0];
				curMaxPriority = priority;
			}
		}
		
		return currentConfig;
	}
	
}
