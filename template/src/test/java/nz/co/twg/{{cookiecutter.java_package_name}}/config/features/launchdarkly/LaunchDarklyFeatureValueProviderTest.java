package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.google.common.util.concurrent.Uninterruptibles;
import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.FeatureFlagsState;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.FileData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LaunchDarklyFeatureValueProviderTest {

    private final String TEST_FEATURE_FLAGS_FILE = "./test-feature-flags.json";

    @Mock private LDClient ldClient;

    // object under test
    private LaunchDarklyFeatureValueProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        this.provider = new LaunchDarklyFeatureValueProvider(ldClient);
        Files.writeString(Paths.get(TEST_FEATURE_FLAGS_FILE), "{\"flagValues\":{}}");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_FEATURE_FLAGS_FILE));
    }

    @Test
    void testGetAllBoolean_intFlags() {
        // given
        LDValue ldValue = mock(LDValue.class);
        when(ldValue.isInt()).thenReturn(true);
        when(ldValue.isString()).thenReturn(false);
        when(ldValue.isNull()).thenReturn(false);
        when(ldValue.isNumber()).thenReturn(false);

        FeatureFlagsState flagsState = mock(FeatureFlagsState.class);
        when(flagsState.toValuesMap()).thenReturn(Collections.singletonMap("key1", ldValue));

        LDUser ldUser = new LDUser("test");
        when(ldClient.allFlagsState(ldUser)).thenReturn(flagsState);

        // when
        Map<String, Boolean> result = provider.getAllBoolean("test");

        // then
        verify(ldClient).allFlagsState(ldUser);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllBoolean_stringFlags() {
        // given
        LDValue ldValue = mock(LDValue.class);
        when(ldValue.isInt()).thenReturn(false);
        when(ldValue.isString()).thenReturn(true);
        when(ldValue.isNull()).thenReturn(false);
        when(ldValue.isNumber()).thenReturn(false);

        FeatureFlagsState flagsState = mock(FeatureFlagsState.class);
        when(flagsState.toValuesMap()).thenReturn(Collections.singletonMap("key1", ldValue));

        LDUser ldUser = new LDUser("test");
        when(ldClient.allFlagsState(ldUser)).thenReturn(flagsState);

        // when
        Map<String, Boolean> result = provider.getAllBoolean("test");

        // then
        verify(ldClient).allFlagsState(ldUser);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllBoolean_nullFlags() {
        // given
        LDValue ldValue = mock(LDValue.class);
        when(ldValue.isInt()).thenReturn(false);
        when(ldValue.isString()).thenReturn(false);
        when(ldValue.isNull()).thenReturn(true);
        when(ldValue.isNumber()).thenReturn(false);

        FeatureFlagsState flagsState = mock(FeatureFlagsState.class);
        when(flagsState.toValuesMap()).thenReturn(Collections.singletonMap("key1", ldValue));

        LDUser ldUser = new LDUser("test");
        when(ldClient.allFlagsState(ldUser)).thenReturn(flagsState);

        // when
        Map<String, Boolean> result = provider.getAllBoolean("test");

        // then
        verify(ldClient).allFlagsState(ldUser);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllBoolean_numberFlags() {
        // given
        LDValue ldValue = mock(LDValue.class);
        when(ldValue.isInt()).thenReturn(false);
        when(ldValue.isString()).thenReturn(false);
        when(ldValue.isNull()).thenReturn(false);
        when(ldValue.isNumber()).thenReturn(true);

        FeatureFlagsState flagsState = mock(FeatureFlagsState.class);
        when(flagsState.toValuesMap()).thenReturn(Collections.singletonMap("key1", ldValue));

        LDUser ldUser = new LDUser("test");
        when(ldClient.allFlagsState(ldUser)).thenReturn(flagsState);

        // when
        Map<String, Boolean> result = provider.getAllBoolean("test");

        // then
        verify(ldClient).allFlagsState(ldUser);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllBoolean_booleanFlags() {
        // given
        LDValue ldValue = mock(LDValue.class);
        when(ldValue.isInt()).thenReturn(false);
        when(ldValue.isString()).thenReturn(false);
        when(ldValue.isNull()).thenReturn(false);
        when(ldValue.isNumber()).thenReturn(false);
        when(ldValue.booleanValue()).thenReturn(true);

        FeatureFlagsState flagsState = mock(FeatureFlagsState.class);
        when(flagsState.toValuesMap()).thenReturn(Collections.singletonMap("key1", ldValue));

        LDUser ldUser = new LDUser("test");
        when(ldClient.allFlagsState(ldUser)).thenReturn(flagsState);

        // when
        Map<String, Boolean> result = provider.getAllBoolean("test");

        // then
        verify(ldClient).allFlagsState(ldUser);
        assertEquals(1, result.size());
        Boolean value = result.get("key1");
        assertTrue(value);
    }

    @Test
    void testGetBoolean() {
        // given
        LDUser ldUser = new LDUser("test");
        when(ldClient.boolVariation("key1", ldUser, false)).thenReturn(true);

        // when
        boolean result = provider.getBoolean("key1", "test", false);

        // then
        assertTrue(result);
    }

    @Test
    void testOnChangeBoolean() throws IOException {
        // given
        Files.writeString(Paths.get(TEST_FEATURE_FLAGS_FILE), "{\"flagValues\":{\"key1\":false}}");
        LDClient ldClient = ldClient();
        this.provider = new LaunchDarklyFeatureValueProvider(ldClient);
        BiConsumer<Boolean, Boolean> consumer = spy(BiConsumer.class);

        // when
        provider.onChangeBoolean("key1", "test", consumer);
        Files.writeString(Paths.get(TEST_FEATURE_FLAGS_FILE), "{\"flagValues\":{\"key1\":true}}");
        // delay for 1-second to allow launchdarkly time to re-evaluate latest state from file
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        // then
        verify(consumer).accept(false, true);
    }

    @Test
    void testOnChangeBoolean_multiple() throws IOException {
        // given
        Files.writeString(Paths.get(TEST_FEATURE_FLAGS_FILE), "{\"flagValues\":{\"key1\":false}}");
        LDClient ldClient = ldClient();
        this.provider = new LaunchDarklyFeatureValueProvider(ldClient);
        BiConsumer<Boolean, Boolean> consumer1 = spy(BiConsumer.class);
        BiConsumer<Boolean, Boolean> consumer2 = spy(BiConsumer.class);

        // when
        provider.onChangeBoolean("key1", "test", consumer1);
        provider.onChangeBoolean("key1", "test", consumer2);
        Files.writeString(Paths.get(TEST_FEATURE_FLAGS_FILE), "{\"flagValues\":{\"key1\":true}}");
        // delay for 1-second to allow launchdarkly time to re-evaluate latest state from file
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        // then
        verify(consumer1).accept(false, true);
        verify(consumer2).accept(false, true);
    }

    /** LD client that reads from file */
    private LDClient ldClient() {
        LDConfig.Builder configBuilder =
                new LDConfig.Builder()
                        .dataSource(FileData.dataSource().filePaths(TEST_FEATURE_FLAGS_FILE).autoUpdate(true))
                        .events(Components.noEvents())
                        .diagnosticOptOut(true);
        return new LDClient("test", configBuilder.build());
    }
}
