package ca.uvic.chisel.bfv.datacache;

import ca.uvic.chisel.bfv.annotations.RegionModel;


public interface RegionChangeListener {

	public enum RegionEventType {
		REGION_ADDED, REGION_REMOVED, REGION_RENAMED, REGION_COLLAPSED, REGION_EXPANDED 
	}
	
	void handleRegionChanged(RegionEventType eventType, RegionModel model);
}
