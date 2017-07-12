package ca.uvic.chisel.bfv.annotations;

import ca.uvic.chisel.bfv.*;
import ca.uvic.chisel.bfv.annotations.DuplicateTagOccurrenceException;
import ca.uvic.chisel.bfv.annotations.Tag;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestTags {

	@Test
	public void testCreateTag() {
		// Normal case
		new Tag("A tag");
		
		// Invalid cases
		try {
			new Tag(null);
			fail("Should not allow null tag name");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Tag("");
			fail("Should not allow empty tag name");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Tag("  \t  \n");
			fail("Should not allow tag name with only whitespace");
		} catch (IllegalArgumentException e) {}
	}

	@Test
	public void testCreateValidTagOccurrence() {
		Tag tag = new Tag("Tag");
		new TagOccurrence(tag, 0, 1, 20, 35); // a multi-line tag
		new TagOccurrence(tag, 1, 1, 1, 1); // single char tags should be fine
		new TagOccurrence(tag, 0, 2, 0, 5); // single line tags should be fine
		new TagOccurrence(tag, 4, 29, 6, 7); // start char > end char is fine as long as the lines are different
	}
	
	@Test
	public void testCreateInvalidTagOccurrence() {
		Tag tag = new Tag("Tag");
		
		try {
			new TagOccurrence(null, 1, 1, 1, 1);
			fail("Should not allow null associated tag");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, -1, 1, 1, 1);
			fail("Should not allow negative start line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, 1, -1, 1, 1);
			fail("Should not allow negative start char");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, 1, 1, -1, 1);
			fail("Should not allow negative end line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, 1, 1, 1, -1);
			fail("Should not allow negative end char");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, 5, 1, 1, 1);
			fail("Should not allow start line > end line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new TagOccurrence(tag, 1, 14, 1, 1);
			fail("Should not allow start char > end char if on the same line");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testCompareTagOccurrences() {
		Tag tag = new Tag("Tag");
		TagOccurrence occurrence = new TagOccurrence(tag, 3, 4, 8, 12);
		
		assertTrue("Non-null occurrence should come before null occurrence", occurrence.compareTo(null) == -1);
		assertTrue("Should come before occurrence with greater start line", occurrence.compareTo(new TagOccurrence(tag, 7, 29, 14, 15)) == -1);
		assertTrue("Should come after occurrence with lesser start line", occurrence.compareTo(new TagOccurrence(tag, 1, 2, 19, 10)) == 1);
		assertTrue("Should come before occurrence with same start line but greater start char", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 22, 14, 7)) == -1);
		assertTrue("Should come after occurrence with same start line but lesser start char", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 0, 19, 18)) == 1);
		assertTrue("Should come before occurrence with same start but greater end line", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 4, 12, 1)) == -1);
		assertTrue("Should come after occurrence with same start but lesser end line", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 4, 6, 9)) == 1);
		assertTrue("Should come before occurrence with same start and same end line but greater end char", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 4, 8, 23)) == -1);
		assertTrue("Should come after occurrence with same start and same end line but lesser end char", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 4, 8, 11)) == 1);
		assertTrue("Should be considered equivalent if start and end locations are the same", 
				occurrence.compareTo(new TagOccurrence(tag, 3, 4, 8, 12)) == 0);
	}
	
	@Test
	public void testAddAndGetOccurrence() throws Exception {
		// Test adding occurrences
		Tag tag = new Tag("Tag");
		tag.addOccurrence(0, 1, 2, 3);
		tag.addOccurrence(1, 4, 9, 16);
		tag.addOccurrence(2, 3, 5, 7);
		tag.addOccurrence(1, 1, 2, 3);
		
		try {
			tag.addOccurrence(0, 1, 2, 3);
			fail("Adding another occurrence at the same location should not be allowed");
		} catch (DuplicateTagOccurrenceException e) {}
		
		// Test retrieving occurrences
		TagOccurrence first = tag.getOccurrenceAt(0, 1, 2, 3);
		TagOccurrence second = tag.getOccurrenceAt(1, 1, 2, 3);
		TagOccurrence third = tag.getOccurrenceAt(1, 4, 9, 16);
		TagOccurrence fourth = tag.getOccurrenceAt(2, 3, 5, 7);
		assertNotNull("Should be able to retrieve added occurrence by location", first);
		assertNotNull("Should be able to retrieve added occurrence by location", second);
		assertNotNull("Should be able to retrieve added occurrence by location", third);
		assertNotNull("Should be able to retrieve added occurrence by location", fourth);
		assertNull("Should return null if there is no occurrence at that location",tag.getOccurrenceAt(2, 4, 8, 16));
		
		// Test that the occurrences are sorted by location
		assertTrue(tag.getOccurrences().indexOf(first) == 0);
		assertTrue(tag.getOccurrences().indexOf(second) == 1);
		assertTrue(tag.getOccurrences().indexOf(third) == 2);
		assertTrue(tag.getOccurrences().indexOf(fourth) == 3);
		
		// Test adding an occurrence of a different tag but at the same location as an occurrence of another tag (should be allowed) 
		Tag other = new Tag("Different tag");
		other.addOccurrence(0, 1, 2, 3);
		
		assertTrue(tag.getOccurrences().size() == 4);
		assertTrue(other.getOccurrences().size() == 1);
	}
	
	@Test
	public void testGetStartAndEndLinesAndChars() throws Exception {
		Tag tag = new Tag("Tag");
		tag.addOccurrence(0, 1, 2, 3);
		TagOccurrence occurrence = tag.getOccurrenceAt(0, 1, 2, 3);
		
		assertTrue("Default getters should use 0-indexing",
				occurrence.getStartLine() == 0 && occurrence.getStartChar() == 1 && occurrence.getEndLine() == 2 && occurrence.getEndChar() == 3);
		assertTrue("Passing false to non-default getters should use 0-indexing",
				occurrence.getStartLine(false) == 0 && occurrence.getStartChar(false) == 1 && 
				occurrence.getEndLine(false) == 2 && occurrence.getEndChar(false) == 3);
		assertTrue("Passing true to non-default getters should use 1-indexing",
				occurrence.getStartLine(true) == 1 && occurrence.getStartChar(true) == 2 && 
				occurrence.getEndLine(true) == 3 && occurrence.getEndChar(true) == 4);
	}
	
	@Test
	public void testDeleteTagOccurrence() throws Exception {
		Tag tag = new Tag("Tag");
		tag.addOccurrence(1, 1, 2, 3);
		TagOccurrence occurrence = tag.getOccurrenceAt(1, 1, 2, 3);
		
		assertFalse("Deleting an occurrence not added to the tag should have no effect", tag.deleteOccurrence(new TagOccurrence(tag, 1, 4, 9, 16)));
		assertTrue("Deleting an occurrence not added to the tag should have no effect", tag.getOccurrences().size() == 1);
	
		assertTrue("Should delete occurrence", tag.deleteOccurrence(occurrence));
		assertTrue("Should delete occurrence", tag.getOccurrences().isEmpty());
	}
	
	@Test
	public void testShowStickyTooltip() throws Exception {
		Tag tag = new Tag("Tag");
		assertTrue("Tag's showStickyTooltip should default to true after construction", tag.getShowStickyTooltip());
		
		tag.addOccurrence(0, 1, 2, 3);
		TagOccurrence occurrenceA = tag.getOccurrenceAt(0, 1, 2, 3);
		assertTrue("New occurrence should inherit showStickyTooltip setting from tag", occurrenceA.getShowStickyTooltip());
		
		tag.addOccurrence(4, 5, 6, 7);
		TagOccurrence occurrenceB = tag.getOccurrenceAt(4, 5, 6, 7);
		occurrenceB.setShowStickyTooltip(false);
		assertTrue("Should set occurrence's showStickyTooltip to false without affecting the tag or other occurrences",
				!occurrenceB.getShowStickyTooltip() && tag.getShowStickyTooltip() && occurrenceA.getShowStickyTooltip());
		
		occurrenceA.setShowStickyTooltip(false);
		assertTrue("Tag's showStickyTooltip should still be true even though it's false for all of its occurrences",
				!occurrenceA.getShowStickyTooltip() && !occurrenceB.getShowStickyTooltip() && tag.getShowStickyTooltip());
		
		occurrenceA.setShowStickyTooltip(true);
		assertTrue("Should set occurrence's showStickyTooltip to true without affecting the tag or other occurrences",
				!occurrenceB.getShowStickyTooltip() && tag.getShowStickyTooltip() && occurrenceA.getShowStickyTooltip());
		
		tag.setShowStickyTooltip(false, false);
		assertTrue("Should set tag's showStickyTooltip without affecting any of its occurrences", 
				!tag.getShowStickyTooltip() && !occurrenceB.getShowStickyTooltip() && occurrenceA.getShowStickyTooltip());
		
		tag.addOccurrence(8, 9, 10, 11);
		TagOccurrence occurrenceC = tag.getOccurrenceAt(8, 9, 10, 11);
		assertFalse("New occurrence should inherit showStickyTooltip setting from tag", occurrenceC.getShowStickyTooltip());
		
		tag.setShowStickyTooltip(true, true);
		assertTrue("Should set showStickyTooltip for the tag and all of its occurrences", tag.getShowStickyTooltip() 
				&& occurrenceA.getShowStickyTooltip() && occurrenceB.getShowStickyTooltip() && occurrenceC.getShowStickyTooltip());
		
		tag.setShowStickyTooltip(false, true);
		occurrenceC.setShowStickyTooltip(false);
		assertTrue("All showStickyTooltip values should be false", !tag.getShowStickyTooltip()
				&& !occurrenceA.getShowStickyTooltip() && !occurrenceB.getShowStickyTooltip() && !occurrenceC.getShowStickyTooltip());
		
		occurrenceC.setShowStickyTooltip(true); 
		assertTrue("Should set occurrence's and the tag's showStickyTooltip to true without affecting other occurrences", tag.getShowStickyTooltip() 
				&& !occurrenceA.getShowStickyTooltip() && !occurrenceB.getShowStickyTooltip() && occurrenceC.getShowStickyTooltip());
	}
	
	@Test
	public void testSetColour() {
		BigFileActivator plugin = BigFileActivator.getDefault();
		if (plugin == null) {
			throw new RuntimeException("Error: this test needs to be run as a JUnit plugin test");
		}
		
		plugin.getColorRegistry(); // makes sure that the colour registry is ready for testing
		assertEquals("Default tag colour should be blue (update this test if default colour was deliberately changed)", 
				ColourConstants.TOOLTIP_BLUE, Tag.DEFAULT_COLOUR);
		
		Tag tag = new Tag("A tag");
		assertEquals("Tag should have default colour after construction", tag.getColour(), Tag.DEFAULT_COLOUR);
		
		tag.setColour(ColourConstants.TOOLTIP_PURPLE);
		assertEquals("Should have changed tag colour to purple", tag.getColour(), ColourConstants.TOOLTIP_PURPLE);
		
		tag.setColour("Invalid");
		assertEquals("Invalid colour should have changed tag colour to default", tag.getColour(), Tag.DEFAULT_COLOUR);
	}
}
