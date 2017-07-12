package ca.uvic.chisel.bfv.editor;

import java.util.List;

import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import ca.uvic.chisel.bfv.datacache.AbstractLine;
import ca.uvic.chisel.bfv.datacache.FileLine;

public interface IFileLineBackend <L extends AbstractLine> {

	/**
	 * 
	 * @param lineNumber
	 * @param blockOffset
	 * @param lineContents
	 * @return
	 * @throws Exception 
	 */
	public boolean saveFileLine(long lineNumber, long blockOffset, String lineContents, L lineData) throws Exception;
	
	public String getFileDelimiter();
	
	/**
	 * Get all of the lines for the designated range.
	 * 
	 * @param startRangelineNumber
	 * @param endRangelineNumber
	 * @return
	 */
	public List<String> getLineRange(int startRangelineNumber, int endRangelineNumber);
	
	/**
	 * Get all of the lines for the designated range.
	 * 
	 * @param startRangelineNumber
	 * @param endRangelineNumber
	 * @return
	 */
	public List<FileLine> getFileLineRange(int startRangelineNumber, int endRangelineNumber);
	
	/**
	 * Returns the number of lines in the file.
	 * 
	 * @return
	 */
	public int getNumberOfLines();
	
	public void initialize();
	
	/**
	 * Indicates whether the backend is ready to be given lines.
	 *  
	 * @return
	 */
	public boolean isFreshlyInitialized();

	/**
	 * Stop processing, successfully finished input. Clean up as necessary.
	 */
	public void finish();
	
	/**
	 * Stop processing input, and abort operations early. Clean up as necessary.
	 */
	public void abortAndDeleteIndex();

	public void cancelCurrentlyRunningSearchStatement();

	/**
	 * When the backend is finished being used, call this.
	 * In particular, when the editor is closed, call this.
	 */
	void close();
	
	/**
	 * Creates a search query which is capable of searching for text in the specified
	 * file backend type.
	 */
	public FileSearchQuery createSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive, FileTextSearchScope scope);

}
