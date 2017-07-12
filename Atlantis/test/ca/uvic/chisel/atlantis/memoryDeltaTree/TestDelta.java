package ca.uvic.chisel.atlantis.memoryDeltaTree;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import ca.uvic.chisel.atlantis.deltatree.Delta;
import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.models.MemoryReference.EventType;
import ca.uvic.chisel.atlantis.models.PostDBMemoryReferenceValue;
import ca.uvic.chisel.bfv.testutils.TestUtils;

// TODO when the time comes initialize the Delta with word size = 2 and addresses per bin = 4

public class TestDelta {

	private Delta underTest;
	
	@Before
	public void init() {
		underTest = new Delta();
	}
	
	@Test
	public void testBasicMemoryReference() {
		underTest.addReference(createMemRef(0, "00000001", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "00000001", 0));
	}
	
	@Test
	public void testMultipleMemoryReferences() {
		underTest.addReference(createMemRef(0, "00000001", 0));
		underTest.addReference(createMemRef(4, "00000002", 1));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "00000001", 0), createMemRef(4, "00000002", 1));
	}
	
	@Test
	public void testManyMemoryReferences() {
		for(int i=0; i<1000; i++) {
			String value = String.format("%08x", i);
			underTest.addReference(createMemRef(i * 4, value, i));
		}
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		Collection<MemoryReference> expectedResults = new ArrayList<>();
		for(int i=0; i<1000; i++) {
			String value = String.format("%08x", i);
			expectedResults.add(createMemRef(i * 4, value, i));
		}
		
		TestUtils.assertContainsExactly(references, expectedResults);
	}
	
	@Test
	public void testPartialMemoryReference() {
		underTest.addReference(createMemRef(0, "01", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "01??????", 0));
	}
	
	@Test
	public void testPartialMemoryOnSameAddress() {
		underTest.addReference(createMemRef(0, "01", 0));
		underTest.addReference(createMemRef(1, "02", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "0102????", 0));
	}
	
	@Test
	public void testManyPartialMemoryOnSameAddress() {
		underTest.addReference(createMemRef(0, "01", 0));
		underTest.addReference(createMemRef(1, "02", 0));
		underTest.addReference(createMemRef(2, "03", 0));
		underTest.addReference(createMemRef(3, "04", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "01020304", 0));
	}
	
	@Test
	public void testPartialMemoryOnSameAddressOverlap() {
		underTest.addReference(createMemRef(0, "0101", 0));
		underTest.addReference(createMemRef(1, "0202", 1));
		underTest.addReference(createMemRef(2, "0303", 2));
		underTest.addReference(createMemRef(3, "04", 3));
		
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "01020304", 3));
	}
	
	@Test
	public void testPartialMemoryOverBinBarrier() {
		underTest.addReference(createMemRef(2, "01020304", 0));
		underTest.addReference(createMemRef(3, "0506", 1));
		underTest.addReference(createMemRef(4, "0708", 1));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "????0105", 1), createMemRef(4, "0708????", 1));
	}
	
	@Test
	public void testPartialMemoryOverBinBarrierWithOverlap() {
		underTest.addReference(createMemRef(3, "0102", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "??????01", 0), createMemRef(4, "02??????", 0));
	}
	
	@Test
	public void testLargeEntry() {
		underTest.addReference(createMemRef(1, "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, 
				createMemRef(0, "??010203", 0),
				createMemRef(4, "04050607", 0),
				createMemRef(8, "08090a0b", 0),
				createMemRef(12, "0c0d0e0f", 0),
				createMemRef(16, "10111213", 0),
				createMemRef(20, "14151617", 0),
				createMemRef(24, "18191a1b", 0),
				createMemRef(28, "1c1d1e1f", 0));
	}
	
	@Test
	public void testLargeEntryOverlapping() {
		underTest.addReference(createMemRef(1, "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 0));
		underTest.addReference(createMemRef(9, "9998979695", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, 
				createMemRef(0, "??010203", 0),
				createMemRef(4, "04050607", 0),
				createMemRef(8, "08999897", 0),
				createMemRef(12, "96950e0f", 0),
				createMemRef(16, "10111213", 0),
				createMemRef(20, "14151617", 0),
				createMemRef(24, "18191a1b", 0),
				createMemRef(28, "1c1d1e1f", 0));
	}
	
	@Test
	public void testQuestionMarkInMerge() {
		underTest.addReference(createMemRef(0, "01??????", 0));
		underTest.addReference(createMemRef(0, "??02????", 1));
		
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "0102????", 1));
	}
	
	@Test
	public void testQuestionMarkInMergeWithOverlapOverBins() {
		underTest.addReference(createMemRef(0, "01??????", 0));
		underTest.addReference(createMemRef(0, "?02?????", 1));
		underTest.addReference(createMemRef(3, "030405??", 2));
		underTest.addReference(createMemRef(5, "??10????", 3));
		
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "002???03", 2), createMemRef(4, "040510??", 3));
	}
	
	@Test
	public void testDeltaWithWordSize4() {
		underTest = new Delta();
		
		underTest.addReference(createMemRef(0, "00000001", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "00000001", 0));
	}
	
	@Test
	public void testMultipleEntriesWordSize4() {
		underTest = new Delta();
		
		underTest.addReference(createMemRef(0, "00000001", 0));
		underTest.addReference(createMemRef(1, "00000002", 1));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "00000001", 0), createMemRef(1, "00000002", 1));
	}
	
	@Test
	public void testManyEntriesWordSize4() {
		underTest = new Delta();
		
		for(int i=0; i<1000; i++) {
			String value = String.format("%08x", i);
			underTest.addReference(createMemRef(i, value, i));
		}
		
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		Collection<MemoryReference> expectedResults = new ArrayList<>();
		for(int i=0; i<1000; i++) {
			String value = String.format("%08x", i);
			expectedResults.add(createMemRef(i, value, i));
		}
		
		TestUtils.assertContainsExactly(references, expectedResults);
	}
	
	@Test
	public void testPartialMemoryReferenceWordSize4() {
		underTest = new Delta();
		
		underTest.addReference(createMemRef(0, "01", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "01??????", 0));
	}
	
	@Test
	public void testOverBinBarrierWordSize4() {
		underTest = new Delta();
		underTest.addReference(createMemRef(0, "0102030405060708", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "01020304", 0), createMemRef(1, "05060708", 0));
	}
	
	@Test
	public void testLargeEntryOverlappingWordSize4() {
		
		underTest = new Delta();
		
		underTest.addReference(createMemRef(0, "??0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 0));
		underTest.addReference(createMemRef(2, "??9998979695", 0));
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, 
				createMemRef(0, "??010203", 0),
				createMemRef(1, "04050607", 0),
				createMemRef(2, "08999897", 0),
				createMemRef(3, "96950e0f", 0),
				createMemRef(4, "10111213", 0),
				createMemRef(5, "14151617", 0),
				createMemRef(6, "18191a1b", 0),
				createMemRef(7, "1c1d1e1f", 0));
	}
	
	@Test
	public void testQuestionMarkInMergeWordSize4() {
		underTest = new Delta();
		
		underTest.addReference(createMemRef(0, "01??????", 0));
		underTest.addReference(createMemRef(0, "??02????", 1));
		
		Collection<MemoryReference> references = underTest.getMemoryReferences();
		
		TestUtils.assertContainsExactly(references, createMemRef(0, "0102????", 1));
	}
	
	private MemoryReference createMemRef(long address, String value, int lineNumber) {
		PostDBMemoryReferenceValue memVal = new PostDBMemoryReferenceValue(address, value.length()*2L, value);
		return new MemoryReference(address, memVal, lineNumber, EventType.MEMORY, false);
	}
	
}
