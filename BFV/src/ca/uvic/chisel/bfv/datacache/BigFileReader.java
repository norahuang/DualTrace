package ca.uvic.chisel.bfv.datacache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.bfv.utils.LineConsumerTimeAccumulator;


public class BigFileReader {
	
	static protected long notifyReadStart(List<IFileContentConsumer> fileContentConsumers){
		long startTime = System.currentTimeMillis();
		for(IFileContentConsumer consumer : fileContentConsumers) {
			consumer.readStart();
		}
		return startTime;
	}
	
	static protected void notifyReadFinish(final List<IFileContentConsumer> fileContentConsumers, long startTime, FileModelProvider fileProvider) {
		System.out.println("Total time to consume prior to finalizing file: "+ (System.currentTimeMillis() - startTime)/1000+"s");
		
		for(IFileContentConsumer consumer : fileContentConsumers) {
			consumer.readFinished();
		}
		
		System.out.println("Total time to consume file: "+ (System.currentTimeMillis() - startTime)/1000+"s");
		try {
			System.out.println("File name: "+fileProvider.getWrappedFile().getFilePath());
			System.out.println("File size: "+fileProvider.getWrappedFile().getFileSize());
			System.out.println("File output size: "+fileProvider.getWrappedFile().getFileOutputSizeOnDisk());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();

	}
	
	
	public interface FileWrapper {
		public boolean validate();
		public int getFileSize();
		public long getFileSizeOnDisk();
		public long getFileOutputSizeOnDisk();
		public String getFilePath();
		/**
		 * Reports the countable work done, reportable as of completion of the preceding getLine() call.
		 * 
		 * TODO should this count from the previous call to it, or just from the previous call to getLine()?
		 * 
		 * @return
		 */
		public int getLinesWorked();
		public long getLinesCompleted();
		public AbstractLine getLine() throws IOException;
		public void dispose() throws IOException;
	}
	
	public static AbstractLineIdGenerator idGen;
	
	/**
	 * The actual file to read could be in a number of formats. Access is via the {@link FileWrapper}, which is provided
	 * by the {@link FileModelDataLayer}. Even details such as how to count progress (lines vs bytes) differs between
	 * currently available formats (plain text, vs binary format).
	 * 
	 * @param monitor
	 * @param fileContentConsumers
	 * @param fileProvider
	 * @return
	 * @throws Exception
	 */
	static protected boolean readFile(IProgressMonitor monitor, List<IFileContentConsumer> fileContentConsumers, FileModelProvider fileProvider) throws Exception {
		
		FileWrapper wrap = fileProvider.getWrappedFile();
		
		if(!wrap.validate()){
			return false;
		}
		
		monitor.beginTask(fileProvider.getFileBuildText(), wrap.getFileSize());
				
		String str = "";
		AbstractLine line;
		long lineRead = 0;
		LineConsumerTimeAccumulator timeAccumulator = new LineConsumerTimeAccumulator();
		timeAccumulator.progCheckIn();
		while((line = wrap.getLine()) != null) {
			str = line.getStringRepresentation();
			
			// This is only needed here if we are supporting textual traces/
			// Binary format gets the isntructionId generated elsewhere, in advance (when making
			// the lineData object).
			if(null != idGen){
				idGen.generateLineId(str, line);
			}
			
			if(lineRead % 1000000 == 0){
				// Convenient for Gibraltar, when we don't have a progress bar.
				System.out.println("Read "+(lineRead/1000000)+" million lines so far...");
				timeAccumulator.checkOutAll();
				System.out.println("Time: "+(timeAccumulator.getProgSeconds()/(60.0*60.0))+" hours ("+timeAccumulator.getProgSeconds()+" seconds)" +"["+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))+"]");
				timeAccumulator.progCheckIn();
				System.out.println("Consumers that did used text format: "+numUsedTextFormat);
			}
			lineRead++;
			numUsedTextFormat = 0;
			for(IFileContentConsumer consumer : fileContentConsumers) {
				if(monitor.isCanceled()){
					return false;
				}
				try{
					numUsedTextFormat += consumer.handleInputLine(str, line);
				} catch(Exception e){
					throw e;
				}
			}
			monitor.worked(wrap.getLinesWorked());
		}
		wrap.dispose();
		return true;
	}

	static int numUsedTextFormat;
	public interface FileModelProvider {
		public void setFileReadComplete(boolean value);
		public String getFileBuildText();
		public FileWrapper getWrappedFile() throws IOException;
		public boolean getFileReadComplete();
		public void setFileReadRunning(boolean value);
		public boolean getFileReadRunning();
		public void updateDecorator();
		public String getFinalizingText();
	}
	
	/**
	 * Reads through the file a line at a time and initializes all of the data that will be kept from the file.
	 */
	static public void doFileRead(final boolean headless, final List<IFileContentConsumer> fileContentConsumers, final FileModelProvider fileProvider) throws Exception {
		// This might remove all consumers for this file load, if all of them are implemented to persist their
		// data. If so, we don't want to read the file...
//		removePersistedConsumers();
		
		// Have each consumer verify that it wants to listen.
		// This is to support consumers that persist their results and
		// can load them without involving themselves in scanning the file.
		for(Iterator<IFileContentConsumer> iterator = fileContentConsumers.iterator(); iterator.hasNext();) {
			IFileContentConsumer consumer = iterator.next();
			if(!consumer.verifyDesireToListen()){
				System.out.println("Removing FileContentListener "+consumer.getClass());
				iterator.remove();
			}
		}
		
		final boolean consumersWantFile = fileContentConsumers.size() > 0;
		
		if(!consumersWantFile) {
			fileProvider.setFileReadComplete(true);
			System.out.println("Trace already processed, DB present. If fresh processing is needed. please delete DB file and try again.");
			return;
		}
		
		// Need it mutable, and final, so I need a wrapper around it.
		final ArrayList<Long> startTime = new ArrayList<Long>();
		ProgressMonitorDialog dialog;
		if(headless){
			dialog = null;
		} else {
			dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
		}
		
		runReadStep(dialog, createFirstRead(fileProvider, startTime, fileContentConsumers));
		if(fileProvider.getFileReadRunning()) {
			runReadStep(dialog, createPostRead(fileProvider, fileContentConsumers));
		}
		if(fileProvider.getFileReadRunning()) {
			runReadStep(dialog, createCompleteRead(fileProvider, fileContentConsumers, startTime, consumersWantFile));
		}
	}

	private static void runReadStep(IRunnableContext dialog, IRunnableWithProgress readStep) {
		try {
			if(null == dialog){
				readStep.run(new NullProgressMonitor());
			} else {
				dialog.run(true, true, readStep);
			}
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	static IRunnableWithProgress createFirstRead(final FileModelProvider fileProvider, final ArrayList<Long> startTime, final List<IFileContentConsumer> fileContentConsumers){
		return new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
				
				try {
					fileProvider.setFileReadRunning(true);
					startTime.add(notifyReadStart(fileContentConsumers));
				
					
					boolean success = readFile(monitor, fileContentConsumers, fileProvider);
					
					if(!success) {
						// Tell all the consumers that the index creation failed
						for(IFileContentConsumer consumer: fileContentConsumers){
							consumer.indexCreationAborted();
						}
						fileProvider.setFileReadComplete(false);
						fileProvider.setFileReadRunning(false);
					}
				} catch (Exception e) {
					e.printStackTrace();
					// Tell all the consumers that the index creation failed
					for(IFileContentConsumer consumer: fileContentConsumers){
						consumer.indexCreationAborted();
					}
					fileProvider.setFileReadComplete(false);
					fileProvider.setFileReadRunning(false);
				} finally {
					// fileRead = (running == false) ? true : false;
					monitor.done();
				}
				try {
					try{
						ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
					} catch(IllegalStateException e){
						// Nothing.
					}
					Display.getDefault().asyncExec(new Runnable(){
						@Override
						public void run(){
							fileProvider.updateDecorator();
						}
					});
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	private static IRunnableWithProgress createPostRead(final FileModelProvider fileProvider, final List<IFileContentConsumer> fileContentConsumers) {
		return new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					for(final IFileContentConsumer consumer : fileContentConsumers) {
						// only if the consumer requires it
						if(consumer.requiresPostProcessing()) {
							consumer.doPostProcessing(monitor);
						}
					}
				} catch(Exception e) {
					// Tell all the consumers that the post processing failed
					for(IFileContentConsumer consumer: fileContentConsumers){
						consumer.indexCreationAborted();
					}
					fileProvider.setFileReadComplete(false);
					fileProvider.setFileReadRunning(false);
					e.printStackTrace();
				}
			}
		};
	}
	
	private static IRunnableWithProgress createCompleteRead(final FileModelProvider fileProvider, final List<IFileContentConsumer> fileContentConsumers, final ArrayList<Long> startTime, final boolean consumersWantFile) {
		return new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask(fileProvider.getFinalizingText(), IProgressMonitor.UNKNOWN);
				
				if(consumersWantFile) {
					try {
						notifyReadFinish(fileContentConsumers, startTime.get(0), fileProvider);
						fileProvider.setFileReadComplete(true);
					} catch(Exception ex) {
						// Tell all the consumers that the index creation failed
						ex.printStackTrace();
						for(IFileContentConsumer consumer: fileContentConsumers){
							consumer.indexCreationAborted();
						}
						fileProvider.setFileReadComplete(false);
					} finally {
						fileProvider.setFileReadRunning(false);
						monitor.done();
					}
				}
				
			}
		};
	}

}
