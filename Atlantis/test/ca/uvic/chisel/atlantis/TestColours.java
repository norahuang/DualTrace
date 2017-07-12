package ca.uvic.chisel.atlantis;

import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;
import ca.uvic.chisel.bfv.ColourConstants;

import java.util.Collection;

import org.eclipse.jface.resource.ColorRegistry;

import static org.junit.Assert.*;
import org.junit.*;

public class TestColours {
	
	private AtlantisActivator plugin;
	
	@Before
	public void setup() {
		plugin = AtlantisActivator.getDefault();
		if (plugin == null) {
			throw new RuntimeException("Error: this test needs to be run as a JUnit plugin test");
		}
	}
	
	@Test
	public void testColourRegistry() {
		ColorRegistry registry = plugin.getColorRegistry();
		assertEquals("There should be 31 colours in the registry", 31, registry.getKeySet().size());
	}
	
	@Test
	public void testTooltipColourList() {
		Collection<String> colourList = ColourConstants.getTooltipColourList();
		assertTrue("Tooltip colour list should have 9 colour IDs", colourList.size() == 9);
		for (String colourID : colourList) {
			assertTrue("IDs in the tooltip colour list should be in the registry", plugin.getColorRegistry().hasValueFor(colourID));
		}
	}
	
	@Test
	public void testSyntaxHighlightingColours() {
		SyntaxHighlightingPreference[] preferences = SyntaxHighlightingPreference.values();
		for (int i = 0; i < preferences.length; i++) {
			assertTrue("Syntax highlighting colour IDs should be in the registry", 
					plugin.getColorRegistry().hasValueFor(preferences[i].getColourID()));
		}
	}
	
	@Test
	public void testTraceVisualizationColours() {
		Collection<String> colourList = AtlantisColourConstants.getTraceVisualizationThreadColours();
		for (String colourID : colourList) {
			assertTrue("Trace visualization colours should be in the registry", plugin.getColorRegistry().hasValueFor(colourID));
		}
	}
	
	@After
	public void tearDown() {
		plugin = null;
	}
}
