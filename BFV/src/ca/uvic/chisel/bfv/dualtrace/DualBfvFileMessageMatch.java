package ca.uvic.chisel.bfv.dualtrace;

import org.eclipse.search.internal.ui.text.FileMatch;

@SuppressWarnings("restriction")
public class DualBfvFileMessageMatch extends FileMatch{

	private BfvFileMessageMatch sendMatch ;
	private BfvFileMessageMatch recvMatch ;

	public DualBfvFileMessageMatch(BfvFileMessageMatch sendMatch, BfvFileMessageMatch recvMatch){
		super(sendMatch.getFile(), sendMatch.getIntraLineOffset(), sendMatch.getLength(), sendMatch.getLineElement());
		this.sendMatch = sendMatch;
		this.recvMatch = recvMatch;
	}
	
	public BfvFileMessageMatch getSendMatch() {
		return sendMatch;
	}

	public void setSendMatch(BfvFileMessageMatch sendMatch) {
		this.sendMatch = sendMatch;
	}

	public BfvFileMessageMatch getRecvMatch() {
		return recvMatch;
	}

	public void setRecvMatch(BfvFileMessageMatch recvMatch) {
		this.recvMatch = recvMatch;
	}
		
	public String toString(){
		return sendMatch.toString();
		
	}
}