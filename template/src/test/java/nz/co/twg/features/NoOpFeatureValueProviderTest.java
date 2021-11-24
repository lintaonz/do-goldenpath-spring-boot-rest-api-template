package nz.co.twg.features;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

class NoOpFeatureValueProviderTest {

    @Test
    void testGetBoolean() {
        // given
        NoOpFeatureValueProvider provider = new NoOpFeatureValueProvider();
        boolean result;

        // when + then
        result = provider.getBoolean("key", "subject", true);
        assertTrue(result);

        // when + then
        result = provider.getBoolean("key", "subject", false);
        assertFalse(result);

        // when + then
        result = provider.getBoolean("key", null, true);
        assertTrue(result);

        // when + then
        result = provider.getBoolean(null, "subject", false);
        assertFalse(result);

        // when + then
        result = provider.getBoolean(null, null, true);
        assertTrue(result);
    }

    @Test
    void testGetAllBoolean() {
        // given
        NoOpFeatureValueProvider provider = new NoOpFeatureValueProvider();
        Map<String, Boolean> result;

        // when + then
        result = provider.getAllBoolean("subject");
        assertTrue(result.isEmpty());

        // when + then
        result = provider.getAllBoolean(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testOnChangeBoolean() {
        // given
        NoOpFeatureValueProvider provider = new NoOpFeatureValueProvider();
        Map<String, Boolean> result;
        BiConsumer<Boolean, Boolean> consumer = spy(BiConsumer.class);

        // when
        provider.onChangeBoolean("key", "subject", consumer);

        // then
        verify(consumer, never()).accept(anyBoolean(), anyBoolean());
    }
}
