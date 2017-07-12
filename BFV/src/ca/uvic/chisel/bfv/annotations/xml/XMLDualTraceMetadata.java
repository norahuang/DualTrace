package ca.uvic.chisel.bfv.annotations.xml;

import javax.xml.bind.annotation.*;

/**
 * Superclass containing fields for data common to all annotation XML metadata classes. Has fields for the annotation folder and annotation file with 
 * which the metadata is associated. (At the moment these fields are not really used, but in the future they could be used to ensure
 * that the names of the annotation folder and annotation file specified in the XML metadata match those of the annotation storage that we are loading)
 * @author Laura Chan
 */
@XmlRootElement
@XmlType(propOrder={"parentFolder"})
public abstract class XMLDualTraceMetadata {
	
	private String parentFolder;
	
	public XMLDualTraceMetadata() {
		parentFolder = null;
	}

	/**
	 * Gets the name of the parent folder of the file that this XML data is associated with.
	 * @return parent folder name
	 */
	public String getParentFolder() {
		return parentFolder;
	}

	/**
	 * Sets the name of the parent folder of the file that this XML data is associated with.
	 * @param parentFolder parent folder name
	 */
	public void setParentFolder(String parentFolder) {
		this.parentFolder = parentFolder;
	}
	
}
