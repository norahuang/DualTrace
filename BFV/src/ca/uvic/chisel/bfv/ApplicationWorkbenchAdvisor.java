package ca.uvic.chisel.bfv;

import java.net.URL;

import ca.uvic.chisel.bfv.ApplicationWorkbenchWindowAdvisor;
import ca.uvic.chisel.bfv.editor.BigFileEditor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.*;
import org.eclipse.ui.application.*;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "ca.uvic.chisel.bfv.perspective"; //$NON-NLS-1$

    @Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	@Override
	public IAdaptable getDefaultPageInput() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		IDE.registerAdapters();		
		
		// here is the work around code
	    /*
	     * This is a hack to get Project tree icons to show up in the Project Explorer.
	     * It is descriped in the Eclipse Help Documents here.
	     * 
	     * http://help.eclipse.org/ganymede/topic/org.eclipse.platform.doc.isv/guide/cnf_rcp.htm
	     * 
	     */
	    final String ICONS_PATH = "icons/full/";

	    Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);

	    declareWorkbenchImage(
	            configurer, 
	            ideBundle,
	            IDE.SharedImages.IMG_OBJ_PROJECT, 
	            ICONS_PATH + "obj16/prj_obj.gif",
	            true);

	    declareWorkbenchImage(
	            configurer, 
	            ideBundle,
	            IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, 
	            ICONS_PATH + "obj16/cprj_obj.gif", 
	            true);


	    /*
	     * End of hack in this method... 
	     */
	}
	
	/* 
	 * This is a hack to get Project tree icons to show up in the Project Explorer.
     * It is descriped in the Eclipse Help Documents here.
     */
	private void declareWorkbenchImage(IWorkbenchConfigurer configurer_p, Bundle ideBundle, String symbolicName, String path, boolean shared)  
	{
	    URL url = ideBundle.getEntry(path);
	    ImageDescriptor desc = ImageDescriptor.createFromURL(url);
	    configurer_p.declareImage(symbolicName, desc, shared);
	}
	
	@Override
	public void preStartup() {
		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		if (handlerService != null) {
			try {
				handlerService.executeCommand("org.eclipse.ui.file.refresh", null);
			} catch (Exception e) {
			}
		}
	}
	
	@Override
	public void postShutdown() {
		// Stops the application from complaining about the workspace exiting with unsaved changes
		try {
			ResourcesPlugin.getWorkspace().save(true, null);
		} catch (CoreException e) {
			StatusManager.getManager().handle(e, BigFileActivator.PLUGIN_ID);
		}
	}
}
