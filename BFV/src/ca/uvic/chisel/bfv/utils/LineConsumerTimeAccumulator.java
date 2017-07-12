package ca.uvic.chisel.bfv.utils;

public class LineConsumerTimeAccumulator {

	private final TimeAccumulator progTimeAccumulator = new TimeAccumulator();
	private final TimeAccumulator dbTimeAccumulator = new TimeAccumulator();
	private final TimeAccumulator fake = new TimeAccumulator();
	
	/**
	 * Track the currently active timer, so we can avoid checking for exceptions
	 * thrown if we break the contract of closing non-open timers.
	 */
	// Initialize and start that timer to avoid checks later on.
	// We will lose this and let it get GCed. It is expendable.
	private TimeAccumulator in;
	
	public LineConsumerTimeAccumulator(){
		// Expendable, but we need to check it in so we can check it out later.
		in = fake;
		this.in.checkIn();
	}
	
	public void progCheckIn(){
		in.checkOut();
		in = progTimeAccumulator;
		in.checkIn();
	}
	
	public void dbCheckIn(){
		in.checkOut();
		in = dbTimeAccumulator;
		in.checkIn();
	}

	public void checkOutAll() {
		in.checkOut();
		in = fake;
		in.checkIn();
	}

	public long getProgSeconds() {
		return progTimeAccumulator.getSeconds();
	}

	public long getDbSeconds() {
		return dbTimeAccumulator.getSeconds();
	}
}
