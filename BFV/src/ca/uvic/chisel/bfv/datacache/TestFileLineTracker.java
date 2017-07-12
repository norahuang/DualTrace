package ca.uvic.chisel.bfv.datacache;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;

import ca.uvic.chisel.bfv.annotations.RegionAnnotation;
import ca.uvic.chisel.bfv.editor.DocumentContentManager;
import ca.uvic.chisel.bfv.editor.IntervalLineTracker;

public class TestFileLineTracker implements ILineTracker {

	public DefaultLineTracker defaultTracker;
	private IntervalLineTracker intervalLineTracker;
	
	public TestFileLineTracker(IntervalLineTracker lineTracker) {
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
		
		return bigResult;
	}

	@Override
	public int computeNumberOfLines(String text) {
		int bigResult = intervalLineTracker.computeNumberOfLines(text);
		int defaultResult = defaultTracker.computeNumberOfLines(text);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A compute NUMBER OF LINES DIFFERENCE");
		}
		
		return bigResult;
	}

	@Override
	public int getNumberOfLines() {
		// for some reason the defaultResult has an extra line
		int bigResult = intervalLineTracker.getNumberOfLines();
		int defaultResult = defaultTracker.getNumberOfLines();
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A get NUMBER OF LINES DIFFERENCE");
		}
		
		return bigResult;
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
		
		return bigResult;
	}

	@Override
	public int getLineOffset(int line) throws BadLocationException {
		
		intervalLineTracker.getNumberOfLines();
		defaultTracker.getNumberOfLines();
		
		int bigResult = intervalLineTracker.getLineOffset(line);
		int defaultResult = defaultTracker.getLineOffset(line);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineOffset DIFFERENCE");
		} 
		
		return bigResult;
	}

	@Override
	public int getLineLength(int line) throws BadLocationException {
		int bigResult = intervalLineTracker.getLineLength(line);
		int defaultResult = defaultTracker.getLineLength(line);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineLength DIFFERENCE");
		}
		
		return bigResult;
	}

	@Override
	public int getLineNumberOfOffset(int offset) throws BadLocationException {
		int bigResult = intervalLineTracker.getLineNumberOfOffset(offset);
		int defaultResult = defaultTracker.getLineNumberOfOffset(offset);
		
		if(bigResult != defaultResult) {
			System.out.println("WE HAVE A getLineNumberOfOffset DIFFERENCE");
		}
		
		return bigResult;
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
		LineRegion bigResult = intervalLineTracker.getLineInformation(line);
		IRegion defaultResult = defaultTracker.getLineInformation(line);
		
		if(bigResult.getOffset() != defaultResult.getOffset() || bigResult.getLength() != defaultResult.getLength()) {
			System.out.println("WE HAVE A getLineInformation DIFFERENCE");
		}
		
		return bigResult;
	}

	@Override
	public void replace(int offset, int length, String text) throws BadLocationException {

		// TODO you may be able to utilize the mapping to get the answers that we need.
		
//		System.out.println("STARTING");
//		try {
//			for(int i=0; i<= 400; i+=1) {
//				System.out.println("Offset" + i + " = Line " + bigLineTracker.getLineNumberOfOffset(i));
//				System.out.println("Offset" + i + " = Line " + defaultTracker.getLineNumberOfOffset(i));
//				System.out.println();
//			}
//		} catch(Exception e) {
//			
//		}
		
		intervalLineTracker.replace(offset, length, text);
		defaultTracker.replace(offset, length, text);
//		
		intervalLineTracker.getNumberOfLines();
		defaultTracker.getNumberOfLines();
		
//		System.out.println("STARTING");
//		try {
//			for(int i=0; i<= 400; i+=1) {
//				System.out.println("big: Offset" + i + " = Line " + bigLineTracker.getLineNumberOfOffset(i));
//				System.out.println("default: Offset" + i + " = Line " + defaultTracker.getLineNumberOfOffset(i));
//				System.out.println();
//			}
//		} catch(Exception e) {
//			
//		}
//		
//		System.out.println("DONE");
	}

	@Override
	public void set(String text) {
		intervalLineTracker.set(text);
		defaultTracker.set(text);
	}
}
