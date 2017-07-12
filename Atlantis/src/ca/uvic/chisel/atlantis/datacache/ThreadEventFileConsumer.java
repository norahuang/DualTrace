package ca.uvic.chisel.atlantis.datacache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.database.ThreadEventDbConnection;
import ca.uvic.chisel.atlantis.database.DbConnectionManager.TableState;
import ca.uvic.chisel.atlantis.models.IEventMarkerModel;
import ca.uvic.chisel.atlantis.models.ThreadEventType;
import ca.uvic.chisel.atlantis.models.TraceThreadEvent;
import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;

public class ThreadEventFileConsumer implements IFileContentConsumer<TraceLine> {
	
	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();


	private int lineNumber;
	private TraceThreadEvent lastEvent;
	private String previousThreadId;

	private ThreadEventDbConnection threadEventDatabase;

	public ThreadEventFileConsumer(ThreadEventDbConnection threadEventDatabase) {
		this.threadEventDatabase = threadEventDatabase;
	}

	@Override
	public boolean verifyDesireToListen() {
		threadEventDatabase.createTables();
		return threadEventDatabase.getTableState() != TableState.POPULATED;
	}
	
	@Override
	public void readStart() {
		lineNumber = 0;
		lastEvent = null;
		previousThreadId = null;
	}

	@Override
	public int handleInputLine(String line, TraceLine lineData) {
		
		timeAccumulator.progCheckIn();
		
		String tid = getTIDFromTraceLine(line, lineData);
		ThreadEventType eventType = ThreadEventType.getThreadEventType(line, previousThreadId, lastEvent, lineData);
		if(null != eventType){
			
			if(lastEvent != null) {
				lastEvent.setNumLines(lineNumber - lastEvent.getLineNumber());
				
				timeAccumulator.dbCheckIn();
				
				this.threadEventDatabase.saveThreadEventMarker(lastEvent);
				
				timeAccumulator.progCheckIn();
			}
			
			int pixelStartingPoint = (null == lastEvent) ? 0 : lastEvent.getPixelEnd();
			String thisTid = (tid == null) ? previousThreadId : tid;
			TraceThreadEvent event = new TraceThreadEvent(thisTid, lineNumber, eventType, pixelStartingPoint);
			
			lastEvent = event;
		}
		
		if(null != tid && !tid.equals(previousThreadId)){
			previousThreadId = tid;
		}
		
		lineNumber++;
		
		if(lastEvent != null) {
			lastEvent.setNumLines((int) (lineNumber - lastEvent.getLineNumber()));
		}
		
		timeAccumulator.checkOutAll();
		
		return 1; // Uses lineData too, but still uses line.
	}

	@Override
	public void readFinished() {
		timeAccumulator.progCheckIn();
		if(lastEvent != null) {
			lastEvent.setNumLines((int) (lineNumber - lastEvent.getLineNumber()));
			
			timeAccumulator.dbCheckIn();
			
			threadEventDatabase.saveThreadEventMarker(lastEvent);
			
			timeAccumulator.progCheckIn();
		}
		
		timeAccumulator.dbCheckIn();
		
		threadEventDatabase.executeInsertLineNumberBatch();
		threadEventDatabase.doFinalCommit();
		
		timeAccumulator.checkOutAll();
		
		System.out.println(this.getClass()+":"+"Total time to preparing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commit thread event info to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println();
	}
	
	@Override
	public void indexCreationAborted() {
		this.threadEventDatabase.abortCommitAndDropTable();
	}
	
	/**
	 * 
	 * Parses a line from a thread start or switch and attempts to get the TID of the thread.
	 * If a TID cannot be found, it returns -1, this is the case for EndThreadEvents
	 * 
	 * @param line
	 * 		The line to be parsed
	 * @param lineData 
	 * @return
	 * 		An integer representing the TID of the thread.
	 */
	private String getTIDFromTraceLine(String line, TraceLine lineData) {
		if(null != lineData){
			return lineData.threadId+"";
		} else {
			return getTIDFromTraceLineLegacy(line);
		}
	}
	
	private String getTIDFromTraceLineLegacy(String line) {
		// XXX TODO Clean this up once we decide we do not need the old data format at all.
		// Mostly, we only need the new format ID, and we don't care about the event type
		// to find it. Also, add the TIB and WindowsTID to the database.
		String tid = null;
		// These are unused for now.
		String threadInformationBlock = null;
		String windowsTID = null;
		
		String currentNewFormatThreadId = ThreadEventType.getNewFormatThreadIdFromLine(line);
		
		// TODO Pass null as previous event here, but we won't even need this later to get the ids when
		// we get rid of the old format.
		ThreadEventType eventType = ThreadEventType.getThreadEventTypeLegacy(line, previousThreadId, null);
		if(eventType == ThreadEventType.THREAD_BEGIN) {
			// Extract TID of new thread
			StringTokenizer tokenizer = new StringTokenizer(line);
			
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				// TODO This is old format...can continue supporting?
				// TODO Support new format, like:
				// 0 01 {64 OFF} ThreadBegin | TIB=000007fffffde000 WindowsTID=00001b0c | RFLAGS=ZP RAX=0000000000400001 RCX=000078d006790000 RDX=0000000000400000 RBX=00000000002237a0 RSP=000000000012f210 RBP=0000000077665468 RSI=0000000000000000 RDI=0000000000000000 R8=00000000000000e8 R9=000000000012e518 R10=0000000000000000 R11=000000000012f200 R12=0000000077455ea0 R13=0000000000000000 R14=0000000000000002 R15=0000000000000000 MXCSR=00001f80 FSBASE=0000000000000000 GSBASE=000007fffffde000 X87CONTROL=027f X87STATUS=0000 X87TAG=00 X87OPCODE=0000 X87LASTIP=86df000000000000 X87LASTDP=0000000000000000 CS=0033 DS=002b ES=002b SS=002b FS=0053 GS=002b ST0=00000000000000000000 ST1=00000000000000000000 ST2=00000000000000000000 ST3=00000000000000000000 ST4=00000000000000000000 ST5=00000000000000000000 ST6=00000000000000000000 ST7=00000000000000000000 XMM0=00000000000000000000000000000000 XMM1=00000000000000000000000000000000 XMM2=00000000000000000000000000000000 XMM3=00000000000000000000000000000000 XMM4=00000000000000000000000000000000 XMM5=00000000000000000000000000000000 XMM6=00000000000000000000000000000000 XMM7=00000000000000000000000000000000 XMM8=00000000000000000000000000000000 XMM9=00000000000000000000000000000000 XMM10=00000000000000000000000000000000 XMM11=00000000000000000000000000000000 XMM12=00000000000000000000000000000000 XMM13=00000000000000000000000000000000 XMM14=00000000000000000000000000000000 XMM15=00000000000000000000000000000000 [000007fffffde000]=(2000){ 00 00 00 00 00 00 00 00 00 00 13 00 00 00 00 00 00 d0 12 00 00 00 00 00 00 00 00 00 00 00 00 00 ... }
				if (token.startsWith("TID=")) {
					// TID is always base 10, old format
					tid = new String(token.substring(token.indexOf("=") + 1));
				} else if(token.startsWith("WindowsTID=")){
					// WindowsTID is always base 16, new format
					threadInformationBlock = new String(token.substring(token.indexOf("=") + 1));
				} else if(token.startsWith("TIB=")){
					// TIB is always base 16, new format
					windowsTID = new String(token.substring(token.indexOf("=") + 1));
				}
			}
			
			// New format puts TID as second hex token (after hex line number)
			if(tid == null){
				tid = currentNewFormatThreadId;
			}
		}
		
		else if(eventType == ThreadEventType.OLD_EXPLICIT_THREAD_SWITCH) {
			// *Explicit* thread switches do not exist in the new format.
			// Extract the ID of the thread to which we are switching
			int colon = line.indexOf(":");
			int threadSwitch = line.indexOf("switch");
			try {
				tid = new String(line.substring(colon + 1, threadSwitch - 1));
				tid = tid.trim();
			} catch (StringIndexOutOfBoundsException e) {
				return null;
			}
		}
		
//		else if(currentNewFormatThreadId != null && currentNewFormatThreadId != previousThreadId){
		else if(eventType == ThreadEventType.IMPLICIT_THREAD_SWITCH){
			tid = currentNewFormatThreadId;
		}
		else if(eventType == ThreadEventType.APPLICATION_END){
			tid = currentNewFormatThreadId;
		}
		else if(eventType == ThreadEventType.THREAD_END){
			// Wasn't handled in old format, but don't really need in new format either...
			tid = currentNewFormatThreadId;
		}
		
		if(null == tid){
			tid = currentNewFormatThreadId;
		}
		
		return tid;
	}	
	
	@Override
	public boolean requiresPostProcessing() {
		return false;
	}

	@Override
	public void doPostProcessing(IProgressMonitor monitor) {}
}
