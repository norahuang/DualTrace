package ca.uvic.chisel.bfv.dualtrace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder={"instructionNameIndex", "moduleName", "moduleId", "fullText", "moduleOffset", "firstLine", "uniqueIdentifier", "parentFunction"})
public class InstructionXml {

	/** 
	 * May be null. Not currently stored in the database, because it is not a unique
	 * identifier. It may also differ for multiple instances of the same instruction.
	 */
	private int instructionNameIndex;

	private String moduleName;
	private int moduleId;
	private String fullText;
	private long moduleOffset;
	private long firstLine;
	private String uniqueIdentifier;
	private String parentFunction;
	
	@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
	private InstructionXml() {}
	
	/**
	 * @param uniqueGlobalIdentifier
	 * @param firstLine
	 * @param name
	 * @param fullText
	 * @param module
	 * @param moduleOffset
	 * @param binaryFormatInternalId	May be null. Not currently stored in the database, because it is not a unique identifier. It may also differ for multiple instances of the same instruction.
	 */
	public InstructionXml(String uniqueGlobalIdentifier, long firstLine, int nameindex, String fullText, String module, int moduleId, long moduleOffset, String parentFunctionId) {
		this.instructionNameIndex = nameindex;
		this.moduleId = moduleId;
		
		this.moduleName = module;
		
		this.fullText = fullText;
		this.moduleOffset = moduleOffset;
		this.firstLine = firstLine;
		this.uniqueIdentifier = uniqueGlobalIdentifier;
		this.parentFunction = parentFunctionId;
	}
	@XmlElement
	public int getInstructionNameIndex() {
		return instructionNameIndex;
	}


	public void setInstructionNameIndex(int instructionNameIndex) {
		this.instructionNameIndex = instructionNameIndex;
	}

	@XmlElement
	public String getModuleName() {
		return moduleName;
	}


	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	@XmlElement
	public int getModuleId() {
		return moduleId;
	}


	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	@XmlElement
	public String getFullText() {
		return fullText;
	}


	public void setFullText(String fullText) {
		this.fullText = fullText;
	}

	@XmlElement
	public long getModuleOffset() {
		return moduleOffset;
	}


	public void setModuleOffset(long moduleOffset) {
		this.moduleOffset = moduleOffset;
	}

	@XmlElement
	public long getFirstLine() {
		return firstLine;
	}


	public void setFirstLine(long firstLine) {
		this.firstLine = firstLine;
	}

	@XmlElement
	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}


	public void setUniqueIdentifier(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	@XmlElement
	public String getParentFunction() {
		return parentFunction;
	}


	public void setParentFunction(String parentFunction) {
		this.parentFunction = parentFunction;
	}
	
	public boolean inSameModuleAs(InstructionXml other) {
		return (moduleId == other.moduleId);
	}
	
	
	@Override
	public String toString() {
		return getModuleName() + "+" + Long.toString(getModuleOffset(), 16) + " " + getFullText();
	}
}
