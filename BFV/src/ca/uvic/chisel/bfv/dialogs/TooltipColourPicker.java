package ca.uvic.chisel.bfv.dialogs;

import ca.uvic.chisel.bfv.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Dialog for selecting colours for sticky tooltips. 
 * @author Laura Chan
 */
public class TooltipColourPicker extends Dialog {

	private class ColourSwatchListener implements Listener {
		private String buttonColour;
		
		public ColourSwatchListener(String colourID) {
			buttonColour = colourID;
		}
		
		@Override
		public void handleEvent(Event event) {
			colour = buttonColour;
			previewSwatch.setBackground(BigFileActivator.getDefault().getColorRegistry().get(colour));
			// TODO is there any way we can mark the chosen colour swatch as being selected?
			// Unfortunately, doing a setFocus() on it has no visible effect.
		}
	}
	
	private String colour; // ID of selected colour
	private Composite previewSwatch;
	
	/**
	 * Creates a new colour picker for picking a sticky tooltip colour.
	 * @param parentShell the parent shell, or null to create a top-level shell
	 * @param defaultColour the colour that should be selected when the colour picker opens.
	 */
	public TooltipColourPicker(Shell parentShell, String defaultColour) {
		super(parentShell);
		if (ColourConstants.getTooltipColourList().contains(defaultColour)) {
			colour = defaultColour;
		} else {
			throw new IllegalArgumentException("Invalid colour " + defaultColour);
		}
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Select a colour");
	}
	
	/**
	 * Creates the dialog area, which displays the available tooltip colours plus a preview of the selected colour.
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(2, true));
		
		Composite colourSwatches = new Composite(parent, SWT.BORDER);
		colourSwatches.setLayout(new GridLayout(3, true));
		for (String colourID : ColourConstants.getTooltipColourList()) {
			Color colour = BigFileActivator.getDefault().getColorRegistry().get(colourID);
			Composite colourSwatch = new Composite(colourSwatches, SWT.BORDER);
			colourSwatch.setBackground(colour);
			colourSwatch.addListener(SWT.MouseDown, new ColourSwatchListener(colourID));
		}
		
		Composite selectedColourArea = new Composite(parent, SWT.BORDER);
		selectedColourArea.setLayout(new GridLayout(1, true));
		Label label = new Label(selectedColourArea, SWT.NONE);
		label.setText("Selected Colour");
		previewSwatch = new Composite(selectedColourArea, SWT.BORDER);
		previewSwatch.setBackground(BigFileActivator.getDefault().getColorRegistry().get(colour));
		
		return parent;
	}
	
	/**
	 * Gets the ID of the colour picker's selected colour.
	 * @return ID of the selected colour
	 */
	public String getColour() {
		return colour;
	}
	
	/**
	 * Sets this colour picker's selected colour.
	 * @param colourID ID of the colour to be selected
	 */
	public void setColour(String colourID) {
		if (ColourConstants.getTooltipColourList().contains(colourID)) {
			colour = colourID;
		} // else, stick with the existing colour 
	}
}
