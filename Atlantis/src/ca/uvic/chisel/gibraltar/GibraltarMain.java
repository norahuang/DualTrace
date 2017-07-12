package ca.uvic.chisel.gibraltar;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

import javax.swing.JFileChooser;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.datacache.BinaryFormatFileModelDataLayer;
import ca.uvic.chisel.atlantis.datacache.TraceLine;
import ca.uvic.chisel.bfv.datacache.BigFileReader;
import ca.uvic.chisel.bfv.datacache.BigFileReader.FileModelProvider;
import ca.uvic.chisel.bfv.datacache.BigFileReader.FileWrapper;
import ca.uvic.chisel.bfv.datacache.AbstractLineIdGenerator;

/**
 * Gibraltar converts a customized binary trace format into a derived database used by Atlantis.
 * 
 * The database is referred to as the Gibraltar Edifice. The process is known as Gibraltar.
 * 
 * Besides being one of the Pillars of Heracles, and a signpost to the lost continent of Atlantis,
 * Gibraltar is also an edifice of imposing bulk and complexity, riddled with natural and man-made
 * caves. Notably, the fossil record is inverted, with the older fossils near the peak, and younger
 * fossils near the base. The geological forces transformed the compacted minerals into a more
 * accessible format, which humans have used for defense and exploration for ages.
 * 
 *
 */
public class GibraltarMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gibraltar gib;
		String path;
		if(args.length >= 1){
			path = args[0];
		} else{
			try(
			Scanner sc = new Scanner(System.in);
			){
				// Replacing pretty Eclipse dir prompt: "${folder_prompt: Binary Trace Folder to Process into an Atlantis SQLite DB}"
				// with a not-so-pretty JFileChooser. The prompt does not work for exported Gibraltar.
				
				JFileChooser f = new JFileChooser("Binary Trace Folder to Process into an Atlantis SQLite DB");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				File runningDir;
				try {
					runningDir = new File(GibraltarMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					f.setCurrentDirectory(runningDir);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				
		        f.showOpenDialog(null);
		        

		        System.out.println(f.getCurrentDirectory());
		        System.out.println(f.getSelectedFile());
		        if(null == f.getSelectedFile()){
		        	return;
		        }
		        path = f.getSelectedFile().getAbsolutePath();
			}
		}
		System.out.println("Gibraltar creating SQLite DB for Atlantis using files in: "+path);
		System.out.println();
		try {
			gib = new Gibraltar(path);
		
			// Check for an index. If not indexed, create consumers, feed them data.
			// Since each consumer has relatively low CPU demand (is it true?), there's
			// not much need for allowing them to be run independently. If there is need
			// later, they can be divided into independent sets and Gibrlatar can receive
			// command line args to specify which one is run.
			GibraltarHeadlessFileModelProvider fileProvider = new GibraltarHeadlessFileModelProvider(gib.binaryFileSet, gib);
			
			// This can be changed (removed?) if we stop supporting textual trace format
			BigFileReader.idGen = new AbstractLineIdGenerator<TraceLine>() {
				// This is a huge POS, and I am doing it this way only to continue to support text traces.
				@Override
				public void generateLineId(String line, TraceLine lineData) {
					fileProvider.gib.instructionConsumer.helpGenerateInstructionId(line, lineData, fileProvider.binaryFileSet);
				}
			};
			
			BigFileReader.doFileRead(true, gib.getConsumers(), fileProvider);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static public class GibraltarHeadlessFileModelProvider implements FileModelProvider {
		private boolean fileRead;
		private boolean running;
		private AtlantisBinaryFormat binaryFileSet;
		private final Gibraltar gib;

		GibraltarHeadlessFileModelProvider(AtlantisBinaryFormat file, Gibraltar gib){
			binaryFileSet = file;
			this.gib = gib;
		}
		
		@Override
		public FileWrapper getWrappedFile() throws IOException {
			return new BinaryFormatFileModelDataLayer.BinaryFileWrapper(binaryFileSet, gib);
		}

		@Override
		public void setFileReadComplete(boolean value) {
			this.fileRead = value;
		}

		@Override
		public boolean getFileReadComplete() {
			return this.fileRead;
		}

		@Override
		public void setFileReadRunning(boolean value) {
			this.running = value;
		}

		@Override
		public boolean getFileReadRunning() {
			return this.running;
		}

		@Override
		public String getFileBuildText() {
			return "";
		}

		@Override
		public void updateDecorator() {
			// NOOP
		}

		@Override
		public String getFinalizingText() {
			return "";
		}
	}

}
