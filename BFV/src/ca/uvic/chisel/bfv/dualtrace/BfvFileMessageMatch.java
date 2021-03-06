package ca.uvic.chisel.bfv.dualtrace;

import java.math.BigInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.internal.ui.text.LineElement;

import ca.uvic.chisel.bfv.views.BfvFileMatch;

@SuppressWarnings("restriction")
public class BfvFileMessageMatch extends BfvFileMatch {

	private final BigInteger targetMemoryAddress;
	private String message;
	private String channelName;

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public BfvFileMessageMatch(IFile element, int offsetIntoLine, int length, LineElement lineEntry, int intraLineOffset,BigInteger targetMemoryAddress, String message, String channelName) {
		super(element, offsetIntoLine, length, lineEntry,intraLineOffset);
		
		this.targetMemoryAddress = targetMemoryAddress;
		this.message = message;
		this.channelName = channelName;
	}
	
	public BigInteger getTargetMemoryAddress() {
		return targetMemoryAddress;
	}
	
	public String toString(){
		return channelName + ":"+message;
		
	}
}