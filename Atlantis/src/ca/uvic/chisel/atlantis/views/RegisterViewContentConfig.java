package ca.uvic.chisel.atlantis.views;

import java.util.ArrayList;

import ca.uvic.chisel.atlantis.controls.RegisterControl;

/**
 * Provides configuration for the {@link RegistersView}, to create the view in a
 * data driven way.
 * 
 * This may later be changed to be config file based, or otherwise modifiable by
 * the end user.
 * 
 * Complete set of registers can most easily be seen in the Table_of_x86_Registers svg
 * in the documentation folder, or on wikipedia:
 * https://upload.wikimedia.org/wikipedia/commons/1/15/Table_of_x86_Registers_svg.svg
 * Further documentation at AMD and Intel:
 * http://support.amd.com/TechDocs/24594.pdf
 * https://software.intel.com/sites/default/files/managed/a4/60/253665-sdm-vol-1.pdf
 * http://sandpile.org/
 * http://wiki.osdev.org/Segmentation
 * 
 * The exclusion of certain registers is on the basis of conservative judgment and
 * requests to include missing ones.
 *
 */
public class RegisterViewContentConfig {

	public static ArrayList<RegisterConfig> getRegisterPopulation() {
		ArrayList<RegisterConfig> configs = new ArrayList<RegisterConfig>();

		configs.add(new RegisterConfig("RAX", "EAX", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RBX", "EBX", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RCX", "ECX", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RDX", "EDX", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RSI", "ESI", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RDI", "EDI", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RBP", "EBP", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RSP", "ESP", 64, 32, RegisterControl.INTEGER_ONLY, true));
		configs.add(new RegisterConfig("RIP", "EIP", 64, 32, RegisterControl.INTEGER_ONLY, true));

		// R8-R15 general purpose registers
		for (int i = 8; i < 16; i++) {
			configs.add(new RegisterConfig("R" + i, "-", 64, 0, RegisterControl.INTEGER_ONLY, true, true));
		}

		configs.add(new RegisterConfig("GSBASE", "GSBASE", 16, 16, RegisterControl.INTEGER_ONLY, true, true));
		configs.add(new RegisterConfig("FSBASE", "FSBASE", 16, 16, RegisterControl.INTEGER_ONLY, true, true));

		configs.add(new RegisterConfig("CS", "CS", 16, 16, RegisterControl.INTEGER_ONLY, true, true));
		configs.add(new RegisterConfig("DS", "DS", 16, 16, RegisterControl.INTEGER_ONLY, true, true));
		configs.add(new RegisterConfig("ES", "ES", 16, 16, RegisterControl.INTEGER_ONLY, true, true));
		configs.add(new RegisterConfig("SS", "SS", 16, 16, RegisterControl.INTEGER_ONLY, true, true));

		for (int i = 0; i < 8; i++) {
			// NB These are super-registers to the deprecated MMX registers, so even when they are present,
			// they will end up as subregisters to these.
			configs.add(new RegisterConfig("ST" + i, "ST" + i, 80, 80, RegisterControl.FLOAT_80, false, true));
		}
		
		// WHy not show the YMM and ZMM registers (super-registers of XMM)? They are 256 and 512 bits long. Add them if asked.
		for (int i = 0; i < 8; i++) {
			// Lower SSE XMM registers, available in both Long and Protected Modes
			configs.add(new RegisterConfig("XMM" + i, "XMM" + i, 128, 128, RegisterControl.FLOAT_IN_128, false, true));
		}
		for (int i = 8; i < 16; i++) {
			// XMM8-15 only available in Long Mode 64b
			configs.add(new RegisterConfig("XMM" + i, "-", 128, 0, RegisterControl.FLOAT_IN_128, false, true));
		}

		return configs;
	}

}
