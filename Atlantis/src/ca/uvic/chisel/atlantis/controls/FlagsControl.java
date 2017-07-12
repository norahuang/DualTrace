package ca.uvic.chisel.atlantis.controls;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class FlagsControl  extends Canvas{

	// the register value in hex (16 chars)
	private String flagsRegisterHex;
	
	// the number font
	private final Font numberFont;
	private final int numberFontHeight;
	
	// the mapping of flag positions to names
	private Map<Integer, String> flagsMap;
	
	// the bits that changed
	private List<Integer> changedBits;
	
	// the value changed color
	private final Color valueChangedColor;
	
	String emptyHex = "0000000000000000";
	
	public FlagsControl(Composite parent, int style) {
		super(parent, style);
		flagsRegisterHex = emptyHex;
		
		changedBits = new LinkedList<Integer>();
		
		flagsMap = new Hashtable<Integer, String>();
		flagsMap.put(63, "C"); // carry
		flagsMap.put(61, "P"); // parity
		flagsMap.put(59, "A"); // adjust
		flagsMap.put(57, "Z"); // zero
		flagsMap.put(56, "S"); // sign
		flagsMap.put(55, "T"); // trap
		flagsMap.put(54, "I"); // interrupt enable
		flagsMap.put(53, "D"); // direction
		flagsMap.put(52, "O"); // overflow
		
		FontData fd = JFaceResources.getFont(JFaceResources.TEXT_FONT).getFontData()[0];
		numberFontHeight = 11;
		fd.setHeight(numberFontHeight);
		numberFont = new Font(getDisplay(), fd);
		
		valueChangedColor = new Color(getDisplay(), 0xDC, 0x38, 0x1F);
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				FlagsControl.this.paintControl(e);
			}
		});
		addDisposeListener(new DisposeListener() {
	         public void widgetDisposed(DisposeEvent e) {
	                numberFont.dispose();
	         }
	    });
	}

	private void paintControl(PaintEvent e) {
		GC g = e.gc;
		g.setFont(numberFont);
		
		Color defaultForeground = g.getForeground();
		
		g.drawString("RFLAGS", 0, 0);
		
		int leftOffset = g.getFontMetrics().getAverageCharWidth() * 7 + 2;
		
		for(Map.Entry<Integer, String> flag : flagsMap.entrySet()) {
			// the bit
			String bit = extractNthBit(flagsRegisterHex, flag.getKey()) ? "1" : "0";
			if(changedBits.contains(flag.getKey())) {
				g.setForeground(valueChangedColor);
			}
			g.drawString(bit, leftOffset + 3, 1);
			g.setForeground(defaultForeground);
			g.drawRectangle(leftOffset, 2, g.getFontMetrics().getAverageCharWidth() + 5, numberFontHeight + 4);
			
			// the character
			g.drawString(flag.getValue(), leftOffset + 3, 1 + g.getFontMetrics().getAverageCharWidth() + 5 + 4);
			
			leftOffset += g.getFontMetrics().getAverageCharWidth() + 5 + 5;
		}
	}
	
	private boolean extractNthBit(String hex, int bit) {
		int charBitIsIn = bit / 4;
		int bitOffset = bit % 4;
		int charAsInt = Integer.parseInt(hex.substring(charBitIsIn, charBitIsIn + 1), 16);
		int bitMask = (int) Math.pow(2, 3 - bitOffset);
		return (charAsInt & bitMask) > 0;
	}
	
	public void setValue(String hex) {
		changedBits.clear();
		for(Integer bit : flagsMap.keySet()) {
			if(extractNthBit(hex, bit) != extractNthBit(flagsRegisterHex, bit)) {
				changedBits.add(bit);
			}
		}
		
		this.flagsRegisterHex = hex;
	}
	
	public void setValueToEmpty() {
		changedBits.clear();
		this.flagsRegisterHex = emptyHex;
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		
		GC measure = new GC(this);
		measure.setFont(numberFont);
		int characterWidth = measure.getFontMetrics().getAverageCharWidth();
		measure.dispose();
		
		int width = 250;
		int height = (numberFontHeight + 4) * 2 + 4; // numbers + spaces + borders
		
		if (wHint != SWT.DEFAULT) width = wHint;
	    if (hHint != SWT.DEFAULT) height = hHint; 
	    
	    if(!getVisible()) {
	    	height = 0;
	    }
		
		return new Point(width, height);
	}
}
