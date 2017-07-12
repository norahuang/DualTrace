package ca.uvic.chisel.atlantis.functionparsing;

import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ModuleColorProvider {

	private Map<String, Color> lightColors;
	private Map<String, Color> darkColors;
	private Map<String, Color> faintColors;
	private boolean disposed;
	private Display display;
	
	public ModuleColorProvider(Display display) {
		this.lightColors = new Hashtable<String, Color>();
		this.darkColors = new Hashtable<String, Color>();
		this.faintColors = new Hashtable<String, Color>();
		this.disposed = false;
		this.display = display;
	}
	
	public void disposeAllColors() {
		this.checkDisposed();
		
		for(Color color : lightColors.values()) {
			color.dispose();
		}
		for(Color color : darkColors.values()) {
			color.dispose();
		}
		for(Color color : faintColors.values()) {
			color.dispose();
		}
		
		this.disposed = true;
	}
	
	public void addModuleAndGenerateColorsIfNotPresent(String module) {
		this.checkDisposed();
		
		if(!lightColors.containsKey(module)) {
			selectNewModuleColors(module);
		}
	}
	
	public Color getModuleLighterColor(String module) {
		this.checkDisposed();
		
		if(lightColors.containsKey(module)) {
			return lightColors.get(module);
		}
		
		return null;
	}
	
	public Color getModuleDarkerColor(String module) {
		this.checkDisposed();
		
		if(darkColors.containsKey(module)) {
			return darkColors.get(module);
		}
		
		return null;
	}
	
	public Color getModuleFaintColor(String module) {
		this.checkDisposed();
		
		if(faintColors.containsKey(module)) {
			return faintColors.get(module);
		}
		
		return null;
	}
	
	private void checkDisposed() {
		if(this.disposed) {
			throw new IllegalStateException("This ModuleColorProvider has had it's colors disposed.");
		}
	}
	
	private void selectNewModuleColors(String module) {
		// seed based on module name
		long seed = module.hashCode() + 0xA;
	    Random random = new Random(seed);
		
		// base mix off light blue
		Color mixBase = new Color(display, 0xCC, 0xFF, 0xFF);
		
		// find the first color such that is is far away from the others, and not too close to base
		Color light = generateRandomColor(mixBase, random);
		
		// safety counter
		int safety = 0;
		
		while (minimumDistanceFromCurrentColors(light) < 40.0 || euclidianDistanceBetween(light, mixBase) < 40.0) {
			light.dispose();
			light = generateRandomColor(mixBase, random);
			
			if(safety++ > 5000) {
				break;
			}
		}
		
		int darkRed = light.getRed() - 0xF < 0 ? 0 : light.getRed() - 0xF;
		int darkGreen = light.getGreen() - 0xF < 0 ? 0 : light.getGreen() - 0xF;
		int darkBlue = light.getBlue() - 0xF < 0 ? 0 : light.getBlue() - 0xF;
		
		Color dark = new Color(display, darkRed, darkGreen, darkBlue);
		
		mixBase.dispose();
		
		Color faint = new Color(display, 0xC0 + light.getRed() / 0x04, 0xC0 + light.getGreen() / 0x04, 0xC0 + light.getBlue() / 0x04);
	
		lightColors.put(module, light);
		darkColors.put(module, dark);
		faintColors.put(module, faint);
	}
	
	// handy method from
	// http://stackoverflow.com/questions/43044/algorithm-to-randomly-generate-an-aesthetically-pleasing-color-palette#answer-43235
	// uses mixing to constrain color variance
	// passing in a Random was added
	private Color generateRandomColor(Color mix, Random random) {
	    int red = random.nextInt(256);
	    int green = random.nextInt(256);
	    int blue = random.nextInt(256);

	    // mix the color
	    if (mix != null) {
	        red = (red + mix.getRed()) / 2;
	        green = (green + mix.getGreen()) / 2;
	        blue = (blue + mix.getBlue()) / 2;
	    }

	    Color color = new Color(display, red, green, blue);
	    return color;
	}
	
	private double minimumDistanceFromCurrentColors(Color color) {
		double minimumDistance = Double.MAX_VALUE;
		
		for(Color lightColor : lightColors.values()) {
			double leftDist = this.euclidianDistanceBetween(lightColor, color);
			
			if(leftDist < minimumDistance) {
				minimumDistance = leftDist;
			}
		}
		
		return minimumDistance;
	}
	
	private double euclidianDistanceBetween(Color color1, Color color2) {
		double diffR = color2.getRed() - color1.getRed();
		double diffG = color2.getGreen() - color1.getGreen();
		double diffB = color2.getBlue() - color1.getBlue();
		
		return Math.sqrt(diffR * diffR + diffG * diffG + diffB * diffB);
	}
}
