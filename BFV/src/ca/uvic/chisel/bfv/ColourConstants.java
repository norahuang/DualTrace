package ca.uvic.chisel.bfv;

import java.util.*;

/**
 * Constants class for the IDs of the various colours in the Activator's colour registry.
 * @author Laura Chan
 */
public class ColourConstants {
	// Tooltip colours
	public static final String TOOLTIP_GREY = "ca.uvic.chisel.bfv.colours.tooltipGrey";
	public static final String TOOLTIP_RED = "ca.uvic.chisel.bfv.colours.tooltipRed";
	public static final String TOOLTIP_ORANGE = "ca.uvic.chisel.bfv.colours.tooltipOrange";
	public static final String TOOLTIP_YELLOW = "ca.uvic.chisel.bfv.colours.tooltipYellow";
	public static final String TOOLTIP_GREEN = "ca.uvic.chisel.bfv.colours.tooltipGreen";
	public static final String TOOLTIP_CYAN = "ca.uvic.chisel.bfv.colours.tooltipCyan";
	public static final String TOOLTIP_BLUE = "ca.uvic.chisel.bfv.colours.tooltipBlue";
	public static final String TOOLTIP_PURPLE = "ca.uvic.chisel.bfv.colours.tooltipPurple";
	public static final String TOOLTIP_CREAM = "ca.uvic.chisel.bfv.colours.tooltipCream";
	
	/**
	 * Returns a list of IDs for all available tooltip colours.
	 * @return list of IDs of tooltip colours.
	 */
	public static Collection<String> getTooltipColourList() {
		Collection<String> tooltipColourList = new ArrayList<String>();
		tooltipColourList.add(TOOLTIP_GREY);
		tooltipColourList.add(TOOLTIP_RED);
		tooltipColourList.add(TOOLTIP_ORANGE);
		tooltipColourList.add(TOOLTIP_YELLOW);
		tooltipColourList.add(TOOLTIP_GREEN);
		tooltipColourList.add(TOOLTIP_CYAN);
		tooltipColourList.add(TOOLTIP_BLUE);
		tooltipColourList.add(TOOLTIP_PURPLE);
		tooltipColourList.add(TOOLTIP_CREAM);
		return tooltipColourList;
	}
}
