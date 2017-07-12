package ca.uvic.chisel.atlantis.handlers;

import ca.uvic.chisel.atlantis.compare.TraceCompareViewer;

import org.eclipse.core.commands.*;

/**
 * Handler for showing only the differing portions of the traces being compared in the active Trace Compare Viewer.
 * Invoked when the Show Differences command is executed.
 * @author Laura Chan
 */
public class ShowDifferencesHandler extends AbstractHandler {
	public static final String PARAM_TRACE_COMPARE_VIEWER_ID = 
			"ca.uvic.chisel.atlantis.commands.parameters.showDifferencesTraceCompareViewerID";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String compareViewerId = event.getParameter(PARAM_TRACE_COMPARE_VIEWER_ID);
		TraceCompareViewer traceCompareViewer = TraceCompareViewer.getInstance(compareViewerId);
		try {
			traceCompareViewer.showFiltered(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
