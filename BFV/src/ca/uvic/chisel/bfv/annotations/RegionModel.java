package ca.uvic.chisel.bfv.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.xml.RegionsMapAdapter;
import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.views.RegionsView;

/**
 * Represents a collapsible section of two or more consecutive lines. Regions can have child regions, that is, regions whose
 * lines are a subset of its parent region's lines. Child regions may not have the same start line as their parent.
 * @author Laura Chan
 */
@XmlRootElement(name="region")
@XmlType(propOrder={"name", "startLine", "endLine", "collapsed", "children"})
public class RegionModel implements IInterval {
	
	private String name;
	
	private boolean isCollapsed;
	
	// Note: start and end lines are use a 0-indexed implementation, but the line number ruler is 1-indexed.
	// When displaying information around the start and end lines to the user, they will need to be adjusted to use 1-indexing.
	// TODO: will start and end lines need to be longs in the future?
	// NOTE: we still need these even though we have the lineInterval just for serialization
	private int startLine;
	private int endLine;
	
	private Interval lineInterval;
	
	private FileAnnotationStorage storage; // the storage that this region is part of (needed for using XMLUtil to update collapsed/expanded state)
	private RegionModel parent;
	
	@XmlJavaTypeAdapter(RegionsMapAdapter.class)
	private SortedMap<Integer, RegionModel> children;
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere as it doesn't initialize everything that's needed!
	private RegionModel() {
		children = new TreeMap<Integer, RegionModel>();
		isCollapsed = false;
		lineInterval = new Interval(-1, -1);
	} 
	
	/**
	 * Creates a new region.
	 * 
	 * Note: startLine and endLine are 0-indexed
	 * 
	 * @param name name to give this region.
	 * @param startLine first line to be covered by this region 
	 * @param endLine last line to be covered by this region
	 */
	public RegionModel(String name, int startLine, int endLine) {
		if (startLine < 0 || endLine < 0) {
			throw new IllegalArgumentException("Line numbers cannot be negative");
		}
		if (startLine >= endLine) {
			throw new IllegalArgumentException("Start line must be strictly less than end line");
		}
		
		this.name = name;
		lineInterval = new Interval(-1, -1);
		setStartLine(startLine);
		setEndLine(endLine);
		
		this.parent = null;
		this.children = new TreeMap<Integer, RegionModel>();
	}
	
	/**
	 * Returns the name of this region
	 * @return region name
	 */
	@XmlElement
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of this region. The name must not be null or empty.
	 * @param name name to use
	 */
	public void setName(String name) {
		if (name != null && !name.trim().equals("")) {
			this.name = name;
		} else {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
	}
	
	@Override
	public String toString() {
		// This will be displayed to the user, so use 1-indexing to be consistent with the line number ruler
		return this.name + ": " + this.getStartLine(true) + "-" + this.getEndLine(true); 
	}

	/**
	 * Returns the first line covered by this region.
	 * @return start line
	 */
	@XmlElement
	public int getStartLine() {
		return startLine;
	}
	
	
	/**
	 * Utility method for providing the option of getting the start line in 1-indexed form (needed when displaying start line information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return start line in 1-indexed form if useOneIndexing == true, start line in 0-indexed form otherwise
	 */
	public int getStartLine(boolean useOneIndexing) {
		if (useOneIndexing) {
			return startLine + 1;
		} else {
			return this.getStartLine();
		}
	}
	
	public void setStartLine(int startLine) {
		this.startLine = startLine;
		lineInterval.setStart(startLine - 1);
	}

	/**
	 * Returns the last line covered by this region.
	 * @return end line
	 */
	@XmlElement
	public int getEndLine() {
		return endLine;
	}
	
	/**
	 * Utility method for providing the option of getting the end line in 1-indexed form (needed when displaying end line information to the user
	 * in views, error messages, etc.)
	 * @param useOneIndexing whether or not to use 1-indexing
	 * @return end line in 1-indexed form if useOneIndexing == true, end line in 0-indexed form otherwise
	 */
	public int getEndLine(boolean useOneIndexing) {
		if (useOneIndexing) {
			return endLine + 1;
		} else {
			return this.getEndLine();
		}
	}
	
	public void setEndLine(int endLine) {
		this.endLine = endLine;
		lineInterval.setEnd(endLine - 1);
	}
	
	public void markCollapsed() {
		isCollapsed = true;
		
		// TODO do the stuff below in the FileModelDAO
		try {
			XMLUtil.writeRegionData(storage);
		} catch (JAXBException e) {
			BigFileApplication.showErrorDialog("Error collapsing region", "Could not update file's regions file", e);
		} catch (CoreException e) {
			BigFileApplication.showErrorDialog("Error collapsing region", "Problem refreshing file's regions file", e);
		}
	}
	
	public void markExpanded() {
		isCollapsed = false;
		
		// TODO do the stuff below in the FileModelDAO
		try {
			XMLUtil.writeRegionData(storage);
		} catch (JAXBException e) {
			BigFileApplication.showErrorDialog("Error expanding region", "Could not update file's regions file", e);
		} catch (CoreException e) {
			BigFileApplication.showErrorDialog("Error expanding region", "Problem refreshing file's regions file", e);
		}
	}
	
	/**
	 * Returns whether or not this region is collapsed.
	 */
	@XmlElement // This method may look pointless, but it's needed to get JAXB to write the collapsed variable to XML
	public boolean isCollapsed() { 
		return isCollapsed;
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setCollapsed(boolean collapsed) {
		if (collapsed) {
			isCollapsed = true;				
		} else {
			isCollapsed = false;
		}
	}
	
	/**
	 * Recursively associate this region and all of its children with the specified storage.
	 * @param storage storage to associate with this region
	 */
	protected void setAnnotationStorage(FileAnnotationStorage storage) {
		this.storage = storage;
		for (RegionModel child : this.getChildren()) {
			child.setAnnotationStorage(storage);
		}
	}
	
	/**
	 * Gets this region's parent region.
	 * @return the parent region (will be null if there is none)
	 */
	public RegionModel getParent() {
		return parent;
	}
	
	/**
	 * Sets this region's parent region. Note: this method does not do any bounds checking on either the parent or the child. 
	 * Clients should not call this method directly.
	 * @param region parent region
	 */
	protected void setParent(RegionModel region) {
		this.parent = region;
	}

	/**
	 * Gets a collection of this region's child regions.
	 * @return child regions
	 */
	public Collection<RegionModel> getChildren() {
		return children.values();
	}
	
	/**
	 * Method to get all of this region's children as a map. Required by FileAnnotationStorage.getRegion()
	 */
	protected Map<Integer, RegionModel> getChildrenMap() {
		return children;
	}
	
	@SuppressWarnings("unused") // Setter is for JAXB's use only--do not use elsewhere!
	private void setChildren(SortedMap<Integer, RegionModel> children) {
		this.children = children;
	}
	
	/**
	 * Add the specified region as a child.
	 * @param region region to add as a child
	 * @throws IllegalArgumentException if the region is not completely within this region or is within one of its existing children
	 */
	public void addChild(RegionModel region) throws InvalidRegionException {
		// Do some error checking: the deepest enclosing region for the entire specified region must be this region, and the region's start
		// line must not be the same as this region's start line
		if(this.getChildren().contains(region)){
			return;
		}
		if (region.getStartLine() == startLine) {
			throw new InvalidRegionException("Cannot add child: child region must not have the same start line as its parent");
		}
		Collection<RegionModel> regions = new ArrayList<RegionModel>();
		regions.add(this);
		Set<RegionModel> newChildren = new HashSet<RegionModel>();
		for (int i = region.getStartLine(); i <= region.getEndLine(); i++) {
			RegionModel nextLineRegion = RegionModel.getEnclosingRegion(regions, i);
			if(region.asInterval().strictEncloses(nextLineRegion)){
				newChildren.add(nextLineRegion);
			}else if (nextLineRegion != this) {
				throw new InvalidRegionException(
						"Cannot add child: child region must be completely within the parent region and not in any of its existing children");
			}
		}
		
		// Child region must be valid if we made it here without throwing an exception
		children.put(region.getStartLine(), region);
		region.setParent(this);
		for(RegionModel child: newChildren){
			child.getParent().removeChild(child);
			region.addChild(child);
		}
	}
	
	/**
	 * Remove and return the child region that begins at the specified line
	 * @param startLine start line of child region to remove
	 * @return the removed child region
	 */
	protected RegionModel removeChild(int startLine) {
		RegionModel removed = this.children.remove(startLine);
		if (removed != null) {
			removed.setParent(null);
		}
		return removed;
	}
	
	protected RegionModel removeChild(RegionModel region) {
		RegionModel removed = this.children.remove(region.getStartLine());
		if (removed != null) {
			removed.setParent(null);
		}
		return removed;
	}
	
	/**
	 * Tests whether the given line is in this region
	 * @param lineNumber the number of the line in question
	 * @return true if the line is in the region, false otherwise
	 */
	public boolean containsLine(int lineNumber) {
		return lineNumber >= startLine && lineNumber <= endLine;
	}
	
	/**
	 * Refreshes the Regions View if it is open.
	 */
	private void updateRegionsView() {
		RegionsView regionsView = (RegionsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(RegionsView.ID);
		if (regionsView != null) {
			regionsView.updateView();
		}
	}
	
	/**
	 * @return An interval from the startLine to the endLine
	 */
	public Interval asInterval() {
		return lineInterval;
	}
	
	/**
	 * Recursively find the deepest region that contains the specified line
	 * @param regions list of regions to check 
	 * @param lineNumber line number for which to find enclosing region
	 * @return the region containing that line, or null if no region contains that line
	 */
	public static RegionModel getEnclosingRegion(Collection<RegionModel> regions, int lineNumber) {
		RegionModel enclosingRegion = null;
		for (RegionModel region : regions) {
			if (region.containsLine(lineNumber)) {
				enclosingRegion = region;				
				// Is there a deeper region that encloses this line? If so, recursively find that and use it instead
				RegionModel deeperRegion = getEnclosingRegion(region.getChildren(), lineNumber);
				if (deeperRegion != null) {
					enclosingRegion = deeperRegion;
				}
			}
		}
		return enclosingRegion;
	}
	
	/**
	 * Recursively determine whether all of this regions' ancestor regions are expanded
	 * @param region region to test
	 * @return true if all ancestor regions are expanded, false otherwise
	 */
	public static boolean allAncestorsExpanded(RegionModel region) {
		RegionModel parent = region.getParent();
		if (parent == null) {
			return true;
		} else if (parent.isCollapsed()) {
			return false;
		} else {
			return allAncestorsExpanded(region.getParent());
		}
	}
	
	public static Comparator<RegionModel> comparator() {
		return new Comparator<RegionModel>() {
			@Override
			public int compare(RegionModel region1, RegionModel region2) {
				int startLine1 = region1.getStartLine();
				int startLine2 = region2.getStartLine();
				
				if (startLine1 < startLine2) {
					return -1;
				} else if (startLine1 > startLine2) {
					return 1;
				} else {
					return 0;
				}
			}
		};
	}

	@Override
	public long getStartValue() {
		return (long) getStartLine();
	}

	@Override
	public long getEndValue() {
		return (long) getEndLine();
	}

	@Override
	public void setStart(long value) {
		this.setStartLine((int) value);
	}

	@Override
	public void setEnd(long value) {
		this.setEndLine((int)value);
		
	}

	// The following methods all delegate to the Interval class
	@Override
	public boolean contains(long value) {
		return lineInterval.contains(value);
	}

	@Override
	public long length() {
		return lineInterval.length();
	}

	@Override
	public long distanceFromStart(long value) {
		return lineInterval.distanceFromStart(value);
	}

	@Override
	public double percentageAlongInterval(long value) {
		return lineInterval.percentageAlongInterval(value);
	}

	@Override
	public long valueAtPercent(double percent) throws Exception {
		return lineInterval.valueAtPercent(percent);
	}

	@Override
	public boolean strictEncloses(IInterval other) {
		return lineInterval.strictEncloses(other);
	}

	@Override
	public boolean encloses(IInterval other) {
		return lineInterval.encloses(other);
	}

	@Override
	public boolean intersects(IInterval other) {
		return lineInterval.intersects(other);
	}

	@Override
	public boolean intersects(long otherStart, long otherEnd) {
		return lineInterval.intersects(otherStart, otherEnd);
	}
}
