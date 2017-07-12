package ca.uvic.chisel.atlantis;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class AtlantisApplicationActionBarAdvisor extends ActionBarAdvisor {

    public AtlantisApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

    @Override
	protected void makeActions(IWorkbenchWindow window) {
    }

    @Override
	protected void fillMenuBar(IMenuManager menuBar) {
    }
}
