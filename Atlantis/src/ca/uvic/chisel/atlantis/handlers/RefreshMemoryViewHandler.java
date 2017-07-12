package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;

public class RefreshMemoryViewHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AtlantisTraceEditor activeTraceDisplayer;
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof AtlantisTraceEditor) {
			activeTraceDisplayer = (AtlantisTraceEditor) editor;
			
			// This is the difference between this one and the GenerateMemoryViewHandler
			// activeTraceDisplayer.updateMemoryView = !activeTraceDisplayer.updateMemoryView;
			
			// Refresh view immediately.
			activeTraceDisplayer.syncMemoryVisualization(true);
		}

		return null;
	}
}
