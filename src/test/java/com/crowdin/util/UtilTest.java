package com.crowdin.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class UtilTest {
    @ParameterizedTest
    @MethodSource
    public void testPrepareListMessageText(String mainText, List<String> items, String expected) {
        String result = Util.prepareListMessageText(mainText, items);
        Assertions.assertEquals(expected, result);
    }


    public static Stream<Arguments> testPrepareListMessageText() {
        return Stream.of(
                arguments("mainText", Arrays.asList("values/strings.xml", "values/strings2.xml"), "<body><p>mainText</p><ul><li>values/strings.xml</li>\n" +
                        "<li>values/strings2.xml</li>\n" +
                        "</ul></body>")
        );
    }

    @Test
    public void extractOrganizationTest() {
        Assertions.assertEquals("test", Util.extractOrganization("https://test.api.crowdin.com"));
        Assertions.assertEquals("test342", Util.extractOrganization("https://test342.crowdin.com"));
        Assertions.assertEquals("org-1", Util.extractOrganization("https://org-1.api.crowdin.com"));
        Assertions.assertEquals("org-test", Util.extractOrganization("https://org-test.api.crowdin.com"));
        Assertions.assertEquals("org-test", Util.extractOrganization("https://org-test.api.crowdin.com/"));
        // Dev environments use http:// and previously threw ArrayIndexOutOfBoundsException here.
        Assertions.assertEquals("org", Util.extractOrganization("http://org.dev.crowdin.com"));
        Assertions.assertEquals("my-org", Util.extractOrganization("http://my-org.test.dev.crowdin.com"));
        // No organization label / malformed input must not crash.
        Assertions.assertEquals("crowdin", Util.extractOrganization("https://crowdin.com"));
        Assertions.assertNull(Util.extractOrganization(null));
        Assertions.assertNull(Util.extractOrganization("not a url"));
    }
}
