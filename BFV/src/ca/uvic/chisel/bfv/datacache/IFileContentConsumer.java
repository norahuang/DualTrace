package ca.uvic.chisel.bfv.datacache;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IFileContentConsumer<L extends AbstractLine> {

	/** 
	 * Verifies with consumer that it will actually be making use of lines provided.
	 * This is to support consumers that might have previous persisted data that they
	 * can load, rather than reproducing it again during the file processing phase.
	 */
	boolean verifyDesireToListen();
	
	/**
	 * Notifies this consumer that reading a file has begun.
	 */
	void readStart();
	
	/**
	 * Gives one line of the file to the consumer so that it can handle it.
	 * It is assumed that this will be called between a readStart and a readFinished and that
	 * it will be called one, sequentially for each line the file.
	 * 
	 * @param line
	 * @param lineData
	 * @return returns 1 if it made use of the String @param line, 0 if it used only @param lineData.
	 * @throws Exception
	 */
	int handleInputLine(String line, L lineData) throws Exception;
	
	/**
	 * Notifies this consumer that we have now finished reading the file.
	 */
	void readFinished();
	
	/**
	 * Verifies if the consumer will require post processing.
	 */
	boolean requiresPostProcessing();
	
	/**
	 * Called immediately before readFinished, this method allows arbitrary post processing on
	 * the file data.
	 * @param monitor A unique progress meter with no task.
	 */
	void doPostProcessing(IProgressMonitor monitor);
	
	/**
	 * If processing is brough to a sudden halt, the consumer may want to know in order
	 * to clean up or otherwise respond.
	 */
	void indexCreationAborted();
	
}
