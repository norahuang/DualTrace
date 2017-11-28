package ca.uvic.chisel.atlantis.functionparsing;

import ca.uvic.chisel.atlantis.database.InstructionId;

public class TCPUDPFunctions {
	private InstructionId socket;
	private InstructionId connect;
	private InstructionId bind;
	private InstructionId send;
	private InstructionId recv;
	private InstructionId closesocket;
	public InstructionId getSocket() {
		return socket;
	}
	public void setSocket(InstructionId socket) {
		this.socket = socket;
	}
	public InstructionId getConnect() {
		return connect;
	}
	public void setConnect(InstructionId connect) {
		this.connect = connect;
	}
	public InstructionId getBind() {
		return bind;
	}
	public void setBind(InstructionId bind) {
		this.bind = bind;
	}
	public InstructionId getSend() {
		return send;
	}
	public void setSend(InstructionId send) {
		this.send = send;
	}
	public InstructionId getRecv() {
		return recv;
	}
	public void setRecv(InstructionId recv) {
		this.recv = recv;
	}
	public InstructionId getClosesocket() {
		return closesocket;
	}
	public void setClosesocket(InstructionId closesocket) {
		this.closesocket = closesocket;
	}
	
	
}
