package ca.uvic.chisel.bfv.dualtracechannel;

import java.math.BigInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.internal.ui.text.LineElement;

import ca.uvic.chisel.bfv.views.BfvFileMatch;

public class BfvFileWithAddrMatch extends BfvFileMatch{

	private final BigInteger targetMemoryAddress;
	private final long messageLength;
	private final String message;
	
	public BfvFileWithAddrMatch(IFile element, int offsetIntoLine, int length, LineElement lineEntry,
			int intraLineOffset, BigInteger targetMemoryAddress, long messageLength, String message) {
		super(element, offsetIntoLine, length, lineEntry, intraLineOffset);
		this.targetMemoryAddress = targetMemoryAddress;
		this.messageLength = messageLength;
		this.message = message;
		// TODO Auto-generated constructor stub
	}

	public long getMessageLength() {
		return messageLength;
	}

	public BigInteger getTargetMemoryAddress() {
		return targetMemoryAddress;
	}
	
	public String getMessage(){
		return message;
	}
	
}
