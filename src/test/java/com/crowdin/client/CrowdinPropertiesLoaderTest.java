package com.crowdin.client;

import org.junit.jupiter.api.Test;

import static com.crowdin.client.CrowdinPropertiesLoader.isBaseUrlValid;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrowdinPropertiesLoaderTest {

    @Test
    public void isBaseUrlValidTest() {
        assertTrue(isBaseUrlValid("https://myorganization.crowdin.com/"));
        assertTrue(isBaseUrlValid("https://crowdin.com"));
        assertTrue(isBaseUrlValid("http://test.dev.crowdin.com"));
        assertTrue(isBaseUrlValid("http://my-organization.test.dev.crowdin.com"));
        assertTrue(isBaseUrlValid("https://ti-it.crowdin.com"));
        assertFalse(isBaseUrlValid("http://my-organization.testdev.crowdin.com"));
        assertFalse(isBaseUrlValid("http://crowdin.com"));
        assertFalse(isBaseUrlValid("http://myorganization.crowdin.com"));
    }
}
