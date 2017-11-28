package ca.uvic.chisel.bfv.dualtracechannel;

import java.math.BigInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.internal.ui.text.LineElement;

import ca.uvic.chisel.bfv.views.BfvFileMatch;

public class BfvFileWithAddrMatch extends BfvFileMatch{

	private final BigInteger targetMemoryAddress;
	
	public BfvFileWithAddrMatch(IFile element, int offsetIntoLine, int length, LineElement lineEntry,
			int intraLineOffset, BigInteger targetMemoryAddress) {
		super(element, offsetIntoLine, length, lineEntry, intraLineOffset);
		this.targetMemoryAddress = targetMemoryAddress;
		// TODO Auto-generated constructor stub
	}

	public BigInteger getTargetMemoryAddress() {
		return targetMemoryAddress;
	}
	
}
