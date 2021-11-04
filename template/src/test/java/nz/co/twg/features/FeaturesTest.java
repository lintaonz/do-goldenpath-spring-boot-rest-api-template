package nz.co.twg.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FeaturesTest {

    private enum TestFeatureFlags {
        FEATURE_1
    }

    @Mock private FeatureValueProvider featureValueProvider;

    @Mock private SubjectProvider subjectProvider;

    // object under test
    private Features features;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        this.features = new Features(featureValueProvider, subjectProvider);
    }

    @Test
    void testGetAll() {
        // given
        Map<String, Boolean> allBooleanFeatures = Collections.singletonMap("key1", true);
        String subject = "test";
        when(featureValueProvider.getAllBoolean(any())).thenReturn(allBooleanFeatures);
        when(subjectProvider.get()).thenReturn(subject);

        // when
        Map<String, Boolean> result = this.features.getAll();

        // then
        verify(subjectProvider).get();
        verify(featureValueProvider).getAllBoolean(subject);
        assertEquals(1, result.size());
        assertTrue(result.getOrDefault("key1", false));
    }

    @Test
    void testIsActive_String() {
        // given
        String featureKey = "key1";
        String subject = "test";
        when(featureValueProvider.getBoolean(featureKey, subject, false)).thenReturn(true);
        when(subjectProvider.get()).thenReturn(subject);

        // when
        boolean result = this.features.isActive(featureKey);

        // then
        verify(subjectProvider).get();
        verify(featureValueProvider).getBoolean(featureKey, subject, false);
        assertTrue(result);
    }

    @Test
    void testIsActive_Enum() {

        // given
        String subject = "test";
        when(featureValueProvider.getBoolean(TestFeatureFlags.FEATURE_1.name(), subject, false))
                .thenReturn(true);
        when(subjectProvider.get()).thenReturn(subject);

        // when
        boolean result = this.features.isActive(TestFeatureFlags.FEATURE_1);

        // then
        verify(subjectProvider).get();
        verify(featureValueProvider).getBoolean(TestFeatureFlags.FEATURE_1.name(), subject, false);
        assertTrue(result);
    }

    @Test
    void testisActiveWithDefault_String() {
        // given
        String featureKey = "key1";
        String subject = "test";
        boolean defaultValue = true;
        when(featureValueProvider.getBoolean(featureKey, subject, true)).thenReturn(true);
        when(subjectProvider.get()).thenReturn(subject);

        // when
        boolean result = this.features.isActive(featureKey, true);

        // then
        verify(subjectProvider).get();
        verify(featureValueProvider).getBoolean(featureKey, subject, true);
        assertTrue(result);
    }

    @Test
    void testisActiveWithDefault_Enum() {

        // given
        String subject = "test";
        when(featureValueProvider.getBoolean(TestFeatureFlags.FEATURE_1.name(), subject, true))
                .thenReturn(true);
        when(subjectProvider.get()).thenReturn(subject);

        // when
        boolean result = this.features.isActive(TestFeatureFlags.FEATURE_1, true);

        // then
        verify(subjectProvider).get();
        verify(featureValueProvider).getBoolean(TestFeatureFlags.FEATURE_1.name(), subject, true);
        assertTrue(result);
    }
}
