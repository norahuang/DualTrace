package ca.uvic.chisel.atlantis.models;

import org.eclipse.swt.graphics.Color;
import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.AtlantisColourConstants;

public enum AssemblyEventType {
	
	SWITCH(0, AtlantisColourConstants.ASSEMBLY_SWITCH_DEFAULT, AtlantisColourConstants.ASSEMBLY_SWITCH_SELECTED);

	private AssemblyEventType(int id, String defaultColor, String selectedColor) {
		this.id = id;
		this.defaultColor = defaultColor;
		this.selectedColor = selectedColor;
	}
	
	private int id;
	
	private String defaultColor;
	
	private String selectedColor;
	
	
	public Color getDefaultColor() {
		return AtlantisActivator.getDefault().getColorRegistry().get(defaultColor);
	}
	
	public Color getSelectedColor() {
		return AtlantisActivator.getDefault().getColorRegistry().get(selectedColor);
	}
	
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the EventType with the corresponding id or null if one cannot be found
	 */
	public static AssemblyEventType getFromId(int id) {
	
		for(AssemblyEventType value : AssemblyEventType.values()) {
			if(value.getId() == id) {
				return value;
			}
		}
		
		return null;
	}
}