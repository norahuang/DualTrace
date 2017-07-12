package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.atlantis.views.RegistersView;

public class ShowAdditionalRegistersHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// http://eclipsesource.com/blogs/2009/01/15/toggling-a-command-contribution/
			Command command = event.getCommand();
			HandlerUtil.toggleCommandState(command);
			
			RegistersView registersView = (RegistersView)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(RegistersView.ID);
			registersView.refresh();
		} catch (PartInitException e) {
			System.err.println("Could not acquire registers view.");
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	static public boolean isToggledToShow(){
		// Simplest behavior, allows us to toggle the button for *all* traces. Will need a lot of work to get per-trace
		// memory view toggling, and that's probably not helpful anyway.
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
	    Command command = commandService.getCommand("ca.uvic.chisel.atlantis.commands.ShowAdditionalRegisters");
	    return (boolean) command.getState("org.eclipse.ui.commands.toggleState").getValue(); //.setValue(false);
	}
}
