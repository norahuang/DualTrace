package ca.uvic.chisel.bfv.dualtrace;



import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;



@XmlRootElement
@XmlType(propOrder={"sendOrRecv", "associatedFileName", "name", "messageAddress", "messageLengthAddress", "messageID", "first"})
public class MessageFunction{
	private String associatedFileName;
	private String name;
	private String messageAddress;
	private String messageLengthAddress;
	private String messageID;
	private String sendOrRecv;
	private InstructionXml first;
	private MessageType type;
	

	@Override
	public String toString() {
		return "MessageFunction [" + sendOrRecv + ": tracefile =" + associatedFileName + ", name=" + name + ", messageAddress=" + messageAddress + ", messageLengthAddress="
				+ messageLengthAddress + ", messageID=" + messageID + "]";
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
	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
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
	public String getSendOrRecv() {
		return sendOrRecv;
	}


	public void setSendOrRecv(String sendOrRecv) {
		this.sendOrRecv = sendOrRecv;
	}
	
	@XmlElement
	public InstructionXml getFirst() {
		return first;
	}
	
	public void setFirst(InstructionXml first) {
		this.first = first;
	}

	public void setFirst(String uniqueGlobalIdentifier, long firstLine, int nameindex, String fullText, String module, int moduleId, long moduleOffset, String parentFunctionId) {
		this.first = new InstructionXml(uniqueGlobalIdentifier, firstLine, nameindex, fullText, module, moduleId, moduleOffset, parentFunctionId);
	}

}
