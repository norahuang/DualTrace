package ca.uvic.chisel.atlantis.handlers;

import ca.uvic.chisel.atlantis.compare.TraceCompareViewer;

import org.eclipse.core.commands.*;

/**
 * Handler for showing the full text of both traces being compared in the active Trace Compare Viewer.
 * Invoked when the Show All command is executed.
 * @author Laura Chan
 */
public class ShowAllHandler extends AbstractHandler {
	public static final String PARAM_TRACE_COMPARE_VIEWER_ID = 
			"ca.uvic.chisel.atlantis.commands.parameters.showAllTraceCompareViewerID";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String compareViewerId = event.getParameter(PARAM_TRACE_COMPARE_VIEWER_ID);
		TraceCompareViewer traceCompareViewer = TraceCompareViewer.getInstance(compareViewerId);
		traceCompareViewer.showAll();
		return null;
	}
}
