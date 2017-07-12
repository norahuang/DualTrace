package ca.uvic.chisel.bfv.annotations;

import ca.uvic.chisel.bfv.*;
import ca.uvic.chisel.bfv.annotations.Comment;
import ca.uvic.chisel.bfv.annotations.CommentGroup;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestComments {

	@Test
	public void testCreateCommentGroup() {
		// Normal case
		new CommentGroup("A group");
		
		// "No group" case
		new CommentGroup(CommentGroup.NO_GROUP);
		
		// Invalid cases
		try {
			new CommentGroup(null);
			fail("Should not allow null comment group name");
		} catch (IllegalArgumentException e) {}
		
		try {
			new CommentGroup("");
			fail("Should not allow empty comment group name");
		} catch (IllegalArgumentException e) {}
		
		try {
			new CommentGroup("  \t\n   ");
			fail("Should not allow comment group name that only contains whitespace");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testCreateComment() {
		CommentGroup group = new CommentGroup("A comment group");
		
		// Valid cases
		new Comment(group, 0, 0, "Hello world!");
		new Comment(group, 4, 2, "World, hello!");
		new Comment(group, 1, 1, null); // null text should be fine
		new Comment(group, 2, 3, ""); // empty text should be fine
		new Comment(group, 6, 5, "    "); // whitespace only text should be fine
		
		// Invalid cases
		try {
			new Comment(null, 1, 1, "This shouldn't work");
			fail("Should not accept null group");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Comment(group, -1, 1, "This shouldn't work");
			fail("Should not accept negative line number");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Comment(group, 1, -1, "This shouldn't work");
			fail("Should not accept negative char number");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testCompareCommentLocations() {
		CommentGroup group = new CommentGroup("Group");
		Comment comment = new Comment(group, 10, 12, "A comment");
		
		assertTrue("Non-null comment should come before null comment", comment.compareTo(null) == -1);
		assertTrue("Should come before comment with greater line", comment.compareTo(new Comment(group, 20, 7, null)) == -1);
		assertTrue("Should come after comment with lesser line", comment.compareTo(new Comment(group, 2, 17, null)) == 1);
		assertTrue("Should come before comment with same line but greater char", comment.compareTo(new Comment(group, 10, 14, "")) == -1);
		assertTrue("Should come after comment with same line but lesser char", comment.compareTo(new Comment(group, 10, 1, "foo")) == 1);
		assertTrue("Location should be considered equivalent if line and char are the same", comment.compareTo(new Comment(group, 10, 12, "bar")) == 0);
	}
	
	@Test
	public void testAddAndGetComment() throws Exception {
		CommentGroup group = new CommentGroup("Group");
		group.addComment(4, 9, "Comment #2");
		group.addComment(0, 3, "Comment #1");
		group.addComment(9, 14, "Comment #4");
		group.addComment(4, 20, "Comment #3");
		
		try {
			group.addComment(4, 9, "This shouldn't work");
			fail("Adding a comment at the same location as another one in the same group isn't allowed");
		} catch (InvalidCommentLocationException e) {}
		
		// Test retrieving comments
		Comment first = group.getCommentAt(0, 3);
		Comment second = group.getCommentAt(4, 9);
		Comment third = group.getCommentAt(4, 20);
		Comment fourth = group.getCommentAt(9, 14);
		assertNotNull("Should be able to retrieve comment by location", first);
		assertNotNull("Should be able to retrieve comment by location", second);
		assertNotNull("Should be able to retrieve comment by location", third);
		assertNotNull("Should be able to retrieve comment by location", fourth);
		assertNull("Should be null if there is no comment at that location", group.getCommentAt(7, 11));
		
		// Test that comments are sorted by location
		assertTrue(group.getComments().indexOf(first) == 0);
		assertTrue(group.getComments().indexOf(second) == 1);
		assertTrue(group.getComments().indexOf(third) == 2);
		assertTrue(group.getComments().indexOf(fourth) == 3);
		
		// Test adding a comment to a different group but at the same location as a comment in another group (should be allowed) 
		CommentGroup other = new CommentGroup("A different group");
		other.addComment(4, 9, "This should be OK");
		
		assertTrue(group.getComments().size() == 4);
		assertTrue(other.getComments().size() == 1);
	}
	
	@Test
	public void testGetLineAndChar() {
		CommentGroup group = new CommentGroup("Group");
		Comment comment = new Comment(group, 0, 13, "A comment");
		
		assertTrue("Default getters should use 0-indexing", comment.getLine() == 0 && comment.getCharacter() == 13);
		assertTrue("Passing false to non-default getters should use 0-indexing", comment.getLine(false) == 0 && comment.getCharacter(false) == 13);
		assertTrue("Passing true to non-default getters should use 1-indexing", comment.getLine(true) == 1 && comment.getCharacter(true) == 14);
	}
	
	@Test
	public void testSetCommentGroupName() {
		CommentGroup group = new CommentGroup("Group");
		String name = "Valid new name";
		group.setName(name);
		
		try {
			group.setName(null);
			fail("Should not accept null name");
		} catch (IllegalArgumentException e) {}
		
		try {
			group.setName("");
			fail("Should not accept empty name");
		} catch (IllegalArgumentException e) {}
		
		try {
			group.setName("\t  \n");
			fail("Should not accept name with only whitespace");
		} catch (IllegalArgumentException e) {}
		
		assertEquals("Comment group name should still be " + name, name, group.getName());
	}
	
	@Test
	public void testChangeCommentGroup() throws Exception {
		CommentGroup groupA = new CommentGroup("Group A");
		groupA.addComment(8, 10, "A comment");
		Comment comment = groupA.getCommentAt(8, 10);
		assertEquals(comment.getCommentGroup(), groupA);
		
		comment.moveToGroup(groupA);
		assertEquals("Moving a comment to the group that it's already in should have no effect", comment.getCommentGroup(), groupA);
		assertTrue("Moving a comment to the group that it's already in should have no effect", groupA.getComments().size() == 1);
		
		CommentGroup groupB = new CommentGroup("Group B");
		comment.moveToGroup(groupB);
		assertEquals("Should have moved comment to different group", comment.getCommentGroup(), groupB);
		assertTrue("Should have moved comment to different group", groupB.getComments().size() == 1);
		assertTrue("Should have moved comment to different group", groupA.getComments().isEmpty());
	}
	
	@Test
	public void testMoveComment() throws Exception {
		CommentGroup group = new CommentGroup("Group");
		group.addComment(19, 63, "Welcome to UVic!");
		group.addComment(20, 12, "Hello world!");
		Comment comment = group.getCommentAt(20, 12);
		Comment other = group.getCommentAt(19, 63);
		
		try {
			comment.move(-1, 1);
			fail("Should not accept negative line number");
		} catch (IllegalArgumentException e) {}
		
		try {
			comment.move(1, -1);
			fail("Should not accept negative char number");
		} catch (IllegalArgumentException e) {}
		
		assertNotNull(group.getCommentAt(19, 63));
		assertNotNull(group.getCommentAt(20, 12));
		assertTrue(group.getComments().indexOf(comment) == 1);
		assertTrue(group.getComments().indexOf(other) == 0);
		
		comment.move(20, 12);
		assertTrue("Moving a comment to the same location should have no effect", comment.getLine() == 20 && comment.getCharacter() == 12);
		assertNotNull("Moving a comment to the same location should have no effect", group.getCommentAt(20, 12));
		
		comment.move(0, 0);
		assertTrue("Should have moved comment", comment.getLine() == 0 && comment.getCharacter() == 0);
		assertNull("Comment group should no longer have a comment at the old location", group.getCommentAt(20, 12));
		assertNotNull("Comment group should have a comment at the new location", group.getCommentAt(0, 0));
		assertTrue("Comments should be sorted by location after move", group.getComments().indexOf(comment) == 0);
		assertTrue("Comments should be sorted by location after move", group.getComments().indexOf(other) == 1);
		
		try {
			comment.move(19, 63);
			fail("Should not be able to move comment to a location where there is already another comment");
		} catch (InvalidCommentLocationException e) {}
		assertTrue("Move shouldn't have occurred", comment.getLine() == 0 && comment.getCharacter() == 0);
		assertNotNull("Move shouldn't have occurred", group.getCommentAt(0, 0));
	}
	
	@Test
	public void testDeleteComment() throws Exception {
		CommentGroup group = new CommentGroup("Group");
		group.addComment(5, 3, "Hello world!");
		Comment comment = group.getCommentAt(5, 3);
		
		assertFalse("Deleting a comment not added to the group should have no effect", group.deleteComment(new Comment(group, 9, 18, null)));
		assertTrue("Deleting a comment not added to the group should have no effect", group.getComments().size() == 1);
		
		assertTrue("Should delete comment", group.deleteComment(comment));
		assertTrue("Should delete comment", group.getComments().isEmpty());
	}
	
	@Test
	public void testShowStickyTooltip() throws Exception {
		CommentGroup group = new CommentGroup("Group");
		assertTrue("Comment group's showStickyTooltip should default to true after construction", group.getShowStickyTooltip());
		
		group.addComment(1, 2, "Comment A");
		Comment commentA = group.getCommentAt(1, 2);
		assertTrue("New comment should inherit showStickyTooltip setting from group", commentA.getShowStickyTooltip());
		
		group.addComment(3, 4, "Comment B");
		Comment commentB = group.getCommentAt(3, 4);
		commentB.setShowStickyTooltip(false);
		assertTrue("Should set comment's showStickyTooltip to false without affecting the group or other comments",
				!commentB.getShowStickyTooltip() && group.getShowStickyTooltip() && commentA.getShowStickyTooltip());
		
		commentA.setShowStickyTooltip(false);
		assertTrue("Group's showStickyTooltip should still be true even though it's false for all of its comments",
				!commentA.getShowStickyTooltip() && !commentB.getShowStickyTooltip() && group.getShowStickyTooltip());
		
		commentA.setShowStickyTooltip(true);
		assertTrue("Should set comment's showStickyTooltip to true without affecting the group or other comments",
				!commentB.getShowStickyTooltip() && group.getShowStickyTooltip() && commentA.getShowStickyTooltip());
		
		group.setShowStickyTooltip(false, false);
		assertTrue("Should set group's showStickyTooltip without affecting any of its comments", 
				!group.getShowStickyTooltip() && !commentB.getShowStickyTooltip() && commentA.getShowStickyTooltip());
		
		group.addComment(5, 6, "Comment A");
		Comment commentC = group.getCommentAt(5, 6);
		assertFalse("New comment should inherit showStickyTooltip setting from group", commentC.getShowStickyTooltip());
		
		group.setShowStickyTooltip(true, true);
		assertTrue("Should set showStickyTooltip for the group and all of its comments", group.getShowStickyTooltip() 
				&& commentA.getShowStickyTooltip() && commentB.getShowStickyTooltip() && commentC.getShowStickyTooltip());
		
		group.setShowStickyTooltip(false, true);
		commentC.setShowStickyTooltip(false);
		assertTrue("All showStickyTooltip values should be false", !group.getShowStickyTooltip()
				&& !commentA.getShowStickyTooltip() && !commentB.getShowStickyTooltip() && !commentC.getShowStickyTooltip());
		
		commentC.setShowStickyTooltip(true); 
		assertTrue("Should set comment's and the group's showStickyTooltip to true without affecting other comments", group.getShowStickyTooltip() 
				&& !commentA.getShowStickyTooltip() && !commentB.getShowStickyTooltip() && commentC.getShowStickyTooltip());
	}
	
	@Test
	public void testSetColour() {
		BigFileActivator plugin = BigFileActivator.getDefault();
		if (plugin == null) {
			throw new RuntimeException("Error: this test needs to be run as a JUnit plugin test");
		}
		
		plugin.getColorRegistry(); // makes sure that the colour registry is ready for testing
		assertEquals("Default comment group colour should be green (update this test if default colour was deliberately changed)", 
				ColourConstants.TOOLTIP_GREEN, CommentGroup.DEFAULT_COLOUR);
		
		CommentGroup group = new CommentGroup("A comment group");
		assertEquals("Comment group should have default colour after construction", group.getColour(), CommentGroup.DEFAULT_COLOUR);
		
		group.setColour(ColourConstants.TOOLTIP_PURPLE);
		assertEquals("Should have changed comment group colour to purple", group.getColour(), ColourConstants.TOOLTIP_PURPLE);
		
		group.setColour("Invalid");
		assertEquals("Invalid colour should have changed comment group colour to default", group.getColour(), CommentGroup.DEFAULT_COLOUR);
	}
}
