package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LaunchDarklyFeatureSupportTest {

    private static final Path OUTPUT_FILE = Paths.get(".", "local-features.json");

    // object under test
    private LaunchDarklyFeaturesSupport support;

    @BeforeEach
    void setUp() throws IOException {
        Files.deleteIfExists(OUTPUT_FILE);
        support = spy(new LaunchDarklyFeaturesSupport(OUTPUT_FILE.toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(OUTPUT_FILE);
    }

    @Test
    void testClear() throws IOException {
        // given
        Files.writeString(OUTPUT_FILE, "{\"flagValues\":{\"key1\":true}}", StandardOpenOption.CREATE);
        assertTrue(Files.exists(OUTPUT_FILE));

        // when
        support.clear();

        // then
        assertTrue(Files.exists(OUTPUT_FILE));
        String fileContent = Files.readString(OUTPUT_FILE);
        assertEquals("{\"flagValues\":{}}", fileContent);
    }

    @Test
    void testConfigure_newFlag() throws IOException {
        // given
        Files.writeString(OUTPUT_FILE, "{\"flagValues\":{}}", StandardOpenOption.CREATE);
        assertTrue(Files.exists(OUTPUT_FILE));

        // when
        support.configure("key1", true);

        // then
        assertTrue(Files.exists(OUTPUT_FILE));
        verify(support).ensureFileExists();
        String fileContent = Files.readString(OUTPUT_FILE);
        assertEquals("{\"flagValues\":{\"key1\":true}}", fileContent);
    }

    @Test
    void testConfigure_existingFlag() throws IOException {
        // given
        Files.writeString(OUTPUT_FILE, "{\"flagValues\":{\"key1\":true}}", StandardOpenOption.CREATE);
        assertTrue(Files.exists(OUTPUT_FILE));

        // when
        support.configure("key1", false);

        // then
        assertTrue(Files.exists(OUTPUT_FILE));
        verify(support).ensureFileExists();
        String fileContent = Files.readString(OUTPUT_FILE);
        assertEquals("{\"flagValues\":{\"key1\":false}}", fileContent);
    }

    @Test
    void testDelete() throws IOException {
        // given
        Files.writeString(OUTPUT_FILE, "{\"flagValues\":{\"key1\":true}}", StandardOpenOption.CREATE);
        assertTrue(Files.exists(OUTPUT_FILE));

        // when
        support.remove("key1");

        // then
        assertTrue(Files.exists(OUTPUT_FILE));
        verify(support).ensureFileExists();
        String fileContent = Files.readString(OUTPUT_FILE);
        assertEquals("{\"flagValues\":{}}", fileContent);
    }

    @Test
    void testEnsureFileExists() throws IOException {
        // given
        assertFalse(Files.exists(OUTPUT_FILE));

        // when
        support.ensureFileExists();

        // then
        assertTrue(Files.exists(OUTPUT_FILE));
        String fileContent = Files.readString(OUTPUT_FILE);
        assertEquals("{\"flagValues\":{}}", fileContent);
    }

    @Test
    void testErrorReadingFile() throws IOException {
        // given
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        support.setObjectMapper(objectMapper);

        when(objectMapper.readValue(
                        anyString(), eq(LaunchDarklyFeaturesSupport.FlagValuesWrapper.class)))
                .thenThrow(JsonMappingException.class);

        // when
        try {
            support.read();
            fail("unchecked io exception should be thrown!");
        } catch (UncheckedIOException expected) {
            // expected
        }
    }

    @Test
    void testErrorWritingFile() throws IOException {
        // given
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        support.setObjectMapper(objectMapper);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        var wrapper = new LaunchDarklyFeaturesSupport.FlagValuesWrapper();

        // when
        try {
            support.write(wrapper);
            fail("unchecked io exception should be thrown!");
        } catch (UncheckedIOException expected) {
            // expected
        }
    }
}
