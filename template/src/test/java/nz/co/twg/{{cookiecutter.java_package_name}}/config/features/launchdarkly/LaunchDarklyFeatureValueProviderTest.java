package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.FeatureFlagsState;
import com.launchdarkly.sdk.server.LDClient;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LaunchDarklyFeatureValueProviderTest {

    @Mock private LDClient ldClient;

    // object under test
    private LaunchDarklyFeatureValueProvider provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.provider = new LaunchDarklyFeatureValueProvider(ldClient);
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
}
