package ca.uvic.chisel.atlantis.bytecodeparsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import ca.uvic.chisel.atlantis.bytecodeparsing.base.ITraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.Pre2015TraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.SyscallUnifiedIdNameTable;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.TraceXml;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMapItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMem;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecLineType;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecPrevNextColumn;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecVtable;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.RmContext;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMapItable.RegInfo;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec.FlagValues;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.AddressableSpaceItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ModuleItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ModuleRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.SyscallRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.SyscallVtable;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.ThreadItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.DecodedIns;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.ExpOper;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.InsRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.LcSlot;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.DecodedIns.SpecialSlotIndex;
import ca.uvic.chisel.atlantis.datacache.TraceLine;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.atlantis.models.ThreadEventType;

// This parser won't be serving as a file backend, it feeds to them. It has some things in common with the
// BFV FileLineFileBackend, in terms of file access, but they aren't related that much.
// We also need to get functionality into or in addition to the which takes into account the files to hide,
// and triggers all this sort of business.
// Open handler might work as is: BigFileOpenActionProvider
public class BinaryFormatParser {
	
	final private ITraceXml traceXml;
	final private ExecVtable execVtable;
	final private InsItable insItable;
	final private ThreadItable threadItable;
	final private SyscallVtable syscallVtable;
	final private ModuleItable moduleItable;
	
	// Less interesting
	@SuppressWarnings("unused")
	private AddressableSpaceItable addressableSpaceItable;
	@SuppressWarnings("unused")
	private ExecPrevNextColumn execPrevNextColumn;

	public BinaryFormatParser(AtlantisBinaryFormat binaryFiles){
		traceXml = createTraceXmlFile(binaryFiles.getTraceXmlFile());
		
		RegisterNames.currentTraceVersion = traceXml.getXedVersion();
		
		insItable = new InsItable(binaryFiles, traceXml);
		
		threadItable = new ThreadItable(binaryFiles, traceXml);
		
		execVtable = new ExecVtable(binaryFiles, insItable, threadItable, traceXml);

		syscallVtable = new SyscallVtable(binaryFiles, traceXml);
		
		moduleItable = new ModuleItable(binaryFiles, traceXml);
		
		// We don't care about these so much.
		// addressableSpaceItable = new AddressableSpaceItable(binaryFiles);
		// execPrevNextColumn = new ExecPrevNextColumn(binaryFiles);
		
		this.maxLineNumber = execVtable.recordCount;
		
	}
	
	public long getNumberOfTraceLines(){
		return this.maxLineNumber;
	}
	
	protected ITraceXml createTraceXmlFile(File traceXmlFile) {
		// return  decoded XML file.
		Serializer parser = new Persister();
		try {
			// Need to remove first line in order to add the fake root element.
			// This is unfortunate, but I cannot change the XML format to be valid.
			
			BufferedReader xml = new BufferedReader(new FileReader(traceXmlFile));
			// Might seem silly, but it's a small file, and I really need to ensure that the malformed XML is fixed.
			// It's much easier to cope with the missing newlines and Data elements if I break this into a string first.
			String collectedString = "";
			String firstLine = xml.readLine()+"\n";
			String line = xml.readLine();
			while(line != null){
				collectedString += line;
				line = xml.readLine();
			}
			collectedString  = collectedString.replace("><", ">\n<");
			String[] xmlList = collectedString.split("\\r?\\n");
			// System.out.println(firstLine);
			String allLeftOver = "";
			boolean startData = false;
			boolean endData = false;
			for(int i = 0; i < xmlList.length; i++){
				String leftOver = xmlList[i];
				startData = startData || leftOver.equalsIgnoreCase("<data>");
				endData = endData || leftOver.equalsIgnoreCase("</data>");
			    // System.out.println(leftOver);
			    allLeftOver += leftOver+"\n";
			}

	        ByteBuffer fixedXml = ByteBuffer.wrap(new byte[firstLine.getBytes().length + allLeftOver.getBytes().length + 20]);
	        fixedXml.put(firstLine.getBytes());
	        if(!startData){
	        	fixedXml.put("<Data>".getBytes());
	        }
	        fixedXml.put(allLeftOver.getBytes());
	        if(!endData){
	        	fixedXml.put("</Data>".getBytes());
			}
	        String xmlForValidation = IOUtils.toString(fixedXml.array(), "UTF-8");
			
	        ITraceXml read = null;
	        boolean exception = false;
	        try{
			parser.validate(TraceXml.class, xmlForValidation);
			read = parser.read(TraceXml.class, xmlForValidation, false);
	        } catch(Exception e){
	        	// When working with older traces, we will let this slide past.
	        	// As we move forward, there could be cases where trace format changes, but there is a backlog
	        	// of trace files in older formats. So, we need to support older formats.
	        	System.out.println("Trace XML reading had a (possibly safe) exception:"+e.getMessage());
	        	exception = true;
	        }
	        if(null == read){
	        	// Backwards compatibility
	        	parser.validate(Pre2015TraceXml.class, xmlForValidation);
	        	read = parser.read(Pre2015TraceXml.class, xmlForValidation, false);
	        	if(exception){
	        		System.out.println("Previous Trace XML reading exception was indeed safe, fell through to legacy xml format parsing.");
	        	}
	        }
			return read;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getMemoryChangesForLine(int lineNumber){
		// TODO This is used for parsing, but also for the memory view.
		// It is hackish to convert models to strings then re-parse them in that view...
		ExecRec execRec = execVtable.getExecRec(lineNumber);
		return this.getMemoryChangesForLine(execRec);
	}
	
	public String getMemoryChangesForLine(ExecRec execRec){
		// Might ask for memory changes on lines that are not instructions
		if(execRec.insRec == null){
			return null;
		}
		String[] allMemChanges = stringifyOperVals(execRec);
		String preMemChangeString = allMemChanges[0];
		String postMemChangeString = allMemChanges[1];
		return " |"+postMemChangeString;
	}
	
	public ArrayList<ModuleRec> getAllModules(){
		return this.moduleItable.getAllModules();
	}

	public boolean isExecutionMode64Bit(int lineNumber){
		return ExecRec.isExecutionMode64Bit(this.execVtable, lineNumber);
	}
	
	/**
	 * Fetches disassembly for the requested line. It will rehydrate memory by
	 * changes by parsing fully if the appropriate argument is set; otherwise,
	 * it only gives the instruction and relevant arguments alone.
	 * 
	 * @param lineNumber
	 * @param parseForDisplayOnly	In the case that only the disassembly and line prefixes are desired.
	 * @return
	 */
	private TraceLine getDisassemblyOfLine(long lineNumber, boolean parseForDisplayOnly){
		// Rehydrate a line, as we expect it to be formatted for old format,
		// on the basis of the parsed binary data.
		if(lineNumber >= maxLineNumber){
			return null;
		}
		
		String line = "";
		
		String hexLineNumber = toHex(lineNumber);
		line += hexLineNumber+" ";
		
		ExecRec execRec = execVtable.getExecRec(lineNumber);
		ExecLineType type = execRec.type;
		
		Long instructionAbsoluteId = null;
		
		ThreadEventType threadEvent = null;
		
		RmContext newContext = null;
		
		Long threadInformationBlockSkipAddress = null;
		
		// Needed near end, but only available for type INSTRUCTION.
		InsRec insRec = null;
		ModuleRec module = null;
		
		if(type == ExecLineType.INSTRUCTION){
			insRec = execRec.insRec;
			module = this.moduleItable.getModuleRec(insRec);
			
			instructionAbsoluteId = insRec.id;
			
			String moduleString = " NoModule";
			if(null != module){
				String dllNameOnly = module.winNameValue.substring(Math.max(module.winNameValue.lastIndexOf("\\")+1, 0));
				moduleString = " "+dllNameOnly+"+"+toHex(insRec.address)+" ("+toHex(module.startAddr+insRec.address)+")";
			}
			
			// TODO Add assembly name, and instructions, and register and memory changes
			line += "I:"+toHex(execRec.insId)+moduleString+" "+insRec.dissasembly;
			
			if(!parseForDisplayOnly){
				line += getMemoryChangesForLine(execRec);
			}
			
		} else if (type == ExecLineType.THREAD_BEGIN){
			threadEvent = ThreadEventType.THREAD_BEGIN;
			newContext = execRec.threadBeginContext;
			
			// I need to *not* print the TIB (Thread Information Block), which happens to be
			// in the memory context, under the memRef with an address matching the threadRec.tibAddr
			// location.
			threadInformationBlockSkipAddress = execRec.threadRec.tibAddr;
			StringBuilder registerContents = rehydrateRegisterMemoryContents(newContext, threadInformationBlockSkipAddress);
			
			line += "S:ThreadBegin"+" "+"TID="+execRec.threadRec.winTid;
			if(!parseForDisplayOnly){
				line += " "+registerContents;
			}
		
		} else if (type == ExecLineType.THREAD_END){
			threadEvent = ThreadEventType.THREAD_END;
			line += "S:ThreadEnd"+" TID="+execRec.threadRec.winTid+" Code="+execRec.threadExitCode;
			
		} else if (type == ExecLineType.APP_END){
			threadEvent = ThreadEventType.APPLICATION_END;
			line += "S:ApplicationEnd"+" Code="+execRec.appExitCode;
		
		} else if (type == ExecLineType.SYSCALL_ENTRY){
			newContext = execRec.syscallEntryContext;
			SyscallRec syscallRec = this.syscallVtable.getSyscallRec(execRec.syscallEnterId);
			
			String reSP;
			int hexByteWidth;
			if(FlagValues.flag64BitInstruction.hexIntValue == (FlagValues.flag64BitInstruction.hexIntValue & execRec.flags)){
				reSP = " RSP=";
				hexByteWidth = 8;
			} else {
				reSP = " ESP=";
				hexByteWidth = 4;
			}
			
			StringBuilder argString = new StringBuilder();
			// Some 64-bit calls have an unknown number of arguments, in which case we will apparently always receive
			// four arguments in the data. Otherwise, we have between 0 and 4 arguments.
			int i = 0;
			for(Long arg: syscallRec.args){
				// Print hex at 64-bit (16 hex) width. 32-bit are zero extended in the data.
				// TODO We might check the convention property of the syscallRec to see
				// how wide the argument should be.
				argString.append("arg"+i+"="+toHex(arg, hexByteWidth)+" ");
				i++;
			}
			
			// TODO The syscall extras are not really usable. Memory modifications are
			// dealt with in their own execution records, but there are kernel objects that
			// are not understandable in user mode. Maybe there is something to do with them,
			// but my reading of the documentation is such that I think we can/should not use that
			// data at this time. See page 15 of Oct 2013 documentation.
			
			line += "S:SyscallEntry"+" "+execRec.syscallEnterId
					+" ("+SyscallUnifiedIdNameTable.get(syscallRec)
					+")";
			if(!parseForDisplayOnly){
				// Although contexts have places for registers and memory, they don't show up in syscalls.
				// Is this absolutely true?
				StringBuilder registerMemoryContents = rehydrateRegisterMemoryContents(newContext);
				argString.append(reSP+toHex(syscallRec.beforeSp, hexByteWidth)+" ");
				line += "| "+argString+"| "+registerMemoryContents.toString();
			}
		
		} else if (type == ExecLineType.SYSCALL_EXIT){
			newContext = execRec.sysCallExitContext;
			// SyscallRec syscallRec = this.syscallVtable.getSyscallRec(execRec.syscallExitId);
			StringBuilder registerMemoryContents = rehydrateRegisterMemoryContents(newContext);
			
			// NB Although we can fetch a SyscallRec based on this exit id, we don't seem to need
			// it; we get syscall arguments from it when it opens the syscall, which are not needed now.
			
			
			if(-1 == execRec.syscallExitId){
				// -1 id is equivalent to a context switch, there is no syscall record to retrieve.
				line += "S:ContextSwitch";
				if(!parseForDisplayOnly){
					line += " | | "+registerMemoryContents;
				}
			} else {
				line += "S:SyscallExit"+" "+execRec.syscallExitId;
				String reAX;
				int hexByteWidth;
				if(FlagValues.flag64BitInstruction.hexIntValue == (FlagValues.flag64BitInstruction.hexIntValue & execRec.flags)){
					reAX = " RAX=";
					hexByteWidth = 8;
				} else {
					reAX = " EAX=";
					hexByteWidth = 4;
				}
				SyscallRec syscallRec = this.syscallVtable.getSyscallRec(execRec.syscallExitId);
				
				if(!parseForDisplayOnly){
					line += " | | "+registerMemoryContents + reAX+toHex(syscallRec.result, hexByteWidth);
				}
			}
		
		} else if (type == ExecLineType.SKIPPED_SYSCALL_EXIT){
			// There is no RmContext for skipped calls, and we don't really need the SyscallRec either.
			// SyscallRec syscallRec = this.syscallVtable.getSyscallRec(execRec.syscallSkippedId);
			line += "S:SyscallSkipped"+" "+execRec.syscallSkippedId;
		
		} else if (type == ExecLineType.CONTEXT_CHANGE_ASYNC_PROC_CALL){
			newContext = execRec.contextChange;
			StringBuilder registerContents = rehydrateRegisterMemoryContents(newContext);
			line += "T:"+" "+execRec.threadRec.winTid;
			if(!parseForDisplayOnly){
				line += " | | "+registerContents;
			}
			
			if(newContext.memCount != 0)
			System.out.println("Uncertain of this context change textual representation. Assume memory count is zero, and we have: "+newContext.memCount);
		
		} else if (type == ExecLineType.CONTEXT_CHANGE_EXCEPTION_HANDLING){
			newContext = execRec.contextChange;
			StringBuilder registerContents = rehydrateRegisterMemoryContents(newContext);
			line += "T:"+" "+execRec.threadRec.winTid;
			if(!parseForDisplayOnly){
				line += " | | "+registerContents;
			}
			
			if(newContext.memCount != 0)
			System.out.println("Uncertain of this context change textual representation. Assume memory count is zero, and we have: "+newContext.memCount);
		
		} else if (type == ExecLineType.CONTEXT_CHANGE_CALLBACK){
			threadEvent = ThreadEventType.IMPLICIT_THREAD_SWITCH;
			newContext = execRec.contextChange;
			StringBuilder registerContents = rehydrateRegisterMemoryContents(newContext);
			line += "T:"+" "+execRec.threadRec.winTid+" switch";
			if(!parseForDisplayOnly){
				line += " | | "+registerContents;
			}
			
			if(newContext.memCount != 0)
			System.out.println("Uncertain of this context change textual representation. Assume memory count is zero, and we have: "+newContext.memCount);
		
		} else if (type == ExecLineType.CONTEXT_CHANGE_UNKNOWN_REASON){
			newContext = execRec.contextChange;
			StringBuilder registerContents = rehydrateRegisterMemoryContents(newContext);
			line += "T:"+" "+execRec.threadRec.winTid;
			if(!parseForDisplayOnly){
				line += " | | "+registerContents;
			}
			
			if(newContext.memCount != 0)
			System.out.println("Uncertain of this context change textual representation. Assume memory count is zero, and we have: "+newContext.memCount);
		
		}
			
		this.prevExecRec = execRec;
		
		// Add trace flags, with some human readable bits
		line += " "+ExecRec.FlagValues.emitHumanReadable(execRec.flags);
		
//		System.out.println("Got: "+line);
		
		// Things did not work without another character added, and this one seemed swell.
		// This is very cargo cultish; I'd look deeper but I have other priorities.
		line += "\n";
		
		// Not quite, but I am not converting output from pure string all at once right now.
		Integer tid = null;
		if(null != execRec.threadRec){
			tid = execRec.threadRec.winTid;
		}

		TraceLine lineData = new TraceLine(line, instructionAbsoluteId, threadEvent, tid, execRec, execVtable, syscallVtable, newContext, threadInformationBlockSkipAddress);
		if(null != insRec && null != module){
			lineData.generateInstructionIdAndModuleId(module.startAddr, insRec.address);
		}
		return lineData;
	}
	
	/**
	 * Used by the paging system. This method must give the length of the strings
	 * returned by {@link BinaryFormatFileBackend#getDiss}
	 * 
	 * @param lineNumber
	 * @return
	 */
	public Integer getLengthOfLineDissassembly(long lineNumber){
		// This is a pain if we don't have rehydrated lines, *if* we also want to show things like line numbers,
		// modules, etc. in the trace editor.
		// Luckily I sorted out a great caching system from the primary (only) user, the paging class.
		// If this gets used elsewhere, beware of performance cost.
		 return fetchLineForDisplay(lineNumber).getStringRepresentation().length();
	}
	
	/**
	 * This method will parse the binary format for editor data, and not for the memory values
	 * or other more parsing process intensive items.
	 * 
	 * @return
	 */
	public TraceLine fetchLineForDisplay(long lineNumber) {
		TraceLine line = getDisassemblyOfLine(lineNumber, true);
		return line;
	}
		
	/**
	 * Reads the next line, according to internal counter of lines read.
	 * Returns a trace line in a format compatible with the older formats.
	 * This can be changed when we decide to not support older formats,
	 * so that the "line" can be an object with separate properties for each
	 * data element available across the binary files.
	 * 
	 * @return
	 */
	public TraceLine parseNextLineToString() {
		long getLine = currentLineTrace++; // so I can skip lines throwing exceptions during debugging...
		
		/*
		// We can skip to deeper points in the trace for debugging by doing the following:
		if(getLine < 55793318){
			getLine = currentLineTrace = 55793318;
		}
		System.out.println();
		System.out.println("Line Number: " +getLine);
		*/
		
		TraceLine line = getDisassemblyOfLine(getLine, false);
		return line;
	}
	
	public long currentLineTrace = 0;
	long maxLineNumber = -1;
	ExecRec prevExecRec;
	
	static public boolean showWarning = true;
	private String[] stringifyOperVals(ExecRec execRec){
		String preMemChange = "";
		String postMemChange = "";
		
		DecodedIns decodedIns = execRec.insRec.decodedIns;
		
		List<Integer> usedSlotIndices = new ArrayList<Integer>();
		for(int i = 0; i < decodedIns.expOpers.length; i++){
			ExpOper oper = decodedIns.expOpers[i];
			for(int beforeSlotsOrAfter = 0; beforeSlotsOrAfter <= 1; beforeSlotsOrAfter++){
				boolean keepBeforeOperandsOnly = (beforeSlotsOrAfter == 0);
				LcSlot lcSlot;
				Integer slotIndex;
				String memoryString;
				if(keepBeforeOperandsOnly){
					lcSlot = oper.beforeSlotObject;
					slotIndex = oper.beforeSlot;
					memoryString = preMemChange;
				} else {
					lcSlot = oper.afterSlotObject;
					slotIndex = oper.afterSlot;
					memoryString = postMemChange;
				}
				
				String constSlot = null;
				Long constVal = null; // for MEM0, MEM1, and some others, the address might be contained there...
				if(slotIndex == SpecialSlotIndex.SLOT_NA.index){
					continue;
				} else if(slotIndex == SpecialSlotIndex.SLOT_IMM0.index){
					// use constSlot[0]
					constVal = decodedIns.constSlot[0];
					constSlot = "IMM0";
				} else if(slotIndex == SpecialSlotIndex.SLOT_IMM1.index){
					// use constSlot[1]
					constVal = decodedIns.constSlot[1];
					constSlot = "IMM1";
				}
				
				// The stack operations are unlikely to be ExpOper, but I want to deal with them lower down in any case.
				if(null != lcSlot
					&& (lcSlot.regName == RegisterNames.SpecRegs.STACKPUSH().regNameId()
					 || lcSlot.regName == RegisterNames.SpecRegs.STACKPOP().regNameId())
					){
					continue;
				}
				//				} else if(operVal.lcSlot.regNameString == RegisterNames.SpecRegs.FSBASE.name
				//							|| operVal.lcSlot.regNameString == RegisterNames.SpecRegs.GSBASE.name){
				//					// FSBASE (from XED) and GSBASE (from XED): in a user-mode trace, it would be pretty useless to provide fs/gs selector values when those are used as segment overrides in memory phrases. Instead, their base linear address is provided.
				
				// Didn't skip out? Log that we used this slot.
				usedSlotIndices.add(slotIndex);
				
				// Append space when we have more coming.
				memoryString += " ";
				
				if(null != lcSlot && (
					(keepBeforeOperandsOnly && lcSlot.isAnAfterWriteOperand)
					|| (!keepBeforeOperandsOnly && (lcSlot.isABeforeWriteOnlyOperand || lcSlot.isABeforeReadOrRWOperand))
					)){
					// If it is a write after operand, skip
					System.out.println("I don't think we should ever get here anymore");
					continue;
				}
				
				// TODO The documentation says that for Memory Operands (the special reg names like MEM0),
				// we can find their memory addresses and their values in the *execution record*. That is
				// their pre-execution value though, so I likely need what I find in the LcSlots anyway.
				// But this can confirm my values if it is indeed stored in both places!
				if(null != constVal){
					// If these are really immediate values, they cannot be written to, and aren't memory, so we
					// shouldn't be including them in memory output.
					memoryString += constSlot;
				} else if(lcSlot.hasMemoryLocationAvailable()){
					// MEM0 and MEM1
					memoryString += "["+lcSlot.memoryLocationString+"]";
				} else {
					// Basic register reference, so no memory address to print
					// Note we have diff reg name in LcSlot, and the enclosing register name:
					// lcSlot.regNameString, lcSlot.regNameLargestEnclosingString
					memoryString += oper.regNameString;
				}
				
				
				// Apply values to the registers/memory addresses
				// Register...or special value! We don't let some special values get here, and deal with them
				// below in the "orphan" LcSlot processing.
				if(null != constVal){
					memoryString += "="+BinaryFormatParser.toHex(constVal);
				} else if(lcSlot.regNameString == RegisterNames.SpecRegs.AGEN.regName){
					// AGEN same as MEM0 in some ways
					String effectiveAddress = BinaryFormatParser.toHex(lcSlot.mem0EffectiveAddress, 8);
					if(null == effectiveAddress){
						System.out.println("Null found for: "+lcSlot.regNameString+": "+lcSlot.mem0MemoryPhrase);
					}
					memoryString += ":"+effectiveAddress;
				} else {
					// Section 7.2.3 says that AGEN does not have memory value, unlike otherwise similar MEM0.
					// It is also only used for the lea instruction, which can offer some validation.
					String value;
					if(keepBeforeOperandsOnly){
						value = oper.regMemoryBeforeValueHexString;
					} else {
						value = oper.regMemoryAfterValueHexString;
					}
					
					memoryString += "="+value;
				}
				
				// Re-assign back to appropriate string variable
				if(keepBeforeOperandsOnly){
					preMemChange = memoryString;
				} else {
					postMemChange = memoryString;
				}
			}
		}
		
		
		// ----
		// Orphan LcSlot Processing (those without ExpOper, as well as stack ops)
		// ----
		
		LcSlot rspBeforeSlot = null;
		LcSlot rspAfterSlot = null;
		for(LcSlot slot: decodedIns.lcSlots){
			if(!slot.regNameString.equals("RSP") && !slot.regNameString.equals("ESP") && !slot.regNameString.equals("SP")){
				continue;
			}
			boolean isBefore = slot.isABeforeReadOrRWOperand || slot.isABeforeWriteOnlyOperand;
			if(isBefore){
				rspBeforeSlot = slot;
			} else {
				rspAfterSlot = slot;
			}
		}
				
		for(int i = 0; i < decodedIns.lcCount; i++){
			if(!usedSlotIndices.contains(i)){
				LcSlot slot = decodedIns.lcSlots.get(i);
				String memoryString;
				boolean isBefore = slot.isABeforeReadOrRWOperand || slot.isABeforeWriteOnlyOperand;
				if(isBefore){
					memoryString = preMemChange;
				} else {
					memoryString = postMemChange;
				}
				
				memoryString += " ";
				
				// RSP, STACK_POP, and STACK_PUSH show up here plenty.
				// For stacks, I need the value of RSP before and after, then I use that value to write the stack
				// related memory changes. So...look for an RSP first, then go through this??
				// TODO Is it correct that we only ever use the *before* RSP value for pop and push?
				if(slot.regName == RegisterNames.SpecRegs.STACKPUSH().regNameId()
					|| slot.regName == RegisterNames.SpecRegs.STACKPOP().regNameId()){
					// STACKPOP (from XED): input operand is on the stack at address rsp/esp/sp. Local context value in execution record provides this address like it does for any memory operand.
					// STACKPUSH (from XED): output operand is on the stack at address rsp/esp/sp - n, where n depends on the operand size and pre-execution stack pointer value is used. Local context value in execution record provides the computed address like it does for any memory operands.
					// To clarify, the memory address operand will be the STACKPUSH computed address, as though it were a memory operand.
					// Note we're using the same member from two different slots. Don't overlook!
					// Can't seem to find the effective width of pushed values, so we probably don't need to truncate the value as done with registers.
					memoryString += "["+rspBeforeSlot.specialAddressForMemoryPhraseSlotsString+"]="+slot.specialAddressForMemoryPhraseSlotsString;
				} else {
					// This often (always) is where RSP shows up. It's not usually an ExpOper, understandably.
					// Includes FSBASE (from XED) and GSBASE (from XED)
					// NB: in a user-mode trace, it would be pretty useless to provide fs/gs selector values when those are
					// used as segment overrides in memory phrases. Instead, their base linear address is provided.
					// Can't seem to find the effective width of pushed values, so we probably don't need to truncate the value as done with registers.
					memoryString += slot.regNameString+"=";
					String value = slot.specialAddressForMemoryPhraseSlotsString;
					if(slot.regNameString.toLowerCase().contains("flags")){
						value = MemoryDeltaTree.convertHexFlagToLetterCodeFlags(value);
					}
					memoryString += value;
				}
				
				// Re-assign back to appropriate string variable
				if(isBefore){
					preMemChange = memoryString;
				} else {
					postMemChange = memoryString;
				}
			}
		}
		
		return new String[]{preMemChange, postMemChange};
	}
	
	private StringBuilder rehydrateRegisterMemoryContents(RmContext context){
		return this.rehydrateRegisterMemoryContents(context, null);
	}
	
	/**
	 * Thread begin lines have a reference to a Thread Information Block, which we do
	 * not want in the textual trace. Skip those if the argument is provided.
	 * 
	 * @param context
	 * @param addressToSkip
	 * @return
	 */
	private StringBuilder rehydrateRegisterMemoryContents(RmContext context, Long addressToSkip){
		// TODO This doesn't appear to be used to its full extent. How much dead code is in this?
		// stringifyOperVals(), for example, is the one that successfully gets register values.
		// regInfo seems to have size 0 all the time...
		StringBuilder registerContents = new StringBuilder();
		
		// Was trying to deactivate this method, making non-functional, then comparing old DB results to this output.
		// Unfortunately, the traces I was looking at did not have any valid regMap (see continue in loop below).
		// I already has suspicions that this was dead code...maybe the whole thing is incorrect. Making issue to figure
		// that out...
		// Should only affect the line table, though I will see memory table differences due to the concommitant change
		// to memory binning structures I removed.
		// if(1+1 == 3){
		// 	return registerContents;
		// }
		
		// Collect *relevant* register values
		boolean is64BitLongMode = false; // get this from somewhere
		ContextMapItable contextMap = this.execVtable.contextMap;
		List<RegInfo> regMap = is64BitLongMode ? this.execVtable.contextMap.regMap64 : this.execVtable.contextMap.regMap32;
		
		context.registerValuesBuffer.position(0); // enforce start of buffer
		for(int i = 0; i < this.execVtable.contextMap.elementCount; i++){
			RegInfo regInfo = regMap.get(i);
			
			if(regInfo.size == 0){
				// Size of 0 or offset of 0xFFFFFF indicates it is not part of this 32/64 bit context
				continue;
			}
			
			if(registerContents.length() != 0){
				registerContents.append(" ");
			}
			
			String registerName = contextMap.strName[i];
			// TODO Should we skip any register that is not the absolute largest enclosing,
			// since we want to display registers as a single value with partitioned sections?
			// No...the new register view does separate subregisters, so we will do so here too.
			registerContents.append(registerName+"=");
			
			// Slice cuts at current position (which should be 0 here).
			ByteBuffer slice = context.registerValuesBuffer.slice();
			slice.position(regInfo.offset).limit(regInfo.offset + regInfo.size);
			System.out.println("Do we need value subsizing? Slicing: "+slice+" with offset "+regInfo.offset+" and size "+regInfo.size);
			if(registerName.toLowerCase().contains("flags")){
				String flagString = MemoryDeltaTree.convertHexFlagToLetterCodeFlags(toHex(slice));
				registerContents.append(flagString);
			} else {
				registerContents.append(toHex(slice));
			}
		}
		
		// try clearing to improve memory usage
		context.registerValuesBuffer.clear();
		
		// Now collect memory references
		// this.rehydrateRegisterMemoryReferences(registerContents, context, addressToSkip);
		// We no longer do this, because some of the memory pointed to by the registers is gigantic.
		// In one Adobe Acrobat example, an early string was 12,890,112 characters long.
		
		return registerContents;
	}
	
	/**
	 * We no longer do this, because some of the memory pointed to by the registers is gigantic.
	 * In one Adobe Acrobat example, an early string was 12,890,112 characters long.
	 * 
	 * @param registerContents
	 * @param context
	 * @param addressToSkip
	 */
	@Deprecated
	private void rehydrateRegisterMemoryReferences(StringBuilder registerContents, RmContext context, Long addressToSkip){
		// Now collect memory references
		for(ContextMem registerEntry: context.memRefs){
			if(null != addressToSkip && addressToSkip == registerEntry.address){
				continue;
			}
			if(registerContents.length() != 0){
				registerContents.append(" ");
			}
			/*
			if(registerEntry.size > 64){
				System.out.println("Big register contents");
			}
			*/
			registerEntry.performRead(null, null);
			registerContents.append("["+toHex(registerEntry.address, 8)+"]=");
			registerContents.append(registerEntry.memoryHexValue);
		}
	}
	
	public void close() {
		// TODO Auto-generated method stub
		// Is there anything that needs to be done?
	}
	
	static public String toHex(Long number) {
		return toHex(number, 0);
	}
	
	static public String toHex(Long number, int byteWidth) {
		if(null == number){
			return null;
		}
		// String res = Long.toHexString(number);
		String padFormat;
		if(byteWidth > 0){
			padFormat = "0"+byteWidth*2;
		} else {
			padFormat = "";
		}
		String res = String.format("%"+padFormat+"X", number);
		return res;
	}
	
	public static String toHex(BigInteger address, int byteWidth) {
		String res = StringUtils.leftPad(address.toString(16), byteWidth, "0").toUpperCase();
		return res;
	}
	
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String toHex(ByteBuffer byteBuffer) {
		return toHex(byteBuffer, false);
	}
	
	/**
	 * 
	 * 
	 * @param byteBuffer
	 * @param removeReservedBitsLegacy It is currently for data comparison as well as for consideration of whether flags should be raw or processed in the DB, that is, with or without reserved bits present.
	 * @return
	 */
	public static String toHex(ByteBuffer byteBuffer, boolean removeReservedBitsLegacy) {
		int remaining = byteBuffer.remaining();
		// Each byte is two hex characters.
		char[] hexChars = new char[remaining * 2];
		
		// ByteBuffer does not reverse the buffer, it uses the endianness
		// only for the decoding methods (getChar, getInt, etc).
		// To get "little endian bytes", treating the entire buffer as
		// an atomic little-endian unit, we need to walk the buffer in
		// reverse order.
	    for ( int j = 0; j < remaining; j++ ) {
	    	int i = remaining - 1 - j;
	        int v = byteBuffer.get(i) & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4]; // Use shifting because this mask fails on some data: hexArray[v & 0xF0];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    
	    long intValue = 0;
	    // Old DB had flag reserved bits removed. If we need to compare against older ones ever, set this flag.
		// NB Certain bits are reserved (i.e. undefined semantics), and those are the 1, 3, 5, and 15 bits.
		// In the DB, we will see values like 0x246, and this is the true value according to the binary,
		// but it has more information than we will use in the UI (where we will mask that to get the CPAZSTIDO values
		// at indices 0,2,4,6,7,8,9,10,11,12,13, and 14). See flagPositions hashmap somewhere for more details.
	    if(removeReservedBitsLegacy){
	    	// This is just for verifying the DB against an older one, trying to verify that flag differences are
	    	// entirely due to reserved bit differences
	    	int hexCharsLength = hexChars.length;
	    	intValue = Integer.parseInt(new String(hexChars), 16);
	    	// reserved are 0x2, 0x8, 0x20
	    	intValue &= reservedRFlagBitMask; // 0x20+0x8+0x2;
	    	hexChars = Long.toHexString(intValue).toCharArray();
	    	String str = new String(hexChars);
	    	str = String.format("%"+hexCharsLength+"s", str).replace(' ', '0');
	    	return str;
	    }
	    
	    return new String(hexChars);
	}
	static long reservedRFlagBitMask = Long.parseLong("FFFFFFFF", 16) - (0x20+0x8+0x2);
	
	public static String toHex(byte byteVal) {
		char[] hexChars = new char[2];
		
        int v = byteVal & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
	    return new String(hexChars);
	}
}
