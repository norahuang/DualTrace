package ca.uvic.chisel.atlantis;

import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.ApplicationWorkbenchAdvisor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.statushandlers.StatusManager;

public class AtlantisApplicationWorkbenchAdvisor extends ApplicationWorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "ca.uvic.chisel.atlantis.perspective"; //$NON-NLS-1$

   @Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new AtlantisApplicationWorkbenchWindowAdvisor(configurer);
    }
	
	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	@Override
	public void preStartup() {
		IEditorRegistry editors = PlatformUI.getWorkbench().getEditorRegistry();
		
		editors.setDefaultEditor("*.trace", AtlantisTraceEditor.ID);		
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
			StatusManager.getManager().handle(e, AtlantisActivator.PLUGIN_ID);
		}
	}
}
