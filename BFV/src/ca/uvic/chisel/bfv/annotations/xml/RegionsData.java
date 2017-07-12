package ca.uvic.chisel.bfv.annotations.xml;

import ca.uvic.chisel.bfv.annotations.*;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * Represents regions data to be written to or read from XML.
 * @author Laura Chan
 */
@XmlRootElement
public class RegionsData extends XMLMetadata {
	
	private Collection<RegionModel> regions;
	
	/**
	 * Creates a new, empty RegionsData instance.
	 */
	public RegionsData() {
		super();
		regions = null;
	}
	
	/**
	 * Gets the regions that were read from XML.
	 * @return regions regions that were read
	 */
	@XmlElementWrapper
	@XmlElement(name="region")
	public Collection<RegionModel> getRegions() {
		return regions;
	}
	
	/**
	 * Sets the regions to be written to XML
	 * @param regions regions to be written
	 */
	public void setRegions(Collection<RegionModel> regions) {
		this.regions = regions;
	}
}
