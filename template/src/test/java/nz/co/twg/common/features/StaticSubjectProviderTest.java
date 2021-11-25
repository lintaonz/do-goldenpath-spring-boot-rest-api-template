package nz.co.twg.common.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class StaticSubjectProviderTest {

    @Test
    void testGet() {
        // given
        String subject = "test";
        StaticSubjectProvider provider = new StaticSubjectProvider(subject);

        // when
        String result = provider.get();

        // then
        assertEquals(subject, result);
    }

    @Test
    void testNullSubject() {
        // given
        String subject = null;

        // when + then
        try {
            new StaticSubjectProvider(subject);
            fail("static subject provider should not accept null subject");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
