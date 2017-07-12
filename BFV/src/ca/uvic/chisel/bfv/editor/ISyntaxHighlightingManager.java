package ca.uvic.chisel.bfv.editor;

public interface ISyntaxHighlightingManager {

	public abstract void adjustHighlighting();

	public abstract void setHighlightingDirty();

	public abstract void beginWatchingViewerTextChanges();
	
	/**
	 * This method tells the syntax highlighting manager whether or not to actually perform 
	 * syntax highlighting every time it sees a text change, or whether or not to just keep track
	 * of the changes, and highlight at a time of the user's choosing.  
	 * 
	 * Note: It is a good idea to turn this off at times when you know you are about to fire
	 * several events that will effect highlighting, for example when you are paging in part of the 
	 * document.  After all the work is done, simply call adjustHighlighting yourself and turn this
	 * method back on.
	 */
	public abstract void setHighlightOnTextChange(boolean highlight);

}