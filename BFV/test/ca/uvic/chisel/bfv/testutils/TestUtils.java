package ca.uvic.chisel.bfv.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;


public class TestUtils {

	/**
	 * This method asserts that the {@code actualIntervals} list contains all of the expectedIntervals, no other intervals, and no duplicates.
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public static <T> void assertContainsExactly(Collection<T> actualCollection, T ... expectedCollection) {
		for(T expectedItem : expectedCollection) {
			assertTrue(expectedItem.toString()+" Is not contained in " + actualCollection, actualCollection.contains(expectedItem));
		}
		
		assertEquals(expectedCollection.length, actualCollection.size());
	}
	
	public static <T> void assertContainsExactly(Collection<T> actualCollection, Collection<T> expectedCollection) {
		for(T expectedItem : expectedCollection) {
			assertTrue(expectedItem.toString()+" Is not contained in " + actualCollection, actualCollection.contains(expectedItem));
		}
		
		assertEquals(expectedCollection.size(), actualCollection.size());
	}
}
