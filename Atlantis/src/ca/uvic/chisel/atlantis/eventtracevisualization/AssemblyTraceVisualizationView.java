package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.List;

public class AssemblyTraceVisualizationView extends AbstractTraceVisualizationView {

	public static final String ID = "ca.uvic.chisel.atlantis.views.AssemblyTraceVisualizationView";
	private AssemblyEventPixelPager pager;

	// TODO this may actually want to check the contents of the file, to ensure that it is valid.
	@Override
	protected boolean traceDisplayerContentApplicable() {
		return true;
	}

	@Override
	protected List<String> getDistinctRowStrings() {
		return this.fileModelCache.getDistinctAssemblyEventNames();
	}
	
	@Override
	protected  int getRightmostElementEndPoint(){
		return this.fileModelCache.getFinalAssemblyEventEndPoint();
	}
	
	@Override
	protected TraceEventPager getEventPager() {
		if(null == pager){
			pager = new AssemblyEventPixelPager(this.fileModelCache);
		}
		return pager;
	}
	
	@Override
	protected void resetEventPager() {
		pager = null;
	}

}
