package nz.co.twg.{{cookiecutter.java_package_name}}.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import nz.co.twg.features.Features;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SampleServiceTest {

    @Mock private Features features;

    private SampleService sampleService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        sampleService = new SampleService(features);
    }

    @Test
    public void testIsFeatureActive_true() {
        // given
        String key = "test";
        when(features.isActive(key)).thenReturn(true);

        // when
        boolean result = sampleService.isFeatureActive(key);

        // then
        assertTrue(result);
    }

    @Test
    public void testIsFeatureActive_false() {
        // given
        String key = "test";
        when(features.isActive(key)).thenReturn(false);

        // when
        boolean result = sampleService.isFeatureActive(key);

        // then
        assertFalse(result);
    }
}
