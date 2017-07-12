package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.List;

/**
 * View for displaying a visualization of thread events that occur throughout a trace.
 * @author Patrick Gorman
 */
@Deprecated
public class ThreadEventTraceVisualizationView extends AbstractTraceVisualizationView {
	
	public static final String ID = "ca.uvic.chisel.atlantis.views.ThreadEventTraceVisualizationView";
	private ThreadEventPixelPager pager;

	@Override
	protected boolean traceDisplayerContentApplicable() {
		boolean state = activeTraceDisplayer.hasExecutionTrace();
		return state;
	}
	
	@Override
	protected List<String> getDistinctRowStrings() {
		return this.fileModelCache.getDistinctThreadNames();
	}
	
	@Override
	protected  int getRightmostElementEndPoint(){
		return this.fileModelCache.getFinalThreadEventEndPoint();
	}

	@Override
	protected TraceEventPager getEventPager() {
		if(null == pager){
			pager = new ThreadEventPixelPager(this.fileModelCache);
		}
		return pager;
	}

	@Override
	protected void resetEventPager() {
		pager = null;
	}

}
