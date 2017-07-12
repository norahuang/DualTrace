package ca.uvic.chisel.atlantis.datacache;

import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.bfv.datacache.IFileContentConsumer;
import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;

/**
 * Convenience class for development, can leave in when removing other consumers for sundry reasons.
 */
public class StrawManConsumer implements IFileContentConsumer<TraceLine> {
	
	private final LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();


	@Override
	public boolean verifyDesireToListen() {
		System.out.println("StrawManConsumer wants to read file. For dev purposes only!");
		return true;
	}

	@Override
	public void readStart() {
		timeAccumulator.progCheckIn();
		timeAccumulator.dbCheckIn();
		timeAccumulator.checkOutAll();
	}

	@Override
	public int handleInputLine(String line, TraceLine lineData) throws Exception {
		return 0;
	}

	@Override
	public void readFinished() {
		System.out.println(this.getClass()+":"+"Total time to preparing nothing for DB: "+ timeAccumulator.getProgSeconds()+"s");
		System.out.println(this.getClass()+":"+"Total time to insert and commit nothing to DB: "+ timeAccumulator.getDbSeconds()+"s");
		System.out.println();
	}

	@Override
	public boolean requiresPostProcessing() {
		return false;
	}

	@Override
	public void doPostProcessing(IProgressMonitor monitor) {
	}

	@Override
	public void indexCreationAborted() {
	}

}
