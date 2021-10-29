package io.split.storages.memory;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SegmentCacheInMemoryImplTest extends TestCase {
    private static final String SEGMENT_NAME = "TestSegment";
    private static final String FAKE_SEGMENT_NAME = "FakeSegment";
    private static final long CHANGE_NUMBER = 123L;
    private static final String KEY = "KEYTEST";
    private static final long DEFAULT_CHANGE_NUMBER = -1L;

    @Test
    public void testUpdateSegment(){
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);

        assertEquals(1L, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }

    @Test
    public void testIsInSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>(), 1L);
        assertTrue(segmentCacheInMemory.isInSegment(SEGMENT_NAME, KEY));
    }
    @Test
    public void testIsInSegmentWithFakeSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>(), 1L);
        assertFalse(segmentCacheInMemory.isInSegment(FAKE_SEGMENT_NAME, KEY));
    }

    @Test
    public void testSetChangeNumber() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);
        segmentCacheInMemory.setChangeNumber(SEGMENT_NAME, CHANGE_NUMBER);
        assertEquals(CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }

    @Test
    public void testGetChangeNumberWithFakeSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);
        assertEquals(DEFAULT_CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(FAKE_SEGMENT_NAME));
    }

    @Test
    public void testClear() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);
        segmentCacheInMemory.setChangeNumber(SEGMENT_NAME, CHANGE_NUMBER);
        segmentCacheInMemory.clear();
        assertEquals(DEFAULT_CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }

    @Test
    public void testGetAll() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);
        segmentCacheInMemory.updateSegment(FAKE_SEGMENT_NAME,new ArrayList<>(), new ArrayList<>(), 1L);
        Assert.assertEquals(2, segmentCacheInMemory.getSegmentCount());
    }

    @Test
    public void testGetAllKeys() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,Stream.of("KEY1", "KEY2").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCacheInMemory.updateSegment(FAKE_SEGMENT_NAME,Stream.of("KEY3", "KEY2").collect(Collectors.toList()), new ArrayList<>(), 1L);
        Assert.assertEquals(4, segmentCacheInMemory.getKeyCount());
    }
}