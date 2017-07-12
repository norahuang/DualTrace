package ca.uvic.chisel.atlantis.deltatree;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.database.DbConnectionManager.TableState;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatFileBackend;
import ca.uvic.chisel.atlantis.bytecodeparsing.BinaryFormatParser;
import ca.uvic.chisel.atlantis.bytecodeparsing.base.RegisterNames;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMapItable;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMem;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecLineType;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.RmContext;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ContextMapItable.RegInfo;
import ca.uvic.chisel.atlantis.bytecodeparsing.execution.ExecRec.FlagValues;
import ca.uvic.chisel.atlantis.bytecodeparsing.externals.SyscallRec;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.DecodedIns;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.ExpOper;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.LcSlot;
import ca.uvic.chisel.atlantis.bytecodeparsing.instruction.DecodedIns.SpecialSlotIndex;
import ca.uvic.chisel.atlantis.database.MemoryDbConnection;
import ca.uvic.chisel.atlantis.datacache.IdGenerator;
import ca.uvic.chisel.atlantis.datacache.TraceLine;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;
import ca.uvic.chisel.atlantis.models.PostDBMemoryReferenceValue;
import ca.uvic.chisel.atlantis.models.PostDBRegistryValue;
import ca.uvic.chisel.atlantis.models.PreDBMemoryReferenceValue;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.bfv.intervaltree.Interval;
/**
 * @author Patrick Gorman
 * 
 * This class constructs a tree that can efficiently be used to determine 
 * the current state of memory or registers in a trace. Each leaf node in 
 * the tree represents a line in the file where a memory location is changed.  
 * Each internal node is an aggregation of all of its child nodes, in terms 
 * of changes as well as line numbers.
 * 
 * Each node in the tree keeps track of a Delta, which contains MemoryReferences, 
 * each of which represents a single change to a single memory location. A parent 
 * Node contains the Delta that is the end result of applying all of its child Deltas 
 * in order.
 * 
 * Although there used to be a grand root node, that contained the end result of all
 * deltas from the first line to the last, it was not necessary for any functional
 * reason. It was removed to preserve runtime memory during processing, which was
 * significant with larger memory footprints in larger trace files.
 * 
 * Note: This class is tested in the TestMemoryDeltaTree class, and any change to the 
 * file should still pass all of those tests.
 *
 */
public class MemoryDeltaTree  implements IFileContentConsumer<TraceLine> {
	
	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();

	private static final int DEFAULT_WORD_SIZE = 1;

	private static final String WORD_SIZE_STRING = "WS";
	
	public static final int TREE_HEIGHT = 4;

	public class MemoryDeltaTreeNode {
		
		int startLine;
		int endLine;
		int id;
		
		//leaf nodes have a childCount of -1
		private int childCount = -1;
		private Delta delta;
		private boolean committedToDb = false;
		private final int level;
		
		protected MemoryDeltaTreeNode parentNode = null;
		
		/**
		 * Constructor for non-leaf nodes
		 */
		public MemoryDeltaTreeNode(int id, int startLine, int endLine, int level) {
			this.startLine = startLine;
			this.endLine = endLine;
			
			childCount = 0;
			delta = new Delta();

			this.level = level;
			
			this.id = id;
		}
		
		/**
		 * This is the constructor for leaf nodes.
		 */
		private MemoryDeltaTreeNode(MemoryReference reference, int id) {
			startLine = reference.getLineNumber();
			endLine = startLine;
			
			delta = new Delta();
			addMemRefToLeaf(reference);
			
			this.level = 0;

			this.id = id;
		}
		
		/**
		 * Returns true if this node is a leaf node and false otherwise.
		 */
		public boolean isLeafNode() {
			return this.level == 0;
		}
		
		protected void setParentNode(MemoryDeltaTreeNode parentNode) {
			this.parentNode = parentNode;
		}
		
		public MemoryDeltaTreeNode getParentNode() {
			return this.parentNode;
		}
		
		/**
		 * Checks to see if the node already has {@code maxNodeChildren} children
		 */
		public boolean isFull() {
			
			if(isLeafNode()) {
				return true;
			}
			
			return childCount >= MemoryDeltaTree.this.branchOutFactor;
		}

		/**
		 * Adds a new child to this node, and updates the startLine and endLine values accordingly.
		 * 
		 * This method assumes that the children list is not full.
		 */
		public void addChild(MemoryDeltaTreeNode newNode) {
			childCount++;
			
			updateStartAndEndLine(newNode);
			updateDelta(newNode);
			
			newNode.setParentNode(this);
		}
		
		/**
		 * Combines this nodes delta with the delta of a recently added child node.
		 */
		private void updateDelta(MemoryDeltaTreeNode newNode) {
			this.delta.combineDeltas(newNode.delta);
			
			// TODO Can't we save effort by only updating when we are committing a delta
			// to the DB? That way huge parent ones don't get updated the same number of
			// times as their children. Then we'd get the complete internal node to update the
			// larger parent in one fell swoop.
			if(parentNode != null) {
				parentNode.updateDelta(newNode);
			}
		}

		/**
		 * Updates the startLine and endLine of the current node to take into account the new
		 * node being added to the children
		 */
		public void updateStartAndEndLine(MemoryDeltaTreeNode memoryDeltaTreeNode) {
			if(startLine == -1 || (memoryDeltaTreeNode.startLine < this.startLine && memoryDeltaTreeNode.startLine != -1)) {
				this.startLine = memoryDeltaTreeNode.startLine;
			}
			
			if(endLine == -1 || (memoryDeltaTreeNode.endLine > this.endLine && memoryDeltaTreeNode.endLine != -1)) {
				this.endLine = memoryDeltaTreeNode.endLine;
			}
			
			if(parentNode != null) {
				parentNode.updateStartAndEndLine(memoryDeltaTreeNode);
			}
		}
		
		/**
		 * Returns true if the line given falls within the start and end line of the node.
		 * false otherwise.
		 */
		public boolean containsLine(int lineNumber) {
			return startLine <= lineNumber  && lineNumber <= endLine;
		}
		
		/**
		 * This method attempts to add the new memory reference to the delta of a child node.
		 * If the node is not a leaf, then this method will do nothing.
		 * @param memRef
		 */
		public void addMemRefToLeaf(MemoryReference memRef){
			if(!isLeafNode()) {
				return;
			}
			
			delta.addReference(memRef);
		}
		
		public int getId() {
			return id;
		}
		
		public int getStart() {
			return startLine;
		}
		
		public int getEnd() {
			return endLine;
		}
		
		/**
		 * This method is for testing purposes only
		 */
		public Interval getNodeInterval() {
			return new Interval(startLine, endLine);
		}
		
		/**
		 * This method is for testing purposes only
		 */
		public Delta getDelta() {
			return delta;
		}

		/**
		 * Flags as committed, and allow GC to occur on the potentially large delta.
		 * Parent nodes have copies, so collecting this here is safe.
		 */
		public void setCommitted() {
			this.committedToDb = true;
			this.delta = null;
		}
		
		public boolean getCommitted(boolean committedToDb) {
			return this.committedToDb;
		}

		public int getLevel(){
			return this.level;
		}
	}
	
	private static final int DEFAULT_MAX_NODE_CHILDREN = 1000;
	
	private final int branchOutFactor;
	
	private IdGenerator idGenerator;
	
	/**
	 * Only used to access the maximum trace line count, thus the funny name.
	 */
	private BinaryFormatFileBackend maxLineProvider;
	
	/**
	 * This method assumes that the Memory References are sorted by their line numbers, which is true
	 * if they have come from the FileModelDAO
	 * @throws Exception 
	 */
	public MemoryDeltaTree(MemoryDbConnection memoryAccessDatabase, BinaryFormatFileBackend binaryFileBackend){
		this(memoryAccessDatabase, DEFAULT_MAX_NODE_CHILDREN, binaryFileBackend);
	}
	
	public MemoryDeltaTree(MemoryDbConnection memoryAccessDatabase, int branchOutFactor, BinaryFormatFileBackend binaryFileBackend) {
		this.branchOutFactor = branchOutFactor;
		this.memoryAccessDatabase = memoryAccessDatabase;
		this.idGenerator = new IdGenerator(branchOutFactor, TREE_HEIGHT);
		this.maxLineProvider = binaryFileBackend;
	}
	
	private Pattern registersPattern = Pattern.compile("(\\w+)\\=([\\d\\w]*)");
	private static Pattern memoryPattern = Pattern.compile("\\[([\\d\\w]+)\\]\\=([\\d\\w]+)");
	
	public static Matcher matchMemoryChangesForline(String line){
		return MemoryDeltaTree.memoryPattern.matcher(line);
	}
	
	// positions of flags (x86/x64) in the flags register by character id
	private static Map<Character, Integer> flagPositions;
	
	// the values of the registers as execution progresses
	// used to determine values for partial register writes
	// all values 64 bit
	private Map<String, String> registerValues;
	
	// the widths of registers (-8 is used for the high bits of the 16 bit registers)
	private static Map<String, Integer> registerBitWidths;
	
	// the 64 bit equivalents of registers
	private static Map<String, String> registerEquivalents;
	
	// Current line is 0 indexed
	private int currentLine;

	private List<MemoryDeltaTreeNode> headNodeAtEachLevel;
	private List<Integer> totalNodeCountAtLevels;
	
	private final MemoryDbConnection memoryAccessDatabase;
	
	@Override
	public boolean verifyDesireToListen(){
		memoryAccessDatabase.createTables();
		boolean b = TableState.CREATED == memoryAccessDatabase.getTableState();
		return b;
		
	}
	
	@Override
	public void readStart() {
		currentLine = 0;
		headNodeAtEachLevel = new ArrayList<MemoryDeltaTreeNode>(TREE_HEIGHT);
		totalNodeCountAtLevels = new ArrayList<Integer>(TREE_HEIGHT);
		for(int i = 0; i < TREE_HEIGHT; i++){
			headNodeAtEachLevel.add(i, null);
			totalNodeCountAtLevels.add(i, 0);
		}
		initializeRegisters();
	}

	boolean printMyDebugs = false;
	
	@Override
	public int handleInputLine(String line, TraceLine lineData) {
		// To support PIN Tool generated and older traces, we have a legacy method.
		// This new method will make use of the structured data of the binary formats.
		if(null == lineData){
			this.handleInputLineLegacy(line);
			return 1;
		}
		
		timeAccumulator.progCheckIn();
		
		if(printMyDebugs) System.out.println();
		if(printMyDebugs) System.out.println();
		if(printMyDebugs) System.out.println(line);
		MemoryDeltaTreeNode newLeaf = null;
		
		RmContext context;
		ExecLineType type = lineData.execRec.type;
		if(lineData.execRec.type == ExecLineType.SYSCALL_EXIT){
			if(-1 == lineData.execRec.syscallExitId){
				// -1 id is equivalent to a context switch, there is no syscall record to retrieve.
				// S:ContextSwitch
				context = null;
			} else {
				context = lineData.execRec.sysCallExitContext;
			}
		} else if(type == ExecLineType.SYSCALL_ENTRY){
			context = lineData.execRec.syscallEntryContext;
		} else if (type == ExecLineType.CONTEXT_CHANGE_UNKNOWN_REASON || type == ExecLineType.CONTEXT_CHANGE_CALLBACK
				|| type == ExecLineType.CONTEXT_CHANGE_EXCEPTION_HANDLING || type == ExecLineType.CONTEXT_CHANGE_ASYNC_PROC_CALL){
				context = lineData.execRec.contextChange;
		} else {
			context = lineData.context;
		}
		
		newLeaf = handleInputLineRegisterLocations(newLeaf, lineData, context, lineData.threadInformationBlockSkipAddress);
		newLeaf = handleInputLineMemoryLocations(newLeaf, lineData);
		
		// Our current strategy does not support lines that are missing memory information, simply add a blank node
		if(newLeaf == null) {
			newLeaf = new MemoryDeltaTreeNode(idGenerator.getId(0, currentLine), currentLine, currentLine, 0);
		}
		
		addNodeToLevel(newLeaf, 0);
		
		currentLine++;
		
		timeAccumulator.checkOutAll();
		
		return 0;
		
	}
	
	/**
	 * This code is related to the older legacy rehydrateRegisterMemoryContents().
	 * 
	 * @param lineData
	 */
	private MemoryDeltaTreeNode handleInputLineRegisterLocations(MemoryDeltaTreeNode newLeaf, TraceLine lineData, RmContext context, Long addressToSkip){
			if(null == context){
				if(printMyDebugs) System.out.println("stringifyOperVal offspring named processMemoryChangesForLine() should handle this, I think...");
				return newLeaf;
			}
			
			// Flags, or RFLAGS
			newLeaf = getFlagsRegisters(lineData, context, newLeaf);
			
			// Collect RSP register from syscall argument records
			// This is in a quite different structure than in regular register usage for instructions.
			if(lineData.execRec.type == ExecLineType.SYSCALL_ENTRY){
				newLeaf = this.getSyscallRegisters(lineData.execRec.syscallEnterId, lineData, newLeaf);
			}
			if(lineData.execRec.type == ExecLineType.SYSCALL_EXIT){
				newLeaf = this.getSyscallRegisters(lineData.execRec.syscallExitId, lineData, newLeaf);
			}
			
			// Now collect memory references
			newLeaf = collectMemoryReferences(lineData, context, newLeaf, addressToSkip);
			
			// all registers have been processed, update register values
			if(newLeaf != null){
				for(MemoryReference register : newLeaf.getDelta().getRegisterReferences()) {
					if(register.getType() == EventType.REGISTER){
						registerValues.put(register.getRegName(), register.getMemoryContent().getMemoryValue());
					}
				}
			}
		
			return newLeaf;
	}
	
	/**
	 * Collect RSP or RAX registers, and values, depending on whether this is a entry or exit point for a syscall.
	 * Also, in case it is a 32-bit instruction or not, use ESP or EAX registers instead.
	 * 
	 * @param syscallEnterExitId
	 * @param lineData
	 * @param newLeaf
	 * @return
	 */
	private MemoryDeltaTreeNode getSyscallRegisters(long syscallEnterExitId, TraceLine lineData, MemoryDeltaTreeNode newLeaf){
		if(-1 == syscallEnterExitId){
			// -1 id is equivalent to a context switch, there is no syscall record to retrieve.
			return newLeaf;
		} 
		
		SyscallRec syscallRec = lineData.syscallVtable.getSyscallRec(syscallEnterExitId);
		
		String registerAddress;
		int hexByteWidth;
		String value;
		boolean isBefore;
		if(FlagValues.flag64BitInstruction.hexIntValue == (FlagValues.flag64BitInstruction.hexIntValue & lineData.execRec.flags)){
			// Entry ones will have RSP/ESP, exit ones will have RAX/EAX
			registerAddress = lineData.execRec.type == ExecLineType.SYSCALL_ENTRY ? "RSP" : "RAX";
			hexByteWidth = 8;
		} else {
			registerAddress = lineData.execRec.type == ExecLineType.SYSCALL_ENTRY ? "ESP" : "EAX";
			hexByteWidth = 4;
		}
		if(lineData.execRec.type == ExecLineType.SYSCALL_ENTRY){
			value = BinaryFormatParser.toHex(syscallRec.beforeSp, hexByteWidth);
			isBefore = true;
		} else {
			value = BinaryFormatParser.toHex(syscallRec.result, hexByteWidth);
			isBefore = false;
		}
		
		// Some 64-bit calls have an unknown number of arguments, in which case we will apparently always receive
		// four arguments in the data. Otherwise, we have between 0 and 4 arguments.
		// For the memory delta tree, we do not want to store the syscall args. But I do want this code present here,
		// in case we want it later. It came from a class that might be refactored and deleted eventually.
		//			int i = 0;
		//			for(Long arg: syscallRec.args){
		//				// Print hex at 64-bit (16 hex) width. 32-bit are zero extended in the data.
		//				// TODO We might check the convention property of the syscallRec to see
		//				// how wide the argument should be.
		//				argString.append("arg"+i+"="+toHex(arg, hexByteWidth)+" ");
		//				i++;
		//			}
		
		PostDBRegistryValue memRefVal = new PostDBRegistryValue(registerAddress, value.length()/2, value);
		MemoryReference memRef = new MemoryReference(registerAddress, null, memRefVal, currentLine, EventType.REGISTER, isBefore);
		if(newLeaf == null) {
			newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
		} else {
			newLeaf.addMemRefToLeaf(memRef);
		}
		
		return newLeaf;
	}
	
	/**
	 * This may be dead code. Check it out, remove it if so.
	 * Very likely dead, I have not seen the 'dead code' sysout inside it yet.
	 */
	@Deprecated
	private MemoryDeltaTreeNode getFlagsRegisters(TraceLine lineData, RmContext context, MemoryDeltaTreeNode newLeaf){
		// Collect *relevant* register values
		boolean is64BitLongMode = false; // get this from somewhere
		ContextMapItable contextMap = lineData.execVtable.contextMap;
		List<RegInfo> regMap = is64BitLongMode ? contextMap.regMap64 : contextMap.regMap32;
		
		context.registerValuesBuffer.position(0); // enforce start of buffer
		for(int i = 0; i < contextMap.elementCount; i++){
			RegInfo regInfo = regMap.get(i);
			
			if(regInfo.size == 0){
				// Size of 0 or offset of 0xFFFFFF indicates it is not part of this 32/64 bit context
				continue;
			}
			
			System.out.println("This is not dead code");
			
			String registerName = contextMap.strName[i];
			// TODO Should we skip any register that is not the absolute largest enclosing,
			// since we want to display registers as a single value with partitioned sections?
			// No...the new register view does separate subregisters, so we will do so here too.
			
			String key;
			String value;
			// Slice cuts at current position (which should be 0 here).
			ByteBuffer slice = context.registerValuesBuffer.slice();
			slice.position(regInfo.offset).limit(regInfo.offset + regInfo.size);
			System.out.println("Do we need value subsizing? Slicing: "+slice+" with offset "+regInfo.offset+" and size "+regInfo.size);
			String regName = registerName.toLowerCase();
			if(regName.contains("flags")){
				System.out.println("flag detected, so this might not be dead code");
				if(!regName.equalsIgnoreCase("rflags") && !regName.equalsIgnoreCase("eflags") && !regName.equalsIgnoreCase("flags")){
					System.err.println("Non-flags entry detected with 'flags' in it: '"+regName+"'");
				}
				key = "RFLAGS"; // TODO kinda crufty, is there a better way?
				/*
		          if(key.equalsIgnoreCase("rflags") || key.equalsIgnoreCase("eflags") || key.equalsIgnoreCase("flags")) {
						key = "RFLAGS";
						int flagsValue = 0;
						
						for(Map.Entry<Character, Integer> flag : flagPositions.entrySet()) {
							if(value.indexOf(flag.getKey().toString()) != -1) {
								flagsValue |= flag.getValue();
							}
						}
						
						// shouldn't this be %08? Why divide 32 bits by 2 and the rest by 4 when looking for hex widths, which are 4 bits a piece?
						// is flags here in hex, if so, is it 32 bit or 64?
						String flagsValueString = String.format("%016x", flagsValue);
						
						memRef = new MemoryReference(key, flagsValueString, currentLine, EventType.REGISTER);
					}
				 */
				System.out.println("This flag processing might not work, if not consider the above comment chunk");
				String flagString = MemoryDeltaTree.convertHexFlagToLetterCodeFlags(BinaryFormatParser.toHex(slice));
				value = flagString;
			} else {
				key = registerName;
				value = BinaryFormatParser.toHex(slice);
			}
			if(printMyDebugs) System.out.println("Line: "+currentLine+" - "+key+"="+value.toString()+" (register processing)");
	
			boolean isBefore = false;
			PostDBRegistryValue memRefVal = new PostDBRegistryValue(key, value.length()/2, value);
			MemoryReference memRef = new MemoryReference(key, key, memRefVal, currentLine, EventType.REGISTER, isBefore);
			if(newLeaf == null) {
				newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
			} else {
				newLeaf.addMemRefToLeaf(memRef);
			}
		}
		
		// try clearing to improve memory usage
		context.registerValuesBuffer.clear();
		
		return newLeaf;
	}
	
	private MemoryDeltaTreeNode collectMemoryReferences(TraceLine lineData, RmContext context, MemoryDeltaTreeNode newLeaf, Long addressToSkip){
		for(ContextMem memoryEntry: context.memRefs){
			if(null != addressToSkip && addressToSkip == memoryEntry.address){
				continue;
			}

			String memoryAddress = BinaryFormatParser.toHex(memoryEntry.address, 0); //, 8);
			memoryAddress = memoryAddress.toLowerCase();
			long memoryByteWidth = memoryEntry.size;
			boolean isBefore = false;
			PreDBMemoryReferenceValue memRefVal;
			
			if(memoryByteWidth <= 8){
				// smaller than 8 bytes? Then it's ok to read it and not use the JIT version.
				memoryEntry.performRead(null, null);
				String value = memoryEntry.memoryHexValue;
				
				if(printMyDebugs) System.out.println("Line: "+currentLine+" - "+memoryAddress+"="+value+" (register memory references)");

				memRefVal = new PostDBMemoryReferenceValue(memoryEntry.address, memoryByteWidth, value);
			} else {
				memRefVal = new PreDBMemoryReferenceValue(memoryEntry.address, memoryByteWidth, memoryEntry);
			}
			
			MemoryReference memRef = new MemoryReference(memoryEntry.address, memRefVal, currentLine, EventType.MEMORY, isBefore);
			if(newLeaf == null) {
				newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
			} else {
				newLeaf.addMemRefToLeaf(memRef);
			}
		}
		return newLeaf;
	}
	
	private MemoryDeltaTreeNode handleInputLineMemoryLocations(MemoryDeltaTreeNode newLeaf, TraceLine lineData){
		// Might ask for memory changes on lines that are not instructions
		if(lineData.execRec.insRec == null){
			return newLeaf;
		}
		DecodedIns decodedIns = lineData.execRec.insRec.decodedIns;
		newLeaf = this.processMemoryChangesForLine(decodedIns, newLeaf);
		
		return newLeaf;
		
	}
	
	static HashMap<String, Boolean> metaRegisters = new HashMap<String, Boolean>();
	static{
		//metaRegisters.put("GSBASE", true);
		//metaRegisters.put("FSBASE", true);
		metaRegisters.put("MEM0", true);
		metaRegisters.put("MEM1", true);
		metaRegisters.put("INVALID", true);
		metaRegisters.put("STACKPOP", true);
		metaRegisters.put("STACKPUSH", true);
		metaRegisters.put("AGEN", true);
	}

	private class RegisterCollated{
		public String key;
		public String value;
	}
	
	private RegisterCollated collateSubregisterContents(String memoryOperandSlotOrName, String memoryOperandValue, EventType eventType, String commonName){
		String key = memoryOperandSlotOrName;
		String value = memoryOperandValue;
		key = key.toUpperCase(Locale.US);
		Integer bitWidth = registerBitWidths.get(key);
		if(null != registerEquivalents.get(key)){
			key = registerEquivalents.get(key);
		}
		String previousValue = registerValues.get(key);
		
		if(null == bitWidth){
			// I do not have widths of all registers in XED collection...do we actually need them explicit,
			// or now that we have the binary format, can we simply trust the value's length?
			bitWidth = value.length()*4;
		}
		
		if(null == previousValue){
			// Seems redundant when we get length from the value size, but was meant for registers with known size.
			// This step might not be relevant if binary format data is always zero extended for the relevant register.
			registerValues.put(key, String.format("%"+(bitWidth/4)+"s", memoryOperandValue).replace(" ",  "0"));
			previousValue = registerValues.get(key);
		}
		
		
		// This only works for 
		Integer canaryOnlyPreviousBitWidth = null;
		boolean fsgsBaseExempt = false;
		// This canary is not hugely important. It is meant to detect and bring developer attention to
		// possibly valid but unexpected variations in data width.
		// FSBASE and GSBASE added on the basis of seeing actual 32 and 64 bit reads of them in the same trace
		if(memoryOperandSlotOrName.equals("GSBASE") || memoryOperandSlotOrName.equals("FSBASE")){
			fsgsBaseExempt = true;
		} else if(!(eventType == EventType.REGISTER && !memoryOperandSlotOrName.equals("AGEN"))
				&& !memoryOperandSlotOrName.equals("RFLAGS")){ // the second instance of this, not the first
			canaryOnlyPreviousBitWidth = null;
		} else if(null != registerValues.get(key)){
			canaryOnlyPreviousBitWidth = registerValues.get(key).length()*4; // separate only because of the issue of enclosing registers.
		} else {
			canaryOnlyPreviousBitWidth = bitWidth; // Can't compare, so make the same.
		}
		
		int previousBitWidth = previousValue.length()*4;
		
		// This whole switch might be moot; the binary format probably has full width data always, whereas the legacy text format did not.
		// Adding tests to report when there is actually a width difference.
		// This canary differs from the similar one above because we only ever find registers that have enclosing-register entries here.
		// Therefore, the difference between the previousValue width and the current value very often necessarily differs.
		if(value.length() != Math.abs(bitWidth)/4 && !fsgsBaseExempt){
			// Canary
			if(null != canaryOnlyPreviousBitWidth){
				System.out.println("Line "+currentLine+" register "+memoryOperandSlotOrName +"("+commonName+")"+" binary format raw entry ("+value+",bit length "+value.length()*4+") does not match bit width ("+bitWidth+") expected, or previous width seen ("+canaryOnlyPreviousBitWidth+")");
			} else {
				System.out.println("Line "+currentLine+" register +"+memoryOperandSlotOrName+" binary format raw entry ("+value+",bit length "+value.length()*4+") does not match bit width ("+bitWidth+") expected");
			}
		}
		switch(bitWidth) {
			case 64: {
				// Noop. Has all its bytes, needs not be combined.
			} break;
			case 32: {
				// Lower 32 bits of 64 bit register (eax of rax), need to keep upper bits
				// shouldn't this be %08? Why divide 32 bits by 2 and the rest by 4 when looking for hex widths, which are 4 bits a piece?
				value = String.format("%08X", Long.parseLong(value, 16));
				value = previousValue.substring(0, (previousBitWidth - 32)/4) + value;
			} break;
			case 16: {
				// Lower 16 bits of a register (ax vs eax vs rax), need to keep upper bits
				value = String.format("%04X", Long.parseLong(value, 16));
				value = previousValue.substring(0, (previousBitWidth - 16)/4) + value;
			} break;
			case 8: {
				// Low bits in 16 bit register (ax's al)
				value = String.format("%02X", Long.parseLong(value, 16));
				value = previousValue.substring(0, (previousBitWidth - 8)/4) + value;
			} break;
			case -8: {
				// High bits in 16 bit register (ax's ah)
				value = String.format("%02X", Long.parseLong(value, 16));
				value = previousValue.substring(0, (previousBitWidth - 16)/4) + value + previousValue.substring((previousBitWidth - 8)/4, (previousBitWidth - 0)/4);
			} break;
		}
		
		RegisterCollated res = new RegisterCollated();
		res.key = key;
		res.value = value;
		return res;
	}
	
	/**
	 * Related to the legacy method {@link BinaryFormatParser#stringifyOperVals()} (private method),
	 * which does the heavy lifting for that class's {@link BinaryFormatParser#getMemoryChangesForLine(int)}
	 * method.
	 * 
	 * @param decodedIns
	 * @param newLeaf
	 */
	public MemoryDeltaTreeNode processMemoryChangesForLine(DecodedIns decodedIns, MemoryDeltaTreeNode newLeaf){
		List<Integer> usedSlotIndices = new ArrayList<Integer>();
		for(int i = 0; i < decodedIns.expOpers.length; i++){
			ExpOper oper = decodedIns.expOpers[i];
			// Funky loop to prevent code duplication
			boolean foundAfterSlot = oper.afterSlotObject != null;
			for(int beforeSlotsOrAfter = 0; beforeSlotsOrAfter <= 1; beforeSlotsOrAfter++){
				boolean keepBeforeOperandsOnly = !foundAfterSlot || (beforeSlotsOrAfter == 0);
				if(keepBeforeOperandsOnly && beforeSlotsOrAfter == 1){
					continue;
				}
				LcSlot lcSlot;
				Integer slotIndex;
				if(keepBeforeOperandsOnly){
					lcSlot = oper.beforeSlotObject;
					slotIndex = oper.beforeSlot;
				} else {
					lcSlot = oper.afterSlotObject;
					slotIndex = oper.afterSlot;
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
				
				if(null != lcSlot && (
					(keepBeforeOperandsOnly && lcSlot.isAnAfterWriteOperand)
					|| (!keepBeforeOperandsOnly && (lcSlot.isABeforeWriteOnlyOperand || lcSlot.isABeforeReadOrRWOperand))
					)){
					// If it is a write after operand, skip
					System.out.println("I don't think we should ever get here anymore");
					continue;
				}
				
				// 1) Determine the memory address that is changing
				
				EventType eventType = null;
				String memoryOperandSlotOrName;
				/**
				 * Only need due to a funny thing done with register identity, described in a TODO below, where this is assigned.
				 */
				String subregister;
				Long memoryOperandMemoryAddress = null; // only for memory addresses
				String memoryOperandValue;
				// TODO The documentation says that for Memory Operands (the special reg names like MEM0),
				// we can find their memory addresses and their values in the *execution record*. That is
				// their pre-execution value though, so I likely need what I find in the LcSlots anyway.
				// But this can confirm my values if it is indeed stored in both places!
				if(null != constVal){
					// If these are really immediate values, they cannot be written to, and aren't memory, so we
					// shouldn't be including them in memory output.
					memoryOperandSlotOrName = constSlot;
				} else if(lcSlot.hasMemoryLocationAvailable()){
					// MEM0 and MEM1
					memoryOperandSlotOrName = lcSlot.memoryLocationString;
					memoryOperandMemoryAddress = lcSlot.memoryLocation;
					eventType = EventType.MEMORY;
				} else {
					// Basic register reference, so no memory address to print
					// Note we have diff reg name in LcSlot, and the enclosing register name:
					// lcSlot.regNameString, lcSlot.regNameLargestEnclosingString
					memoryOperandSlotOrName = oper.regNameString;
					eventType = EventType.REGISTER;
					
					// I think I need to *not* use the string version, that is for humans only...
					// I used to use this the swithc case on register widths, as seen elsewhere.
					
				}
				
				// 2) Determine the value that the above memory address is being given.
				
				// Apply values to the registers/memory addresses
				// Register...or special value! We don't let some special values get here, and deal with them
				// below in the "orphan" LcSlot processing.
				if(null != constVal){
					memoryOperandValue = BinaryFormatParser.toHex(constVal);
				} else if(lcSlot.regNameString == RegisterNames.SpecRegs.AGEN.regName){
					// Legacy parsing of AGEN entries produced a string with ':' instead of '=', and this served to distinguish them so that we didn't enter them into the memory DB.
					// We don't need them in the memory DB because there is not an actual register named AGEN nor does it affect a memory location.
					// That being said...it might be very handy to have this value pre-computed during analysis, so I have changed things to compute and store AGEN, even though it is
					// a trace fabrication.
					// AGEN same as MEM0 in some ways
					String effectiveAddress = BinaryFormatParser.toHex(lcSlot.mem0EffectiveAddress, 8);
					if(null == effectiveAddress){
						System.out.println("Null found for: "+lcSlot.regNameString+": "+lcSlot.mem0MemoryPhrase);
					}
					memoryOperandValue = effectiveAddress;
				} else {
					// Section 7.2.3 says that AGEN does not have memory value, unlike otherwise similar MEM0.
					// It is also only used for the lea instruction, which can offer some validation.
					String value;
					if(keepBeforeOperandsOnly){
						value = oper.regMemoryBeforeValueHexString;
					} else {
						value = oper.regMemoryAfterValueHexString;
					}
					
					memoryOperandValue = value;
				}
				
				// This part raises the register up to the enclosing register's identity and value.
				// Given that this works, we could keep it. But I find it misleading, and it ultimately
				// make the code more complicated while actually *removing* information from the database.
				// That is, if we later want to know the exact register and value change, it is hidden
				// inside the statement that the enclosing register changed.
				// TODO Fix things to not require this.
				subregister = memoryOperandSlotOrName; // just need for some debugging in delta merging code. Not critical as long as collateSubregisterContents is still in use.
				if(eventType == EventType.REGISTER && !memoryOperandSlotOrName.equals("AGEN")){
					RegisterCollated res = collateSubregisterContents(memoryOperandSlotOrName, memoryOperandValue, eventType, lcSlot.regNameCommonRegNameString);
					memoryOperandSlotOrName = res.key;
					memoryOperandValue = res.value;
				}
				
				if(null != eventType){
					// Basically, this skips immediate values/constants.
					if(printMyDebugs) System.out.println("Line: "+currentLine+" - "+memoryOperandSlotOrName+"="+memoryOperandValue+" ("+eventType+" for mem proc expOper section, "+(keepBeforeOperandsOnly ?  "no after slot" : "skipped before slot, used after slot) ")+" index: "+slotIndex);
					boolean isBefore = lcSlot.isABeforeReadOrRWOperand || lcSlot.isABeforeWriteOnlyOperand;
					MemoryReference memRef;
					if(eventType == EventType.MEMORY){
						PostDBMemoryReferenceValue memoryRefVal = new PostDBMemoryReferenceValue(memoryOperandMemoryAddress, memoryOperandValue.length()/2L, memoryOperandValue);
						memRef = new MemoryReference(memoryOperandMemoryAddress, memoryRefVal, currentLine, eventType, isBefore);
					} else {
						PostDBRegistryValue memoryRefVal = new PostDBRegistryValue(memoryOperandSlotOrName, memoryOperandValue.length()/2, memoryOperandValue);
						memRef = new MemoryReference(memoryOperandSlotOrName, subregister, memoryRefVal, currentLine, eventType, isBefore);
					}
					if(newLeaf == null) {
						newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
					} else {
						newLeaf.addMemRefToLeaf(memRef);
					}
				}
				
				// all registers have been processed, update register values
				if(newLeaf != null) {
					for(MemoryReference register : newLeaf.getDelta().getRegisterReferences()) {
						registerValues.put(register.getRegName(), register.getMemoryContent().getMemoryValue());
					}
				}
			}
		}
		
		
		// ----
		// Orphan LcSlot Processing (those without ExpOper, as well as stack ops)
		// ----
		
		LcSlot rspBeforeSlot = null;
		LcSlot rspAfterSlot = null;
		for(LcSlot slot: decodedIns.lcSlots){
			if(!slot.regNameString.equals("RSP") && !slot.regNameString.equals("ESP")){
				continue;
			}
			boolean isBefore = slot.isABeforeReadOrRWOperand || slot.isABeforeWriteOnlyOperand;
			if(isBefore){
				rspBeforeSlot = slot;
			} else {
				rspAfterSlot = slot;
			}
		}
		
		ArrayList<String> processedLcSlots = new ArrayList<String>();
		// Decrement loop so that we can approach after-slots first, and skip using before-slots if an after slot was found.
		// There are occasions where a value in a register or memory location can be seen in the before-slot,
		// which was not yet set in the trace as recorded. This is very likely because the tracing was not started with the
		// program.
		// TODO Do something smart with the before-slot memory values that were *not* set during tracing, like setting their previous
		// values back to the beginning of the trace.
		for(int i = 0; i < decodedIns.lcCount; i++){
			LcSlot slot = decodedIns.lcSlots.get(i);
			if(!usedSlotIndices.contains(i)
//					&& !processedLcSlots.contains(slot.regNameString)
				){
				processedLcSlots.add(slot.regNameString);
				
				boolean isBefore = slot.isABeforeReadOrRWOperand || slot.isABeforeWriteOnlyOperand;
				
				EventType eventType = null;
				String memoryOperandSlotOrName = null;
				String memoryOperandValue = null;
				
				MemoryReference memRef;
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
					memoryOperandSlotOrName = rspBeforeSlot.specialAddressForMemoryPhraseSlotsString.toLowerCase();
					BigInteger memoryOperandAddress = rspBeforeSlot.specialAddressForMemoryPhraseSlots;
					memoryOperandValue = slot.specialAddressForMemoryPhraseSlotsString;
					
					eventType = EventType.MEMORY;
					PostDBMemoryReferenceValue memoryRefVal = new PostDBMemoryReferenceValue(memoryOperandAddress.longValue(), memoryOperandValue.length()/2L, memoryOperandValue);
					memRef = new MemoryReference(memoryOperandAddress.longValue(), memoryRefVal, currentLine, eventType, isBefore);
					if(printMyDebugs) System.out.println("specialAddressForMemoryPhrase: "+memoryOperandSlotOrName+" vs regName: "+slot.regNameString);
				} else if(metaRegisters.containsKey(slot.regNameString)) {
					continue;
				} else {
					// This often (always) is where RSP shows up. It's not usually an ExpOper, understandably.
					// Includes FSBASE (from XED) and GSBASE (from XED)
					// NB: in a user-mode trace, it would be pretty useless to provide fs/gs selector values when those are
					// used as segment overrides in memory phrases. Instead, their base linear address is provided.
					// Can't seem to find the effective width of pushed values, so we probably don't need to truncate the value as done with registers.
					String value = slot.specialAddressForMemoryPhraseSlotsString;
					if(slot.regNameString.toLowerCase().contains("flags")){
						// We *DO* want this conversion, even though it will look funny in the DB??? Maybe...but comment out for testing just now
//						value = MemoryDeltaTree.convertHexFlagToLetterCodeFlags(value);
					}
					memoryOperandSlotOrName = slot.regNameString;
					memoryOperandValue = value;
					eventType = EventType.REGISTER;
					
					// RFLAGS is definitely (successfully) handed to the DB in this conditional.
					// Go over other parts that claim to do RFLAGS, disable, test, delete.
					String subRegister = memoryOperandSlotOrName;
					if(!memoryOperandSlotOrName.equals("RFLAGS")){
						RegisterCollated res = collateSubregisterContents(memoryOperandSlotOrName, memoryOperandValue, eventType, slot.regNameCommonRegNameString);
						memoryOperandSlotOrName = res.key;
						memoryOperandValue = res.value;
					}
					PostDBRegistryValue memoryRefVal = new PostDBRegistryValue(memoryOperandSlotOrName, memoryOperandValue.length()/2, memoryOperandValue);
					memRef = new MemoryReference(memoryOperandSlotOrName, subRegister, memoryRefVal, currentLine, eventType, isBefore);
				}
				
				if(null != eventType){
					if(printMyDebugs) System.out.println("Line: "+currentLine+" - "+memoryOperandSlotOrName+"="+memoryOperandValue.toString()+" ("+eventType+" memory proc for lcslots) index: "+i);
					if(newLeaf == null) {
						newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
					} else {
						newLeaf.addMemRefToLeaf(memRef);
					}
				}
			}
		}
		
		return newLeaf;
	}
	 
	
	public void handleInputLineLegacy(String line){
		
		timeAccumulator.progCheckIn();
		
		// Find the first '=', then back it up to the space before that. This will start us ad the identifier/address for the first entry.
		int start = line.indexOf('=');
		if(start == -1) start = 0;
		start = line.lastIndexOf(' ', start) + 1;
		
		MemoryDeltaTreeNode newLeaf = null;
		
		String tmpLine = new String(line.substring(start));
		
		Matcher registersMatcher = registersPattern.matcher(tmpLine);
		while(registersMatcher.find()) {
			String key = new String(registersMatcher.group(1));
			String value = new String(registersMatcher.group(2));
			MemoryReference memRef = null;
			
			// if this is not a register, continue
			if(!registerEquivalents.keySet().contains(key)
					&& !key.equalsIgnoreCase("rflags")
					&& !key.equalsIgnoreCase("eflags")
					&& !key.equalsIgnoreCase("flags")) {
				continue;
			}
			int indexOfAfterPipe = tmpLine.indexOf("|");
			boolean isBefore = registersMatcher.end() <= indexOfAfterPipe;
			
			// flag values must be translated to bits explicitly
			if(key.equalsIgnoreCase("rflags") || key.equalsIgnoreCase("eflags") || key.equalsIgnoreCase("flags")) {
				key = "RFLAGS";
				int flagsValue = 0;
				
				for(Map.Entry<Character, Integer> flag : flagPositions.entrySet()) {
					if(value.indexOf(flag.getKey().toString()) != -1) {
						// if the letter name of the flag appears in the key, add it to the flagsValue.
						flagsValue |= flag.getValue();
					}
				}
				
				// shouldn't this be %08? Why divide 32 bits by 2 and the rest by 4 when looking for hex widths, which are 4 bits a piece?
				// flags is 32 bit right? This flagsvalue is in hex, right?
				// No, FLAGS is 16 bit. EFLAGS is 32, but has generally uninteresting content. RFLAGS adds only Reserved bits. This means I should
				// probably write *exactly* what the binary format has for the value, no extending or other manipulation.
				// I can extend in the view if necessary, but I expect that the binary format files will always have full width written, as opposed
				// to the legacy text format way of handling register changes.
				// Legacy...won't fix.
				String flagsValueString = String.format("%016x", flagsValue);
				
				PostDBRegistryValue val = new PostDBRegistryValue(key, flagsValueString.length()/2, flagsValueString);
				memRef = new MemoryReference(key, key, val, currentLine, EventType.REGISTER, isBefore);
			} else {
				key = key.toUpperCase(Locale.US);
				String subRegister = key;
				int bitWidth = registerBitWidths.get(key);
				key = registerEquivalents.get(key);
				String previousValue = registerValues.get(key);
				int previousBitWidth = previousValue.length()*4;
				System.out.println("Legacy text format not tested with revised register collation.");

				// This switch is necessary for legacy, probably not for new approach. Check that.
				switch(bitWidth) {
					case 64: {
						// // LegacyNoop. Has all its bytes, needs not be combined.
					} break;
					case 32: {
						value = String.format("%08x", Long.parseLong(value, 16));
						value = previousValue.substring(0, (previousBitWidth - 32)/4) + value;
					} break;
					case 16: {
						value = String.format("%04x", Long.parseLong(value, 16));
						value = previousValue.substring(0, (previousBitWidth - 16)/4) + value;
					} break;
					case 8: {
						value = String.format("%02x", Long.parseLong(value, 16));
						value = previousValue.substring(0, (previousBitWidth - 8)/4) + value;
					} break;
					case -8: {
						value = String.format("%02x", Long.parseLong(value, 16));
						value = previousValue.substring(0, (previousBitWidth - 8)/4) + value + previousValue.substring((previousBitWidth - 4)/4, (previousBitWidth - 0)/4);
					} break;
				}
				PostDBRegistryValue val = new PostDBRegistryValue(key, value.length()/2, value);
				memRef = new MemoryReference(key, subRegister, val, currentLine, EventType.REGISTER, isBefore);
			}
			
			if(newLeaf == null) {
				newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
			} else {
				newLeaf.addMemRefToLeaf(memRef);
			}
		}
		
		// all registers have been processed, update register values
		if(newLeaf != null) {
			for(MemoryReference register : newLeaf.getDelta().getRegisterReferences()) {
				registerValues.put(register.getRegName(), register.getMemoryContent().getMemoryValue());
			}
		}
		
		Matcher memoryMatcher = memoryPattern.matcher(tmpLine);
		while(memoryMatcher.find()) {
			String key = new String(memoryMatcher.group(1));
			String value = new String(memoryMatcher.group(2));
			
			int indexOfAfterPipe = tmpLine.indexOf("|");
			boolean isBefore = registersMatcher.end() <= indexOfAfterPipe;
			PostDBRegistryValue val = new PostDBRegistryValue(key, value.length()/2, value);
			MemoryReference memRef = new MemoryReference(key, key, val, currentLine, EventType.MEMORY, isBefore);
			
			if(newLeaf == null) {
				newLeaf = new MemoryDeltaTreeNode(memRef, idGenerator.getId(0, currentLine));
			} else {
				newLeaf.addMemRefToLeaf(memRef);
			}
		}
		
		// Our current strategy does not support lines that are missing memory information, simply add a blank node
		if(newLeaf == null) {
			newLeaf = new MemoryDeltaTreeNode(idGenerator.getId(0, currentLine), currentLine, currentLine, 0); 
		}
		
		addNodeToLevel(newLeaf, 0);
		
		currentLine++;
		
		timeAccumulator.checkOutAll();
	}

	private void addNodeToLevel(MemoryDeltaTreeNode newNode, int i) {
		
		if(headNodeAtEachLevel.get(i) != null) {
			// We can keep leaf node level null always, nulled when committed,
			// and null the parents when they are committed too.
			throw new RuntimeException("Predecessor should be full (actually should be committed, so this should be null");
		}
		
		// Only really tracks the most recent parent (therefore tracks parents that can take children).
		// When committed, leaf or parent, this is nulled.
		headNodeAtEachLevel.set(i, newNode);
		
		int numberOfLinesInTrace = this.maxLineProvider.getNumberOfLines() - 1;
		boolean uselessRootDoNotCreate = Math.round(Math.pow(this.branchOutFactor, i+1)) >= numberOfLinesInTrace;
		
		
		incrementNodeLevelCount(i);
		
		
		if((i+1) < headNodeAtEachLevel.size() && headNodeAtEachLevel.get(i+1) == null){
			if(uselessRootDoNotCreate){
				// Do not make the old fashioned, ultra root nodes. They have no functional value,
				// and take up the most space of anything. They express the largest memory footprint,
				// and can only express total memory state for the very last line
			} else {
				// headNodeAtEachLevel will be set for this parent when it gets through this same method itself.
				// its parents will also be created when it gets through this method itself.
				createNewParentNode(i+1);
			}
			
		}

		// then we need to find a parent for this node
		MemoryDeltaTreeNode potentialParent = (i+1) < headNodeAtEachLevel.size() ? headNodeAtEachLevel.get(i+1) : null;
			
		// Seems redundant, but is it? Or is the tree height check incorrect?
		if(potentialParent == null && (TREE_HEIGHT == (i+1) || uselessRootDoNotCreate)){
			// nuthin
		} else if(potentialParent == null) {
			System.out.println("Does this ever happen? Yes, on the first run for sure. Does it need to or can we handle diff?");
			createNewParentNode(i, newNode);
		} else {
			potentialParent.addChild(newNode);
			// Shouldn't need to check isFull, that is logically incorrect, but it's an ok guard, right?
			// it won't be full if it is not a leaf, after all, because it is a *new node*.
			if(newNode.isLeafNode() || newNode.isFull()){
				// Now commit that leaf node, and commit the parent node if it is full, recursively, in that order.
				saveNodeMaybeParentToo(newNode);
			}
		}
	}

	protected void incrementNodeLevelCount(int level) {
		totalNodeCountAtLevels.set(level, totalNodeCountAtLevels.get(level) + 1);
	}
	
	protected int getNodeLevelCount(int level) {
		return totalNodeCountAtLevels.get(level);
	}

	/**
	 * This method will create a new parent node which has the child nodes as children, and add it
	 * to the level above the children. 
	 * 
	 * @param i the level that the child nodes appear at
	 * @param childNodes the nodes that will be the children of the new parent
	 */
	private void createNewParentNode(int childLevel, MemoryDeltaTreeNode ... childNodes) {
		
		if(childNodes.length > this.branchOutFactor) {
			throw new RuntimeException("Cannot create a parent node to take more than " + branchOutFactor + " children.");
		}
		
		MemoryDeltaTreeNode newParent = new MemoryDeltaTreeNode(idGenerator.getId(childLevel+1, getNodeLevelCount(childLevel+1)), -1, -1, childLevel+1);
		
		for(MemoryDeltaTreeNode childNode : childNodes) {
			newParent.addChild(childNode);
		}
		
		addNodeToLevel(newParent, childLevel+1);
	}
	
	/**
	 * This method will create a new parent node which has the child nodes as children, and add it
	 * to the level above the children. 
	 * 
	 * @param i the level that the child nodes appear at
	 * @param childNodes the nodes that will be the children of the new parent
	 */
	private void createNewParentNode(int level) {
		
		MemoryDeltaTreeNode newParent = new MemoryDeltaTreeNode(idGenerator.getId(level, getNodeLevelCount(level)), -1, -1, level);
		
		addNodeToLevel(newParent, level);
	}
	
	public void saveNodeMaybeParentToo(MemoryDeltaTreeNode node){
		// Already checked in to prog timer
		
		timeAccumulator.dbCheckIn();
		
		memoryAccessDatabase.saveMemRefNode(node);
		
		timeAccumulator.progCheckIn();
		
		// Each committed node needs to leave memory. This should be the only container that
		// they are added to, except that children reference parents (but not the other way around).
		headNodeAtEachLevel.set(node.getLevel(), null);
		// The topmost parents will have null parent. This can be at the maximum level for larger files, or at lower levels
		// for file with fewer lines to represent.
		if(null != node.getParentNode() && node.getParentNode().isFull()){
			saveNodeMaybeParentToo(node.getParentNode());
		}
	}
	
	public static String convertHexFlagToLetterCodeFlags(String hexString){
		int hexValue = Integer.parseInt(hexString, 16);
		String flagChars = "";
		// Order of flags doesn't matter
		for(Map.Entry<Character, Integer> flag : flagPositions.entrySet()) {
			if((hexValue & flag.getValue()) != 0) {
				flagChars += flag.getKey();
			}
		}
		return flagChars;
	}
	
	/**
	 * sets up the registers with initial empty values
	 */
	private void initializeRegisters() {
		registerValues = new HashMap<String, String>();
		registerValues.put("RAX", "0000000000000000");
		registerValues.put("RBX", "0000000000000000");
		registerValues.put("RCX", "0000000000000000");
		registerValues.put("RDX", "0000000000000000");
		registerValues.put("RSI", "0000000000000000");
		registerValues.put("RDI", "0000000000000000");
		registerValues.put("RBP", "0000000000000000");
		registerValues.put("RSP", "0000000000000000");
		registerValues.put("RIP", "0000000000000000");
		registerValues.put("RFLAGS", "0000000000000000");
		registerValues.put("R8", "0000000000000000");
		registerValues.put("R9", "0000000000000000");
		registerValues.put("R10", "0000000000000000");
		registerValues.put("R11", "0000000000000000");
		registerValues.put("R12", "0000000000000000");
		registerValues.put("R13", "0000000000000000");
		registerValues.put("R14", "0000000000000000");
		registerValues.put("R15", "0000000000000000");
		registerValues.put("FSBASE", "0000000000000000");
		registerValues.put("GSBASE", "0000000000000000");
		
		for(int i = 0; i < 8; i++) {
			registerValues.put("ST" + i, "00000000000000000000");
		}
		for(int i = 0; i < 16; i++) {
			registerValues.put("XMM" + i, "00000000000000000000000000000000");
		}
	}
	
	static{
		/**
		 * fills out the positions of common flags (x86/x64) in the flags register by character id
		 */
		flagPositions = new HashMap<Character, Integer>();
		
		// TODO Make this an enum for use in multiple places. Search for these hexes and find other uses.
		flagPositions.put('C', 0x00000001); // carry
		// 0x00000002 is reserved
		flagPositions.put('P', 0x00000004); // parity
		// ox00000008 is reserved
		flagPositions.put('A', 0x00000010); // adjust
		// 0x00000020 is reserved
		flagPositions.put('Z', 0x00000040); // zero
		flagPositions.put('S', 0x00000080); // sign
		flagPositions.put('T', 0x00000100); // trap
		flagPositions.put('I', 0x00000200); // interrupt enable
		flagPositions.put('D', 0x00000400); // direction
		flagPositions.put('O', 0x00000800); // overflow
		// We don't use past 'O' in the registers view, and bits 22 onward are reserved.
		
		
		registerBitWidths = new HashMap<String, Integer>();
		
		registerBitWidths.put("RAX", 64);		registerBitWidths.put("EAX", 32);
		registerBitWidths.put("RBX", 64);		registerBitWidths.put("EBX", 32);
		registerBitWidths.put("RCX", 64);		registerBitWidths.put("ECX", 32);
		registerBitWidths.put("RDX", 64);		registerBitWidths.put("EDX", 32);
		registerBitWidths.put("RSI", 64);		registerBitWidths.put("ESI", 32);
		registerBitWidths.put("RDI", 64);		registerBitWidths.put("EDI", 32);
		registerBitWidths.put("RBP", 64);		registerBitWidths.put("EBP", 32);
		registerBitWidths.put("RSP", 64);		registerBitWidths.put("ESP", 32);
		registerBitWidths.put("R8", 64);		registerBitWidths.put("R8D", 32);
		registerBitWidths.put("R9", 64);		registerBitWidths.put("R9D", 32);
		registerBitWidths.put("R10", 64);		registerBitWidths.put("R10D", 32);
		registerBitWidths.put("R11", 64);		registerBitWidths.put("R11D", 32);
		registerBitWidths.put("R12", 64);		registerBitWidths.put("R12D", 32);
		registerBitWidths.put("R13", 64);		registerBitWidths.put("R13D", 32);
		registerBitWidths.put("R14", 64);		registerBitWidths.put("R14D", 32);
		registerBitWidths.put("R15", 64);		registerBitWidths.put("R15D", 32);
		
		registerBitWidths.put("AX", 16);		registerBitWidths.put("AL", 8);
		registerBitWidths.put("BX", 16);		registerBitWidths.put("BL", 8);
		registerBitWidths.put("CX", 16);		registerBitWidths.put("CL", 8);
		registerBitWidths.put("DX", 16);		registerBitWidths.put("DL", 8);
		registerBitWidths.put("SI", 16);		registerBitWidths.put("SIL", 8);
		registerBitWidths.put("DI", 16);		registerBitWidths.put("DIL", 8);
		registerBitWidths.put("BP", 16);		registerBitWidths.put("BPL", 8);
		registerBitWidths.put("SP", 16);		registerBitWidths.put("SPL", 8);
		registerBitWidths.put("R8W", 16);		registerBitWidths.put("R8B", 8);
		registerBitWidths.put("R9W", 16);		registerBitWidths.put("R9B", 8);
		registerBitWidths.put("R10W", 16);		registerBitWidths.put("R10B", 8);
		registerBitWidths.put("R11W", 16);		registerBitWidths.put("R11B", 8);
		registerBitWidths.put("R12W", 16);		registerBitWidths.put("R12B", 8);
		registerBitWidths.put("R13W", 16);		registerBitWidths.put("R13B", 8);
		registerBitWidths.put("R14W", 16);		registerBitWidths.put("R14B", 8);
		registerBitWidths.put("R15W", 16);		registerBitWidths.put("R15B", 8);
		
		registerBitWidths.put("AH", -8);
		registerBitWidths.put("BH", -8);
		registerBitWidths.put("CH", -8);
		registerBitWidths.put("DH", -8);

		registerBitWidths.put("RIP", 64);		
		registerBitWidths.put("EIP", 32);
		
		registerBitWidths.put("GSBASE", 64);	
		registerBitWidths.put("FSBASE", 64);
		
		registerEquivalents = new HashMap<String, String>();
		
		registerEquivalents.put("RAX", "RAX");		registerEquivalents.put("EAX", "RAX");
		registerEquivalents.put("RBX", "RBX");		registerEquivalents.put("EBX", "RBX");
		registerEquivalents.put("RCX", "RCX");		registerEquivalents.put("ECX", "RCX");
		registerEquivalents.put("RDX", "RDX");		registerEquivalents.put("EDX", "RDX");
		registerEquivalents.put("RSI", "RSI");		registerEquivalents.put("ESI", "RSI");
		registerEquivalents.put("RDI", "RDI");		registerEquivalents.put("EDI", "RDI");
		registerEquivalents.put("RBP", "RBP");		registerEquivalents.put("EBP", "RBP");
		registerEquivalents.put("RSP", "RSP");		registerEquivalents.put("ESP", "RSP");
		registerEquivalents.put("R8", "R8");		registerEquivalents.put("R8D", "R8");
		registerEquivalents.put("R9", "R9");		registerEquivalents.put("R9D", "R9");
		registerEquivalents.put("R10", "R10");		registerEquivalents.put("R10D", "R10");
		registerEquivalents.put("R11", "R11");		registerEquivalents.put("R11D", "R11");
		registerEquivalents.put("R12", "R12");		registerEquivalents.put("R12D", "R12");
		registerEquivalents.put("R13", "R13");		registerEquivalents.put("R13D", "R13");
		registerEquivalents.put("R14", "R14");		registerEquivalents.put("R14D", "R14");
		registerEquivalents.put("R15", "R15");		registerEquivalents.put("R15D", "R15");
		
		registerEquivalents.put("AX", "RAX");		registerEquivalents.put("AL", "RAX");
		registerEquivalents.put("BX", "RBX");		registerEquivalents.put("BL", "RBX");
		registerEquivalents.put("CX", "RCX");		registerEquivalents.put("CL", "RCX");
		registerEquivalents.put("DX", "RDX");		registerEquivalents.put("DL", "RDX");
		registerEquivalents.put("SI", "RSI");		registerEquivalents.put("SIL", "RSI");
		registerEquivalents.put("DI", "RDI");		registerEquivalents.put("DIL", "RDI");
		registerEquivalents.put("BP", "RBP");		registerEquivalents.put("BPL", "RBP");
		registerEquivalents.put("SP", "RSP");		registerEquivalents.put("SPL", "RSP");
		registerEquivalents.put("R8W", "R8");		registerEquivalents.put("R8B", "R8");
		registerEquivalents.put("R9W", "R9");		registerEquivalents.put("R9B", "R9");
		registerEquivalents.put("R10W", "R10");		registerEquivalents.put("R10B", "R10");
		registerEquivalents.put("R11W", "R11");		registerEquivalents.put("R11B", "R11");
		registerEquivalents.put("R12W", "R12");		registerEquivalents.put("R12B", "R12");
		registerEquivalents.put("R13W", "R13");		registerEquivalents.put("R13B", "R13");
		registerEquivalents.put("R14W", "R14");		registerEquivalents.put("R14B", "R14");
		registerEquivalents.put("R15W", "R15");		registerEquivalents.put("R15B", "R15");
		
		registerEquivalents.put("AH", "RAX");
		registerEquivalents.put("BH", "RBX");
		registerEquivalents.put("CH", "RCX");
		registerEquivalents.put("DH", "RDX");

		registerEquivalents.put("RIP", "RIP");		
		registerEquivalents.put("EIP", "RIP");
		
		registerEquivalents.put("GSBASE", "GSBASE");		
		registerEquivalents.put("FSBASE", "FSBASE");
		
		for(int i = 0; i < 8; i++) {
			registerBitWidths.put("ST" + i, 80);
			registerEquivalents.put("ST" + i, "ST" + i);	
		}
		for(int i = 0; i < 16; i++) {
			registerBitWidths.put("XMM" + i, 128);
			registerEquivalents.put("XMM" + i, "XMM" + i);	
		}
	}
	
	public String getCurrentFlagsRegisterValue() {
		return registerValues.get("RFLAGS");
	}

	@Override
	public void readFinished() {
		
		timeAccumulator.progCheckIn();
		
		for(int i = 0; i < headNodeAtEachLevel.size(); i++){
			if(headNodeAtEachLevel.get(i) != null) {
				timeAccumulator.dbCheckIn();
				
				memoryAccessDatabase.saveMemRefNode(headNodeAtEachLevel.get(i));timeAccumulator.dbCheckIn();
				
				timeAccumulator.progCheckIn();
				
				headNodeAtEachLevel.set(i, null);
			}
		}
		
		long startTime = System.currentTimeMillis();
		
		timeAccumulator.dbCheckIn();
		
		memoryAccessDatabase.executeInsertMemRefBatch();
		
		memoryAccessDatabase.doFinalCommit();
		
		timeAccumulator.checkOutAll();
		
		System.out.println(this.getClass()+":"+"Total time to preparing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commit memory delta tree to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to do final insert and commit memory delta tree info to db: "+ (System.currentTimeMillis() - startTime)/1000+"s");
		System.out.println();
	}
	
	@Override
	public void indexCreationAborted() {
		this.memoryAccessDatabase.abortCommitAndDropTable();
	}	
	
	@Override
	public boolean requiresPostProcessing() {
		return false;
	}

	@Override
	public void doPostProcessing(IProgressMonitor monitor) {}
}


