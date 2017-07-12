package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.atlantis.functionparsing.LoadLibraryExportsDialog;
import ca.uvic.chisel.atlantis.views.RegistersView;

public class LoadLibraryExportsHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		LoadLibraryExportsDialog lled = new LoadLibraryExportsDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		
		if(lled.open() == Window.OK) {
			
		}
		
		return null;
	}
}
