package ca.uvic.chisel.bfv.annotations.xml;

import ca.uvic.chisel.bfv.annotations.RegionModel;

import java.util.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XML adapter class for converting the SortedMap<Integer, Region> objects that Regions use to store their children into a form 
 * that can be marshalled into XML or unmarshalled from XML by JAXB
 * @author Laura Chan
 */
public class RegionsMapAdapter extends XmlAdapter<RegionsMap, SortedMap<Integer, RegionModel>> {

	/**
	 * Converts a SortedMap<Integer, Region> into a RegionsMap instance that can be marshalled into XML.
	 */
	@Override
	public RegionsMap marshal(SortedMap<Integer, RegionModel> v) throws Exception {
		RegionsMap regions = new RegionsMap();
		regions.setRegions(v.values());
		return regions;
	}

	/**
	 * Converts a RegionsMap representation obtained by unmarshalling from XML back into a SortedMap<Integer, Region>.
	 */
	@Override
	public SortedMap<Integer, RegionModel> unmarshal(RegionsMap v) throws Exception {
		SortedMap<Integer, RegionModel> regionsMap = new TreeMap<Integer, RegionModel>();
		for (RegionModel r : v.getRegions()) {
			regionsMap.put(r.getStartLine(), r);
		}
		return regionsMap;
	}
}

