package ca.uvic.chisel.bfv.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.LineElement;

@SuppressWarnings("restriction")
public class BfvFileMatch extends FileMatch {

	private final int intraLineOffset;

	public BfvFileMatch(IFile element, int offsetIntoLine, int length, LineElement lineEntry, int intraLineOffset) {
		super(element, offsetIntoLine, length, lineEntry);
		
		this.intraLineOffset = intraLineOffset;
	}
	
	public int getIntraLineOffset() {
		return intraLineOffset;
	}
	
}