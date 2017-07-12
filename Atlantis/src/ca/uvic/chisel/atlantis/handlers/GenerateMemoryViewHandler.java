package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;

public class GenerateMemoryViewHandler extends AbstractHandler {
	
	static {
		// Default state to true on every Atlantis run
	    getCommand().getState("org.eclipse.ui.commands.toggleState").setValue(true);
	}
	
	private static Command getCommand(){
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
	    return commandService.getCommand("ca.uvic.chisel.atlantis.commands.GenerateMemoryView");
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AtlantisTraceEditor activeTraceDisplayer;
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof AtlantisTraceEditor) {
			activeTraceDisplayer = (AtlantisTraceEditor) editor;
			
			// http://eclipsesource.com/blogs/2009/01/15/toggling-a-command-contribution/
			Command command = event.getCommand();
			boolean oldState = HandlerUtil.toggleCommandState(command);
			
			// TODO Decide on whether to clear memory view when toggled off.
			if(oldState){
				// Do we really want to clear the memory view? Technically, the user may want
				// to keep the memory view filled, and only refreshed via the context menu on lines...
				activeTraceDisplayer.clearMemoryViewContents();
			} else {
			// Refresh view immediately.
			activeTraceDisplayer.syncMemoryVisualization(true);
			}
		}

		return null;
	}
	
	static public boolean getCurrentState(){
		// Simplest behavior, allows us to toggle the button for *all* traces. Will need a lot of work to get per-trace
		// memory view toggling, and that's probably not helpful anyway.
	    return (boolean) getCommand().getState("org.eclipse.ui.commands.toggleState").getValue();
	}
}
