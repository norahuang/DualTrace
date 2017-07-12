package ca.uvic.chisel.atlantis.bytecodeparsing.execution;


/** ver Oct 2013
 * Trace line type (e.g. Begin, Ins, End)
 *
 * Fields in execRec.Data depend on execRec.Type. The following table list all possible types, their meaning and the fields found in execRec.Data. Some fields are structures and those structure types are defined after the table. Multiple fields, if there are, are always packed without any padding. 
 * 
 * {
 *  Type	Name
 * 	[Description]
 * 	Field type	Field name 
 *  Field description
 * }
 *
 * 0	Instruction 
 * 	i64	InsID	Corresponding instruction record ID in Instruction table
 * 	buf[?]	OperVals	Values of all instruction operands, both implicit and explicit. Provides the instruction “local context” (see 6.1.1 What’s in an execution record). The size and data layout of the buffer is given in the instruction record. 
 *
 * 1	Thread begin
 * Exactly once for each thread, before any other record for that thread.
 * 	rmContext	Context	Registers context at the thread start point, before the first user-mode instruction, as well as the thread environment block (TEB).
 *
 * 2	Thread end 
 * Exactly once for each thread that terminated before the traced application exited. This is the last record for a thread. 
 * 	i32	ExitCode	Windows thread exit code
 *
 * 3	Application end 
 * The very last record of the trace. Present only if the traced application exited gracefully. 
 * 	i32	ExitCode	Windows process exit code
 *
 * 4	System call entry
 * Thread is about to enter kernel mode through one of the various kind of system calls. Typically follows a syscall/sysenter/int 2e instruction record. 
 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
 * 	rmContext	Context	Value of all registers just before the call. Additional information, such as call parameters on the stack, is in the System calls table.
 *
 * 5	System call exit 
 * Thread just exit from kernel mode system call. Does not always immediately follow a system call entry record, and may not even match an entry at all (matching is done according to the stack pointer). Actually, system calls can be nested, involve user-mode callbacks, and can end on an exception or a context change. Also, some system calls, such as NtContinue, can shortcut a few system call exits at once.
 * 	i64	SyscallID	Corresponding system call record ID in System calls table, or -1 if there is no corresponding system call entry record (thus nothing in System calls table either). The latter case is semantically equivalent to a context change.
 * 	rmContext	Context	Value of all registers right after the call. Additional information may be found in the System calls table.
 *
 * 6	Skipped system call exit 
 * A previous system call entry occurred, but starting from this record, the stack was unwound further than it would be on a normal system call exit (a “return” to the user-mode caller). In other words, the thread is not anymore in the context of that system call, and that call will never have a “System call exit” record.
 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
 * 
 *  8	Context change – Asynchronous Procedure Call 
 *  9	Context change – Exception Handling 
 *  10	Context change – Callback 
 *  11	Context change – Unknown reason 
 * For one of the above reasons, the thread execution context just changed. 
 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
 */

public enum ExecLineType {
	/**
	 * 0	Instruction 
	 * 	i64	InsID	Corresponding instruction record ID in Instruction table
	 * 	buf[?]	OperVals	Values of all instruction operands, both implicit and explicit. Provides the instruction “local context” (see 6.1.1 What’s in an execution record). The size and data layout of the buffer is given in the instruction record. 
	 *
	 */
	INSTRUCTION(0),
	
	/**
	 * 1	Thread begin
	 * Exactly once for each thread, before any other record for that thread.
	 * 	rmContext	Context	Registers context at the thread start point, before the first user-mode instruction, as well as the thread environment block (TEB).
	 *
	 */
	THREAD_BEGIN(1),
	/**
	 * 2	Thread end 
	 * Exactly once for each thread that terminated before the traced application exited. This is the last record for a thread. 
	 * 	i32	ExitCode	Windows thread exit code
	 *
	 */
	THREAD_END(2),
	
	/**
	 * 3	Application end 
	 * The very last record of the trace. Present only if the traced application exited gracefully. 
	 * 	i32	ExitCode	Windows process exit code
	 *
	 */
	APP_END(3),
	
	/**
	 * 4	System call entry
	 * Thread is about to enter kernel mode through one of the various kind of system calls. Typically follows a syscall/sysenter/int 2e instruction record. 
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
	 * 	rmContext	Context	Value of all registers just before the call. Additional information, such as call parameters on the stack, is in the System calls table.
	 *
	 */
	SYSCALL_ENTRY(4),
	/**
	 * 5	System call exit 
	 * Thread just exit from kernel mode system call. Does not always immediately follow a system call entry record, and may not even match an entry at all (matching is done according to the stack pointer). Actually, system calls can be nested, involve user-mode callbacks, and can end on an exception or a context change. Also, some system calls, such as NtContinue, can shortcut a few system call exits at once.
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table, or -1 if there is no corresponding system call entry record (thus nothing in System calls table either). The latter case is semantically equivalent to a context change.
	 * 	rmContext	Context	Value of all registers right after the call. Additional information may be found in the System calls table.
	 *
	 */
	SYSCALL_EXIT(5),
	/**
	 * 6	Skipped system call exit 
	 * A previous system call entry occurred, but starting from this record, the stack was unwound further than it would be on a normal system call exit (a “return” to the user-mode caller). In other words, the thread is not anymore in the context of that system call, and that call will never have a “System call exit” record.
	 * 	i64	SyscallID	Corresponding system call record ID in System calls table.
	 * 
	 */
	SKIPPED_SYSCALL_EXIT(6),
	
	/**
	 *  8	Context change – Asynchronous Procedure Call 
	 * For one of the above reasons, the thread execution context just changed. 
	 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
	 */
	CONTEXT_CHANGE_ASYNC_PROC_CALL(8),
	/**
	 *  9	Context change – Exception Handling 
	 * For one of the above reasons, the thread execution context just changed. 
	 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
	 */
	CONTEXT_CHANGE_EXCEPTION_HANDLING(9),
	/**
	 *  10	Context change – Callback 
	 * For one of the above reasons, the thread execution context just changed. 
	 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
	 */
	CONTEXT_CHANGE_CALLBACK(10),
	/**
	 *  11	Context change – Unknown reason 
	 * For one of the above reasons, the thread execution context just changed. 
	 * 	rmContext	Context	Value of all registers right after the context change, before carrying execution of the next instruction.
	 */
	CONTEXT_CHANGE_UNKNOWN_REASON(11),
	
	NULL(-1);
	
	final public int number;
	
	private ExecLineType(int number){
		this.number = number;
	}
	
	public static ExecLineType getEnum(int number){
		for(ExecLineType value: ExecLineType.values()){
			if(value.number == number){
				return value;
			}
		}
		return NULL;
	}
}
