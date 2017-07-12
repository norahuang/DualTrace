package ca.uvic.chisel.bfv.annotations;

import java.util.*;
import static org.junit.Assert.*;
import org.junit.*;

import ca.uvic.chisel.bfv.annotations.RegionModel;

public class TestRegions {
	
	@Test
	public void testValidBounds() {
		new RegionModel("Region", 1, 10);
	}
	
	@Test
	public void testInvalidBounds()  {
		try {
			new RegionModel("Region", -1, 4);
			fail("Failed to detect negative start line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new RegionModel("Region", 4, -5);
			fail("Failed to detect negative end line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new RegionModel("Region", 3, 2);
			fail("Failed to detect end line > start line");
		} catch (IllegalArgumentException e) {}
		
		try {
			new RegionModel("Region", 6, 6);
			fail("Failed to detect start line == end line");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testSetName() {
		RegionModel region = new RegionModel("Region", 2, 5);
		String name = "Valid new name";
		region.setName(name);
		
		try {
			region.setName(null);
			fail("Should not accept null name");
		} catch (IllegalArgumentException e) {}
		
		try {
			region.setName("");
			fail("Should not accept empty name");
		} catch (IllegalArgumentException e) {}
		
		try {
			region.setName("\n  \t  ");
			fail("Should not accept name with only whitespace");
		} catch (IllegalArgumentException e) {}
		
		assertEquals("Region name should still be " + name, name, region.getName());
	}
	
	@Test
	public void testContainsLine() {
		RegionModel region = new RegionModel("Region", 2, 10);
		assertTrue("Start line should be in region", region.containsLine(2));
		assertTrue("End line should be in region", region.containsLine(10));
		assertTrue("Line between start and end should be in region", region.containsLine(5));
		assertFalse("Line before start should not be in region", region.containsLine(1));
		assertFalse("Line after end should not be in region", region.containsLine(11));
	}
	
	@Test
	public void testAddValidChild() throws InvalidRegionException {
		RegionModel parent = new RegionModel("Parent", 3, 12);
		RegionModel child = new RegionModel("Child", 6, 9);
		parent.addChild(child);
		RegionModel grandchild = new RegionModel("Grandchild", 7, 8);
		child.addChild(grandchild);
		RegionModel anotherChild = new RegionModel("Another child", 10, 11);
		parent.addChild(anotherChild);
		
		assertNull(parent.getParent());
		assertTrue(parent.getChildren().contains(child));
		assertTrue(parent.getChildren().contains(anotherChild));
		assertTrue(parent.getChildren().size() == 2);
		
		assertEquals(child.getParent(), parent);
		assertTrue(child.getChildren().contains(grandchild));
		assertTrue(child.getChildren().size() == 1);
		
		assertEquals(anotherChild.getParent(), parent);
		assertTrue(anotherChild.getChildren().isEmpty());
		
		assertEquals(grandchild.getParent(), child);
		assertTrue(grandchild.getChildren().isEmpty());
	}
	
	@Test
	public void testAddInvalidChild()  throws InvalidRegionException{
		RegionModel parent = new RegionModel("Parent", 4, 23);
		RegionModel child = new RegionModel("Child", 8, 19);
		parent.addChild(child);
		
		RegionModel r = new RegionModel("Region", 1, 2);
		try {
			parent.addChild(r);
			fail("Should not be able to add a region entirely before the parent as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 24, 29);
		try {
			parent.addChild(r);
			fail("Should not be able to add a region entirely after the parent as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 1, 6);
		try {
			parent.addChild(r);
			fail("Should not be able to add a region partially before the parent as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 0, 40);
		try {
			parent.addChild(r);
			fail("Should not be able to add a region that would contain the parent as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 21, 29);
		try {
			parent.addChild(r);
			fail("Should not be able to add a region partially after the parent as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 12, 16);
		try {
			parent.addChild(r);
			fail("Parent should not accept a region entirely within one of its existing children as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 6, 13);
		try {
			parent.addChild(r);
			fail("Parent should not accept a region partially within one of its existing children as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 18, 22);
		try {
			parent.addChild(r);
			fail("Parent should not accept a region partially within one of its existing children as a child");
		} catch (IllegalArgumentException e) {}
		
		r = new RegionModel("Region", 7, 20);
		try {
			parent.addChild(r);
			fail("Parent should not accept a region that completely overlaps with one of its existing children as a child");
		} catch (IllegalArgumentException e) {}
		
		assertNull(parent.getParent());
		assertTrue(parent.getChildren().contains(child));
		assertTrue(parent.getChildren().size() == 1);
		assertEquals(child.getParent(), parent);
		assertTrue(child.getChildren().isEmpty());
	}
	
	@Test
	public void testAddChildBoundaryCases() throws InvalidRegionException {
		// The child's end line is permitted to be the same as its parent's end line...
		RegionModel parent = new RegionModel("Parent", 3, 34);
		RegionModel child = new RegionModel("Child", 20, 34);
		parent.addChild(child);
		
		// ...but the child's start line must not be the same as its parent's start line
		RegionModel invalid = new RegionModel("Invalid child", 3, 10);
		try {
			parent.addChild(invalid);
			fail("Parent should not accept a region with the same start line as a child");
		} catch (IllegalArgumentException e) {}
		
		assertNull(parent.getParent());
		assertTrue(parent.getChildren().contains(child) && parent.getChildren().size() == 1);
		assertEquals(child.getParent(), parent);
		assertTrue(child.getChildren().isEmpty());
		assertNull(invalid.getParent());
		assertTrue(invalid.getChildren().isEmpty());
	}
	
	@Test
	public void testRemoveChild() throws InvalidRegionException {
		RegionModel parent = new RegionModel("Parent", 9, 16);
		RegionModel child = new RegionModel("Child", 12, 14);
		parent.addChild(child);
		
		parent.removeChild(13);
		assertTrue("Nothing should happen if no child starts at the specified line, even if that line is in a child", 
				parent.getChildren().contains(child) && parent.getChildren().size() == 1);
		assertEquals(child.getParent(), parent);
		
		parent.removeChild(12);
		assertTrue("Should remove child that starts at the specified line", parent.getChildren().isEmpty());
		assertNull(child.getParent());
	}
	
	@Test
	public void testGetEnclosingRegion() throws InvalidRegionException {
		RegionModel root = new RegionModel("Root", 0, 35);
		RegionModel parent = new RegionModel("Parent", 7, 24);
		root.addChild(parent);
		RegionModel child = new RegionModel("Child", 10, 14);
		parent.addChild(child);
		RegionModel sibling = new RegionModel("Sibling", 15, 19);
		parent.addChild(sibling);
		RegionModel other = new RegionModel("Other", 37, 50);
		
		List<RegionModel> regions = new ArrayList<RegionModel>();
		regions.add(root);
		regions.add(other);
		
		assertEquals("Line 1 should be in region " + root, RegionModel.getEnclosingRegion(regions, 1), root);
		assertEquals("Line 50 should be in region " + other, RegionModel.getEnclosingRegion(regions, 50), other);
		assertEquals("Line 7 should be in region " + parent, RegionModel.getEnclosingRegion(regions, 7), parent);
		assertEquals("Line 12 should be in region " + child, RegionModel.getEnclosingRegion(regions, 12), child);
		assertEquals("Line 18 should be in region " + sibling, RegionModel.getEnclosingRegion(regions, 18), sibling);
		assertNull("Line 60 should not be in any region", RegionModel.getEnclosingRegion(regions, 60));
	}
	
	@Test
	public void testGetStartOrEndLines() {
		RegionModel region = new RegionModel("Region", 0, 49);
		assertTrue("Default getter should use 0-indexing", region.getStartLine() == 0 && region.getEndLine() == 49);
		assertTrue("Passing 'false' to non-default getter should use 0-indexing", region.getStartLine(false) == 0 && region.getEndLine(false) == 49);
		assertTrue("Passing 'true' to non-default getter should use 1-indexing", region.getStartLine(true) == 1 && region.getEndLine(true) == 50);
	}
}
