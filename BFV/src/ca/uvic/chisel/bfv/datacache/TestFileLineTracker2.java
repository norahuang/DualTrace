package ca.uvic.chisel.bfv.datacache;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import ca.uvic.chisel.bfv.editor.IntervalLineTracker;

public class TestFileLineTracker2 implements ILineTracker {

	public DefaultLineTracker defaultTracker;
	private IntervalLineTracker intervalLineTracker;
	
	public TestFileLineTracker2(IntervalLineTracker lineTracker) {
		this.intervalLineTracker = lineTracker;
		defaultTracker = new DefaultLineTracker();
	}
	
	@Override
	public String[] getLegalLineDelimiters() {
		String[] bigResult = intervalLineTracker.getLegalLineDelimiters();
		String[] defaultResult = defaultTracker.getLegalLineDelimiters();
		return defaultResult;
	}

	@Override
	public String getLineDelimiter(int line) throws BadLocationException {
		String bigResult = intervalLineTracker.getLineDelimiter(line);
		String defaultResult = defaultTracker.getLineDelimiter(line);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A LINE DELIM DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public int computeNumberOfLines(String text) {
		int bigResult = intervalLineTracker.computeNumberOfLines(text);
		int defaultResult = defaultTracker.computeNumberOfLines(text);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A compute NUMBER OF LINES DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public int getNumberOfLines() {
		// for some reason the defaultResult has an extra line
		int bigResult = intervalLineTracker.getNumberOfLines();
		int defaultResult = defaultTracker.getNumberOfLines();
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A get NUMBER OF LINES DIFFERENCE");
		}
		
		return defaultResult;
	}

	/**
	 * WTF there are times when this does pretend the lines aren't there, I do not understand.....
	 */
	@Override
	public int getNumberOfLines(int offset, int length) throws BadLocationException {
		int bigResult = intervalLineTracker.getNumberOfLines(offset, length);
		int defaultResult = defaultTracker.getNumberOfLines(offset, length);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A get NUMBER OF LINES DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public int getLineOffset(int line) throws BadLocationException {
		
		int bigResult = intervalLineTracker.getLineOffset(line);
		int defaultResult = defaultTracker.getLineOffset(line);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineOffset DIFFERENCE");
		} 
		
		return defaultResult;
	}

	@Override
	public int getLineLength(int line) throws BadLocationException {
		int bigResult = intervalLineTracker.getLineLength(line);
		int defaultResult = defaultTracker.getLineLength(line);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineLength DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public int getLineNumberOfOffset(int offset) throws BadLocationException {
		
		int bigResult = intervalLineTracker.getLineNumberOfOffset(offset);
		int defaultResult = defaultTracker.getLineNumberOfOffset(offset);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineNumberOfOffset DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
		
		intervalLineTracker.getLineInformation(getNumberOfLines() - 1);
		defaultTracker.getLineInformation(getNumberOfLines() - 1);
		
		
		LineRegion bigResult = intervalLineTracker.getLineInformationOfOffset(offset);
		IRegion defaultResult = defaultTracker.getLineInformationOfOffset(offset);
		
		if(bigResult.getOffset() != defaultResult.getOffset() || bigResult.getLength() != defaultResult.getLength() ) {
			System.out.println("WE HAVE A getLineInformationOfOffset DIFFERENCE");
		}
		
		return defaultResult;
	}

	/*
	 * Is it possible that after a point it does pretend that lines aren't there if they are bad news?
	 * If so, when does this occur?
	 */
	@Override
	public IRegion getLineInformation(int line) throws BadLocationException {
		LineRegion bigResult = null;
		try {
			bigResult = intervalLineTracker.getLineInformation(line);
			defaultTracker.getLineInformation(line).getOffset();
		} catch(Exception e) {}
		IRegion defaultResult = defaultTracker.getLineInformation(line);
		
		if(bigResult == null || bigResult.getOffset() != defaultResult.getOffset() || bigResult.getLength() != defaultResult.getLength()) {
			System.out.println("WE HAVE A getLineInformation DIFFERENCE");
		}
		
		return defaultResult;
	}

	@Override
	public void replace(int offset, int length, String text) throws BadLocationException {
		
		intervalLineTracker.replace(offset, length, text);
		defaultTracker.replace(offset, length, text);
		
		int bigResult = intervalLineTracker.getNumberOfLines();
		int defaultResult = defaultTracker.getNumberOfLines();
		
		if(bigResult != defaultResult) {
			System.out.println("Replace messed up.");
		}
	}

	@Override
	public void set(String text) {
		intervalLineTracker.set(text);
		defaultTracker.set(text);
	}
}
