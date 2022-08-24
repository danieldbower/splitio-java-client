package io.split.client.impressions;

import io.split.client.SplitClientConfig;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;

import io.split.storages.enums.OperationMode;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import pluggable.CustomStorageWrapper;

import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.split.client.impressions.ImpressionTestUtils.keyImpression;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by patricioe on 6/20/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class ImpressionsManagerImplTest {
    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Before
    public void setUp() {
        TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    }

    @Captor
    private ArgumentCaptor<List<TestImpressions>> impressionsCaptor;

    @Captor
    private ArgumentCaptor<HashMap<ImpressionCounter.Key, Integer>> impressionCountCaptor;

    @Test
    public void works() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(4)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(2, captured.size());
    }

    @Test
    public void worksButDropsImpressions() throws URISyntaxException {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(3)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 2L, null);
        KeyImpression ki3 = keyImpression("test3", "pato", "on", 3L, null);
        KeyImpression ki4 = keyImpression("test4", "pato", "on", 4L, null);

        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, null, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, null, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, null, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, null, null)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(3, captured.size());
        verify(TELEMETRY_STORAGE, times(1)).recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 1);
    }

    @Test
    public void works4ImpressionsInOneTest() throws URISyntaxException {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(1, captured.size());
        Assert.assertEquals(4, captured.get(0).keyImpressions.size());
        Assert.assertEquals(ki1, captured.get(0).keyImpressions.get(0));
        verify(TELEMETRY_STORAGE, times(4)).recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, 1);
    }

    @Test
    public void worksNoImpressions() throws URISyntaxException {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        // There are no impressions to post.

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock, never()).postImpressionsBulk(impressionsCaptor.capture());
    }

    @Test
    @Ignore // TODO: This test needs to be updated
    public void alreadySeenImpressionsAreMarked() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil2", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato2", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }

        // Do it again. Now they should all have a `seenAt` value
        Mockito.reset(senderMock);
        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        captured = impressionsCaptor.getValue();
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(Optional.of(keyImpression.time), Optional.of(keyImpression.previousTime));
            }
        }
    }

    @Test
    public void testImpressionsOptimizedMode() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(2, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));

        treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 2)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testCounterStandaloneMode() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);
        Assert.assertNotNull(manager.getCounter());
    }

    @Test
    public void testCounterConsumerMode() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, null, TELEMETRY_STORAGE, storage, storage, telemetrySynchronizer);
        Assert.assertNotNull(manager.getCounter());
    }
}