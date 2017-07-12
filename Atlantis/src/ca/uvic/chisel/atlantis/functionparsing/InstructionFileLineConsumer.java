package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.database.BasicBlockDbConnection;
import ca.uvic.chisel.atlantis.database.CallDbConnection;
import ca.uvic.chisel.atlantis.database.FunctionDbConnection;
import ca.uvic.chisel.atlantis.database.InstructionDbConnection;
import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.database.DbConnectionManager.TableState;
import ca.uvic.chisel.atlantis.database.JumpDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadFunctionBlockDbConnection;
import ca.uvic.chisel.atlantis.database.ThreadLengthDbConnection;
import ca.uvic.chisel.atlantis.datacache.TraceLine;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;
import ca.uvic.chisel.atlantis.deltatree.MemoryDeltaTree;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;

public class InstructionFileLineConsumer implements IFileContentConsumer<TraceLine> {
	
	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();


	private InstructionDbConnection instructionDatabase;
	private BasicBlocks blocks;
	private Jumps jumps;
	private FuncDb functions;
	private CallAssociations calls;
	private ThreadFunctions threadFuncs;
	private ThreadLengthDbConnection threadLengthDatabase;
	int threads = 0;
	long tfbs = 0;
	
	private MemoryDeltaTree coExecutingMemoryDeltaTreeBuilder;
	
	private Pattern instructionPattern;
	private Pattern threadPattern;
	
	private long currentLine;
	/**
	 * Only detected on thread changes, so we keep it around across and between lines.
	 */
	private int currentThread;
	private Map<Integer, List<Pair<Instruction, Pair<Long, String>>>> threadInstructions;
	private Instruction finalInstruction;
	private int finalThreadId;
	long instructionCount = 0;
	
	public InstructionFileLineConsumer(InstructionDbConnection instructionDatabase, BasicBlockDbConnection basicBlockDatabase,
			JumpDbConnection jumpDatabase, FunctionDbConnection functionDatabase, CallDbConnection callDatabase,
			ThreadFunctionBlockDbConnection threadFunctionBlockDatabase, ThreadLengthDbConnection threadLengthDatabase,
			MemoryDeltaTree coExecutingMemoryDeltaTreeBuilder) {
		this.instructionDatabase = instructionDatabase;
		this.blocks = new BasicBlocks(basicBlockDatabase);
		this.jumps = new Jumps(jumpDatabase);
		this.functions = new FuncDb(functionDatabase);
		this.calls = new CallAssociations(callDatabase);
		this.threadFuncs = new ThreadFunctions(threadFunctionBlockDatabase);
		this.threadLengthDatabase = threadLengthDatabase;
		this.coExecutingMemoryDeltaTreeBuilder = coExecutingMemoryDeltaTreeBuilder;
	}
	
	@Override
	public boolean verifyDesireToListen() {
		instructionDatabase.createTables();
		this.blocks.basicBlockDatabase.createTables();
		this.jumps.jumpDatabase.createTables();
		this.functions.functionDatabase.createTables();
		this.calls.callDatabase.createTables();
		this.threadFuncs.threadFunctionBlockDatabase.createTables();
		threadLengthDatabase.createTables();
		return instructionDatabase.getTableState() != TableState.POPULATED;
	}

	@Override
	public void readStart() {
		currentLine = 0;
		currentThread = 0;
		
		// we haven't read any instructions yet
		finalInstruction = null; // this is a replacement for instructions.last(), for refactoring.
		threadInstructions = new TreeMap<Integer, List<Pair<Instruction, Pair<Long, String>>>>();
		
		// initialize the regexs
		instructionPattern = Pattern.compile("[0-9a-fA-F]+ I:[0-9a-fA-F]+ ([A-Za-z0-9_.]+)\\+([0-9a-fA-F]+) (\\([0-9a-fA-F]+\\))? ?([A-Za-z]+) ([^|]+).*");
		threadPattern = Pattern.compile("(T:|TID=)(\\d+)");
	}
	
	// I hate this, but I need access to the instruction id earlier in processing.
	// it would not need to be done this way if we did not support textual traces.
	public void helpGenerateInstructionId(String line, TraceLine lineData, AtlantisBinaryFormat binaryFileSet){
		if(null == lineData.getInstructionId()){
			// Binary format traces already have an instructionId, we only do this here for
			// purely textual traces. If we stop supporting these, we can remove all this.
			Matcher instructionMatcher = instructionPattern.matcher(line.trim());
			if(instructionMatcher.matches()) {
				String module = instructionMatcher.group(1);
				Long moduleOffset = Long.parseLong(instructionMatcher.group(2), 16);
				lineData.generateInstructionIdAndModuleId(module, moduleOffset);
			}
		}
	}

	
	@Override
	public int handleInputLine(String line, TraceLine lineData) {
		
		timeAccumulator.progCheckIn();
		
		Matcher instructionMatcher = instructionPattern.matcher(line.trim());
		Matcher threadMatcher = threadPattern.matcher(line);
		
		if(threadMatcher.find()) {
			currentThread = Integer.parseInt(threadMatcher.group(2));
			
			if(!threadInstructions.containsKey(currentThread)) {
				threadInstructions.put(currentThread, new ArrayList<Pair<Instruction, Pair<Long, String>>>());
			}
		}
		
		if(instructionMatcher.matches()) {
			String module = instructionMatcher.group(1);
			long moduleOffset = Long.parseLong(instructionMatcher.group(2), 16);
			String name = instructionMatcher.group(4);
			String text = name + " " + instructionMatcher.group(5).trim();

			int moduleId = lineData.getModuleId();
			
			InstructionId currentFunction = this.functions.getLastFuncForThread(currentThread);
			Instruction inst = new Instruction(lineData.getInstructionId(), currentLine, name, text, module, moduleId, moduleOffset-moduleId, lineData.instAbsoluteId, currentFunction);
			
			// Do we need this for anything? Simple counting?
			instructionCount++;
			
			timeAccumulator.dbCheckIn();
			
			// Adds as a function entry only if the previous instruction was a 'call', does not analyze current line to determine.
			this.functions.insertUniqueFunctionsToDb(inst, currentThread);
			
			// Repeat insertions...but we use INSERT OR IGNORE with a good primary key
			instructionDatabase.insertInstruction(inst);

			timeAccumulator.progCheckIn();
			
			finalInstruction = inst;
			finalThreadId = currentThread;
			
			// TODO Should/can these be refactored to be in a/some different consumer class(es)?
			
			this.blocks.makeBasicBlocks(inst, currentThread);
			
			this.jumps.makeJumpThings(inst, coExecutingMemoryDeltaTreeBuilder.getCurrentFlagsRegisterValue(), currentThread);
			
			this.calls.createCallAssociations(inst, currentThread);
			
			this.threadFuncs.insertThreadFunctionBlock(inst, currentLine, currentThread);
		}
		
		
		currentLine++;

		timeAccumulator.checkOutAll();
		
		return 1;
	}
	
	@Override
	public void readFinished() {
		// Finish off any steps that require work on previous items on the basis of current items.
		this.blocks.makeBasicBlocks(null, -1);
		this.threadFuncs.insertThreadFunctionBlock(null, null, null);
		
		long startTime = System.currentTimeMillis();
		
		timeAccumulator.dbCheckIn();
		
		instructionDatabase.finalizeInsertionBatch();
		this.blocks.basicBlockDatabase.finalizeInsertionBatch();
		this.jumps.jumpDatabase.finalizeInsertionBatch();
		this.functions.functionDatabase.finalizeInsertionBatch();
		this.calls.callDatabase.finalizeInsertionBatch();
		this.threadFuncs.threadFunctionBlockDatabase.finalizeInsertionBatch();
		// threadLengthDatabase has no insertion batch
		
		instructionDatabase.doFinalCommit();
		this.blocks.basicBlockDatabase.doFinalCommit();
		this.jumps.jumpDatabase.doFinalCommit();
		this.functions.functionDatabase.doFinalCommit();
		this.calls.callDatabase.doFinalCommit();
		this.threadFuncs.threadFunctionBlockDatabase.doFinalCommit();
		threadLengthDatabase.doFinalCommit();
		

		timeAccumulator.checkOutAll();
		
		System.out.println(this.getClass()+":"+"Total time to preparing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commit function data to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println();
		
		System.out.println("Instructions: " + instructionCount);
//		System.out.println("Basic Blocks: " + basicBlocks.size());
		System.out.println("Jumps: " + jumps.jumps);
		System.out.println("Functions: " + this.functions.functionDatabase.getNumberUniqueFunctions());
		System.out.println("Functions: " + this.calls.callDatabase.getNumberUniqueCalls());
		System.out.println("Thread Function Blocks: " + tfbs);
		System.out.println("Threads: " + threads);
	}

	@Override
	public void indexCreationAborted() {
		instructionDatabase.abortCommitAndDropTable();
		this.blocks.basicBlockDatabase.abortCommitAndDropTable();
		this.jumps.jumpDatabase.abortCommitAndDropTable();
		this.functions.functionDatabase.abortCommitAndDropTable();
		this.calls.callDatabase.abortCommitAndDropTable();
		this.threadFuncs.threadFunctionBlockDatabase.abortCommitAndDropTable();
		threadLengthDatabase.abortCommitAndDropTable();
	}
	
	@Override
	public boolean requiresPostProcessing() {
		return true;
	}
		
	@Override
	public void doPostProcessing(IProgressMonitor monitor) {
	}
	
	private class FuncDb {
		private final Map<Integer, Instruction> lastInst = new HashMap<Integer, Instruction>();
		private final Map<Integer, Instruction> lastFunc = new HashMap<Integer, Instruction>();
		public FunctionDbConnection functionDatabase;
		
		FuncDb(FunctionDbConnection functionDatabase){
			this.functionDatabase = functionDatabase;
		}
		
		void insertUniqueFunctionsToDb(Instruction inst, int threadId){
			Instruction prevInst = lastInst.get(threadId);
			if(null != prevInst && prevInst.getInstruction().equalsIgnoreCase("call")){
				functionDatabase.insertIfUniqueFunction(inst);
				lastFunc.put(threadId, inst);
			}
			lastInst.put(threadId, inst);
		}
		
		public InstructionId getLastFuncForThread(int threadId){
			if(null == this.lastFunc.get(threadId)){
				return null;
			} else {
				return this.lastFunc.get(threadId).getIdGlobalUnique();
			}
		}
	}
	
	private class CallAssociations {
		private final Map<Integer, Instruction> last = new HashMap<Integer, Instruction>();
		private final Map<Integer, Deque<ImmutablePair<Function, Instruction>>> perThreadStack = new HashMap<Integer, Deque<ImmutablePair<Function, Instruction>>>();
		private final Map<Integer, Instruction> prevFuncStartInstruction = new HashMap<Integer, Instruction>();
		public CallDbConnection callDatabase;
		
		CallAssociations(CallDbConnection callDatabase){
			this.callDatabase = callDatabase;
		}
		
		public void createCallAssociations(Instruction inst, int threadId){
			if(null == perThreadStack.get(threadId)){
				perThreadStack.put(threadId, new LinkedList<ImmutablePair<Function, Instruction>>());
			}
			if(null == prevFuncStartInstruction.get(threadId)){
				prevFuncStartInstruction.put(threadId, inst);
			}
			Deque<ImmutablePair<Function, Instruction>> stackcreate = perThreadStack.get(threadId);
			
			if(last.get(threadId) != null) {
				if(last.get(threadId).getInstruction().equalsIgnoreCase("call")) {
					Function callTo = new Function(inst, "");
					
					if(stackcreate.size() != 0) {
						// 	insertCall(Instruction fromFuncStartInst, Instruction toFuncStartInst, Instruction at) {
						callDatabase.insertCall(prevFuncStartInstruction.get(threadId), inst, last.get(threadId));
						prevFuncStartInstruction.put(threadId, inst);
					}
					// This is needed, but will not grow large during processing, since it pops off as functions return.
					stackcreate.push(new ImmutablePair<Function, Instruction>(callTo, inst));
				} else if(last.get(threadId).getInstruction().equalsIgnoreCase("ret")) {
					if(stackcreate.size() > 0) {
						ImmutablePair<Function, Instruction> popped = stackcreate.pop();
						// If we don't re-instate the previous function start, we lose it.
						prevFuncStartInstruction.put(threadId, popped.getRight());
					}
				}
			}
			last.put(threadId, inst);
		}
	}
	
	private class ThreadFunctions {
		private final Map<Integer, Long> currentXOffset = new HashMap<Integer, Long>();
		private final Map<Integer, Instruction> last = new HashMap<Integer, Instruction>();
		private final Map<Integer, Deque<Instruction>> stack = new HashMap<Integer, Deque<Instruction>>();
		private final Map<Integer, ThreadFunctionBlock> tfbMap = new HashMap<Integer, ThreadFunctionBlock>();
		private ThreadFunctionBlockDbConnection threadFunctionBlockDatabase;
		
		ThreadFunctions(ThreadFunctionBlockDbConnection threadFunctionBlockDatabase){
			this.threadFunctionBlockDatabase= threadFunctionBlockDatabase; 
		}
		
		/**
		 * Maintains its own stacks of data as it is called in iterations over instructions.
		 * 
		 * Call with all null arguments at the end of all instructions to finish processing.
		 * 
		 * Has to deal with functions that were underway when tracing began, for which we cannot
		 * determine the actual start instruction for their function. It should actually be possible
		 * to do this in cases where said function has a call subsequently, and where an instruction
		 * is re-visited in that known-function context. The way things are set up currently would
		 * require a database update when such an occurrence occurs. If implementing that, keep
		 * a small container of all such "bareEntryToFunction" instructions, and only query or update
		 * the database when those instructions are re-encountered. For those that are never re-encountered,
		 * we can treat them as starting the function they are in.
		 * 
		 * @param tfb
		 * @param inst
		 * @param startLineNumber
		 * @param threadId
		 * @return
		 */
		public int insertThreadFunctionBlock(Instruction inst, Long startLineNumber, Integer threadId){
			
			// Initialize storage if it's a new thread
			if(null == stack.get(threadId) && null != threadId){
				stack.put(threadId, new LinkedList<Instruction>());
				currentXOffset.put(threadId, 0L);
			}
	
			boolean incrementCaller = false;
			
			// When arguments are null, we are dealing with the final steps.
			if(null == inst || null == threadId || null == startLineNumber){
				// If one of these arguments is null, all must be
				assert null == inst && null == threadId && null == startLineNumber;
				// tie up loose ends of threads
				for(Integer thread: currentXOffset.keySet()){
					if(null == thread){
						continue;
					}
					threadId = thread; // threadId shall be 
					tfbMap.get(threadId).setEndInstruction(last.get(threadId));
					tfbMap.get(threadId).setWidth(currentXOffset.get(threadId) - tfbMap.get(threadId).getXOffset());
					threadFunctionBlockDatabase.insertThreadFunctionBlock(tfbMap.get(threadId));
					
					threadLengthDatabase.insertThreadLength(threadId, currentXOffset.get(threadId));
					threads++;
				}
				return 0;
			}
			
			// If there isn't a current block for the thread, initialize with a new block
			if(null == tfbMap.get(threadId)){
				tfbMap.put(threadId, new ThreadFunctionBlock());
			}
			
			if(last.get(threadId) != null) {
				// If the last instruction for the thread ends a block with call or ret, create new function or end one.
				if(last.get(threadId).getInstruction().equalsIgnoreCase("call")
						|| last.get(threadId).getInstruction().equalsIgnoreCase("ret")
					) {
					
					tfbMap.get(threadId).setEndInstruction(last.get(threadId));
					tfbMap.get(threadId).setWidth(currentXOffset.get(threadId) - tfbMap.get(threadId).getXOffset());
					
					threadFunctionBlockDatabase.insertThreadFunctionBlock(tfbMap.get(threadId));
					
					tfbs++;
					incrementCaller = true;
					
					tfbMap.put(threadId, new ThreadFunctionBlock());
					tfbMap.get(threadId).setStartLineNumber(startLineNumber);
					tfbMap.get(threadId).setThreadId(threadId);
					tfbMap.get(threadId).setStartInstruction(inst);
					tfbMap.get(threadId).setXOffset(currentXOffset.get(threadId));
					
					if(last.get(threadId).getInstruction().equalsIgnoreCase("call")) {
						tfbMap.get(threadId).setFunctionStartInstruction(inst);
						stack.get(threadId).push(inst);
					} else if(last.get(threadId).getInstruction().equalsIgnoreCase("ret")){
						// Pop the one we just returned from off the stack
						if(stack.get(threadId).size() > 0) {
							// If we change things to detect jmp as functions ever, this needs adjustment
							Instruction prev = stack.get(threadId).pop();
						}
						
						// Set this block to have the same function start as the block prior to that one we just returned
						// from and popped
						if(stack.get(threadId).size() > 0) {
							tfbMap.get(threadId).setFunctionStartInstruction(stack.get(threadId).peek());
						}
						// stack is totally empty, thus we parachuted in the middle of a function, and we return upward to other unknown functions
						else if(null == stack.get(threadId) || stack.get(threadId).isEmpty()){
							// If we have no last instruction in the thread, then the first instruction for the current
							// function is going to be the current instruction. We don't yet know the function this instruction
							// is actually a part of, but thread function blocks are line oriented and strictly chronological,
							// so finding that out later would not matter.
							tfbMap.get(threadId).setFunctionStartInstruction(inst);
							stack.get(threadId).push(inst);
							inst.bareEntryToFunction = true;
						}
					}
				}
			} else {
				// if there was no previous instruction in the thread, initialize this block's properties,
				// and push it on the function stack.
				tfbMap.get(threadId).setStartLineNumber(startLineNumber);
				tfbMap.get(threadId).setThreadId(threadId);
				tfbMap.get(threadId).setStartInstruction(inst);
				tfbMap.get(threadId).setXOffset(currentXOffset.get(threadId));
				// If we have no last instruction in the thread, then the first instruction for the current
				// function is going to be the current instruction. We don't yet know the function this instruction
				// is actually a part of, but thread function blocks are line oriented and strictly chronological,
				// so finding that out later would not matter.
				tfbMap.get(threadId).setFunctionStartInstruction(inst);
				stack.get(threadId).push(inst);
				inst.bareEntryToFunction = true;
			}
			
			last.put(threadId, inst);
			currentXOffset.put(threadId, (currentXOffset.get(threadId)+1));
			
			return incrementCaller ? 1 : 0;
		}
	}
	
	private class Jumps {
		private int jumps = 0;
		private final Map<Integer, Instruction> lastInst = new HashMap<Integer, Instruction>();
		private final Map<Integer, String> lastFlags = new HashMap<Integer, String>();
		private JumpDbConnection jumpDatabase;
		
		Jumps(JumpDbConnection jumpDatabase){
			this.jumpDatabase= jumpDatabase; 
		}
		
		void makeJumpThings(Instruction inst, String currentFlag, int threadId) {
			
			if(lastInst.get(threadId) == null) {
				// NOOP??
			} else if(lastInst.get(threadId).wouldEndBasicBlock()) {			
				boolean wouldBranch = inst.wouldBeBranchedToBy(lastInst.get(threadId), lastFlags.get(threadId));
	
				// Oh, actually, whenever one basic block follows another, it is strictly either branched to or not. No "on duplicate" needed.
			    // Although the original code collected basic blocks and only entered those that were newly found successors into the DB, a block is either jumped to or not jumped to,
			    // never both.
				jumpDatabase.insertJump(lastInst.get(threadId), inst, wouldBranch);
				jumps++; // No longer counting *new* jumps only...can we get feedback on the insertion? Only if we give up batch inserts on this one.
				
			}
			
			lastInst.put(threadId, inst);
			lastFlags.put(threadId, currentFlag);
		}
	}

	private class BasicBlocks {
	
		private final Map<Integer, Integer> length = new HashMap<Integer, Integer>();
		private final Map<Integer, BasicBlock> currentBasicBlock = new HashMap<Integer, BasicBlock>();
		private final Map<Integer, Instruction> lastInst = new HashMap<Integer, Instruction>();
				
		private final Map<Integer, Instruction> lastInstructionPerThread = new HashMap<Integer, Instruction>();
		public BasicBlockDbConnection basicBlockDatabase;
		
		BasicBlocks(BasicBlockDbConnection basicBlockDatabase){
			this.basicBlockDatabase= basicBlockDatabase;  
		}
		
		public void makeBasicBlocks(Instruction inst, int threadId){
			// Tie up loose ends. Requires a call after the final instruction.
			if(null == inst){
				currentBasicBlock.get(finalThreadId).setEnd(finalInstruction);
				currentBasicBlock.get(finalThreadId).setLength(length.get(finalThreadId) - 1);
				basicBlockDatabase.insertBasicBlock(currentBasicBlock.get(finalThreadId));
				
				return;
			} else if(null != inst){
			
				if(null == length.get(threadId)){
					length.put(threadId, 0);
				}
				
				Instruction sameThreadLast = lastInstructionPerThread.get(threadId);
				lastInstructionPerThread.put(threadId, inst);
				
				// lastInst used to be used *across* threads, as a class member. I was refactoring Murray's code when
				// I realized that this was likely a bug; lastInt was being used for ending basic blocks, but could be
				// from another thread, which is meaningless. I have changed it so that we track
				// it per thread instead.
				this.lastInst.put(threadId, sameThreadLast);

				if(lastInst.get(threadId) == null) {
					// Initialize on first instruction of thread
					currentBasicBlock.put(threadId, new BasicBlock(inst, null, 0));
				} else if(lastInst.get(threadId).wouldEndBasicBlock()) {
					currentBasicBlock.get(threadId).setEnd(lastInst.get(threadId));
					currentBasicBlock.get(threadId).setLength(length.get(threadId));
					
//					TODO I think I need to close off the most recent call function if the basic block
//					ends because of a ret. That can happen where ret is in some distant jmp function.
//					Note  that the basic block of the call is likely completed, just that the function is not.
//					This is because we got to that distant ret via a jmp, which should have ended the basic block.
//					But are jmp being detected as functions even, or are remote ret working as is?
					
					if(null != sameThreadLast && sameThreadLast.getInstruction().equalsIgnoreCase("call")){
						currentBasicBlock.get(threadId).setFunctionStart(true);
					}
					
					// Original code had check to see if instruction would end a basic block.
					// I think that it is definitely *not* a function if it merely follows a jump
					// or a branch...the original setFunction was not right.
					
					basicBlockDatabase.insertBasicBlock(currentBasicBlock.get(threadId));
					currentBasicBlock.put(threadId, new BasicBlock(inst, null, 0));
					length.put(threadId, 0);
				}
				
				lastInst.put(threadId, inst);
				length.put(threadId, length.get(threadId) + 1); // could make property of basic block, set there
				
			}
		}
	}

}
