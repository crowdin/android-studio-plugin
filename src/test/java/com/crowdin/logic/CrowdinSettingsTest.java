package com.crowdin.logic;

import com.crowdin.util.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.jdom.Element;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CrowdinSettingsTest {
    CrowdinSettings crowdinSettings = new CrowdinSettings();

    @ParameterizedTest
    @MethodSource
    public void testGetState(String expected) {
        Element element = crowdinSettings.getState();
        assertEquals(expected, element.getName());
    }

    public static Stream<Arguments> testGetState() {
        return Stream.of(
                arguments("CrowdinSettings"));
    }

    @ParameterizedTest
    @MethodSource
    public void testLoadState(String name) {
        assertDoesNotThrow(()->crowdinSettings.loadState(new Element(name)));
    }

    public static Stream<Arguments> testLoadState() {
        return Stream.of(
                arguments("CrowdinSettings"));
    }

    @ParameterizedTest
    @MethodSource
    public void testLoadStateWithSetAttribute(String name, String attribute, String value) {
        Element element = new Element(name);
        element.setAttribute(attribute, value);
        assertDoesNotThrow(()->crowdinSettings.loadState(element));
    }

    public static Stream<Arguments> testLoadStateWithSetAttribute() {
        return Stream.of(
                arguments("test", "DoNotShowConfirms", "true"));
    }

}
