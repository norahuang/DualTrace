package ca.uvic.chisel.bfv.filebackend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import ca.uvic.chisel.bfv.datacache.AbstractLine;
import ca.uvic.chisel.bfv.datacache.FileLine;
import ca.uvic.chisel.bfv.editor.IFileLineBackend;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

/**
 * This backend uses random access on two files: on the index file, which acts as an array
 * of character offsets for the corresponding lines in the file; and the file itself.
 * When accessing a line, the index is accessed with random access by multiplying the offset width
 * by the line number; this gives the character offset to get to the desired line in the index file.
 * That line in the index file contains the actual line's offset; thus the offset found there
 * is used to do a random access on the actual file, giving the desired line contents.
 * 
 * This approach avoids scanning files, and also reduces dependency on a RDBMS for the purposes of
 * viewing a large file. Other functionality may depend on the previous DB based file access. 
 * 
 * The index file should look like the following (except stuff in brackets):
 * [0] 			01379850	[count of lines in file]
 * [1] 			00000000	[first line's offset, always 0]
 * [2] 			00000013	[second line starts 12 characters plus a newline away, which adds to 13 bytes]
 * [...]		...
 * [1379851]	00985746	[the 1379850th line starts 985746 bytes into the file. Last line!]
 * [1379852]	00985755	[the 1379850th line *ends* 985755 bytes into the file. Could redesign by including line lengths beside each offset as well.]
 * 
 * Line offsets are always stored at the line number + 1, because we must store the total number of lines
 * at the beginning of the file.
 *
 */
public class FileLineFileBackend implements IFileLineBackend {
	
	static IFileUtils fileUtil = RegistryUtils.getFileUtils();
	
	private final File file;
	/**
	 * During writing, it points at a temp file, and when completed, the file is renamed,
	 * then deleted, at which point we have to use the other File object.
	 */
	private final IFile indexFileTemp;
	private final IFile indexFile;
	private long linesWritten;
	private RandomAccessFile writer;
	private RandomAccessFile indexReader = null;
	private RandomAccessFile fileReader;
//	private final String indexFilePath;
	private long totalNumberOfLines;
	
	// XXX is this a safe assumption to be making?
	private Charset fileCharset = StandardCharsets.ISO_8859_1;
	private Charset indexCharset = StandardCharsets.UTF_8;
	
	/**
	 * For final block offset, we need the next line to have its end of line offset,
	 * se we need to track the final offset, to compute that one.
	 */
	private long prevWrittenLineOffset;
	private long prevOffsetLineLength;
	
	// TODO This is shorter than the maximum long length, which is 19 (9,223,372,036,854,775,807)
	// If we need longer, make it bigger. If backwards compatibility is an issue,
	// make this check the index file's first line entry to determine the block index char width.
	// 99 billion can be represented with 16 digits, so we'll try that for starters.
	private static int LONG_MAX_WIDTH = 19; // one digit bigger than an int can represent
	private static String LONG_FORMAT = "%0"+LONG_MAX_WIDTH+"d";
		
	private static final char NEW_LINE_ENDING = '\n';
	private static final char CARRIAGE_RETURN_ENDING = '\r';
	
	/**
	 * Temporary index. This should only be used while processing, and should
	 * be renamed to the actual index extension upon completion.
	 */
	private static final String TEMP_FILE_EXTENSION = "._tmp_index";
	
	/**
	 * This extension indicates the file index, and is best used *including* the original
	 * file extension, for the general case where files could be used that only differ in
	 * their original extension.
	 */
	private static final String FILE_EXTENSION = "._index";
	
	/**
	 * The index files reside in a directory hidden from Eclipse's navigator view.
	 *
	 */
	public static final String INDEX_FILE_DIRECTORY_NAME = ".index";
		
	static public boolean checkIfFileHasIndex(File file){
		IFile indexFile = convertFilePathToIndexFileDirPath(file);
		return indexFile.exists();
	}
	
	public FileLineFileBackend(File file){
		this.file = file;
		IFile f = BfvFileUtils.convertFileIFile(file);
		this.indexFileTemp = convertFilePathToTemporaryIndexFileDirPath(f);
		this.indexFile = convertFilePathToIndexFileDirPath(file);
		
		try {
			// Read only. The file better exist if we are trying to open it.
			this.fileReader = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// On runs subsequent to the first, we use this for access, and to indicate that
		// the consumer doesn't need to run.
		prepareRandomAccessIndexFile();
	}
	
	@Override
	public void initialize() {
		try{
			if(indexFile.exists()){
				return;
			}
			
//			boolean success = this.indexFileTemp.createNewFile();
			
			// Create the temporary index file.
			fileUtil.createEmptyFile(this.indexFileTemp);
				
			// Just for truncating if necessary.
			writer = new RandomAccessFile(this.indexFileTemp.getRawLocation().toOSString(), "rw");
			
			// Do what? If it exists already, let's clobber it.
			// We may have exited poorly before.
			// Given we rename it on success, this is safe.
			this.linesWritten = 0;
			writer.setLength(0);
			writeTotalLinesHeader();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public boolean isFreshlyInitialized(){
		// Binary...compared to trinary possible values for the DB implementation.
		return null == this.indexReader;
	}

	@Override
	public void finish() {
		try {
			// Must not write the final line prior to writing total lines header
			// because it uses some of the same member variables for line counting.
			writeTotalLinesHeader();
			writeFinalLineOffset();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		// Move, then delete temp that we point at.
//		this.indexFileTemp.renameTo(new File(indexFilePath));
//		this.indexFileTemp.delete();
		// With IFile, I think moving is sufficient.
		try {
			this.indexFileTemp.refreshLocal(IResource.DEPTH_ZERO, null);
			this.indexFileTemp.move(this.indexFile.getFullPath(), false, null);
		} catch (CoreException e) {
			// Shouldn't be able to hit this...
			e.printStackTrace();
		}
		this.writer = null;
		prepareRandomAccessIndexFile();
	}
	
	private void prepareRandomAccessIndexFile(){
		// Check to see if the final version of the index exists, not the temp version.
		if(indexFile.exists() && isFreshlyInitialized()){
			try {
				this.indexReader = new RandomAccessFile(this.indexFile.getRawLocation().toOSString(), "r");
				// The best efficient way to do this with a single file
				// is to make the *first line* the *total number of lines*,
				// then to use +1 offsets on all line offset lookups.
				// See class documentation.
				// Thus here we actually ask for the -1 line! Ha!
				totalNumberOfLines = getOffsetFromIndexForLine(-1);
			} catch (FileNotFoundException e) {
				// Bad news if we get here, because the file exists according to conditional...
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void close(){
		if(null != this.indexReader){
			try {
				this.indexReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(null != this.writer){
			try {
				this.writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void abortAndDeleteIndex() {
		try {
			this.indexFileTemp.refreshLocal(IResource.DEPTH_ZERO, null);
			if(indexFileTemp.exists()){
				if(null != this.writer){
					try {
						this.writer.close();
					} catch (IOException e) {
					}
				}
				this.indexFileTemp.delete(true, null);
			}
			
			try { this.fileReader.close(); } catch (IOException e) {}
			File emptyFile = fileUtil.convertFileToBlankFile(file);
			if(emptyFile.exists()){
				emptyFile.delete();
			}
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean saveFileLine(long lineNumber, long lineOffset, String lineContents, AbstractLine lineData) {
		// This mostly ignores the actual line contents, since we do random access to the original
		// file to grab contents when required. We are merely finding each line's byte offset
		// within that original file.
		// The block offset *must* be of a fixed width.
		
		String lineOffsetString =  formatBlock(lineOffset);
		
		try {
			// writeChars does it wrong.
			writer.seek((1 + lineNumber) * (LONG_MAX_WIDTH + 1));
			writer.writeBytes(lineOffsetString);
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		    return false;
		}
		
		prevWrittenLineOffset = lineOffset;
		prevOffsetLineLength = lineContents.length();
		
		linesWritten++;
		return true;
	}
	
	private String formatBlock(long lineOffset){
		String string = String.format(LONG_FORMAT, lineOffset)+NEW_LINE_ENDING;
		return string;
	}
	
	private void writeFinalLineOffset(){
		saveFileLine(linesWritten, prevWrittenLineOffset+prevOffsetLineLength, "", null);
	}
	
	private void writeTotalLinesHeader() {
		// Write a placeholder line for the total file length in lines.
		// Later, in finalizeIndexFile(), we will overwrite this.
		try {
			writer.seek(0);
			writer.writeBytes(formatBlock(linesWritten));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<String> getLineRange(int startRangelineNumber, int endRangelineNumber) {
		List<String> lineResults = new ArrayList<String>();
		StringFileContentGatherer gatherer = new StringFileContentGatherer(startRangelineNumber, lineResults);
		
		return getFileLines(startRangelineNumber, endRangelineNumber, gatherer);
	}

	@Override
	public List<FileLine> getFileLineRange(int startRangelineNumber, int endRangelineNumber) {
		List<FileLine> lineResults = new ArrayList<FileLine>();
		FileLineFileContentGatherer gatherer = new FileLineFileContentGatherer(startRangelineNumber, lineResults);
		
		return getFileLines(startRangelineNumber, endRangelineNumber, gatherer);
	}
	
	// This method makes the assumption that all valid files end in either \r\n or in \n
	public String getFileDelimiter() {
		String line = getFileLines(0, 0, new StringFileContentGatherer(0, new ArrayList<String>())).get(0);
		
		if(line.endsWith("" + CARRIAGE_RETURN_ENDING + NEW_LINE_ENDING)) {
			return "" + CARRIAGE_RETURN_ENDING + NEW_LINE_ENDING;
		} else {
			return "" + NEW_LINE_ENDING;
		}
	}
	
	protected <T> List<T> getFileLines(int startRangelineNumber, int endRangelineNumber, AbstractFileContentGatherer<T> gatherer){
		if(totalNumberOfLines == 0) {
			return new ArrayList<>();
		}
		
		// NB No matter what method we use, using byte arrays with associated get() method
		// leads to the fastest file read times.
		// See http://nadeausoftware.com/articles/2008/02/java_tip_how_read_files_quickly#RandomAccessFilewithbytearrayreads
		// Advises strongly:
		// 1. FileChannel with MappedByteBuffer and array reads, with buffer/array size of 2-4KBytes
		// 2. FileChannel with a direct ByteBuffer and array reads, with buffer of 128KBytes
		// Since memory mapping files takes up virtual address space, and some users could conceivably
		// be unlucky enough to be running 32-bit systems, we will use the non-mapping approach.
		// ALthough it seems better, the benchmarks here indicate that the direct ByteBuffer approach
		// is as fast.
		// But...the RandomAccessFile method is approximately 25% slower (on order of 0.4s to 0.5s), but has a much nicer interface.
		// I am going to use the RandomAccessFile approach, instead of prematurely optimizing.
		// http://www.codinghorror.com/blog/2009/01/the-sad-tragedy-of-micro-optimization-theater/comments/page/2/
		

		// Ut oh! I need the length of the final line to do this correctly!
		// That is, I actually need the start offset for the *next* line,
		// which is also interpretable as the offset for the end of the line (+1 for the newline).
		// I will make sure to write the offset to the end of the very final line.
		// Other than that, we have to add +1 to the second offset lookup.
		final long firstOffset = getOffsetFromIndexForLine(startRangelineNumber);
		final long secondOffset = getOffsetFromIndexForLine(endRangelineNumber + 1);
		
		try {
			this.fileReader.seek(firstOffset);
			long totalLineRangeByteWidth = secondOffset - firstOffset;
			if(totalLineRangeByteWidth > (long)(Integer.MAX_VALUE)){
				// If this happens, we have to do multiple reads...
				// I don't know if the caller should be satisfied if we
				// are giving back 2GB in one chunk.
				// If we do have need for this, we right a loop.
				System.out.println("Large chunk of lines request, technical limitation prevent a larger chunk.");
				return null;
			}
			// Would loop here if we allowed larger requests by breaking it up.
			// Would we actually want to allow that in production? Editors won't ask for that much
			// given the way things page in. Therefore, no loop over gigantic byte requests yet.
			
			byte[] lineRangeResultByteArray = new byte[(int)totalLineRangeByteWidth];
			fileReader.readFully(lineRangeResultByteArray, 0, (int)totalLineRangeByteWidth);
			// Gather into return value
			StringBuilder lineBuffer = new StringBuilder();
			for(int i = 0; i < lineRangeResultByteArray.length; i++){
				lineBuffer.append((char)lineRangeResultByteArray[i]);
				// If this is an EOL, and the next one isn't, we are done the line.
				if((i+1 >= lineRangeResultByteArray.length) ||
				   ((NEW_LINE_ENDING == lineRangeResultByteArray[i] || CARRIAGE_RETURN_ENDING == lineRangeResultByteArray[i])
						&&
				   (NEW_LINE_ENDING != lineRangeResultByteArray[i+1] && CARRIAGE_RETURN_ENDING != lineRangeResultByteArray[i+1]))
				   ){
					// Drop newline and add collected characters.
					gatherer.addLine(lineBuffer.toString());
					// Re-allocating with a new one is supposed to be cheap.
					lineBuffer = new StringBuilder();
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return gatherer.resultsList;
	}
	
	/**
	 * This is used to gather file lines into the specialized container that is provided.
	 *
	 */
	private abstract class AbstractFileContentGatherer<T> {
		protected int startLine;
		protected final List<T> resultsList;
		
		public AbstractFileContentGatherer(int startLine, List<T> resultsList){
			this.startLine = startLine;
			this.resultsList = resultsList;
		}
		
		public void addLine(String line){
			resultsList.add(transformLine(line));
		}
		
		abstract public T transformLine(String line);
		
	}
	
	private class StringFileContentGatherer extends AbstractFileContentGatherer<String> {
		public StringFileContentGatherer(int startLine, List<String> resultsList) {
			super(startLine, resultsList);
		}

		@Override
		public String transformLine(String line) {
			return line;
		}

	}
	
	/**
	 * Specialized gatherer for diff system. Has a +1 offset to starting line, as
	 * expected of that system.
	 *
	 */
	private class FileLineFileContentGatherer extends AbstractFileContentGatherer<FileLine> {
		public FileLineFileContentGatherer(int startLine, List<FileLine> resultsList) {
			super(startLine + 1, resultsList);
		}
		
		@Override
		public FileLine transformLine(String line) {
			int lineNumber = startLine + resultsList.size();
			FileLine fileLine = new FileLine(lineNumber, line);
			// Start line needs to be +1 for external users of FileLine, but we need
			// the original line number to get the offset.
			fileLine.setLineOffset(getOffsetFromIndexForLine(lineNumber - 1));
			return fileLine;
		}
	}

	@Override
	public int getNumberOfLines() {
		return (int) totalNumberOfLines;
	}
	
	/**
	 * This method will return the number of lines in the file if passed -1, otherwise it will return the offset to the line number passed in.
	 */
	public long getOffsetFromIndexForLine(long lineNumber){
		byte[] indexLookupResultByteArray = new byte[LONG_MAX_WIDTH];
		try {
			// First +1 for header line, second +1 for LINE_ENDING length
			long entryStartPosition = (1 + lineNumber) * (LONG_MAX_WIDTH + 1);
			this.indexReader.seek(entryStartPosition);
			indexReader.readFully(indexLookupResultByteArray, 0, LONG_MAX_WIDTH);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String offset = new String(indexLookupResultByteArray, indexCharset);
		
		return Long.parseLong(offset);
	}

	@Override
	public void cancelCurrentlyRunningSearchStatement() {
		// TODO Auto-generated method stub
		
	}
	
	public static IFile convertFilePathToTemporaryIndexFileDirPath(IFile filePath){
		// The index file directory will be placed at project root, so prepend this onto the
		// part of the path that begins with the project path. I do it this way because I cannot
		// anticipate relative or absolute paths as arguments.
		String fileName = filePath.getProjectRelativePath().toString();
		fileName = fileUtil.convertFilePathToSuffix(fileName, TEMP_FILE_EXTENSION);
		return filePath.getParent().getFile(new Path(INDEX_FILE_DIRECTORY_NAME+"/"+fileName));
	
	}
	
	public static IFile convertFilePathToIndexFileDirPath(File filePath){
		// The index file directory will be placed at project root, so prepend this onto the
		// part of the path that begins with the project path. I do it this way because I cannot
		// anticipate relative or absolute paths as arguments.
		IFile f = BfvFileUtils.convertFileIFile(filePath);
		String fileName = f.getProjectRelativePath().toString();
		fileName = fileUtil.convertFilePathToSuffix(fileName, FILE_EXTENSION);
		return f.getParent().getFile(new Path(INDEX_FILE_DIRECTORY_NAME+"/"+fileName));
	}

	@Override
	public FileSearchQuery createSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive, FileTextSearchScope scope) {
		return new FileSearchQuery(searchText, isRegEx, isCaseSensitive, scope);
	}
	
}
