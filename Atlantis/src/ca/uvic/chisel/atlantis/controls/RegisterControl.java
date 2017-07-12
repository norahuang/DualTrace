package ca.uvic.chisel.atlantis.controls;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.views.HexVisualization;
import ca.uvic.chisel.atlantis.views.MemoryVisualization;
import ca.uvic.chisel.atlantis.views.RegisterConfig;

public class RegisterControl extends Canvas {

	// the number of bits to display (8,16,32,64...)
	private final int bitsWideLongMode;
	
	// sub-display width for processor modes with less register visibility.
	// I have doubts about this idea.
	private final int bitsWideProtectedMode;
	
	// control whether we are showing in x86_64 Protected Mode (e.g. EAX instead of RAX)
	// or in Long Mode (64-bit full mode)
	private boolean display64BitLongMode = true;
	
	// the line number link color
	private final Color lineNumberLinkColor;
	
	// the current value to display
	private String value;
	
	// the label of this register
	private String labelLongMode;
	/**
	 * For secondary modes, such as x86 32b Protected Mode (as opposed to 64b Long Mode).
	 * I find this a dubious addition for user comfort.
	 */
	private String labelProtectedMode;
	
	// the display mode of the integer
	private int integerDisplay;
	
	// the line number this register was last changed on
	private long lineNumber;
	
	// was value actually changed sine previous line?
	// Based on change in relevant line, rather than actual value
	// since we are interested in memory touches, even when
	// the value happened to be the same.
	private boolean valueChanged;
	
	// the possible integer display modes
	public static final int INTEGER_U64 = 1;
	public static final int INTEGER_S64 = 2;
	public static final int INTEGER_U32 = 4;
	public static final int INTEGER_S32 = 8;
	public static final int FLOAT_80 = 16;
	public static final int FLOAT_64_1 = 32;
	public static final int FLOAT_64_2 = 64;
	public static final int FLOAT_32_1 = 128;
	public static final int FLOAT_32_2 = 256;
	public static final int FLOAT_32_3 = 512;
	public static final int FLOAT_32_4 = 1024;
	public static final int NO_DISPLAY_MODE = 2048;
	
	public static final int INTEGER_ONLY = INTEGER_U64 | INTEGER_S64 | INTEGER_U32 | INTEGER_S32;
	public static final int FLOAT_IN_128 = FLOAT_64_1 | FLOAT_64_2 | FLOAT_32_1 | FLOAT_32_2 | FLOAT_32_3 | FLOAT_32_4;
	
	// the types of displays allowed
	private final int numberTypeDisplayMask;
	
	// the font to use for displaying numbers
	private final Font numberFont;
	
	// the font to use for rendering the sign button
	private final Font signFont;
	
	// the font to user for the line number
	private final Font lineFont;
	
	// the height of the number font
	private final int numberFontHeight;
	
	// the height of the sign font
	private final int signFontHeight;
	
	// the height of the line font
	private final int lineFontHeight;
	
	// the button images
	private final Image u64;
	private final Image s64;
	private final Image u32;
	private final Image s32;
	private final Image f80;
	
	private final Image f164;
	private final Image f264;
	private final Image f132;
	private final Image f232;
	private final Image f332;
	private final Image f432;
	
	// the button colors
	private final Color signButtonTextColor;
	private final Color signButtonBorderColor;
	
	private final Color u64ButtonColor;
	private final Color s64ButtonColor;
	private final Color u32ButtonColor;
	private final Color s32ButtonColor;
	private final Color f80ButtonColor;
	
	private final Color f164ButtonColor;
	private final Color f264ButtonColor;
	private final Color f132ButtonColor;
	private final Color f232ButtonColor;
	private final Color f332ButtonColor;
	private final Color f432ButtonColor;
	
	private final Color gotoMemoryButtonColor;
	
	// the value changed color
	private final Color valueChangedColor;
	
	// the click region rectangles
	private Rectangle signButtonRect;
	private Rectangle lineNumberRect;
	private Rectangle gotoMemoryRect;
	
	// do we show the go to memory button at all
	private final boolean showGotoMemory;
	
	// the cursors
	private Cursor defaultCursor;
	private Cursor hoverCursor;
	
	// know 80bit floating point values dictionary
	private static Map<String, String> known80FloatingPointValues = new HashMap<String, String>();
	
	public RegisterControl(Composite parent, int style, RegisterConfig regConf) {
		super(parent, style);
				
		this.labelLongMode = regConf.label;
		this.labelProtectedMode = regConf.protectedModeLabel;
		this.bitsWideLongMode = regConf.bitsWide;
		this.bitsWideProtectedMode = regConf.protectedModeBitsWide;
		this.numberTypeDisplayMask = regConf.numberTypeDisplayMask;
		this.showGotoMemory = regConf.showGotoMemory;
		
		// setup the fonts
		FontData fd = JFaceResources.getFont(JFaceResources.TEXT_FONT).getFontData()[0];
		numberFontHeight = 11;
		fd.setHeight(numberFontHeight);
		numberFont = new Font(getDisplay(), fd);
		signFontHeight = 6;
		fd.setHeight(signFontHeight);
		signFont = new Font(getDisplay(), fd);
		lineFontHeight = 10;
		fd.setHeight(lineFontHeight);
		lineFont = new Font(getDisplay(), fd);
		
		// the integer button color
		signButtonTextColor = new Color(getDisplay(), 0x00, 0x00, 0x00);
		signButtonBorderColor = signButtonTextColor;
		u64ButtonColor = new Color(getDisplay(), 0x82, 0xCA, 0xFA);
		s64ButtonColor = new Color(getDisplay(), 0xC4, 0x74, 0x51);
		u32ButtonColor = new Color(getDisplay(), 0x35, 0x7E, 0xC7);
		s32ButtonColor = new Color(getDisplay(), 0xF6, 0x22, 0x17);
		f80ButtonColor = new Color(getDisplay(), 0xFD, 0xD7, 0xE4);
		
		f164ButtonColor = new Color(getDisplay(), 0x64, 0xE9, 0x86);
		f264ButtonColor = new Color(getDisplay(), 0xFF, 0xDB, 0x58);
		f132ButtonColor = new Color(getDisplay(), 0xA1, 0xC9, 0x35);
		f232ButtonColor = new Color(getDisplay(), 0xF3, 0xE5, 0xAB);
		f332ButtonColor = new Color(getDisplay(), 0x82, 0x78, 0x39);
		f432ButtonColor = new Color(getDisplay(), 0x78, 0x6D, 0x5F);
		
		// the line number link color
		lineNumberLinkColor = new Color(getDisplay(), 0x2B, 0x65, 0xEC);
		
		// the value changed color
		valueChangedColor = new Color(getDisplay(), 0xDC, 0x38, 0x1F);
		
		// the memory button color
		gotoMemoryButtonColor = new Color(getDisplay(), 0x68, 0xB2, 0xB2);
		
		// start the rectangles as null
		signButtonRect = null;
		lineNumberRect = null;
		gotoMemoryRect = null;
		
		// the integer format button images
		u64 = createIntegerButtonImage("U64", u64ButtonColor);
		s64 = createIntegerButtonImage("S64", s64ButtonColor);
		u32 = createIntegerButtonImage("U32", u32ButtonColor);
		s32 = createIntegerButtonImage("S32", s32ButtonColor);
		f80 = createIntegerButtonImage("F80", f80ButtonColor);
		
		f164 = createIntegerButtonImage("64a", f164ButtonColor);
		f264 = createIntegerButtonImage("64b", f264ButtonColor);
		f132 = createIntegerButtonImage("32a", f132ButtonColor);
		f232 = createIntegerButtonImage("32b", f232ButtonColor);
		f332 = createIntegerButtonImage("32c", f332ButtonColor);
		f432 = createIntegerButtonImage("32d", f432ButtonColor);
		
		// set the cursors
		defaultCursor = getCursor();
		hoverCursor = new Cursor(getDisplay(), SWT.CURSOR_HAND);
		
		// set values
		setValueToEmpty();
		redraw();
		integerDisplay = 1;
		do {
			integerDisplay *= 2;
			if(integerDisplay == NO_DISPLAY_MODE) {
				integerDisplay = 1;
			}
		} while((integerDisplay & numberTypeDisplayMask) == 0);
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				RegisterControl.this.paintControl(e);
			}
		});
		addDisposeListener(new DisposeListener() {
	         public void widgetDisposed(DisposeEvent e) {
	                numberFont.dispose();
	                signFont.dispose();
	                lineFont.dispose();
	                u64.dispose();
	                s64.dispose();
	                u32.dispose();
	                s32.dispose();
	                f80.dispose();
	                
	                f164.dispose();
	                f264.dispose();
	                f132.dispose();
	                f232.dispose();
	                f332.dispose();
	                f432.dispose();
	         }
	    });
		addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				RegisterControl.this.mouseUp(e);
			}
			
			@Override
			public void mouseDown(MouseEvent e) { }
			
			@Override
			public void mouseDoubleClick(MouseEvent e) { }
		});
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				RegisterControl.this.mouseMove(e);
			}
		});
	}
	
	private void mouseMove(MouseEvent e) {
		if((lineNumberRect != null && lineNumberRect.contains(e.x, e.y))
				|| (signButtonRect != null && signButtonRect.contains(e.x, e.y))
				|| (gotoMemoryRect != null && gotoMemoryRect.contains(e.x, e.y))) {
			setCursor(hoverCursor);
		} else {
			setCursor(defaultCursor);
		}
	}
	
	private void mouseUp(MouseEvent e) {
		if(lineNumberRect != null) {
			if(lineNumberRect.contains(e.x, e.y)) {
				try {
					AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().getActiveEditor();
					if (activeTraceDisplayer != null) {
						activeTraceDisplayer.getProjectionViewer().gotoLineAtOffset((int)getLineNumber(), 0);
					}
				} catch (Exception ex) {
					System.err.println("Error getting line number for register " + getLabel());
					ex.printStackTrace();
				}
			}
		}
		
		if(signButtonRect != null) {
			if(signButtonRect.contains(e.x, e.y)) {
				int integerDisplay = getIntegerDisplay();
				do {
					integerDisplay *= 2;
					if(integerDisplay == NO_DISPLAY_MODE) {
						integerDisplay = 1;
					}
				} while((integerDisplay & numberTypeDisplayMask) == 0);
				setIntegerDisplay(integerDisplay);
			}
		}
		
		if(gotoMemoryRect != null) {
			if(gotoMemoryRect.contains(e.x, e.y)) {
				try {
				HexVisualization hexView = (HexVisualization) 
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(HexVisualization.ID);
				if(hexView != null) {
					hexView.setAddress(new BigInteger(value, 16));
				}
				} catch (PartInitException e1) {
					// Failure is ok
				}
			}
		}
	}
	
	private Image createIntegerButtonImage(String text, Color background) {
	    Image stringImage = new Image(getDisplay(), 
	    		signFont.getFontData()[0].getHeight() * text.length() + 10, 
	    		signFont.getFontData()[0].getHeight() + 10);
	    GC g = new GC(stringImage);

	    g.setFont(signFont);
	    g.setForeground(signButtonTextColor);
	    
	    // turn of AA
	    g.setAntialias(SWT.OFF);
	    
	    Color defualtBackground = g.getBackground();

		g.setBackground(background);
		g.fillRoundRectangle(1, 1, numberFontHeight + 5, signFontHeight + 1, 3, 3);
		
		g.setForeground(signButtonTextColor);
	    g.drawText(text, 3, 0);

		g.setForeground(signButtonBorderColor);
		g.setBackground(defualtBackground);
		g.drawRoundRectangle(0, 0, numberFontHeight + 6, signFontHeight + 2, 3, 3);
		
		// set foreground to background and render a correction line
		g.setForeground(defualtBackground);
		g.drawLine(0, signFontHeight + 3, numberFontHeight + 6, signFontHeight + 3);

	    ImageData sd = stringImage.getImageData();
	    ImageData dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);

	    // Determine which way to rotate, depending on up or down
	    boolean up = true;

	    for (int sx = 0; sx < sd.width; sx++) {
	      for (int sy = 0; sy < sd.height; sy++) {
	        // Determine where to move pixel to in destination image data
	        int dx = up ? sy : sd.height - sy - 1;
	        int dy = up ? sd.width - sx - 1 : sx;
	        
	        dd.setPixel(dx, dy, sd.getPixel(sx, sy));
	      }
	    }
	    
	    Image finalImage = new Image(getDisplay(), dd);
		
		g.dispose();
		
		return finalImage;
	}
	
	private void paintControl(PaintEvent e) {
		GC g = e.gc;
		g.setFont(numberFont);

		Color defaultForeground = g.getForeground();
		Color defaultBackground = g.getBackground();
		
		int componentStart = g.getFontMetrics().getAverageCharWidth() * 7 + 2;		
		
		// the label
		g.drawString(getLabel(), 0, 0);
		
		// the goto memory button
		if(showGotoMemory) {
			int gotoMemY = numberFontHeight + 10;
			int gotoMemX = componentStart - 15 - 5;
			g.setForeground(signButtonTextColor);
			g.setBackground(gotoMemoryButtonColor);
			g.fillRoundRectangle(gotoMemX, gotoMemY, 19, 8, 2, 2);
			g.setBackground(defaultBackground);
			g.drawRoundRectangle(gotoMemX, gotoMemY, 18, 8, 2, 2);
			g.drawLine(gotoMemX + 2, gotoMemY + 4, gotoMemX + 15, gotoMemY + 4);
			g.drawLine(gotoMemX + 12, gotoMemY + 2, gotoMemX + 15, gotoMemY + 4);
			g.drawLine(gotoMemX + 12, gotoMemY + 6, gotoMemX + 15, gotoMemY + 4);
			g.setBackground(defaultBackground);
			
			if(gotoMemoryRect == null || gotoMemoryRect.x != gotoMemX
					|| gotoMemoryRect.y != gotoMemY || gotoMemoryRect.width != 19
					|| gotoMemoryRect.height != 9) {
				gotoMemoryRect = new Rectangle(gotoMemX, gotoMemY, 19, 9);
			}
		}

		// the hex digits
		int numBytes = getByteWidth();
		// Skip rendering either 0 or some bytes, on the left side of the value, if we are in protected mode
		int numBytesSkipFromLeftSide = this.bitsWideLongMode/8 - numBytes;
		
		if(valueChanged) {
			g.setForeground(valueChangedColor);
		} else {
			g.setForeground(defaultForeground);
		}
		int hexLeftOffset = componentStart + 4 - 3; // start with -3 so that first loop enters and increments to desired offset
		int hexYOffset = 0;
		for(int i = numBytesSkipFromLeftSide; i < numBytes + numBytesSkipFromLeftSide; i++) {
			if(i % 4 == 0) {
				hexLeftOffset += 3;
			}
			g.drawString(getValue().substring(i * 2, i * 2 + 2), hexLeftOffset, hexYOffset, true);
			hexLeftOffset += g.textExtent(getValue().substring(i * 2, i * 2 + 2)).x;
		}
		hexLeftOffset += 3;
		g.setForeground(defaultForeground);
		
		// the string characters
		int stringLeftOffset = componentStart + 4 + 4;
		int stringY = numberFontHeight + 6 + hexYOffset;
		for(int i = numBytesSkipFromLeftSide; i < numBytes + numBytesSkipFromLeftSide; i++) {
			
			String byteString = getValue().substring(i * 2, i * 2 + 2);
			int byteValue = Integer.parseInt(byteString, 16);
			String character = null;
			
			if(byteValue < 32 || byteValue > 126) {
				g.setForeground(new Color(getDisplay(), 0xCC, 0xCC, 0xCC));
				character = ".";
			} else {
				g.setForeground(defaultForeground);
				character = MemoryVisualization.convertHexToString(byteString);
			}
			
			g.drawString(character, stringLeftOffset, stringY);
			
			stringLeftOffset += g.textExtent("00").x;
			if((i + 1) % 4 == 0) {
				stringLeftOffset += 3;
			}
		}
		

		// hex border
		g.setForeground(defaultForeground);
		g.drawRectangle(componentStart, 0, hexLeftOffset - componentStart, numberFontHeight + 6);
		
		// the sign button
		g.setForeground(u64ButtonColor);
		switch(getIntegerDisplay()) {
		case INTEGER_U64:
			g.drawImage(u64, hexLeftOffset + 5, -10);
			break;
		case INTEGER_S64:
			g.drawImage(s64, hexLeftOffset + 5, -10);
			break;
		case INTEGER_U32:
			g.drawImage(u32, hexLeftOffset + 5, -10);
			break;
		case INTEGER_S32:
			g.drawImage(s32, hexLeftOffset + 5, -10);
			break;
		case FLOAT_64_1:
			g.drawImage(f164, hexLeftOffset + 5, -10);
			break;
		case FLOAT_64_2:
			g.drawImage(f264, hexLeftOffset + 5, -10);
			break;
		case FLOAT_32_1:
			g.drawImage(f132, hexLeftOffset + 5, -10);
			break;
		case FLOAT_32_2:
			g.drawImage(f232, hexLeftOffset + 5, -10);
			break;
		case FLOAT_32_3:
			g.drawImage(f332, hexLeftOffset + 5, -10);
			break;
		case FLOAT_32_4:
			g.drawImage(f432, hexLeftOffset + 5, -10);
			break;
		case FLOAT_80:
			g.drawImage(f80, hexLeftOffset + 5, -10);
			break;
		}
		if(signButtonRect == null || signButtonRect.x != hexLeftOffset + 5
			|| signButtonRect.y != -10 || signButtonRect.width != signFontHeight + 2 + 1
			|| signButtonRect.height != numberFontHeight + 6 + 1) {
			signButtonRect = new Rectangle(hexLeftOffset + 5, 0, signFontHeight + 2 + 1, numberFontHeight + 6 + 1);
		}
		g.setForeground(defaultForeground);
		
		// the display number
		g.drawString(getValueAsDisplayNumber(), hexLeftOffset + 5 + numberFontHeight + 1, hexYOffset);
		
		// the line number
		g.setFont(lineFont);
		g.drawString("Line: ", hexLeftOffset + 5, stringY + 2);
		g.setForeground(lineNumberLinkColor);
		g.drawString(Long.toString(getDisplayLineNumber()), hexLeftOffset + 5 + g.textExtent("Line: ").x - 3, stringY + 2);
		g.drawLine(hexLeftOffset + 5 + g.textExtent("Line: ").x - 3, 
				stringY + lineFontHeight + 5, 
				hexLeftOffset + 5 + g.textExtent("Line: ").x - 3 + g.textExtent(Long.toString(getDisplayLineNumber())).x - 1, 
				stringY + lineFontHeight + 5);
		if(lineNumberRect == null || lineNumberRect.x != hexLeftOffset + 5 + g.textExtent("Line: ").x - 3
			|| lineNumberRect.y != stringY + 6 || lineNumberRect.width != g.textExtent(Long.toString(getDisplayLineNumber())).x - 1 + 1
			|| lineNumberRect.height != signFontHeight + 3 + 1) {
			lineNumberRect = new Rectangle(hexLeftOffset + 5 + g.textExtent("Line: ").x - 3,
					stringY + 6, 
					g.textExtent(Long.toString(getDisplayLineNumber())).x - 1 + 1, 
					signFontHeight + 3 + 1);
		}
		g.setForeground(defaultForeground);
	}
	
	public void setDisplay64BitLongMode(boolean setToLongMode){
		this.display64BitLongMode = setToLongMode;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public void setValueToEmpty() {
		StringBuilder defValBuilder = new StringBuilder();
		for(int i = 0; i < bitsWideLongMode; i += 8) {
			defValBuilder.append("00");
		}
		setValue(defValBuilder.toString());
		valueChanged = false;
	}
	
	public int getByteWidth(){
		if(display64BitLongMode){
			return bitsWideLongMode/8;
		} else {
			return bitsWideProtectedMode/8;
		}
	}
	
	public String getCanonicalLabel(){
		return this.labelLongMode;
	}

	public String getLabel() {
		// Use the shorter register labels (e.g. EAX) when rendering in Protected Mode for x86
		if(display64BitLongMode){
			return labelLongMode;
		} else {
			return labelProtectedMode;
		}
	}
	
	public int getIntegerDisplay() {
		return integerDisplay;
	}

	public void setIntegerDisplay(int integerDisplay) {
		if((integerDisplay & numberTypeDisplayMask) == 0) {
			System.err.println("Invalid display mode. Mask failed.");
			return;
		}
		
		this.integerDisplay = integerDisplay;
		redraw();
	}

	public long getLineNumber() {
		return lineNumber;
	}
	
	public long getDisplayLineNumber() {
		return getLineNumber() + 1;
	}

	public void setLineNumber(long lineNumber, int lastUpdatedTraceLine) {
		valueChanged = (lineNumber == lastUpdatedTraceLine && lastUpdatedTraceLine != 0);
		this.lineNumber = lineNumber;
	}
	
	public boolean hasValueJustChanged() {
		return valueChanged;
	}
	
	public String getValueAsDisplayNumber() {
		String result = null;
		
		switch(getIntegerDisplay()) {
			case INTEGER_U64: {
				BigInteger bigValue = new BigInteger(value, 16);
				result = bigValue.toString();
			} break;
			case INTEGER_S64: {
				BigInteger bigValue = new BigInteger(value, 16);
				result = Long.toString(bigValue.longValue());
			} break;
			case INTEGER_U32: {
				BigInteger bigValue = new BigInteger(value.substring(8), 16);
				result = bigValue.toString();
			} break;
			case INTEGER_S32: {
				BigInteger bigValue = new BigInteger(value.substring(8), 16);
				result = Integer.toString(bigValue.intValue());
			} break;
			case FLOAT_64_1: {
				String sub = value.substring(0, 16);
				double doubleVal = Double.longBitsToDouble(new BigInteger(sub, 16).longValue());
				result = Double.toString(doubleVal);
			} break;
			case FLOAT_64_2: {
				String sub = value.substring(16, 32);
				double doubleVal = Double.longBitsToDouble(new BigInteger(sub, 16).longValue());
				result = Double.toString(doubleVal);
			} break;
			case FLOAT_32_1: {
				String sub = value.substring(0, 8);
				float floatVal = Float.intBitsToFloat(new BigInteger(sub, 16).intValue());
				result = Float.toString(floatVal);
			} break;
			case FLOAT_32_2: {
				String sub = value.substring(8, 16);
				float floatVal = Float.intBitsToFloat(new BigInteger(sub, 16).intValue());
				result = Float.toString(floatVal);
			} break;
			case FLOAT_32_3: {
				String sub = value.substring(16, 24);
				float floatVal = Float.intBitsToFloat(new BigInteger(sub, 16).intValue());
				result = Float.toString(floatVal);
			} break;
			case FLOAT_32_4: {
				String sub = value.substring(24, 32);
				float floatVal = Float.intBitsToFloat(new BigInteger(sub, 16).intValue());
				result = Float.toString(floatVal);
			} break;
			case FLOAT_80: {
				result = get80BitFloatingPointValue(value);
			}
		}
		
		if(result.length() > 13) {
			result = result.substring(0, 10) + "...";
		}
		
		return result;
	}
	
	private String get80BitFloatingPointValue(String hex) {
		if(hex.length() != 20) {
			System.err.println("Invalid 80bit floating point hex string length. Expected 20 characters, got " + hex.length() + ".");
			return "";
		}
		
		// test value: -3215.216354
		//hex = "c00ac8f3762f9ca5bd94";
		
		if(known80FloatingPointValues.containsKey(hex)) {
			return known80FloatingPointValues.get(hex);
		}
		
		for(int i = 0; i < hex.length(); i++) {
			if(!((hex.charAt(i) >= 48 && hex.charAt(i) <= 57) || // 0-9
				(hex.charAt(i) >= 65 && hex.charAt(i) <= 70) || // A-F
				(hex.charAt(i) >= 97 && hex.charAt(i) <= 102))) { // a-f
				System.err.println("Invalid character '" + hex.charAt(i) + "' in hex string '" + hex + "'.");
			}
		}
		
		
		boolean signBit = Integer.parseInt(hex.substring(0, 1), 16) >= 8;
		
		int exponent = Integer.parseInt(hex.substring(0, 4), 16);
		if(signBit) {
			exponent -= 32768;
		}
		
		boolean bit63 = extractNthBit(hex, 16);
		boolean bit62 = extractNthBit(hex, 17);
		
		BigDecimal result = getBase2Fraction(hex, 63, 17);
		String stringResult = "Unknown";
		
		// solve down the cases
		if(exponent == 0) {
			if(bit63 == false) {
				if(result.compareTo(new BigDecimal("0")) == 0) {
					return "0.0";
				} else {
					// denormal
					if(bit63) {
						result = result.add(BigDecimal.ONE);
					}
					if(signBit) {
						result = result.multiply(BigDecimal.valueOf(-1));
					}
					result = result.multiply(BigDecimal.ONE.divide(new BigDecimal("2.0").pow(16382)));
					stringResult = result.toString();
				}
			} else {
				// pseudo denormal, still valid
				if(bit63) {
					result = result.add(BigDecimal.ONE);
				}
				if(signBit) {
					result = result.multiply(BigDecimal.valueOf(-1));
				}
				result = result.multiply(BigDecimal.ONE.divide(new BigDecimal("2.0").pow(16382)));
				stringResult = result.toString();
			}
		} else if(exponent == 32767) {
			if(bit63) {
				if(bit62) {
					if(result.compareTo(new BigDecimal("0")) == 0) {
						stringResult = "Floating-point Indefinite";
					} else {
						stringResult = "Quiet NaN";
					}
				} else {
					if(result.compareTo(new BigDecimal("0")) == 0) {
						if(signBit) {
							stringResult = "-Infinity";
						} else {
							stringResult = "Infinity";
						}
					} else {
						stringResult = "Signalling NaN";
					}
				}
			} else {
				// pseudo NaN, invalid on all relevant systems
				stringResult = "Invalid";
			}
		} else {
			if(bit63) {
				// normalized value
				result = result.add(BigDecimal.ONE);
				if(signBit) {
					result = result.multiply(BigDecimal.valueOf(-1));
				}
				int power = exponent - 16383;
				BigDecimal powerOfTwo = new BigDecimal("2.0").pow(Math.abs(power));
				if(exponent < 0) {
					powerOfTwo = BigDecimal.ONE.divide(powerOfTwo);
				}
				result = result.multiply(powerOfTwo);
				stringResult = result.toString();
			} else {
				// unnormal, invalid on all relevant systems
				stringResult = "Invalid";
			}
		}
		
		known80FloatingPointValues.put(hex, stringResult);
		return stringResult;
	}
	
	private BigDecimal getBase2Fraction(String hex, int bits, int offset) {
		BigDecimal result = new BigDecimal("0.0");
		BigDecimal currentFraction = new BigDecimal("1.0");
		BigDecimal two = new BigDecimal("2.0");
		
		for(int i = offset; i < bits + offset; i++) {
			currentFraction = currentFraction.divide(two);
			if(extractNthBit(hex, i)) {
				result = result.add(currentFraction);
			}
		}
		
		return result;
	}
	
	private boolean extractNthBit(String hex, int bit) {
		int charBitIsIn = bit / 4;
		int bitOffset = bit % 4;
		int charAsInt = Integer.parseInt(hex.substring(charBitIsIn, charBitIsIn + 1), 16);
		int bitMask = (int) Math.pow(2, 3 - bitOffset);
		return (charAsInt & bitMask) > 0;
	}
	
	/**
	 * The secondary width for some registers is 0, which occurs when registers available in Long Mode
	 * are not available in Protected Mode on x86_64. Detect when a register is "shrunk" to 0 bits wide
	 * using this, and respond appropriately in the UI.
	 * @return
	 */
	public boolean registerWidthIsZero(){
		return !display64BitLongMode && (bitsWideProtectedMode == 0);
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		int numBytes = getByteWidth();
		
		GC measure = new GC(this);
		measure.setFont(numberFont);
		int characterWidth = measure.getFontMetrics().getAverageCharWidth();
		measure.dispose();
		
		int width = characterWidth * 7 + 2 // label
				  + numBytes // spaces between hex digits
				  + (numBytes * 2 * characterWidth) // hex digit characters
				  + (numBytes - 1) / 4 * 3 // separators between DWORDS
				  + numberFontHeight + 5 // signed/unsigned button
				  + 13 * (characterWidth); // integer
		int height = (numberFontHeight + 4) * 2 + 4; // numbers + spaces + borders
		
		if (wHint != SWT.DEFAULT) width = wHint;
	    if (hHint != SWT.DEFAULT) height = hHint; 
	    
	    if(!getVisible()) {
	    	height = 0;
	    }
		
		return new Point(width, height);
	}
	
}
