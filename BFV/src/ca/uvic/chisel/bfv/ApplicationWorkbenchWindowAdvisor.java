package ca.uvic.chisel.bfv;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import ca.uvic.chisel.bfv.ApplicationActionBarAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
        configurer.setTitle("Big File Viewer Environment");
        // configurer.getWindow().getShell().setSize(new Point(1600, 920));  
    }

    @Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {    	
    	return new ApplicationActionBarAdvisor(configurer);
    }
    
    @Override
    public void postWindowClose() {
    	super.postWindowClose();
    	// Want to detect first run, so we can maximize the window then and only then.
		if(InstanceScope.INSTANCE.getNode(BigFileActivator.PLUGIN_ID).getBoolean("firstRun", true)){
			InstanceScope.INSTANCE.getNode(BigFileActivator.PLUGIN_ID).putBoolean("firstRun", false);
		}
    };
    
    @Override
	public void preWindowOpen() {
    }
    
    @Override
    public void postWindowOpen() {
	    // remove unwanted UI contributions that eclipse makes by default
	    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
	    for (int i = 0; i < windows.length; ++i) {
	        IWorkbenchPage page = windows[i].getActivePage();
	        if (page != null) {
	            // hide generic 'File' commands
//	            page.hideActionSet("org.eclipse.ui.actionSet.openFiles");
	            // hide 'Convert Line Delimiters To...'
//	            page.hideActionSet("org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo");
	            // hide 'Search' commands
	            page.hideActionSet("org.eclipse.search.searchActionSet");
	            // hide 'Annotation' commands
	            page.hideActionSet("org.eclipse.ui.edit.text.actionSet.annotationNavigation");
	            // hide 'Forward/Back' type navigation commands
	            page.hideActionSet("org.eclipse.ui.edit.text.actionSet.navigation");
	        }
	    }
	    
	    if(InstanceScope.INSTANCE.getNode(BigFileActivator.PLUGIN_ID).getBoolean("firstRun", true)){
	    	getWindowConfigurer().getWindow().getShell().setMaximized(true);
	    }
    }
}
