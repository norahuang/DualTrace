package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import java.util.HashMap;
import java.util.Map;

public class RegisterNames {

	public static final String REG_NAME_MISSING = "?REG?";
	
	static public String currentTraceVersion = null;
	
	/**
	 * Use with ExpOper and LcSlot register name integers.
	 * 
	 * @param regName
	 * @return
	 */
	static public String lookupRegistryNumberReference(int regName){
		String regNameString = SpecRegs.lookup(regName);
		if(null == regNameString){
			switch (currentTraceVersion) {
				case RegisterNames20140106.VERSION:
					// fall through
				case RegisterNames20140106.XED_VERSION_FROM_XML:
					regNameString = RegisterNames20140106.Regs.lookup(regName);
					break;
				case RegisterNames20150121.VERSION:
					regNameString = RegisterNames20150121.Regs.lookup(regName);
					break;
			}
		}
		if(null == regNameString){
			regNameString = RegisterNames.REG_NAME_MISSING;
		}
		if(null == regNameString){
			Thread.dumpStack();
			System.out.println("Missing regnameString for: "+regName);
		}
		return regNameString;
	}
	
	/**
	 * Special register names (ids) and string values.
	 * These are referenced in Explicit Operands (expOpers) and LCSlots.
	 *
	 * LCSlot Notes from p24 v2014.05:
	 * 
	 * Operand names can either be physical register names (enum constants from Intel XED, subject to
	 * change over time, see section 14.1) and/or one of the following special values:
	 * 
	 * · MEM0 (0xffff): first memory-based operand of an instruction, referenced by a memory phrase
	 *   of the form seg:[base + scale*index + offset], where most components are optional.
	 * · MEM1 (0xfffe): second memory-based operand of an instruction
	 * · AGEN (0xfffd): address generator: integral result of a memory phrase, without reference to
	 *   memory at that “address”.
	 * · STACKPUSH (from XED): output operand is on the stack at address rsp/esp/sp -
	 *   n, where n depends on the operand size and pre-execution stack pointer value is used. Local
	 * context value in execution record provides the computed address like it does for any memory
	 * operands.
	 * · STACKPOP (from XED): input operand is on the stack at address rsp/esp/sp. Local
	 *   context value in execution record provides this address like it does for any memory operand.
	 * · FSBASE (from XED) and GSBASE (0x0087, from XED): in a user-mode trace, it
	 *   would be pretty useless to provide fs/gs selector values when those are used as segment
	 *   overrides in memory phrases. Instead, their base linear address is provided.
	 * · INVALID (from XED): no associated register (to name a constant or non-existent
	 *   operand for instance).
	 */
	public enum SpecRegs {
		
		// MEM0 (0xffff): first memory-based operand of an instruction, referenced by a memory phrase of the form seg:[base + scale*index + offset], where most components are optional.
		MEM0(0xffff),
		// MEM1 (0xfffe): second memory-based operand of an instruction
		MEM1(0xfffe),
		// AGEN (0xfffd): address generator: integral result of a memory phrase, without reference to memory at that “address”.
		/**
		 * Nice notes: http://stackoverflow.com/questions/1658294/whats-the-purpose-of-the-lea-instruction
		 */
		AGEN(0xfffd)
		;
		
		// STACKPUSH (from XED): output operand is on the stack at address rsp/esp/sp - n, where n depends on the operand size and pre-execution stack pointer value is used. Local context value in execution record provides the computed address like it does for any memory operands.
		// STACKPOP (from XED): input operand is on the stack at address rsp/esp/sp. Local context value in execution record provides this address like it does for any memory operand.
		// FSBASE (from XED) and GSBASE (0x0087, from XED): in a user-mode trace, it would be pretty useless to provide fs/gs selector values when those are used as segment overrides in memory phrases. Instead, their base linear address is provided.
		// INVALID ( from XED): no associated register (to name a constant or non-existent operand for instance).
		// See the SpecialRegisterName sub-system below. These have changeable values, but we still need
		// to reference them, so we couldn't hard code them like AGEN and the others.

		private enum SpecialRegisterName {
			STACKPUSH("STACKPUSH"),
			STACKPOP("STACKPOP"),
			FSBASE("FSBASE"),
			GSBASE("GSBASE"),
			INVALID("INVALID")
			;
			
			public String name;
			SpecialRegisterName(String name) {
				this.name = name;
			}
		}
		
		public int regNameId;
		public String regName;
		
		SpecRegs(int regNameId){
			this.regNameId = regNameId;
			this.regName = this.name();
			SpecRegLookup.LCSLOT_SPECIAL_REGISTER_NAMES.put(regNameId, this);
		}
		
		public static String lookup(int regName) {
			SpecRegs reg = SpecRegLookup.LCSLOT_SPECIAL_REGISTER_NAMES.get(regName);
			return reg == null ? null : reg.regName;
		}

		static public class SpecRegLookup { 
			
			public final static Map<Integer, SpecRegs> LCSLOT_SPECIAL_REGISTER_NAMES = new HashMap<>();
		}

		public static Register STACKPUSH() {
			return lookupErrantSpecialRegister(SpecialRegisterName.STACKPUSH);
		}
		
		public static Register STACKPOP() {
			return lookupErrantSpecialRegister(SpecialRegisterName.STACKPOP);
		}
		
		public static Register FSBASE() {
			return lookupErrantSpecialRegister(SpecialRegisterName.FSBASE);
		}
		
		public static Register GSBASE() {
			return lookupErrantSpecialRegister(SpecialRegisterName.GSBASE);
		}
		
		public static Register INVALID() {
			return lookupErrantSpecialRegister(SpecialRegisterName.INVALID);
		}
		
		/**
		 * We need to have this extra level of indirection for these few registers because their values
		 * move in different XED versions. We need to refer to them by name, conditionally on the version,
		 * in a way that uses their integer representation. This achieves that. prior to this, we had them
		 * hard coded, which was clobbering MMX registers that are near them in the list (oops!).
		 * 
		 * @param regName
		 * @return
		 */
		private static Register lookupErrantSpecialRegister(SpecialRegisterName regName){
			Register reg = null;
			
			switch (currentTraceVersion) {
				case RegisterNames20140106.VERSION:
					// fall through
				case RegisterNames20140106.XED_VERSION_FROM_XML:
					switch (regName) {
						case INVALID:
							reg = RegisterNames20150121.Regs.XED_REG_INVALID;
							break;
						case STACKPUSH:
							reg = RegisterNames20140106.Regs.XED_REG_STACKPUSH;
							break;
						case STACKPOP:
							reg = RegisterNames20140106.Regs.XED_REG_STACKPOP;
							break;
						case FSBASE:
							reg = RegisterNames20140106.Regs.XED_REG_FSBASE;
							break;
						case GSBASE:
							reg = RegisterNames20140106.Regs.XED_REG_GSBASE;
							break;
					}
					break;
				case RegisterNames20150121.VERSION:
					switch (regName) {
					case INVALID:
						reg = RegisterNames20150121.Regs.XED_REG_INVALID;
						break;
					case STACKPUSH:
						reg = RegisterNames20150121.Regs.XED_REG_STACKPUSH;
						break;
					case STACKPOP:
						reg = RegisterNames20150121.Regs.XED_REG_STACKPOP;
						break;
					case FSBASE:
						reg = RegisterNames20150121.Regs.XED_REG_FSBASE;
						break;
					case GSBASE:
						reg = RegisterNames20150121.Regs.XED_REG_GSBASE;
				}
					break;
			}
			return reg;
		}
	
	}
	
}
