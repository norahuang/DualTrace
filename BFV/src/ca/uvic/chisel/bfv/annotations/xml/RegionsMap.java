package ca.uvic.chisel.bfv.annotations.xml;

import ca.uvic.chisel.bfv.annotations.RegionModel;

import java.util.*;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class used by RegionsMapAdapter to represent a SortedMap<Integer, Region> (used by Region objects to store their children) 
 * in a form that can be marshalled to XML by JAXB. Since those maps' entries are use the child region's start line as the key
 * and the child region itself as a value, we can convert those maps into a simple collection of regions without losing
 * any data--we just use the values from the map without the keys.
 * 
 * @see RegionsMapAdapter
 * @author Laura Chan
 */
public class RegionsMap {
	private Collection<RegionModel> regions;
	
	/**
	 * Creates a new, empty regions map representation.
	 */
	public RegionsMap() {
		regions = new ArrayList<RegionModel>();
	}
	
	/**
	 * Gets the regions contained in this representation.
	 * @return the regions
	 */
	@XmlElement(name="region") // this makes sure that all of the XML child elements of <children> are called <region> rather than <regions>!
	public Collection<RegionModel> getRegions() {
		return regions;
	}
	
	/**
	 * Sets the regions to be contained in this representation.
	 * @param regions
	 */
	public void setRegions(Collection<RegionModel> regions) {
		this.regions = regions;
	}
}
