package com.crowdin.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class UtilTest {
    @ParameterizedTest
    @MethodSource
    public void testPrepareListMessageText(String mainText, List<String> items, String expected) {
        String result = Util.prepareListMessageText(mainText, items);
        assertEquals(expected, result);
    }


    public static Stream<Arguments> testPrepareListMessageText(){
        return Stream.of(
                arguments("mainText", Arrays.asList("values/strings.xml", "values/strings2.xml"), "<body><p>mainText</p><ul><li>values/strings.xml</li>\n" +
                        "<li>values/strings2.xml</li>\n" +
                        "</ul></body>")
        );
    }
}
