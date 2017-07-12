package ca.uvic.chisel.atlantis.bytecodeparsing.externals;

public class ThreadRec {
	
	static private final int BYTE_SIZE_THREAD_REC = ((64 * 4) + (32 * 2)) / 8; // defined as 2 ints and 4 longs

	ThreadRec(ThreadItable parentParser, long threadIdFromExecRec){
		// There are this.recordCount of these, and are all direct addressable via the offset file.
		long offsetForLineNumber = parentParser.tableFp + threadIdFromExecRec * BYTE_SIZE_THREAD_REC; // 40 matched rowSize when I checked
		
		// The offset file happens to have an extra entry at the very end for precisely this porpoise. Fintastic!
		parentParser.seekToByteAddress(offsetForLineNumber);
		
		this.id = parentParser.getNextInt();

		this.winTid = parentParser.getNextInt();

		this.tibAddr = parentParser.getNextLong();

		this.firstEid = parentParser.getNextLong();

		this.lastEid = parentParser.getNextLong();

		this.recCount = parentParser.getNextLong();
	}
	
	/** ver Oct 2013
	 * Self-ID in the current table.
	 * 
	 * 32-bit int
	 */
	public final int id;

	/** ver Oct 2013
	 * Windows thread ID while it was running.
	 * 
	 * 32-bit int
	 */
	public final int winTid;

	/** ver Oct 2013
	 * Address of Thread Information Block. TIB contents is recorded
	 * along with registers context in “thread begin” execution records (see section 6.3).
	 * 
	 * 64-bit long
	 */
	public final long tibAddr;

	/** ver Oct 2013
	 * First execution record for that thread, normally of type “thread begin”.
	 * 
	 * 64-bit long
	 */
	public final long firstEid;

	/** ver Oct 2013
	 * Last execution record for that thread, normally of type “thread end”,
	 * or  1 if the tracer was interrupted while the thread was still running.
	 * 
	 * 64-bit long
	 */
	public final long lastEid;

	/** ver Oct 2013
	 * Number of execution records that belong to that thread.
	 * 
	 * 64-bit long
	 */
	public final long recCount;
}