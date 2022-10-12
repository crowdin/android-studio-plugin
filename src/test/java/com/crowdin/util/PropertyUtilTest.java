package com.crowdin.util;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PropertyUtilTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @BeforeEach
    public void setup() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        super.tearDown();
    }

    @ParameterizedTest
    @MethodSource
    public void testGetSources(String project, String expected, String key) {
        myFixture.copyFileToProject(project);
        String result = PropertyUtil.getPropertyValue(key, myFixture.getProject());
        assertEquals(expected, result);
    }

    public static Stream<Arguments> testGetSources() {
        return Stream.of(
            arguments("values/strings2.xml",
                    "", null)
        );
    }

}
