package ca.uvic.chisel.bfv.dualtrace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = { "functionType", "associatedFileName", "name", "messageAddress", "messageLengthAddress",
		"channelIdReg", "channelNameAddress", "first" })
public class MessageFunction {
	private String associatedFileName;
	private String name;
	private String messageAddress;
	private String messageLengthAddress;
	private String channelIdReg;
	private String channelNameAddress;
	private String functionType;
	private InstructionXml first;
	private MessageType type;

	@Override
	public String toString() {
		if (functionType.equals("Send") || functionType.equals("Receive")) {
			return "MessageFunction [" + functionType + ": tracefile =" + associatedFileName + ", name=" + name
					+ ", messageAddress=" + messageAddress + ", messageLengthAddress=" + messageLengthAddress + "]";
		} else if (functionType.equals("SendChannelCreate") || functionType.equals("ReceiveChannelCreate") ) {
			return "ChannelFunction [" + functionType + ": tracefile =" + associatedFileName + ", name=" + name
					+ ", channelIdReg=" + channelIdReg + ", channelNameAddress=" + channelNameAddress + "]";
		}
		return "";
	}

	@XmlElement
	public String getAssociatedFileName() {
		return associatedFileName;
	}

	public void setAssociatedFileName(String associatedFileName) {
		this.associatedFileName = associatedFileName;
	}

	@XmlElement
	public String getMessageAddress() {
		return messageAddress;
	}

	public void setMessageAddress(String messageAddress) {
		this.messageAddress = messageAddress;
	}

	@XmlElement
	public String getMessageLengthAddress() {
		return messageLengthAddress;
	}

	public void setMessageLengthAddress(String messageLengthAddress) {
		this.messageLengthAddress = messageLengthAddress;
	}

	@XmlElement
	public String getChannelIdReg() {
		return channelIdReg;
	}

	public void setChannelIdReg(String channelIdReg) {
		this.channelIdReg = channelIdReg;
	}

	@XmlElement
	public String getChannelNameAddress() {
		return channelNameAddress;
	}

	public void setChannelNameAddress(String channelNameAddress) {
		this.channelNameAddress = channelNameAddress;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MessageType getType() {
		return type;
	}

	protected void setType(MessageType type) {
		this.type = type;
	}

	@XmlElement
	public String getFunctionType() {
		return functionType;
	}

	public void setFunctionType(String functionType) {
		this.functionType = functionType;
	}

	@XmlElement
	public InstructionXml getFirst() {
		return first;
	}

	public void setFirst(InstructionXml first) {
		this.first = first;
	}

	public void setFirst(String uniqueGlobalIdentifier, long firstLine, int nameindex, String fullText, String module,
			int moduleId, long moduleOffset, String parentFunctionId) {
		this.first = new InstructionXml(uniqueGlobalIdentifier, firstLine, nameindex, fullText, module, moduleId,
				moduleOffset, parentFunctionId);
	}

}
