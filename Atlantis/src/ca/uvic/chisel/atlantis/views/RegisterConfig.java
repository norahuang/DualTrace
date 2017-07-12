package ca.uvic.chisel.atlantis.views;

public class RegisterConfig {

	final public String label;
	final public String protectedModeLabel;
	final public int bitsWide;
	// I have doubts about modifying register UI or model on the basis of processor mode (at least with x86_64)
	final public int protectedModeBitsWide;
	final public int numberTypeDisplayMask;
	final public boolean showGotoMemory;
	/**
	 * Whether the register display is less important and should be rendered only when toggled into view by the container.
	 */
	final public boolean placeUnderHideToggle;
	
	RegisterConfig(String label, String protectedModeLabel, int bitsWide, int protectedModeBitsWide, int numberTypeDisplayMask, boolean showGotoMemory){
		this(label, protectedModeLabel, bitsWide, protectedModeBitsWide, numberTypeDisplayMask, showGotoMemory, false);
	}
	
	RegisterConfig(String label, String protectedModeLabel, int bitsWide, int protectedModeBitsWide, int numberTypeDisplayMask, boolean showGotoMemory, boolean placeUnderHideToggle){
		this.label = label;
		this.protectedModeLabel = protectedModeLabel;
		this.bitsWide = bitsWide;
		this.protectedModeBitsWide = protectedModeBitsWide;
		this.numberTypeDisplayMask = numberTypeDisplayMask;
		this.showGotoMemory = showGotoMemory;
		this.placeUnderHideToggle = placeUnderHideToggle;
	}
	
}
