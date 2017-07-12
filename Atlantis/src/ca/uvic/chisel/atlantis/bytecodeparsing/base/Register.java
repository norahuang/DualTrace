package ca.uvic.chisel.atlantis.bytecodeparsing.base;

/**
 * Needed simply to allow lookup of the important STACKPUSH, STACKPOP, FSBASE, and GSBASE registers.
 * Their XED ordering and thus integer ids change across versions, but we need to look them up regardless.
 * This helps.
 */
public interface Register {
	// Nothing shared except the type. Need for some special register lookups (stackpush, stackpop, fsbase, gsbase,
	// because their position changed in different versions, and we need to reference them regularly.
	
	public int regNameId();
	
	public String regName();
	
}
