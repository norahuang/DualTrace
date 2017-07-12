package ca.uvic.chisel.atlantis.datacache;

import java.util.ArrayList;

import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecVtable;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.RmContext;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.SyscallVtable;
import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.models.ThreadEventType;
import ca.uvic.chisel.bfv.datacache.AbstractLine;

public class TraceLine implements AbstractLine {

	public final String str;
	public final Long instAbsoluteId;
	public final ThreadEventType threadEventType;
	public final Integer threadId;
	public final ExecRec execRec;
	public final ExecVtable execVtable;
	public final SyscallVtable syscallVtable;
	public final RmContext context;
	public final Long threadInformationBlockSkipAddress;

	public TraceLine(String str, Long inst, ThreadEventType threadEventType, Integer tid, ExecRec execRec, ExecVtable execVtable, SyscallVtable syscallVtable, RmContext context, Long threadInformationBlockSkipAddress){
		this.str = str;
		this.instAbsoluteId = inst;
		this.threadEventType = threadEventType;
		this.threadId = tid;
		this.execRec = execRec;
		this.execVtable = execVtable;
		this.syscallVtable = syscallVtable;
		this.context = context;
		this.threadInformationBlockSkipAddress = threadInformationBlockSkipAddress;
	}
	
	@Override
	public String getStringRepresentation() {
		return str;
	}

	/**
	 * NB because this is static, we can get different databases depending on which traces we have processed
	 * during the same run of Atlantis. this is less relevant to Gibraltar, which only takes one trace per run.
	 */
	private static ArrayList<String> modules = new ArrayList<String>();
	private InstructionId instructionId = null;
	private int moduleId;
	/*
	 * Should only run when we do text trace parsing. When we have a binary format,
	 * this should end up being derived differently.
	 */
	public void generateInstructionIdAndModuleId(String module, Long moduleOffset) {
		/**
		 * This module number conversion is not good; but it allows us to support textual traces.
		 * Perhaps we do not want to support textual traces...then remove it, and use the moduleId
		 * derived directly from the binary format files.
		 */
		this.moduleId = modules.indexOf(module);
		if(moduleId == -1) {
			moduleId = modules.size();
			modules.add(module);
		}
		this.instructionId = new InstructionId(this.moduleId+":"+moduleOffset);
	}	
	public void generateInstructionIdAndModuleId(long moduleId, Long instructionOffsetFromModule) {
		// moduleId is module starting address in binary format, because that will be unique in the trace.
		/**
		 * This module number conversion is not good; but it allows us to support textual traces.
		 * Perhaps we do not want to support textual traces...then remove it, and use the moduleId
		 * derived directly from the binary format files.
		 */
		this.moduleId = (int)moduleId;
		this.instructionId = new InstructionId(this.moduleId+":"+instructionOffsetFromModule);
	}
	
	public InstructionId getInstructionId(){
		return this.instructionId;
	}
	
	public int getModuleId(){
		return this.moduleId;
	}

}
