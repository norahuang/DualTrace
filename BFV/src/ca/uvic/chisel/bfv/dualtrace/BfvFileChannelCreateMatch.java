package ca.uvic.chisel.bfv.dualtrace;

import java.math.BigInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.internal.ui.text.LineElement;

import ca.uvic.chisel.bfv.views.BfvFileMatch;

@SuppressWarnings("restriction")
public class BfvFileChannelCreateMatch extends BfvFileMatch {

	private String id;
	private String channelName;
	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	private BigInteger targetMemoryAddress;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public BfvFileChannelCreateMatch(IFile element, int offsetIntoLine, int length, LineElement lineEntry, int intraLineOffset,BigInteger targetMemoryAddress, String id, String channelName) {
		super(element, offsetIntoLine, length, lineEntry,intraLineOffset);
		
		this.id = id;
		this.channelName = channelName;
	}
	
	public BigInteger getTargetMemoryAddress() {
		return targetMemoryAddress;
	}
	
	public String toString(){
		return channelName;
		
	}
}